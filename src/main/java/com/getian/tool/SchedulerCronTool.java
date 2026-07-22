package com.getian.tool;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.getian.cron.CronScheduler;

/**
  *@Author: sonicge
 * @CreateTime: 2026-07-22
 * 负责创建cron job Tool
 */

public class SchedulerCronTool implements  Tool{
    private final CronScheduler scheduler;

    public SchedulerCronTool(CronScheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * {
     * "name": "schedule_cron",
     * "description": "Schedule a cron job. cron is 5-field: min hour dom month dow.",
     * "input_schema": {
     *  "type": "object",
     *  "properties": {
     *      "cron": {"type": "string", "description": "5-field cron expression"},
     *      "prompt": {"type": "string", "description": "Message to inject when fired"},
     *      "recurring": {"type": "boolean", "description": "True=recurring, False=one-shot"},
     *      "durable": {"type": "boolean", "description": "True=persist to disk"}
     *  },
     * "required": ["cron", "prompt"]
     * }
     * }
     */
    @Override
    public ToolDefinition getDefinition() {
        JSONObject properties = new JSONObject()
                .fluentPut("cron", new JSONObject()
                        .fluentPut("type", "string")
                        .fluentPut("description", "5-field cron expression"))
                .fluentPut("prompt", new JSONObject()
                        .fluentPut("type", "string")
                        .fluentPut("description", "Message to inject when fired"))
                .fluentPut("recurring", new JSONObject()
                        .fluentPut("type", "boolean")
                        .fluentPut("description", "True=recurring, False=one-shot"))
                .fluentPut("durable", new JSONObject()
                        .fluentPut("type", "boolean")
                        .fluentPut("description", "True=persist to disk"));
        JSONObject schema = new JSONObject()
                .fluentPut("type", "object")
                .fluentPut("properties", properties)
                .fluentPut("required", new JSONArray().fluentAdd("cron").fluentAdd("prompt"));
        return new ToolDefinition("schedule_cron",
                "Schedule a cron job. cron is 5-field: min hour dom month dow.", schema);
    }

    @Override
    public ToolResult execute(JSONObject input) {
        try {
            String cron = input != null ? input.getString("cron") : null;
            String prompt = input != null ? input.getString("prompt") : null;
            if (cron == null || cron.isBlank() || prompt == null || prompt.isBlank()) {
                return new ToolResult("Error: cron or prompt is invalid");
            }
            boolean recurring = input.getBoolean("recurring") !=null && input.getBoolean("recurring");
            boolean durable = input.getBoolean("durable") !=null && input.getBoolean("durable");
            String res = scheduler.schedule(cron, prompt, recurring, durable);
            return new ToolResult(res);
        } catch (Exception e) {
            return new ToolResult("Error: " + e.getMessage());
        }
    }
}
