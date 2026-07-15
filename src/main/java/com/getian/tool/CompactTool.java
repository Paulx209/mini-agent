package com.getian.tool;

import com.alibaba.fastjson.JSONObject;

/**
 *@Author: sonicge
 *@CreateTime: 2026-07-15
 */

public class CompactTool implements Tool{
    /**
     * {
     *      "name": "compact",
     *      "description": "Summarize earlier conversation to free context space.",
     *      "input_schema": {
     *          "type": "object",
     *          "properties": {
     *              "focus": {"type": "string"}
     *           }
     *      }
     * }
     */
    @Override
    public ToolDefinition getDefinition() {
        String name = "compact";
        String description = "Summarize earlier conversation to free context space.";
        JSONObject inputSchema = new JSONObject()
                .fluentPut("type", "object")
                .fluentPut("properties", new JSONObject()
                        .fluentPut("focus", new JSONObject()
                                .fluentPut("type", "string")));
        return new ToolDefinition(name,description,inputSchema);
    }

    @Override
    public ToolResult execute(JSONObject input) {
        return new ToolResult("Compact should be handled by CompactingAgentLoop");
    }
}
