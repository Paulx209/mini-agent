package com.getian.demo;


import com.alibaba.fastjson.JSONObject;
import com.getian.background.BackgroundTasks;
import com.getian.core.*;
import com.getian.cron.CronScheduler;
import com.getian.cron.CronStore;
import com.getian.llm.AnthropicLLMClient;
import com.getian.task.TaskService;
import com.getian.task.TaskStore;
import com.getian.tool.*;
import com.getian.utils.AnthropicClientUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @Author: sonicge
 * @CreateTime: 2026-07-23
 *
 */

public class S12CronSchedulerDemo {
    static AtomicReference<CronScheduler> schedulerRef = new AtomicReference<>();
    private static final String SYSTEM_PROMPT = "You are a coding agent. Act, don't explain.\n\n"
            + "Available tools: bash, read_file, write_file, "
            + "create_task, list_tasks, get_task, claim_task, complete_task, "
            + "schedule_cron, list_crons, cancel_cron.\n\n"
            + "The bash tool accepts an optional run_in_background parameter. "
            + "Set it to true for slow commands like install, build, test, deploy.\n\n"
            + "Working directory: " + System.getProperty("user.dir");

    public static void main(String[] args) {
        File workDir = new File(".");
        TaskService taskService = new TaskService(new TaskStore(workDir));
        BackgroundTasks manager = new BackgroundTasks();
        AnthropicLLMClient client = AnthropicClientUtils.createClient(SYSTEM_PROMPT);
        AgentLoopListener listener = AnthropicClientUtils.createSimpleAgentLoopListener();
        ToolRegistry registry = AnthropicClientUtils.createSimpleToolRegistry(workDir);
        registry.registry(new CreateTaskTool(taskService))
                .registry(new GetTaskTool(taskService))
                .registry(new ListTaskTool(taskService))
                .registry(new ClaimTaskTool(taskService))
                .registry(new CompleteTaskTool(taskService));
        BackgroundAgentLoop agentLoop = new BackgroundAgentLoop(manager, client, registry, listener);

        //构建CronScheduler
        List<Message> history = new ArrayList<>();
        Object agentLock = new Object();
        CronScheduler scheduler = new CronScheduler(new CronStore(workDir), job -> {
            synchronized (agentLock) {
                CronScheduler preScheduler = schedulerRef.get();
                if (preScheduler.isValid(job)) {
                    System.out.println("  [cron inject] " + job.getPrompt());
                    history.add(Message.user(job.getPrompt()));
                    AssistantMessage answer = agentLoop.run(history);
                    printText(answer);
                }
            }
        });
        schedulerRef.set(scheduler);

        registry.registry(new SchedulerCronTool(scheduler))
                .registry(new ListCronsTool(scheduler))
                .registry(new CancelCronTool(scheduler));

        //定时任务启动
        scheduler.start();

        System.out.println("s12: Cron Scheduler");
        System.out.println("输入问题，回车发送。输入 q 退出。\n");

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.print("s12 >> ");
            if (!sc.hasNextLine()) {
                break;
            }
            String query = sc.nextLine();
            if (query == null || query.isBlank() || "q".equalsIgnoreCase(query.trim())
                    || "exit".equalsIgnoreCase(query.trim())) {
                break;
            }
            synchronized (agentLock) {
                history.add(Message.user(query));
                AssistantMessage answer = agentLoop.run(history);
                printText(answer);
            }
        }

        scheduler.stop();
    }

    private static void printText(AssistantMessage resp) {
        for (ContentBlock block : resp.getContent()) {
            if (block instanceof TextBlock) {
                System.out.println(((TextBlock) block).getText());
            }
        }
        System.out.println();
    }
}
