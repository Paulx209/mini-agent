package com.getian.demo;

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

public class S10TaskSystemDemo {
    private static final String SYSTEM_PROMPT = "You are a coding agent. Act, don't explain.\n\n"
            + "Available tools: bash, read_file, write_file, "
            + "create_task, list_tasks, get_task, claim_task, complete_task.\n\n"
            + "Working directory: " + System.getProperty("user.dir");

    public static void main(String[] args) {
        File workDir = new File(".");
        TaskService  taskService = new TaskService(new TaskStore(workDir));
        ToolRegistry toolRegistry = new ToolRegistry()
                .registry(new BashTool(workDir))
                .registry(new ReadFileTool(workDir))
                .registry(new WriteFileTool(workDir))
                .registry(new GetTaskTool(taskService))
                .registry(new ListTaskTool(taskService))
                .registry(new CreateTaskTool(taskService))
                .registry(new ClaimTaskTool(taskService))
                .registry(new CompleteTaskTool(taskService));
        AnthropicLLMClient client = AnthropicClientUtils.createClient(SYSTEM_PROMPT);
        AgentLoopListener listener = AnthropicClientUtils.createSimpleAgentLoopListener();

        AgentLoop loop = new AgentLoop(client,toolRegistry,listener);

        System.out.println("s10: Task System");
        System.out.println("输入问题，回车发送。输入 q 退出。\n");

        List<Message> history  = new ArrayList<>();
        Scanner sc = new Scanner(System.in);
        while(true){
            System.out.print("s10 >> ");
            if (!sc.hasNextLine()) {
                break;
            }
            String query = sc.nextLine();
            if (query == null || query.isBlank() || "q".equalsIgnoreCase(query.trim())
                    || "exit".equalsIgnoreCase(query.trim())) {
                break;
            }
            history.add(Message.user(query));
            AssistantMessage resp = loop.run(history);
            for (ContentBlock block : resp.getContent()) {
                if (block instanceof TextBlock) {
                    System.out.println(((TextBlock) block).getText());
                }
            }
            System.out.println();
        }
    }
}
