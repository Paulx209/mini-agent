package com.getian.demo;

import com.getian.core.*;
import com.getian.llm.AnthropicLLMClient;
import com.getian.mcp.McpManager;
import com.getian.mcp.McpToolPool;
import com.getian.tool.*;
import com.getian.utils.AnthropicClientUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 *@Author: sonicge
 *@CreateTime: 2026-08-01
 */

public class S16McpPluginDemo {
    private static final String SYSTEM_PROMPT = "You are a coding agent. Act, don't explain.\n\n"
            + "Available tools: bash, read_file, write_file, connect_mcp.\n"
            + "MCP tools are prefixed mcp__{server}__{tool}.\n\n"
            + "Working directory: " + System.getProperty("user.dir");

    public static void main(String[] args) {
        File workDir = new File(".");
        AnthropicLLMClient client = AnthropicClientUtils.createClient(SYSTEM_PROMPT);
        AgentLoopListener listener = AnthropicClientUtils.createSimpleAgentLoopListener();
        List<Tool> builtinTools = Arrays.asList(
                new BashTool(workDir),
                new WriteFileTool(workDir),
                new ReadFileTool(workDir)
        );

        McpManager mcpManager = new McpManager();
        McpToolPool mcpToolPool = new McpToolPool(builtinTools,mcpManager);
        DynamicMcpAgentLoop agentLoop = new DynamicMcpAgentLoop(client,mcpToolPool,listener);

        System.out.println("s16: MCP Plugin");
        System.out.println("输入问题，回车发送。输入 q 退出。\n");

        List<Message> history = new ArrayList<>();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("s16 >> ");
            if (!scanner.hasNextLine()) {
                break;
            }

            String query = scanner.nextLine();
            if (query == null || query.isBlank() || "q".equalsIgnoreCase(query.trim())
                    || "exit".equalsIgnoreCase(query.trim())) {
                break;
            }

            history.add(Message.user(query));
            AssistantMessage answer = agentLoop.run(history);
            printText(answer);
            System.out.println();
        }
    }

    private static void printText(AssistantMessage answer) {
        if (answer == null || answer.getContent() == null) {
            return;
        }
        for (ContentBlock block : answer.getContent()) {
            if (block instanceof TextBlock) {
                System.out.println(((TextBlock) block).getText());
            }
        }
    }
}
