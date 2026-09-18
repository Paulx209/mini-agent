package com.getian.demo;


import com.getian.core.*;
import com.getian.llm.AnthropicConfig;
import com.getian.llm.AnthropicLLMClient;
import com.getian.tool.ToolRegistry;
import com.getian.utils.AnthropicClientUtils;
import com.getian.utils.ConfigUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

/**
 *@Author: sonicge
 *@CreateTime: 2026-09-18
 */

public class T01ReadFileToolDemo {
    private static final String SYSTEM_PROMPT = "You are a coding agent at " + System.getProperty("user.dir")
            + ". Use the available tools to solve tasks. Act, don't explain.";

    public static void main(String[] args) {
        File workDir = new File(".");
        Properties properties = ConfigUtils.loadPropertiesFromResource("config.properties");
        String baseUrl = properties.getProperty("deepseek.base_url");
        String apiKey = properties.getProperty("deepseek.api_key");
        String model = properties.getProperty("deepseek.model");

        AnthropicConfig config = new AnthropicConfig(baseUrl, model, apiKey, SYSTEM_PROMPT);
        AnthropicLLMClient client = new AnthropicLLMClient(config);
        ToolRegistry toolRegistry = AnthropicClientUtils.createSimpleToolRegistry(workDir);
        AgentLoopListener listener = AnthropicClientUtils.createSimpleAgentLoopListener();

        AgentLoop agentLoop = new AgentLoop(client, toolRegistry, listener);

        Scanner sc = new Scanner(System.in);
        List<Message> history = new ArrayList<>();
        while (true) {
            System.out.print("t01 >> ");
            if (!sc.hasNextLine()) {
                break;
            }
            String query = sc.nextLine();
            if (query == null
                    || query.isEmpty()
                    || "q".equalsIgnoreCase(query.trim())
                    || "exit".equalsIgnoreCase(query.trim())) {
                break;
            }
            history.add(Message.user(query));
            AssistantMessage answer = agentLoop.run(history);
            for (ContentBlock block : answer.getContent()) {
                if ("text".equals(block.getType())) {
                    System.out.println(((TextBlock) block).getText());
                }
            }
            System.out.println();
        }

    }
}
