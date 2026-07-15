package com.getian.core;

import com.alibaba.fastjson.JSONObject;
import com.getian.compact.CompactionPipeline;
import com.getian.compact.ToolResultStore;
import com.getian.llm.AnthropicLLMClient;
import com.getian.tool.Tool;
import com.getian.tool.ToolRegistry;
import com.getian.tool.ToolResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CompactingAgentLoop {
    private static final int DEFAULT_MAX_TURNS = 20;
    private final AnthropicLLMClient anthropicLLMClient;
    private final AgentLoopListener listener;
    private final ToolRegistry toolRegistry;
    private final CompactionPipeline compactionPipeline;
    private int maxTurns;

    public CompactingAgentLoop(AnthropicLLMClient client, ToolRegistry toolRegistry, CompactionPipeline compactionPipeline) {
        this(client, new AgentLoopListener() {
        }, toolRegistry, compactionPipeline, DEFAULT_MAX_TURNS);
    }

    public CompactingAgentLoop(AnthropicLLMClient client, AgentLoopListener listener, ToolRegistry toolRegistry, CompactionPipeline compactionPipeline) {
        this(client, listener, toolRegistry, compactionPipeline, DEFAULT_MAX_TURNS);
    }

    public CompactingAgentLoop(AnthropicLLMClient llmClient, AgentLoopListener agentLoopListener, ToolRegistry toolRegistry, CompactionPipeline compactionPipeline, int maxTurns) {
        this.anthropicLLMClient = llmClient;
        this.listener = agentLoopListener;
        this.toolRegistry = toolRegistry;
        this.compactionPipeline = compactionPipeline;
        this.maxTurns = maxTurns;
    }

    /**
     * 不要使用Collections.singletonList
     */
    public AssistantMessage run(String prompt) {
        List<Message> messageList = new ArrayList<>();
        messageList.add(Message.user(prompt));
        return run(messageList);
    }

    public AssistantMessage run(List<Message> messageList) {
        int reactiveRetries = 0;
        for (int i = 0; i < maxTurns; i++) {
            //1.调用之前先压缩一下
            compactionPipeline.beforeLLM(messageList);
            //2.chat调用
            AssistantMessage resp = null;
            try {
                resp = anthropicLLMClient.chat(messageList, toolRegistry.definitions());
            } catch (Exception e) {
                //3.message length too long  -> llm compact + recent messages
                if (isPromptTooLong(e) && reactiveRetries < 1) {
                    List<Message> messages = compactionPipeline.reactiveCompact(messageList);
                    messageList.clear();
                    messageList.addAll(messages);
                }
            }
            listener.onAssistantMessage(resp);
            //4.判断是否包括compact tool
            if (hasCompactTool(resp)) {
                handleCompactToolUse(messageList, resp);
                continue;
            }
            //5.不包含compact tool
            messageList.add(Message.assistant(resp.getContent()));
            List<ToolResultBlock> toolResults = executeToolUses(resp);
            if (!"tool_use".equals(resp.getStopReason()) || toolResults.isEmpty()) {
                listener.onStop(resp);
                return resp;
            }
            //toolResults加入Message
            messageList.add(Message.toolResults(toolResults));
        }
        throw new IllegalStateException("Compacting agent loop reached max turns: " + maxTurns);
    }

    private void handleCompactToolUse(List<Message> messages, AssistantMessage resp) {
        //第一步 找到compact tool_use 找到focus
        String focus = "manual compact";
        for (ContentBlock block : resp.getContent()) {
            if (block instanceof ToolUseBlock && "compact".equals(((ToolUseBlock) block).getName())) {
                ToolUseBlock toolUse = (ToolUseBlock) block;
                JSONObject inputSchema = toolUse.getInput();
                String focusContext = inputSchema.getString("focus");
                if (focusContext != null && !focusContext.isBlank()) {
                    focus = focusContext;
                }
            }
        }
        //第二步 调用compactHistory
        List<Message> summaryMessage = compactionPipeline.compactHistory(messages, focus);
        messages.clear();
        messages.addAll(summaryMessage);
        //第三步 将resp当前的content加入到messages中
        messages.add(Message.assistant(resp.getContent()));
        //第四步 开始执行resp中的其他tool
        List<ToolResultBlock> results = new ArrayList<>();
        for (ContentBlock block : resp.getContent()) {
            if (block instanceof ToolUseBlock) {
                ToolUseBlock toolUseBlock = (ToolUseBlock) block;
                String toolName = toolUseBlock.getName();
                listener.beforeToolUse(toolUseBlock);
                ToolResult result;
                if ("compact".equals(toolName)) {
                    result = new ToolResult("[Compacted. History summarized.]");
                } else {
                    result = executeToolUse(toolUseBlock);
                }
                listener.afterToolUse(toolUseBlock,result);
                results.add(new ToolResultBlock(toolUseBlock.getId(), result.getContent()));
            }
        }
        messages.add(Message.toolResults(results));
    }

    private List<ToolResultBlock> executeToolUses(AssistantMessage message) {
        List<ToolResultBlock> res = new ArrayList<>();
        for (ContentBlock block : message.getContent()) {
            if (block instanceof ToolUseBlock) {
                ToolUseBlock toolUseBlock = (ToolUseBlock) block;
                listener.beforeToolUse(toolUseBlock);
                ToolResult resp = executeToolUse(toolUseBlock);
                listener.afterToolUse(toolUseBlock, resp);
                res.add(new ToolResultBlock(toolUseBlock.getId(), resp.getContent()));
            }
        }
        return res;
    }

    private ToolResult executeToolUse(ToolUseBlock toolUse) {
        String name = toolUse.getName();
        Tool tool = toolRegistry.find(name);
        if (tool == null) {
            return new ToolResult("Unknown tool: " + toolUse.getName());
        }
        return tool.execute(toolUse.getInput());
    }

    public boolean isPromptTooLong(Exception e) {
        String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        return (message.contains("prompt") && message.contains("long"))
                || message.contains("prompt_is_too_long")
                || message.contains("context") && message.contains("long");
    }

    public boolean hasCompactTool(AssistantMessage message) {
        if (message == null || message.getContent().isEmpty()) {
            return false;
        }
        List<ContentBlock> content = message.getContent();
        for (ContentBlock block : content) {
            if (block instanceof ToolUseBlock && "compact".equals(((ToolUseBlock) block).getName())) {
                return true;
            }
        }
        return false;
    }


}
