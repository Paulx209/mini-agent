package com.getian.core;

import com.alibaba.fastjson.JSONObject;
import com.getian.background.BackgroundDecider;
import com.getian.background.BackgroundTasks;
import com.getian.llm.LLMClient;
import com.getian.tool.Tool;
import com.getian.tool.ToolRegistry;
import com.getian.tool.ToolResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: sonicge
 * @CreateTime: 2026-07-20
 *
 * BackgroundAgentLoop : 专用agentLoop工具
 * 1.执行工具时显式判断是否需要后台执行，需要的话，开启daemon线程执行，且回填tool_result
 * 2.后台命令执行完毕，<task_notification></task_notification> 格式将内容注入到下一轮对话中
 */

public class BackgroundAgentLoop {
    private static final int DEFAULT_TURNS = 20;
    private final BackgroundTasks manager;
    private final LLMClient llmClient;
    private final ToolRegistry toolRegistry;
    private final AgentLoopListener listener;
    private final int maxTurns;

    public BackgroundAgentLoop(BackgroundTasks manager, LLMClient llmClient, ToolRegistry toolRegistry, AgentLoopListener listener) {
        this(manager, llmClient, toolRegistry, listener, DEFAULT_TURNS);
    }

    public BackgroundAgentLoop(BackgroundTasks manager, LLMClient llmClient, ToolRegistry toolRegistry, AgentLoopListener listener, int maxTurns) {
        this.manager = manager;
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.listener = listener;
        this.maxTurns = maxTurns;
    }

    public AssistantMessage run(String prompt) {
        List<Message> messageList = new ArrayList<>();
        messageList.add(Message.user(prompt));
        return run(messageList);
    }

    public AssistantMessage run(List<Message> messageList) {
        for (int i = 0; i < maxTurns; i++) {
            //0.每一轮调用llm之前 先调用上一轮完成任务的内容
            injectBackgroundNotifications(messageList);
            //1.调用llm chat
            AssistantMessage resp = llmClient.chat(messageList, toolRegistry.definitions());
            listener.onAssistantMessage(resp);
            //2.补充上下文
            List<ContentBlock> content = resp.getContent();
            messageList.add(Message.assistant(content));
            //3.调用工具
            List<ToolResultBlock> blocks = executeToolUses(resp);
            if (!"tool_use".equals(resp.getStopReason()) || blocks.isEmpty()) {
                listener.onStop(resp);
                return resp;
            }
            messageList.add(Message.toolResults(blocks));
        }
        throw new IllegalStateException("Background agent loop reached max turns: " + maxTurns);
    }

    private List<ToolResultBlock> executeToolUses(AssistantMessage resp) {
        List<ToolResultBlock> res = new ArrayList<>();
        List<ContentBlock> content = resp.getContent();
        for (ContentBlock block : content) {
            if (block instanceof ToolUseBlock) {
                ToolUseBlock toolUseBlock = (ToolUseBlock) block;
                listener.beforeToolUse(toolUseBlock);
                String toolName = toolUseBlock.getName();
                JSONObject input = toolUseBlock.getInput();
                boolean runBackGround = BackgroundDecider.isRunBackGround(toolName, input);

                ToolResult toolResult;
                if (runBackGround) {
                    String bgTaskId = manager.start(toolUseBlock, toolRegistry);
                    String command = toolUseBlock.getInput() != null ? toolUseBlock.getInput().getString("command") : "";
                    toolResult = new ToolResult(
                            "[Background task " + bgTaskId + " started] "
                                    + "Command: " + command + ". "
                                    + "Result will be available when complete.");
                } else {
                    toolResult = executeTool(toolUseBlock);
                }
                ToolResultBlock resultBlock = new ToolResultBlock(toolUseBlock.getId(), toolResult.getContent());
                res.add(resultBlock);
            }
        }
        return res;
    }

    private ToolResult executeTool(ToolUseBlock toolUseBlock) {
        String name = toolUseBlock.getName();
        Tool tool = toolRegistry.find(name);
        if (tool == null) {
            return new ToolResult("Unknown tool:" + toolUseBlock.getName());
        }
        return tool.execute(toolUseBlock.getInput());
    }

    private void injectBackgroundNotifications(List<Message> messageList) {
        List<String> notifications = manager.collectionNotifications();
        if (!notifications.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (String toolRes : notifications) {
                builder.append(toolRes).append("\n");
            }
            messageList.add(Message.user(builder.toString().trim()));
            System.out.println("  [inject] " + notifications.size()
                    + " background notification(s)");
        }
    }
}
