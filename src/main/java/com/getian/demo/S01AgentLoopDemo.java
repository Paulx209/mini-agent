package com.getian.demo;

import com.getian.core.*;
import com.getian.llm.AnthropicConfig;
import com.getian.llm.AnthropicLLMClient;
import com.getian.tool.BashTool;
import com.getian.tool.Tool;
import com.getian.tool.ToolResult;
import com.getian.utils.ConfigUtils;

import java.io.File;
import java.util.*;

public class S01AgentLoopDemo {
    private static final String SYSTEM_PROMPT = "You are a coding agent at " + System.getProperty("user.dir")
            + ". Use bash to solve tasks. Act, don't explain.";

    public static void main(String[] args) {
        Properties properties =
                ConfigUtils.loadPropertiesFromResource("config.properties");
        String model = properties.getProperty("deepseek.model");
        String apiKey = properties.getProperty("deepseek.api_key");
        String baseUrl = properties.getProperty("deepseek.base_url");

        AnthropicConfig config = new AnthropicConfig();
        config.setModel(model);
        config.setApiKey(apiKey);
        config.setBaseUrl(baseUrl);
        config.setSystemPrompt(SYSTEM_PROMPT);
        AnthropicLLMClient client = new AnthropicLLMClient(config);

        List<Tool> tools = Collections.singletonList(new BashTool(new File(".")));
        AgentLoopListener listener = new AgentLoopListener() {
            @Override
            public void beforeToolUse(ToolUseBlock toolUseBlock) {
                System.out.println("Tool> " + toolUseBlock.getName() + " " + toolUseBlock.getInput());
            }

            @Override
            public void afterToolUse(ToolUseBlock toolUse, ToolResult result) {
                System.out.println("ToolResult> " + result.getContent());
            }
        };

        AgentLoop agentLoop = new AgentLoop(client, tools, listener);
        System.out.println("s01: Agent Loop");
        System.out.println("输入问题，回车发送。输入 q 退出。\n");

        List<Message> history = new ArrayList<>();
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.print("s01 >> ");
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
                if (block instanceof TextBlock) {
                    System.out.println(((TextBlock) block).getText());
                }
            }
            System.out.println();
        }
    }

}
