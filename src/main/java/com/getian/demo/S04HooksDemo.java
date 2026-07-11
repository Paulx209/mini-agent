package com.getian.demo;

import com.getian.core.*;
import com.getian.utils.AnthropicClientUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 *@Author: sonicge
 *@CreateTime: 2026-07-11
 */

public class S04HooksDemo {
    public static void main(String[] args) {
        AgentLoop simpleAgentLoop = AnthropicClientUtils.createSimpleAgentLoop();
        System.out.println("s04: Hooks");
        System.out.println("输入问题，回车发送。输入 q 退出。\n");
        Scanner sc = new Scanner(System.in);
        List<Message> history = new ArrayList<>();
        while (true) {
            if (!sc.hasNextLine()) {
                break;
            }
            String prompt = sc.nextLine();
            if (prompt == null || prompt.isBlank() || "q".equalsIgnoreCase(prompt.trim())
                    || "exit".equalsIgnoreCase(prompt.trim())) {
                break;
            }

            Message user = Message.user(prompt);
            history.add(user);
            AssistantMessage resp = simpleAgentLoop.run(history);
            for (ContentBlock block : resp.getContent()) {
                if (block instanceof TextBlock) {
                    System.out.println(((TextBlock) block).getText());
                }
            }
            System.out.println();
        }
    }
}
