package com.getian.tool;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.getian.core.*;
import com.getian.llm.LLMClient;


public class TaskTool implements Tool {
    private final LLMClient llmClient;
    private final ToolRegistry toolRegistry;

    public TaskTool(LLMClient llmClient, ToolRegistry toolRegistry) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
    }

    /**
     *   "name": "task",
     *   "description": "Launch a subagent for a complex subtask. Returns only the final summary.",
     *   "input_schema": {
     *     "type": "object",
     *     "properties": {
     *       "description": {"type": "string", "description": "Subtask for a fresh-context subagent"}
     *     },
     *     "required": ["description"]
     *   }
     * }
     */
    @Override
    public ToolDefinition getDefinition() {
        String name = "task";
        String description = "Launch a subagent for a complex subtask. Returns only the final summary.";
        JSONObject inputSchema = new JSONObject()
                .fluentPut("type", "object")
                .fluentPut("properties", new JSONObject()
                        .fluentPut("description", new JSONObject()
                                .fluentPut("type","string")
                                .fluentPut("description","Subtask for a fresh-context subagent")))
                .fluentPut("required", new JSONArray()
                        .fluentAdd("description"));
        return new ToolDefinition(name, description, inputSchema);
    }

    @Override
    public ToolResult execute(JSONObject input) {
        String description = input == null ? "" : input.getString("description");
        if (description == null || description.isBlank()) {
            return new ToolResult("Error: No description provided");
        }
        //开始创建子agent -> Agent_Loop
        System.out.println("[Subagent spawned]");
        AgentLoop subAgent = new AgentLoop(llmClient, toolRegistry, new AgentLoopListener() {
            @Override
            public void beforeToolUse(ToolUseBlock toolUseBlck) {
                System.out.println("  [sub] Tool> " + toolUseBlck.getName() + " " + toolUseBlck.getInput());
            }
        }, 30);

        AssistantMessage answer = subAgent.run(description);
        System.out.println("【SubAgent completed】");
        return new ToolResult(extractText(answer));
    }

    private String extractText(AssistantMessage answer) {
        StringBuilder sb = new StringBuilder();
        for (ContentBlock block : answer.getContent()) {
            if (block instanceof TextBlock) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(((TextBlock) block).getText());
            }
        }
        if (sb.length() == 0) {
            return "Subagent finished without text summary.";
        }
        return sb.toString();
    }
}
