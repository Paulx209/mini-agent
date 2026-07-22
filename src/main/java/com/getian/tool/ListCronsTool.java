package com.getian.tool;

import com.alibaba.fastjson.JSONObject;
import com.getian.cron.CronJob;
import com.getian.cron.CronScheduler;

import java.util.List;

/**
 * @Author: sonicge
 * @CreateTime: 2026-07-22
 * 负责获取所有cron job 的tool
 */

public class ListCronsTool implements Tool {
    private final CronScheduler scheduler;

    public ListCronsTool(CronScheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * {
     *  "name": "list_crons",
     *  "description": "List all registered cron jobs.",
     *  "input_schema": {"type": "object", "properties": {}}
     * }
     */
    @Override
    public ToolDefinition getDefinition() {
        return new ToolDefinition("list_crons",
                "List all registered cron jobs.",
                new JSONObject()
                        .fluentPut("type", "object")
                        .fluentPut("properties", new JSONObject()));
    }

    @Override
    public ToolResult execute(JSONObject input) {
        List<CronJob> cronJobs = scheduler.list();
        if (cronJobs.isEmpty()) {
            return new ToolResult("No cron jobs. Use schedule_cron to add one.");
        }
        StringBuilder builder = new StringBuilder();
        for (CronJob job : cronJobs) {
            String tag = job.isRecurring() ? "recurring" : "one-shot";
            String dur = job.isDurable() ? "durable" : "session";
            builder.append("  ").append(job.getId()).append(": '").append(job.getCron())
                    .append("' → ").append(job.getPrompt())
                    .append(" [").append(tag).append(", ").append(dur).append("]\n");
        }
        return new ToolResult(builder.toString());
    }
}
