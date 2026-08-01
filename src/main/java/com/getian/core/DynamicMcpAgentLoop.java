package com.getian.core;

import com.getian.llm.AnthropicLLMClient;
import com.getian.mcp.McpToolPool;
import com.getian.tool.Tool;
import com.getian.tool.ToolRegistry;
import com.getian.tool.ToolResult;

import java.util.ArrayList;
import java.util.List;

/**
 *@Author: sonicge
 *@CreateTime: 2026-08-01
 * s16 专用动态工具池循环
 */

public class DynamicMcpAgentLoop {
    private static final int DEFAULT_MAX_TURNS_LIMIT = 20;
    private final AnthropicLLMClient llmClient;
    private final McpToolPool mcpToolPool;
    private final AgentLoopListener listener;
    private final int maxTurns;

    public DynamicMcpAgentLoop(AnthropicLLMClient llmClient, McpToolPool mcpToolPool) {
        this(llmClient, mcpToolPool, new AgentLoopListener() {
        }, DEFAULT_MAX_TURNS_LIMIT);
    }

    public DynamicMcpAgentLoop(AnthropicLLMClient llmClient, McpToolPool mcpToolPool, AgentLoopListener listener) {
        this(llmClient, mcpToolPool, listener, DEFAULT_MAX_TURNS_LIMIT);
    }

    public DynamicMcpAgentLoop(AnthropicLLMClient llmClient, McpToolPool mcpToolPool, AgentLoopListener listener, int maxTurns) {
        this.llmClient = llmClient;
        this.mcpToolPool = mcpToolPool;
        this.listener = listener;
        this.maxTurns = maxTurns;
    }

    public AssistantMessage run(String prompt) {
        List<Message> history = new ArrayList<>();
        history.add(Message.user(prompt));
        return this.run(history);
    }

    public AssistantMessage run(List<Message> history) {
        for (int i = 0; i < maxTurns; i++) {
            //重组工具池
            ToolRegistry toolRegistry = mcpToolPool.assemble();
            AssistantMessage resp = llmClient.chat(history, toolRegistry.definitions());
            listener.onAssistantMessage(resp);
            history.add(Message.assistant(resp.getContent()));

            //执行工具
            List<ToolResultBlock> toolResultBlocks = executeTools(resp, toolRegistry);
            if(!"tool_use".equals(resp.getStopReason()) || toolResultBlocks.isEmpty()){
                listener.onStop(resp);
                return resp;
            }
            history.add(Message.toolResults(toolResultBlocks));
        }
        throw new IllegalStateException("Dynamic MCP agent loop reached max turns: " + maxTurns);
    }

    private List<ToolResultBlock> executeTools(AssistantMessage resp, ToolRegistry toolRegistry) {
        List<ToolResultBlock> answer = new ArrayList<>();
        for (ContentBlock block : resp.getContent()) {
            if(block instanceof ToolUseBlock){
                ToolUseBlock toolUseBlock = (ToolUseBlock) block;
                listener.beforeToolUse(toolUseBlock);
                ToolResult toolResult = executeTool(toolUseBlock, toolRegistry);
                listener.afterToolUse(toolUseBlock,toolResult);
                answer.add(new ToolResultBlock(toolUseBlock.getId(),toolResult.getContent()));
            }
        }
        return answer;
    }

    private ToolResult executeTool(ToolUseBlock toolUse,ToolRegistry toolRegistry){
        Tool tool = toolRegistry.find(toolUse.getName());
        if (tool == null) {
            return new ToolResult("Unknown tool: " + toolUse.getName());
        }
        return tool.execute(toolUse.getInput());
    }


}
