package com.getian.demo;

import com.getian.background.BackgroundTasks;
import com.getian.core.*;
import com.getian.llm.AnthropicLLMClient;
import com.getian.task.TaskService;
import com.getian.task.TaskStore;
import com.getian.tool.*;
import com.getian.utils.AnthropicClientUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class S11BackgroundTaskDemo {
    private static final String SYSTEM_PROMPT = "You are a coding agent. Act, don't explain.\n\n"
            + "Available tools: bash, read_file, write_file, "
            + "create_task, list_tasks, get_task, claim_task, complete_task.\n\n"
            + "The bash tool accepts an optional run_in_background parameter. "
            + "Set it to true for slow commands like install, build, test, deploy.\n\n"
            + "Working directory: " + System.getProperty("user.dir");

    public static void main(String[] args) {
        File workDir = new File(".");
        BackgroundTasks manager = new BackgroundTasks();
        TaskService taskService = new TaskService(new TaskStore(workDir));
        AnthropicLLMClient client = AnthropicClientUtils.createClient(SYSTEM_PROMPT);
        ToolRegistry toolRegistry = AnthropicClientUtils.createSimpleToolRegistry(workDir);
        toolRegistry.registry(new ListTaskTool(taskService))
                .registry(new CreateTaskTool(taskService))
                .registry(new GetTaskTool(taskService))
                .registry(new ClaimTaskTool(taskService))
                .registry(new CompleteTaskTool(taskService));
        AgentLoopListener listener = AnthropicClientUtils.createSimpleAgentLoopListener();
        BackgroundAgentLoop agentLoop = new BackgroundAgentLoop(manager, client, toolRegistry, listener);

        System.out.println("s11: Background Tasks");
        System.out.println("输入问题，回车发送。输入 q 退出。\n");

        List<Message> history = new ArrayList<>();

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.print("s11 >> ");
            if (!sc.hasNextLine()) {
                break;
            }
            String query = sc.nextLine();
            if (query == null || query.isBlank() || "q".equalsIgnoreCase(query.trim())
                    || "exit".equalsIgnoreCase(query.trim())) {
                break;
            }
            history.add(Message.user(query));
            AssistantMessage resp = agentLoop.run(history);
            if (resp.getContent() != null) {
                for (ContentBlock block : resp.getContent()) {
                    if (block instanceof TextBlock) {
                        System.out.println(((TextBlock) block).getText());
                    }
                }
            }
            System.out.println();
        }
    }
}
