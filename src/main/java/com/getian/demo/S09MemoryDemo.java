package com.getian.demo;

import com.getian.compact.CompactionPipeline;
import com.getian.core.*;
import com.getian.llm.AnthropicConfig;
import com.getian.llm.AnthropicLLMClient;
import com.getian.memory.*;
import com.getian.tool.TaskTool;
import com.getian.tool.ToolRegistry;
import com.getian.utils.AnthropicClientUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class S09MemoryDemo {
    //主要用来执行主任务 如果出现了"请记住"这样的关键字 就需要总结memory
    private static final String SYSTEM_PROMPT_TEMPLATE = "You are a coding agent at "
            + System.getProperty("user.dir")
            + ".%s\n"
            + "Relevant memories are injected below. Respect user preferences from memory.\n"
            + "When the user says 'remember' or expresses a clear preference, extract it as a memory.";

    //主要用来执行概括总结任务
    private static final String SUBAGENT_SYSTEM_PROMPT = "You are a coding agent at "
            + System.getProperty("user.dir")
            + ". Complete the task you were given, then return a concise summary. Do not delegate further.";

    public static void main(String[] args) {
        File workDir = new File(".");
        //1.先创建两个client mainAgent subAgent
        AnthropicConfig config = AnthropicClientUtils.defaultAnthropicConfig("");
        AnthropicLLMClient mainClient = new AnthropicLLMClient(config);

        AnthropicConfig subagentConfig = AnthropicClientUtils.defaultAnthropicConfig(SUBAGENT_SYSTEM_PROMPT);
        AnthropicLLMClient subClient = new AnthropicLLMClient(subagentConfig);
        //2.准备创建memoryAgentLoop需要的一些参数
        ToolRegistry subToolRegistry = AnthropicClientUtils.createSimpleToolRegistry(workDir);
        ToolRegistry mainToolRegistry = AnthropicClientUtils.createSimpleToolRegistry(workDir).registry(new TaskTool(subClient, subToolRegistry));

        AgentLoopListener listener = AnthropicClientUtils.createSimpleAgentLoopListener();
        MemoryManager memoryManager = new MemoryManager(
                new MemoryStore(workDir),
                new MemorySelector(mainClient),
                new MemoryExtractor(mainClient),
                new MemoryConsolidator(mainClient)
        );

        CompactionPipeline compactionPipeline = new CompactionPipeline(workDir, mainClient);
        CompactingAgentLoop compactingAgentLoop = new CompactingAgentLoop(mainClient, listener, mainToolRegistry, compactionPipeline);
        MemoryAgentLoop agentLoop = new MemoryAgentLoop(memoryManager, compactingAgentLoop);

        System.out.println("s09: Memory");
        System.out.println("输入问题，回车发送。输入 q 退出。\n");

        //3.while(true)开始调用
        List<Message> history = new ArrayList<>();
        Scanner sc = new Scanner(System.in);
        while(true){
            System.out.print("s09 >> ");
            if (!sc.hasNextLine()) {
                break;
            }
            String query = sc.nextLine();
            if (query == null || query.isBlank() || "q".equalsIgnoreCase(query.trim())
                    || "exit".equalsIgnoreCase(query.trim())) {
                break;
            }


            //MEMORY.md 是便宜索引，每轮都放进 system prompt；正文只在相关时注入用户消息。
            config.setSystemPrompt(systemPrompt(memoryManager.systemMemoryIndex()));

            //给当前的query 携带相关的记忆
            Message message = memoryManager.injectRelevantMemories(history, query);
            history.add(message);

            //保留会话之前的上下文 防止被压缩掉
            List<Message> preCompactSnapShot = agentLoop.snapshot(history);
            //开始一次完整对话
            AssistantMessage resp = agentLoop.run(history, preCompactSnapShot);
            for(ContentBlock block :resp.getContent()){
                if(block instanceof TextBlock){
                    System.out.println(((TextBlock)block).getText());
                }
            }
        }
    }

    private static String systemPrompt(String systemMemoryIndex){
        String memoriesSection = "";
        if (systemMemoryIndex != null && !systemMemoryIndex.isBlank() && !"(no memories yet)".equals(systemMemoryIndex)) {
            memoriesSection = "\n\nMemories available:\n" + systemMemoryIndex;
        }
        return String.format(SYSTEM_PROMPT_TEMPLATE,memoriesSection);
    }

}
