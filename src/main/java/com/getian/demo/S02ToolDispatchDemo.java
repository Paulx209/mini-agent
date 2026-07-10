package com.getian.demo;

import com.getian.core.*;
import com.getian.llm.AnthropicLLMClient;
import com.getian.tool.*;
import com.getian.utils.AnthropicClientUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class S02ToolDispatchDemo {
    private static final String SYSTEM_PROMPT = "You are a coding agent at " + System.getProperty("user.dir")
            + ". Use tools to solve tasks. Act, don't explain.";

    public static void main(String[] args) {
        File workDir = new File(".");
        AnthropicLLMClient client = AnthropicClientUtils.createClient();
        ToolRegistry registry = new ToolRegistry()
                .registry(new BashTool(workDir))
                .registry(new WriteFileTool(workDir))
                .registry(new ReadFileTool(workDir))
                .registry(new GlobTool(workDir))
                .registry(new EditFileTool(workDir));
        AgentLoop loop = new AgentLoop(client, registry, new AgentLoopListener() {
            @Override
            public void beforeToolUse(ToolUseBlock toolUse) {
                System.out.println("Tool> " + toolUse.getName() + " " + toolUse.getInput());
            }

            @Override
            public void afterToolUse(ToolUseBlock toolUse, ToolResult result) {
                System.out.println("ToolResult> " + preview(result.getContent()));
            }
        });

        System.out.println("s02: Tool Dispatch");
        System.out.println("输入问题，回车发送。输入 q 退出。\n");

        List<Message> history = new ArrayList<>();
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("s02 >>");
            if(!sc.hasNextLine()){
                break;
            }
            String query = sc.nextLine();
            if (query == null || query.isBlank() || "q".equalsIgnoreCase(query.trim())
                    || "exit".equalsIgnoreCase(query.trim())) {
                break;
            }
            Message user = Message.user(query);
            history.add(user);
            AssistantMessage answer = loop.run(history);
            for (ContentBlock block : answer.getContent()) {
                if (block instanceof TextBlock) {
                    System.out.println(((TextBlock) block).getText());
                }
            }
            System.out.println();
        }
    }

    private static String preview(String content) {
        return content == null || content.length() < 500 ? content : content.substring(0, 500) + "\n... (" + (content.length() - 500) + " more chars";
    }
}
