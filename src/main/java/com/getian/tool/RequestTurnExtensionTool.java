package com.getian.tool;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.getian.protocol.ProtocolService;

public class RequestTurnExtensionTool implements Tool {
    private final ProtocolService protocolService;
    private final String agentName;

    public RequestTurnExtensionTool(ProtocolService protocolService, String agentName) {
        this.protocolService = protocolService;
        this.agentName = agentName;
    }

    /**
     * {
     *   "name": "request_turn_extension",
     *   "description": "Request additional turns from the lead.",
     *   "input_schema": {
     *     "type": "object",
     *     "properties": {
     *       "task_id": {"type": "string", "description": "ID of the current task."},
     *       "progress": {"type": "string", "description": "Work completed so far."},
     *       "remaining_work": {"type": "string", "description": "Work that still needs to be completed."},
     *       "reason": {"type": "string", "description": "Why additional turns are required."}
     *     },
     *     "required": ["progress", "remaining_work", "reason"]
     *   }
     * }
     */
    @Override
    public ToolDefinition getDefinition() {
        JSONObject properties = new JSONObject()
                .fluentPut("task_id", stringProperty("ID of the current task."))
                .fluentPut("progress", stringProperty("Work completed so far."))
                .fluentPut("remaining_work", stringProperty("Work that still needs to be completed."))
                .fluentPut("reason", stringProperty("Why additional turns are required."));
        JSONObject schema = new JSONObject()
                .fluentPut("type", "object")
                .fluentPut("properties", properties)
                .fluentPut("required", new JSONArray()
                        .fluentAdd("progress")
                        .fluentAdd("remaining_work")
                        .fluentAdd("reason"));
        return new ToolDefinition("request_turn_extension",
                "Request additional turns from the lead.", schema);
    }

    @Override
    public ToolResult execute(JSONObject input) {
        if (input == null) {
            return new ToolResult("Error: input is required");
        }
        String progress = value(input, "progress");
        String remainingWork = value(input, "remaining_work");
        String reason = value(input, "reason");
        if (progress.isBlank() || remainingWork.isBlank() || reason.isBlank()) {
            return new ToolResult("Error: progress, remaining_work and reason are required");
        }
        String requestId = protocolService.requestTurnExtension(agentName,
                value(input, "task_id"), progress, remainingWork, reason);
        return new ToolResult("Turn extension requested (" + requestId + ")");
    }

    private JSONObject stringProperty(String description) {
        return new JSONObject().fluentPut("type", "string")
                .fluentPut("description", description);
    }

    private String value(JSONObject input, String key) {
        String value = input.getString(key);
        return value == null ? "" : value.trim();
    }
}
