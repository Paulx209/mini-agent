package com.getian.demo;

import com.getian.background.BackgroundTasks;
import com.getian.core.*;
import com.getian.cron.CronScheduler;
import com.getian.cron.CronStore;
import com.getian.llm.AnthropicConfig;
import com.getian.llm.AnthropicLLMClient;
import com.getian.task.TaskService;
import com.getian.task.TaskStore;
import com.getian.team.MessageBus;
import com.getian.team.TeamMessage;
import com.getian.tool.*;
import com.getian.utils.AnthropicClientUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 *@Author: sonicge
 *@CreateTime: 2026-07-27
 */

public class S13AgentTeamsDemo {
    private static final String LEAD_SYSTEM_PROMPT = "You are a coding agent. Act, don't explain.\n\n"
            + "Available tools: bash, read_file, write_file, "
            + "create_task, list_tasks, get_task, claim_task, complete_task, "
            + "schedule_cron, list_crons, cancel_cron, "
            + "spawn_teammate, send_message, check_inbox.\n\n"
            + "The bash tool accepts an optional run_in_background parameter. "
            + "Set it to true for slow commands like install, build, test, deploy.\n\n"
            + "Working directory: " + System.getProperty("user.dir");

    private static final String TEAMMATE_SYSTEM_PROMPT_TEMPLATE =
            "You are '%s', a %s. Use tools to complete tasks. "
                    + "Send results via send_message to 'lead'.\n\n"
                    + "Working directory: %s";

    public static void main(String[] args) {
        File workDir = new File(".");
        MessageBus bus = new MessageBus(workDir);
        TaskService taskService  = new TaskService(new TaskStore(workDir));
        BackgroundTasks manager = new BackgroundTasks();
        AnthropicConfig config = AnthropicClientUtils.defaultAnthropicConfig(LEAD_SYSTEM_PROMPT);
        AnthropicLLMClient mainClient = new AnthropicLLMClient(config);
        AgentLoopListener listener = AnthropicClientUtils.createSimpleAgentLoopListener();
        ToolRegistry registry = AnthropicClientUtils.createSimpleToolRegistry(workDir);
        registry.registry(new ListTaskTool(taskService))
                .registry(new CreateTaskTool(taskService))
                .registry(new GetTaskTool(taskService))
                .registry(new ClaimTaskTool(taskService))
                .registry(new CompleteTaskTool(taskService));
        BackgroundAgentLoop agentLoop = new BackgroundAgentLoop(manager,mainClient,registry,listener);

        CronStore cronStore = new CronStore(workDir);
        Object lock = new Object();
        List<Message> history = new ArrayList<>();
        CronScheduler scheduler = new CronScheduler(cronStore,cronJob -> {
            synchronized (lock){
                System.out.println("  [cron inject] " + cronJob.getPrompt());
                String prompt = cronJob.getPrompt();
                history.add(Message.user(prompt));
                AssistantMessage resp = agentLoop.run(history);
                printText(resp);
                injectInboxMessage(bus,history);
            }
        });

        registry.registry(new ListCronsTool(scheduler))
                .registry(new SchedulerCronTool(scheduler))
                .registry(new CancelCronTool(scheduler))
                .registry(new ReadInBoxTool(bus,"lead"))
                .registry(new SendMessageTool(bus,"lead"))
                .registry(new SpawnTeammateTool(workDir,bus,config.getBaseUrl(),config.getApiKey(), config.getModel(),
                        TEAMMATE_SYSTEM_PROMPT_TEMPLATE));

        scheduler.start();
        System.out.println("s13: Agent Teams");
        System.out.println("输入问题，回车发送。输入 q 退出。\n");

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("s13 >> ");
            if (!scanner.hasNextLine()) {
                break;
            }
            String query = scanner.nextLine();
            if (query == null || query.isBlank() || "q".equalsIgnoreCase(query.trim())
                    || "exit".equalsIgnoreCase(query.trim())) {
                break;
            }
            synchronized (lock) {
                injectInboxMessage(bus, history);
                history.add(Message.user(query));
                AssistantMessage resp = agentLoop.run(history);
                printText(resp);
            }
        }
        scheduler.stop();
    }

    private static  void injectInboxMessage(MessageBus bus,List<Message> history){
        List<TeamMessage> leadMessages = bus.read("lead");
        if(leadMessages.isEmpty()){
            return;
        }
        history.add(Message.user("[Inbox]\n" + bus.formatInbox(leadMessages)));
        System.out.println("  [Inbox: " + leadMessages.size() + " messages injected]");
    }

    private static  void printText(AssistantMessage resp){
        if(resp != null){
            for(ContentBlock block : resp.getContent()){
                if(block instanceof TextBlock){
                    System.out.println(((TextBlock) block).getText());
                }
            }
            System.out.println();
        }
    }
}
