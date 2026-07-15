package com.getian.demo;

import com.getian.compact.CompactionPipeline;
import com.getian.core.*;
import com.getian.llm.AnthropicLLMClient;
import com.getian.tool.CompactTool;
import com.getian.tool.ToolRegistry;
import com.getian.tool.ToolResult;
import com.getian.utils.AnthropicClientUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class S08ContextCompactDemo {
    private static final String SYSTEM_PROMPT = "You are a coding agent at " + System.getProperty("user.dir")
            + ". Use tools to solve tasks. "
            + "Use compact when the conversation is getting long or the user asks you to compact. "
            + "Act, don't explain.";

    public static void main(String[] args) {
        File workDir = new File(".");
        AnthropicLLMClient client = AnthropicClientUtils.createClient(SYSTEM_PROMPT);
        CompactionPipeline pipeline = new CompactionPipeline(workDir, client);
        ToolRegistry registry = AnthropicClientUtils.createSimpleToolRegistry(workDir).registry(new CompactTool());
        AgentLoopListener agentLoopListener = new AgentLoopListener() {
            @Override
            public void beforeToolUse(ToolUseBlock toolUse) {
                System.out.println("Tool> " + toolUse.getName() + " " + toolUse.getInput());
            }

            @Override
            public void afterToolUse(ToolUseBlock toolUse, ToolResult result) {
                System.out.println("ToolResult> " + preview(result.getContent()));
            }
        };
        CompactingAgentLoop agentLoop = new CompactingAgentLoop(client, agentLoopListener, registry, pipeline);
        System.out.println("s08: Context Compact");
        System.out.println("输入问题，回车发送。输入 q 退出。\n");

        List<Message> historyList = new ArrayList<>();
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("s08 >> ");
            if (!scanner.hasNextLine()) {
                break;
            }
            String query = scanner.nextLine();
            if (query == null || query.isBlank() || "q".equalsIgnoreCase(query.trim())
                    || "exit".equalsIgnoreCase(query.trim())) {
                break;
            }
            historyList.add(Message.user(query));
            AssistantMessage answer = agentLoop.run(historyList);
            for (ContentBlock block : answer.getContent()) {
                if (block instanceof TextBlock) {
                    System.out.println(((TextBlock) block).getText());
                }
            }
            System.out.println();
        }
    }

    private static String preview(String content) {
        return content == null || content.length() <= 500 ? content : content.substring(0, 500) + "\n ... " + (content.length() - 500) + " more chars";
    }
}
