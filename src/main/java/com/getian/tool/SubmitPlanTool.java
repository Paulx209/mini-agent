package com.getian.tool;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.getian.protocol.ProtocolService;

/**
 *@Author: sonicge
 *@CreateTime: 2026-07-28
 * subAgent -> mainAgent
 */

public class SubmitPlanTool implements Tool{
    private final  ProtocolService service;
    private final String agentName;

    public SubmitPlanTool(ProtocolService service,String agentName){
        this.service = service;
        this.agentName =agentName;
    }

    /**
     * {
     *   "name": "submit_plan",
     *   "description": "Submit a plan for Lead approval.",
     *   "input_schema": {
     *     "type": "object",
     *     "properties": {"plan": {"type": "string"}},
     *     "required": ["plan"]
     *   }
     * }
     */
    @Override
    public ToolDefinition getDefinition() {
        JSONObject inputSchema = new JSONObject()
                .fluentPut("type","object")
                .fluentPut("properties",new JSONObject()
                        .fluentPut("plan",new JSONObject()
                                .fluentPut("type","string")))
                .fluentPut("required",new JSONArray()
                        .fluentAdd("plan"));
        return new ToolDefinition("submit_plan","Submit a plan for Lead approval.",inputSchema);
    }

    @Override
    public ToolResult execute(JSONObject input) {
        String plan = input == null ? "" : input.getString("plan");
        if (plan == null || plan.isBlank()) {
            return new ToolResult("Error: plan is required");
        }
        return new ToolResult(service.submitPlan(agentName, plan.trim()));
    }
}
