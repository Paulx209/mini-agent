package com.getian.core;

import com.getian.llm.LLMClient;
import com.getian.permission.PermissionDecision;
import com.getian.permission.PermissionManager;
import com.getian.tool.Tool;
import com.getian.tool.ToolDefinition;
import com.getian.tool.ToolRegistry;
import com.getian.tool.ToolResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AgentLoop {
    private final LLMClient llmClient;

    private final ToolRegistry toolRegistry;

    private final AgentLoopListener listener;

    private final PermissionManager permissionManager;

    public AgentLoop(LLMClient llmClient, List<Tool> tools) {
        this(llmClient, tools, new AgentLoopListener() {
        });
    }

    public AgentLoop(LLMClient llmClient, List<Tool> tools, AgentLoopListener listener) {
        this(llmClient,toolRegistry(tools),listener);
    }

    public static ToolRegistry toolRegistry(List<Tool> tools){
        ToolRegistry toolRegistry = new ToolRegistry();
        for(Tool tool:tools){
            toolRegistry.registry(tool);
        }
        return toolRegistry;
    }

    public AgentLoop(LLMClient llmClient,ToolRegistry toolRegistry,AgentLoopListener listener){
        this(llmClient,toolRegistry,listener,null);
    }

    public AgentLoop(LLMClient llmClient, ToolRegistry toolRegistry, AgentLoopListener listener, PermissionManager permissionManager){
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.listener = listener;
        this.permissionManager = permissionManager;

    }

    public AssistantMessage run(String prompt) {
        List<Message> messages = new ArrayList<>();
        messages.add(Message.user(prompt));
        return this.run(messages);
    }

    public AssistantMessage run(List<Message> messages) {
        for (int turnIndex = 0; turnIndex < 20; turnIndex++) {
            AssistantMessage response = llmClient.chat(messages, toolDefinitions());
            //hooks-1 trigger
            listener.onAssistantMessage(response);
            messages.add(Message.assistant(response.getContent()));

            //调用工具函数
            List<ToolResultBlock> toolResultBlocks = executeToolUse(response);
            if (!"tool_use".equals(response.getStopReason()) || toolResultBlocks.isEmpty()) {
                listener.onStop(response);
                return response;
            }
            messages.add(Message.toolResults(toolResultBlocks));
        }
        throw new IllegalStateException("Agent loop reached max turns");
    }

    /**
     * 遍历response中content所有的tool_use内容
     *
     * @param response
     * @return
     */
    private List<ToolResultBlock> executeToolUse(AssistantMessage response) {
        List<ToolResultBlock> results = new ArrayList<>();
        for (ContentBlock block : response.getContent()) {
            if ("tool_use".equals(block.getType())) {
                ToolUseBlock toolUseBlock = (ToolUseBlock) block;
                listener.beforeToolUse(toolUseBlock);

                //permission manager
                PermissionDecision decision = permissionManager.check(toolUseBlock);
                //被拒绝
                if(!decision.isAllowed()){
                    results.add(new ToolResultBlock(toolUseBlock.getId(),decision.getMessage()));
                    continue;
                }

                ToolResult res = executeTool(toolUseBlock);

                listener.afterToolUse(toolUseBlock, res);
                results.add(new ToolResultBlock(toolUseBlock.getId(), res.getContent()));
            }
        }
        return results;
    }

    private ToolResult executeTool(ToolUseBlock toolUseBlock) {
        Tool tool = toolRegistry.find(toolUseBlock.getName());
        if (tool == null) {
            return new ToolResult("Unknown tool :" + toolUseBlock.getName());
        }
        return tool.execute(toolUseBlock.getInput());
    }


    public List<ToolDefinition> toolDefinitions() {
        return toolRegistry.definitions();
    }

}
