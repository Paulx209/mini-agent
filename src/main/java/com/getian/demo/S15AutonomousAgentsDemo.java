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

import javax.sound.sampled.Port;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * S15 autonomous agents: idle teammates poll the task board and claim available work.
 */
public class S15AutonomousAgentsDemo {

    private static final String LEAD_SYSTEM_PROMPT = "You are a coding agent. Act, don't explain.\n\n"
            + "Available tools: bash, read_file, write_file, "
            + "create_task, list_tasks, get_task, claim_task, complete_task, "
            + "spawn_teammate, send_message, check_inbox, "
            + "request_shutdown, request_plan, review_plan.\n\n"
            + "Working directory: " + System.getProperty("user.dir");

    private static final String TEAMMATE_SYSTEM_PROMPT_TEMPLATE =
            "You are '%s', a %s. Use tools to complete tasks. "
                    + "You can list and claim tasks from the board. "
                    + "Check inbox for protocol messages.\n\n"
                    + "Working directory: %s";


    public static void main(String[] args) {
        File workDir = new File(".");
        BackgroundTasks manager = new BackgroundTasks();
        MessageBus messageBus = new MessageBus(workDir);
        ProtocolService protocolService = new ProtocolService(messageBus);
        TaskService  taskService = new TaskService(new TaskStore(workDir));
        AnthropicConfig config = AnthropicClientUtils.defaultAnthropicConfig(LEAD_SYSTEM_PROMPT);
        AnthropicLLMClient llmClient = new AnthropicLLMClient(config);
        AgentLoopListener listener = AnthropicClientUtils.createSimpleAgentLoopListener();
        ToolRegistry registry = AnthropicClientUtils.createSimpleToolRegistry(workDir);
        //subtask相关
        registry.registry(new CreateTaskTool(taskService))
                .registry(new GetTaskTool(taskService))
                .registry(new CompleteTaskTool(taskService))
                .registry(new ListTaskTool(taskService))
                .registry(new ClaimTaskTool(taskService));

        //spawn 相关
        registry.registry(new SpawnTeammateTool(workDir,messageBus,config.getBaseUrl(),
                config.getApiKey(),config.getModel(),TEAMMATE_SYSTEM_PROMPT_TEMPLATE,protocolService,taskService))
                .registry(new SendMessageTool(messageBus,"lead"))
                .registry(new ProtocolCheckInboxTool(protocolService,messageBus))
                .registry(new RequestShutdownTool(protocolService))
                .registry(new RequestPlanTool(protocolService))
                .registry(new ReviewPlanTool(protocolService));


        BackgroundAgentLoop agentLoop = new BackgroundAgentLoop(manager,llmClient,registry,listener,50);

        List<Message> history = new ArrayList<>();

        System.out.println("s15: Autonomous Agents");
        System.out.println("输入问题，回车发送。输入 q 退出。\n");
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("s15 >> ");
            injectLeadIndex(protocolService,messageBus,history);
            if (!scanner.hasNextLine()) {
                break;
            }
            String query = scanner.nextLine();
            if (query == null || query.isBlank() || "q".equalsIgnoreCase(query.trim())
                    || "exit".equalsIgnoreCase(query.trim())) {
                break;
            }
            history.add(Message.user(query));
            AssistantMessage resp = agentLoop.run(history);
            printText(resp);
        }
    }
    private static void injectLeadIndex(ProtocolService protocol,MessageBus messageBus,List<Message> history){
        List<TeamMessage> messageList = protocol.consumeLeadInBox();
        if(messageList.isEmpty()){
            return;
        }
        history.add(Message.user("[Inbox]\n" + messageBus.formatInbox(messageList)));
        System.out.println("  [Inbox: " + messageList.size() + " messages injected]");
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
