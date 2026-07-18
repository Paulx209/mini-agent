package com.getian.utils;

import com.getian.core.*;
import com.getian.hooks.Hook;
import com.getian.hooks.HookDecision;
import com.getian.hooks.HookEvent;
import com.getian.hooks.HookManager;
import com.getian.llm.AnthropicConfig;
import com.getian.llm.AnthropicLLMClient;
import com.getian.permission.ConsoleApprovalPrompter;
import com.getian.permission.PermissionManager;
import com.getian.tool.*;

import java.io.File;
import java.util.List;
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

    public static AnthropicConfig defaultAnthropicConfig(String systemPrompt){
        Properties properties = ConfigUtils.loadPropertiesFromResource(DEFAULT_CONFIG_RESOURCE);
        AnthropicConfig config = new AnthropicConfig();
        config.setModel(required(properties, MODEL_KEY));
        config.setApiKey(required(properties, API_KEY));
        config.setBaseUrl(required(properties, BASE_URL_KEY));
        config.setSystemPrompt(systemPrompt);
        return config;
    }

    public static ToolRegistry createSimpleToolRegistry(File workDir) {
        return new ToolRegistry()
                .registry(new BashTool(workDir))
                .registry(new GlobTool(workDir))
                .registry(new EditFileTool(workDir))
                .registry(new WriteFileTool(workDir))
                .registry(new ReadFileTool(workDir));
    }

    public static AgentLoopListener createSimpleAgentLoopListener() {
        return new AgentLoopListener() {
            @Override
            public void beforeToolUse(ToolUseBlock toolUse) {
                System.out.println("Tool> " + toolUse.getName() + " " + toolUse.getInput());
            }

            @Override
            public void afterToolUse(ToolUseBlock toolUse, ToolResult result) {
                System.out.println("ToolResult> " + preview(result.getContent()));
            }
        };
    }

    public static PermissionManager createPermissionManager(File workDir) {
        return new PermissionManager(workDir, new ConsoleApprovalPrompter(new Scanner(System.in)));
    }

    public static HookManager createHookManager(File workDir){
        HookManager hookManager = new HookManager();
        hookManager.register(HookEvent.USER_PROMPT_SUBMIT, context -> {
            System.out.println("[HOOK] UserPromptSubmit: working in " + workDir.getAbsolutePath());
            return HookDecision.pass();
        });

        hookManager.register(HookEvent.PRE_TOOL_USE,context -> {
            ToolUseBlock toolUseBlock = context.getToolUseBlock();
            System.out.println("[HOOK] PreToolUse: " + toolUseBlock.getName() + ", input: " + toolUseBlock.getInput());
            return HookDecision.pass();
        });

        hookManager.register(HookEvent.POST_TOOL_USE,context -> {
            String content = context.getToolResult() == null ? "" : context.getToolResult().getContent();
            System.out.println("[HOOK] PostToolUse: " + context.getToolUseBlock().getName()+", output: "+content);
            return HookDecision.pass();
        });

        hookManager.register(HookEvent.STOP, context -> {
            System.out.println("[HOOK] Stop: session used " + toolResultCount(context.getMessageList()) + " tool calls");
            return HookDecision.pass();
        });

        return hookManager;
    }

    public static int toolResultCount(List<Message> messages){
        if(messages == null || messages.isEmpty())return 0;
        return (int) messages.stream()
                .filter(m -> m.getContent()!=null)
                .flatMap(m -> m.getContent().stream())
                .filter(block -> block instanceof ToolResultBlock)
                .count();
    }

    public static AgentLoop createSimpleAgentLoop() {
        File workDir = new File(".");
        AnthropicLLMClient client = createClient();
        ToolRegistry toolRegistry = createSimpleToolRegistry(workDir);
        AgentLoopListener agentLoopListener = createSimpleAgentLoopListener();
        PermissionManager permissionManager = createPermissionManager(workDir);
        HookManager hookManager = createHookManager(workDir);
        return new AgentLoop(client, toolRegistry, agentLoopListener, permissionManager,hookManager);
    }

    private static String preview(String content) {
        return content == null || content.length() < 500 ? content : content.substring(0, 500) + "\n... (" + (content.length() - 500) + " more chars";
    }


    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing config property: " + key);
        }
        return value;
    }
}
