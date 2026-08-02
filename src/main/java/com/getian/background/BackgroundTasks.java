package com.getian.background;

import com.alibaba.fastjson.JSONObject;
import com.getian.core.ToolUseBlock;
import com.getian.tool.Tool;
import com.getian.tool.ToolRegistry;
import com.getian.tool.ToolResult;

import java.util.*;
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
    private static  final long MAX_TIMEOUT_TIMES = 60 * 1000L;

    private final AtomicInteger counter = new AtomicInteger(0);
    private final ConcurrentHashMap<String, BackgroundTask> tasks = new ConcurrentHashMap<>();
    private final Map<String, String> results = new LinkedHashMap<>();
    private final Map<String,Long> deadlinesMap = new ConcurrentHashMap<>();
    private final Map<String,Thread> threadMap = new ConcurrentHashMap<>();
    private final long waitTime;

    public BackgroundTasks(){
        this(MAX_TIMEOUT_TIMES);
    }
    public BackgroundTasks(long waitTime){
        this.waitTime  = waitTime;
    }

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
        threadMap.put(bgId,thread);
        thread.start();

        System.out.println("  [background] dispatched " + bgId + ": " + command);
        return bgId;
    }

    public List<String> collectionNotifications() {
        List<String> notifications = new ArrayList<>();
        List<String> bgIds = new ArrayList<>();
        markTimeoutTasks();
        for (Map.Entry<String, BackgroundTask> entry : tasks.entrySet()) {
            BackgroundTask task = entry.getValue();
            if (!RUNNING.equals(task.getStatus())) {
                bgIds.add(entry.getKey());
            }
        }
        for (String bgId : bgIds) {
            BackgroundTask task = tasks.remove(bgId);
            if (task == null) {
                continue;
            }

            String output = results.remove(bgId);
            if (output == null) {
                output = "no output";
            }

            deadlinesMap.remove(bgId);
            Thread timeoutThread = threadMap.remove(bgId);
            if(TIMEOUT.equals(task.getStatus())){
                if(timeoutThread != null){
                    timeoutThread.interrupt();
                }
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

    private void markTimeoutTasks(){
        for(String taskId :deadlinesMap.keySet()){
            BackgroundTask task = tasks.get(taskId);
            if(task == null || !RUNNING.equals(task.getStatus())){
                continue;
            }
            long now = System.currentTimeMillis();
            if(now - deadlinesMap.get(taskId) > waitTime){
                task.setStatus(TIMEOUT);
                String content =  "Timed out after " + (now - deadlinesMap.get(taskId)) + " ms.";
                results.put(taskId,content);
            }
        }
    }

    //todo 缺少对时间的判断 timeout超时
    private void executeInBackground(String bgId, ToolUseBlock block, ToolRegistry toolRegistry) {
        try {
            //记录命令开始执行时间
            deadlinesMap.put(bgId,System.currentTimeMillis());
            Tool tool = toolRegistry.find(block.getName());
            if (tool == null) {
                tasks.get(bgId).setStatus(ERROR);
                results.put(bgId, "Unknown tool: " + block.getName());
                return;
            }
            JSONObject input = block.getInput();
            ToolResult res = tool.execute(input);
            BackgroundTask task = tasks.get(bgId);
            if(task != null && RUNNING.equals(task.getStatus())){
                task.setStatus(COMPLETED);
                results.put(bgId, res.getContent() != null ? res.getContent() : "(no output)");
            }
        } catch (Exception e) {
            BackgroundTask task = tasks.get(bgId);
            if (task != null && RUNNING.equals(task.getStatus())) {
                task.setStatus(ERROR);
                results.put(bgId, "Error:" + e.getMessage());
            }
        }
    }

    private String escapeXml(String summary) {
        if (summary == null) {
            return "";
        }
        return summary.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }


}
