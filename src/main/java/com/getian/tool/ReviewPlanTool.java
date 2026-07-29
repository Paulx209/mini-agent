package com.getian.tool;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.getian.protocol.ProtocolService;

/**
 *@Author: sonicge
 *@CreateTime: 2026-07-29
 */

public class ReviewPlanTool implements Tool{
    private final ProtocolService service;
    public ReviewPlanTool(ProtocolService service){
        this.service = service;
    }

    /**
     * {
     *   "name": "review_plan",
     *   "description": "Approve or reject a submitted plan by request_id.",
     *   "input_schema": {
     *     "type": "object",
     *     "properties": {
     *       "request_id": {"type": "string"},
     *       "approve": {"type": "boolean"},
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
                .fluentPut("feedback", new JSONObject().fluentPut("type", "string"));
        JSONObject schema = new JSONObject()
                .fluentPut("type", "object")
                .fluentPut("properties", properties)
                .fluentPut("required", new JSONArray()
                        .fluentAdd("request_id").fluentAdd("approve"));
        return new ToolDefinition("review_plan",
                "Approve or reject a submitted plan by request_id.", schema);
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
        String feedback = input.getString("feedback");
        return new ToolResult(service.reviewPlan(requestId.trim(), approve, feedback));
    }
}
