package com.getian.demo;


import com.getian.core.*;
import com.getian.hooks.HookManager;
import com.getian.llm.AnthropicLLMClient;
import com.getian.permission.PermissionManager;
import com.getian.tool.TodoWriteTool;
import com.getian.tool.ToolRegistry;
import com.getian.utils.AnthropicClientUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 *@Author: sonicge
 *@CreateTime: 2026-07-11
 */

public class S05TodoDemo {
    private static final String SYSTEM_PROMPT = "You are a coding agent at " + System.getProperty("user.dir")
            + ". Before starting any multi-step task, use todo_write to plan your steps. "
            + "Keep exactly one task in_progress while working. Mark tasks completed as you finish. "
            + "Act, don't explain.";
    public static void main(String[] args) {
        AnthropicLLMClient client = AnthropicClientUtils.createClient(SYSTEM_PROMPT);
        ToolRegistry registry = AnthropicClientUtils.createSimpleToolRegistry(new File(".")).registry(new TodoWriteTool());
        AgentLoopListener listener = AnthropicClientUtils.createSimpleAgentLoopListener();
        PermissionManager permissionManager = AnthropicClientUtils.createPermissionManager(new File("."));
        HookManager hookManager = AnthropicClientUtils.createHookManager(new File("."));
        AgentLoop agentLoop = new AgentLoop(client,registry,listener,permissionManager,hookManager);

        System.out.println("s05: Todo");
        System.out.println("输入问题，回车发送。输入 q 退出。\n");

        List<Message> history =new ArrayList<>();
        while(true){
            Scanner sc = new Scanner(System.in);
            if(!sc.hasNextLine()){
                break;
            }
            String prompt = sc.nextLine();
            if (prompt == null || prompt.isBlank() || "q".equalsIgnoreCase(prompt.trim())
                    || "exit".equalsIgnoreCase(prompt.trim())) {
                break;
            }

            history.add(Message.user(prompt));
            AssistantMessage resp = agentLoop.run(history);

            for(ContentBlock block :resp.getContent()){
                if(block instanceof TextBlock){
                    System.out.println(((TextBlock) block).getText());
                }
                System.out.println();
            }
        }
    }
}
