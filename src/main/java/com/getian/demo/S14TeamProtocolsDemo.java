package com.getian.demo;

import com.getian.background.BackgroundTasks;
import com.getian.core.*;
import com.getian.llm.AnthropicConfig;
import com.getian.llm.AnthropicLLMClient;
import com.getian.protocol.ProtocolService;
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
 *@CreateTime: 2026-07-30
 */

public class S14TeamProtocolsDemo {

    private static final String LEAD_SYSTEM_PROMPT = "You are a coding agent. Act, don't explain.\n\n"
            + "Available tools: bash, read_file, write_file, "
            + "get_task, create_task, list_tasks, claim_task, complete_task, "
            + "spawn_teammate, send_message, check_inbox, "
            + "request_shutdown, request_plan, review_plan, review_turn_extension.\n\n"
            + "Working directory: " + System.getProperty("user.dir");

    //SpawnTeammateTool 在派生subAgent的时候会分配对应的name role userPrompt
    private static final String TEAMMATE_SYSTEM_PROMPT_TEMPLATE =
            "You are '%s', a %s. Use tools to complete tasks. "
                    + "Check inbox for protocol messages (shutdown_request, etc).\n\n"
                    + "Working directory: %s";

    public static void main(String[] args) {
        File workdir = new File(".");
        BackgroundTasks backgroundTasks = new BackgroundTasks();

        AnthropicConfig config = AnthropicClientUtils.defaultAnthropicConfig(LEAD_SYSTEM_PROMPT);
        AnthropicLLMClient mainClient = new AnthropicLLMClient(config);
        AgentLoopListener listener = AnthropicClientUtils.createSimpleAgentLoopListener();
        ToolRegistry registry = AnthropicClientUtils.createSimpleToolRegistry(workdir);

        //注册 subtask 相关
        TaskService taskService = new TaskService(new TaskStore(workdir));
        registry.registry(new CreateTaskTool(taskService))
                .registry(new GetTaskTool(taskService))
                .registry(new CompleteTaskTool(taskService))
                .registry(new ListTaskTool(taskService))
                .registry(new ClaimTaskTool(taskService));

        //注册 主子agent通信 相关
        MessageBus bus = new MessageBus(workdir);
        ProtocolService protocol = new ProtocolService(bus);
        SpawnTeammateTool spawnTeammateTool = new SpawnTeammateTool(workdir, bus, config.getBaseUrl(), config.getApiKey(), config.getModel(), TEAMMATE_SYSTEM_PROMPT_TEMPLATE, protocol);
        registry.registry(new SendMessageTool(bus, "lead"))
                .registry(new RequestPlanTool(protocol))
                .registry(new RequestShutdownTool(protocol))
                .registry(new ReviewPlanTool(protocol))
                .registry(new ReviewTurnExtensionTool(protocol))
                .registry(new ProtocolCheckInboxTool(protocol, bus))
                .registry(spawnTeammateTool);

        BackgroundAgentLoop agentLoop = new BackgroundAgentLoop(backgroundTasks,mainClient,registry,listener,100);

        List<Message> history = new ArrayList<>();

        System.out.println("s14: Team Protocols");
        System.out.println("输入问题，回车发送。输入 q 退出。\n");

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("s14 >> ");
            injectLeadInbox(protocol, bus, history);
            if (!scanner.hasNextLine()) {
                break;
            }
            String query = scanner.nextLine();
            if (query == null || query.isBlank() || "q".equalsIgnoreCase(query.trim())
                    || "exit".equalsIgnoreCase(query.trim())) {
                break;
            }
            history.add(Message.user(query));
            AssistantMessage answer = agentLoop.run(history);
            printText(answer);
        }
    }

    private static void injectLeadInbox(ProtocolService protocol, MessageBus bus,
                                        List<Message> history) {
        List<TeamMessage> inbox = protocol.consumeLeadInBox();
        if (inbox.isEmpty()) {
            return;
        }
        // check_inbox 工具和主循环共用同一个消费入口，协议状态只在一个地方更新。
        history.add(Message.user("[Inbox]\n" + bus.formatInbox(inbox)));
        System.out.println("  [Inbox: " + inbox.size() + " messages injected]");
    }

    private static void printText(AssistantMessage answer) {
        if (answer == null || answer.getContent() == null) {
            return;
        }
        for (ContentBlock block : answer.getContent()) {
            if (block instanceof TextBlock) {
                System.out.println(((TextBlock) block).getText());
            }
        }
    }
}
