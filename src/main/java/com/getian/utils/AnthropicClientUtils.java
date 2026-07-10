package com.getian.utils;

import com.getian.core.AgentLoop;
import com.getian.core.AgentLoopListener;
import com.getian.core.ToolUseBlock;
import com.getian.llm.AnthropicConfig;
import com.getian.llm.AnthropicLLMClient;
import com.getian.permission.ConsoleApprovalPrompter;
import com.getian.permission.PermissionManager;
import com.getian.tool.*;

import java.io.File;
import java.util.Properties;
import java.util.Scanner;

public class AnthropicClientUtils {
    private static final String DEFAULT_CONFIG_RESOURCE = "config.properties";
    private static final String MODEL_KEY = "deepseek.model";
    private static final String API_KEY = "deepseek.api_key";
    private static final String BASE_URL_KEY = "deepseek.base_url";

    private static final String SYSTEM_PROMPT = "You are a coding agent at " + System.getProperty("user.dir")
            + ". Use tools to solve tasks. Act, don't explain.";
    ;
    private AnthropicClientUtils() {
    }

    public static AnthropicLLMClient createClient() {
        return createClient(SYSTEM_PROMPT);
    }

    public static AnthropicLLMClient createClient(String systemPrompt) {
        Properties properties = ConfigUtils.loadPropertiesFromResource(DEFAULT_CONFIG_RESOURCE);
        return createClient(properties, systemPrompt);
    }

    public static AnthropicLLMClient createClient(Properties properties, String systemPrompt) {
        AnthropicConfig config = new AnthropicConfig();
        config.setModel(required(properties, MODEL_KEY));
        config.setApiKey(required(properties, API_KEY));
        config.setBaseUrl(required(properties, BASE_URL_KEY));
        config.setSystemPrompt(systemPrompt);
        return new AnthropicLLMClient(config);
    }

    public static AgentLoop createDefaultAgentLoop() {
        AnthropicLLMClient client = createClient();
        File workDir = new File(".");
        ToolRegistry toolRegistry = new ToolRegistry()
                .registry(new BashTool(workDir))
                .registry(new GlobTool(workDir))
                .registry(new EditFileTool(workDir))
                .registry(new WriteFileTool(workDir))
                .registry(new WriteFileTool(workDir));
        PermissionManager manager = new PermissionManager(workDir,new ConsoleApprovalPrompter(new Scanner(System.in)));
        return new AgentLoop(client, toolRegistry, new AgentLoopListener() {
            @Override
            public void beforeToolUse(ToolUseBlock toolUse) {
                System.out.println("Tool> " + toolUse.getName() + " " + toolUse.getInput());
            }

            @Override
            public void afterToolUse(ToolUseBlock toolUse, ToolResult result) {
                System.out.println("ToolResult> " + preview(result.getContent()));
            }
        },manager);
    }

    private static String preview(String content){
        return content == null || content.length() < 500 ? content : content.substring(0,500) + "\n... (" + (content.length()-500) + " more chars";
    }



    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing config property: " + key);
        }
        return value;
    }
}
