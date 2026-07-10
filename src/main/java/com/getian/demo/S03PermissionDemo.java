package com.getian.demo;

import com.getian.core.*;
import com.getian.utils.AnthropicClientUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class S03PermissionDemo {
    private static File workDir = new File(".");

    public static void main(String[] args) {
        AgentLoop agentLoop = AnthropicClientUtils.createDefaultAgentLoop();
        System.out.println("s03: Permission");
        System.out.println("输入问题，回车发送。输入 q 退出。\n");

        Scanner sc = new Scanner(System.in);
        List<Message> history = new ArrayList<>();
        while (true) {
            System.out.println("s03 >> ");
            if (!sc.hasNextLine()) {
                break;
            }
            String query = sc.nextLine();
            if (query == null || query.isBlank() || "q".equalsIgnoreCase(query.trim())
                    || "exit".equalsIgnoreCase(query.trim())) {
                break;
            }
            history.add(Message.user(query));
            AssistantMessage response = agentLoop.run(history);
            for (ContentBlock block : response.getContent()) {
                if (block instanceof TextBlock) {
                    System.out.println(((TextBlock) block).getText());
                }
            }
            System.out.println();
        }
    }
}
