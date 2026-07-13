package com.getian.demo;

import cn.hutool.cron.task.Task;
import com.getian.core.*;
import com.getian.llm.AnthropicLLMClient;
import com.getian.tool.TaskTool;
import com.getian.tool.ToolRegistry;
import com.getian.utils.AnthropicClientUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class S06SubAgentDemo {
    // 父 Agent 可以使用 task 工具把复杂子问题委托出去。
    private static final String PARENT_SYSTEM_PROMPT = "You are a coding agent at " + System.getProperty("user.dir")
            + ". For complex sub-problems, use the task tool to spawn a subagent.";

    // 子 Agent 不注册 task 工具，提示词也明确禁止继续委托。
    private static final String SUBAGENT_SYSTEM_PROMPT = "You are a coding agent at " + System.getProperty("user.dir")
            + ". Complete the task you were given, then return a concise summary. Do not delegate further.";


    public static void main(String[] args) {
        File workDir = new File(".");
        AnthropicLLMClient mainClient = AnthropicClientUtils.createClient(PARENT_SYSTEM_PROMPT);
        AnthropicLLMClient subClient = AnthropicClientUtils.createClient(SUBAGENT_SYSTEM_PROMPT);

        ToolRegistry subTools = AnthropicClientUtils.createSimpleToolRegistry(workDir);
        //主agent的工具要多一个taskTool(用来给subAgent派遣子任务的)
        ToolRegistry mainTools = AnthropicClientUtils.createSimpleToolRegistry(workDir)
                .registry(new TaskTool(subClient,subTools));

        AgentLoopListener listener = AnthropicClientUtils.createSimpleAgentLoopListener();
        AgentLoop agentLoop = new AgentLoop(mainClient,mainTools,listener);

        System.out.println("s06: Subagent");
        System.out.println("输入问题，回车发送。输入 q 退出。\n");

        List<Message> history = new ArrayList<>();
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("s06 >> ");
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
            for (ContentBlock block : resp.getContent()) {
                if (block instanceof TextBlock) {
                    System.out.println(((TextBlock) block).getText());
                }
            }
            System.out.println();
        }

    }
}
