package com.getian.tool;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.getian.protocol.ProtocolService;

public class ReviewTurnExtensionTool implements Tool {
    private final ProtocolService protocolService;

    public ReviewTurnExtensionTool(ProtocolService protocolService) {
        this.protocolService = protocolService;
    }

    /**
     * {
     *   "name": "review_turn_extension",
     *   "description": "Approve or reject a teammate turn extension request.",
     *   "input_schema": {
     *     "type": "object",
     *     "properties": {
     *       "request_id": {"type": "string"},
     *       "approve": {"type": "boolean"},
     *       "additional_turns": {"type": "integer"},
     *       "feedback": {"type": "string"}
     *     },
     *     "required": ["request_id", "approve"]
     *   }
     * }
     */
    @Override
    public ToolDefinition getDefinition() {
        JSONObject properties = new JSONObject()
                .fluentPut("request_id", new JSONObject().fluentPut("type", "string"))
                .fluentPut("approve", new JSONObject().fluentPut("type", "boolean"))
                .fluentPut("additional_turns", new JSONObject().fluentPut("type", "integer"))
                .fluentPut("feedback", new JSONObject().fluentPut("type", "string"));
        JSONObject schema = new JSONObject()
                .fluentPut("type", "object")
                .fluentPut("properties", properties)
                .fluentPut("required", new JSONArray()
                        .fluentAdd("request_id").fluentAdd("approve"));
        return new ToolDefinition("review_turn_extension",
                "Approve or reject a teammate turn extension request.", schema);
    }

    @Override
    public ToolResult execute(JSONObject input) {
        String requestId = input == null ? "" : input.getString("request_id");
        if (requestId == null || requestId.isBlank()) {
            return new ToolResult("Error: request_id is required");
        }
        if (input == null || !input.containsKey("approve")) {
            return new ToolResult("Error: approve is required");
        }
        boolean approve = input.getBooleanValue("approve");
        int additionalTurns = input.containsKey("additional_turns")
                ? input.getIntValue("additional_turns") : 10;
        return new ToolResult(protocolService.reviewTurnExtension(
                requestId.trim(), approve, additionalTurns,
                input.getString("feedback")));
    }
}
