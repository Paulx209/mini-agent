package com.getian.background;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.AdderSerializer;
import com.getian.core.ToolUseBlock;
import com.getian.tool.Tool;
import com.getian.tool.ToolRegistry;
import com.getian.tool.ToolResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 捋清楚：start接口对外暴露 -> toolUseBlock toolRegistry -> 创建任
 * @Author: sonicge
 * @CreateTime: 2026-07-20
 */

public class BackgroundTasks {
    private final String COMPLETED = "completed";
    private final String RUNNING = "running";
    private final String TIMEOUT = "timeout";
    private final String ERROR = "error";
    private final AtomicInteger counter = new AtomicInteger(0);
    private final ConcurrentHashMap<String, BackgroundTask> tasks = new ConcurrentHashMap<>();
    private final Map<String, String> results = new LinkedHashMap<>();

    public String start(ToolUseBlock block, ToolRegistry toolRegistry) {
        int index = counter.incrementAndGet();
        String bgId = String.format("bgId_%04d", index);
        String command = block.getInput() != null ? block.getInput().getString("command") : block.getName();
        if (command != null && command.length() > 80) {
            command = command.substring(0, 80);
        }

        BackgroundTask task = new BackgroundTask(
                bgId,
                block.getId(),
                command,
                RUNNING);
        tasks.put(bgId, task);

        Thread thread = new Thread(() -> {
            executeInBackground(bgId, block, toolRegistry);
        });
        thread.setDaemon(true);
        thread.setName("bgId-" + bgId);
        thread.start();

        System.out.println("  [background] dispatched " + bgId + ": " + command);
        return bgId;
    }

    public List<String> collectionNotifications() {
        List<String> notifications = new ArrayList<>();
        List<String> bgIds = new ArrayList<>();
        for (Map.Entry<String, BackgroundTask> entry : tasks.entrySet()) {
            BackgroundTask task = entry.getValue();
            if (COMPLETED.equals(task.getStatus())) {
                bgIds.add(entry.getKey());
            }
        }
        for (String bgId : bgIds) {
            BackgroundTask task = tasks.remove(bgId);
            String output = results.remove(bgId);
            if (task == null) {
                continue;
            }
            if (output == null) {
                output = "no output";
            }
            String summary = output.length() > 500 ? output.substring(0, 500) + "... more " + (output.length() - 500) + " chars" : output;
            String notification =
                    "<task_notification>\n"
                            + " <task_id>" + bgId + "</task_id>\n"
                            + " <status>" + task.getStatus() + "</status>\n"
                            + " <command>" + task.getCommand() + " </command>\n"
                            + " <summary>" + escapeXml(summary) + "</summary>\n"
                            + "</task_notification>";
            notifications.add(notification);
            System.out.println("  [background done] " + bgId + ": "
                    + task.getCommand() + " (" + output.length() + " chars)");
        }
        return notifications;
    }

    private String escapeXml(String summary) {
        if (summary == null) {
            return "";
        }
        return summary.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private void executeInBackground(String bgId, ToolUseBlock block, ToolRegistry toolRegistry) {
        try {
            Tool tool = toolRegistry.find(block.getName());
            if (tool == null) {
                tasks.get(bgId).setStatus(ERROR);
                results.put(bgId, "Unknown tool: " + block.getName());
                return;
            }
            JSONObject input = block.getInput();
            ToolResult res = tool.execute(input);
            tasks.get(bgId).setStatus(COMPLETED);
            results.put(bgId, res.getContent() != null ? res.getContent() : "(no output)");
        } catch (Exception e) {
            BackgroundTask task = tasks.get(bgId);
            if (task != null) {
                task.setStatus(ERROR);
            }
            results.put(bgId, "Error:" + e.getMessage());
        }
    }

}
