package com.getian.tool;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.getian.core.*;
import com.getian.llm.AnthropicConfig;
import com.getian.llm.AnthropicLLMClient;
import com.getian.llm.LLMClient;
import com.getian.protocol.ProtocolService;
import com.getian.task.TaskRecord;
import com.getian.task.TaskService;
import com.getian.team.MessageBus;
import com.getian.team.TeamMessage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *@Author: sonicge
 *@CreateTime: 2026-07-27
 */

public class SpawnTeammateTool implements Tool {
    private static final int MAX_TEAMMATE_TURNS = 10;
    private static final long IDLE_TIMEOUT_TIME = 60000; // 60s
    private static final long IDLE_SLEEP_TIME = 5000; // 5s
    private final int INBOX_NONE = 0;
    private final int INBOX_CONTINUE = 1;
    private final int INBOX_SHUTDOWN = 2;
    private final int INBOX_TIMEOUT = 3; //s15 超时
    private final File workdir;
    private final MessageBus messageBus;
    private final String model;
    private final String baseUrl;
    private final String apiKey;
    private final String promptTemplate;
    private final ProtocolService protocolService;
    private final TaskService taskService;
    private final Set<String> activeTeammates = ConcurrentHashMap.newKeySet();

    public SpawnTeammateTool(File workdir, MessageBus messageBus, String baseUrl, String apiKey, String model, String promptTemplate) {
        this(workdir, messageBus, baseUrl, apiKey, model, promptTemplate, null);
    }

    public SpawnTeammateTool(File workdir, MessageBus messageBus, String baseUrl, String apiKey, String model, String promptTemplate, ProtocolService protocolService) {
        this(workdir, messageBus, baseUrl, apiKey, model, promptTemplate, protocolService, null);
    }

    public SpawnTeammateTool(File workdir, MessageBus messageBus, String baseUrl, String apiKey, String model, String promptTemplate, ProtocolService protocolService, TaskService taskService) {
        this.workdir = workdir;
        this.messageBus = messageBus;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.promptTemplate = promptTemplate;
        this.protocolService = protocolService;
        this.taskService = taskService;
    }

    /**
     * {
     *   "name": "spawn_teammate",
     *   "description": "Spawn a teammate agent in a background thread.",
     *   "input_schema": {
     *     "type": "object",
     *     "properties": {
     *       "name": {"type": "string"},
     *       "role": {"type": "string"},
     *       "prompt": {"type": "string"}
     *     },
     *     "required": ["name", "role", "prompt"]
     *   }
     * }
     */
    @Override
    public ToolDefinition getDefinition() {
        JSONObject properties = new JSONObject()
                .fluentPut("name", new JSONObject().fluentPut("type", "string"))
                .fluentPut("role", new JSONObject().fluentPut("type", "string"))
                .fluentPut("prompt", new JSONObject().fluentPut("type", "string"));
        JSONObject schema = new JSONObject()
                .fluentPut("type", "object")
                .fluentPut("properties", properties)
                .fluentPut("required", new JSONArray()
                        .fluentAdd("name").fluentAdd("role").fluentAdd("prompt"));
        return new ToolDefinition("spawn_teammate",
                "Spawn a teammate agent in a background thread.", schema);
    }

    @Override
    public ToolResult execute(JSONObject input) {
        //1.解析参数
        String name = input != null ? input.getString("name") : "";
        String agentRole = input != null ? input.getString("role") : "";
        String agentPrompt = input != null ? input.getString("prompt") : "";
        if (name == null || name.isBlank()) {
            return new ToolResult("Error : name is required");
        }
        if (agentRole == null || agentRole.isBlank()) {
            return new ToolResult("Error : role is required");
        }
        if (agentPrompt == null || agentPrompt.isBlank()) {
            return new ToolResult("Error : prompt is required");
        }

        //2.同步subagent name 到set集合中
        String agentName = name.trim();
        if (!activeTeammates.add(agentName)) {
            return new ToolResult("Teammate '" + agentName + "' already exists");
        }

        //3.创建subAgentLoop
        Thread thread = new Thread(() -> {
            try {
                runTeammate(agentName, agentRole.trim(), agentPrompt.trim());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, "my-claude-code-teammate-" + agentName);

        thread.setDaemon(true);
        thread.start();
        System.out.println("  [teammate] " + agentName + " spawned as " + agentRole.trim());
        return new ToolResult("Teammate '" + agentName + "' spawned as " + agentRole.trim());
    }

    /**
     * subAgent启动
     */
    private void runTeammate(String agentName, String agentRole, String agentPrompt) {
        try {
            ToolRegistry registry = new ToolRegistry()
                    .registry(new BashTool(workdir))
                    .registry(new WriteFileTool(workdir))
                    .registry(new ReadFileTool(workdir))
                    .registry(new SendMessageTool(messageBus, agentName));
            if (protocolService != null) {
                registry.registry(new SubmitPlanTool(protocolService, agentName));
            }
            if (taskService != null) {
                registry.registry(new ClaimTaskTool(taskService,agentName))
                        .registry(new CompleteTaskTool(taskService))
                        .registry(new ListTaskTool(taskService));

            }
            AnthropicLLMClient client = new AnthropicLLMClient(config(String.format(promptTemplate, agentName, agentRole, workdir.getAbsolutePath())));
            //普通模式 or 协议模式
            AssistantMessage resp = protocolService == null ? runSimpleTurnLoop(agentName, agentPrompt, client, registry)
                    : runProtocolLoop(agentName, agentPrompt, client, registry);
            String summary = extractText(resp);
            if (summary.isBlank()) {
                summary = "Done.";
            }
            messageBus.send(agentName, "lead", summary, "result");
            System.out.println("  [teammate] " + agentName + " finished");
        } catch (Exception e) {
            messageBus.send(agentName, "lead", "Error: " + e.getMessage(), "result");
        } finally {
            //处理完毕之后 remove掉
            activeTeammates.remove(agentName);
        }
    }

    /**
     * 普通模式 agentLoop
     */
    private AssistantMessage runSimpleTurnLoop(String name, String prompt, LLMClient llmClient, ToolRegistry registry) {
        List<Message> history = new ArrayList<>();
        history.add(Message.user(prompt));
        AssistantMessage lastResponse = null;
        for (int i = 0; i < MAX_TEAMMATE_TURNS; i++) {
            //注入其他agent给该agent发送的message
            injectTeammateInbox(name, history);

            AssistantMessage resp = llmClient.chat(history, registry.definitions());
            lastResponse = resp;
            history.add(Message.assistant(resp.getContent()));
            List<ToolResultBlock> resultBlocks = executeToolUses(resp, registry);
            if (!"tool_use".equals(resp.getStopReason()) || resultBlocks.isEmpty()) {
                return resp;
            }
            history.add(Message.toolResults(resultBlocks));
        }
        return lastResponse;
    }

    /**
     * 协议模式 agentLoop  当llm返回end_turn之后，不是直接返回，而是继续等待mailBox中的消息
     */
    private AssistantMessage runProtocolLoop(String name, String prompt, LLMClient client, ToolRegistry toolRegistry) {
        List<Message> history = new ArrayList<>();
        history.add(Message.user(prompt));
        AssistantMessage lastResp = null;
        while(true){
            boolean reachMaxTurns = true;
            for (int i = 0; i < MAX_TEAMMATE_TURNS; i++) {
                //inject最新的mailBox
                int shouldAction = injectTeammateInbox(name, history);
                if (shouldAction == INBOX_SHUTDOWN) {
                    return lastResp;
                }
                //chat
                AssistantMessage resp = client.chat(history, toolRegistry.definitions());
                lastResp = resp;
                history.add(Message.assistant(resp.getContent()));

                //execute tool
                List<ToolResultBlock> resultBlocks = executeToolUses(resp, toolRegistry);
                //进入idle状态
                if (!"tool_use".equals(resp.getStopReason()) || resultBlocks.isEmpty()) {
                    // s15 -> 等待mailBox中的消息 or 认领实现无人做的任务
                    int idleAction = idleDo(name, history);
                    if (idleAction == INBOX_SHUTDOWN || idleAction == INBOX_TIMEOUT) {
                        return lastResp;
                    }
                    reachMaxTurns = false;
                    //新一轮agentLoop开始
                    break;
                }
                history.add(Message.toolResults(resultBlocks));
            }
            //进入idle
            if(reachMaxTurns){
                int idleAction = idleDo(name, history);
                if(idleAction == INBOX_SHUTDOWN || idleAction == INBOX_TIMEOUT){
                    return lastResp;
                }
            }
        }
    }

    private int idleDo(String agentName, List<Message> history) {
        return taskService == null
                ? idleUntilMessage(agentName, history)
                : idleUntilClaimTask(agentName,history);
    }
    private int idleUntilClaimTask(String agentName,List<Message> history){
        long endTime = System.currentTimeMillis() + IDLE_TIMEOUT_TIME;
        while(System.currentTimeMillis() < endTime){
            sleep(IDLE_SLEEP_TIME);
            //优先级1 -> 处理mailBox的消息
            int mailboxCode = injectTeammateInbox(agentName, history);
            if(mailboxCode != INBOX_NONE){
                return mailboxCode;
            }
            //优先级2 -> claim idle task
            int taskCode = injectAutoClaimedTask(agentName,history);
            if(taskCode != INBOX_NONE){
                return taskCode;
            }
        }
        System.out.println("  [idle] " + agentName + " timeout (60s)");
        return INBOX_TIMEOUT;
    }

    private int injectAutoClaimedTask(String agentName,List<Message> history){
        if(taskService == null){
            return INBOX_NONE;
        }
        List<TaskRecord> taskRecords = taskService.scanUnClaimedTask();
        for(TaskRecord record : taskRecords){
            String result = taskService.claimTask(record.getId(), agentName);
            if(result.startsWith("Claimed ")){
                String content = "<auto-claimed>\n"
                        + "Task " + record.getId() + ": " + record.getSubject() + "\n"
                        + "Description: " + nullToEmpty(record.getDescription()) + "\n"
                        + "</auto-claimed>";
                history.add(Message.user(content));
                System.out.println("  [idle] " + agentName + " auto-claimed: "
                        + record.getSubject());
                return INBOX_CONTINUE;
            }
            System.out.println("  [idle] " + agentName + " claim failed: " + result);
        }
        return INBOX_NONE;
    }

    private String nullToEmpty(String str){
        return str == null ? "" : str;
    }

    private int idleUntilMessage(String name, List<Message> messages) {
        while (true) {
            sleep(1000L);
            int inboxAction = injectTeammateInbox(name, messages);
            if (inboxAction != INBOX_NONE) {
                return inboxAction;
            }
        }
    }

    private void sleep(long sleepTime) {
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Teammate interrupted", e);
        }
    }

    private String extractText(AssistantMessage resp) {
        if (resp == null || resp.getContent().isEmpty()) {
            return "";
        }
        List<ContentBlock> content = resp.getContent();
        StringBuilder builder = new StringBuilder();
        for (ContentBlock block : content) {
            if (block instanceof TextBlock) {
                builder.append(((TextBlock) block).getText()).append("\n");
            }
        }
        return builder.toString();
    }


    private int injectTeammateInbox(String name, List<Message> messages) {
        //从 .mailboxes/name.jsonl中读取消息 读取完就delete掉
        List<TeamMessage> inbox = messageBus.read(name);
        if (inbox.isEmpty()) {
            return INBOX_NONE;
        }
        System.out.println("  [teammate inbox] " + name + ": "
                + inbox.size() + " message(s)");

        List<TeamMessage> normalMessages = new ArrayList<>();
        boolean shouldContinue = false;
        for (TeamMessage message : inbox) {
            if (protocolService != null && protocolService.isTeammateProtocolMessage(message)) {
                //协议消息
                boolean shouldStop = protocolService.handleTeammateProtocolMessage(name, message, messages);
                if (shouldStop) {
                    return INBOX_SHUTDOWN;
                }else{
                    shouldContinue = true;
                }
            } else {
                //普通消息
                normalMessages.add(message);
            }
        }
        if (!normalMessages.isEmpty()) {
            messages.add(Message.user("<inbox>\n"
                    + messageBus.formatInbox(normalMessages) + "\n</inbox>"));
            return INBOX_CONTINUE;
        }
        if(shouldContinue){
            return INBOX_CONTINUE;
        }
        return INBOX_NONE;
    }


    private List<ToolResultBlock> executeToolUses(AssistantMessage resp, ToolRegistry registry) {
        List<ToolResultBlock> res = new ArrayList<>();
        if (resp == null || resp.getContent() == null) {
            return res;
        }
        List<ContentBlock> contentBlocks = resp.getContent();
        for (ContentBlock block : contentBlocks) {
            if (block instanceof ToolUseBlock) {
                ToolUseBlock toolUseBlock = (ToolUseBlock) block;
                String name = ((ToolUseBlock) block).getName();
                Tool tool = registry.find(name);
                ToolResult result = tool == null
                        ? new ToolResult("Unknown tool: " + toolUseBlock.getName())
                        : tool.execute(toolUseBlock.getInput());
                System.out.println("  [teammate tool] " + toolUseBlock.getName()
                        + " -> " + preview(result.getContent()));
                res.add(new ToolResultBlock(((ToolUseBlock) block).getId(), result.getContent()));
            }
        }
        return res;
    }

    private AnthropicConfig config(String systemPrompt) {
        return new AnthropicConfig(baseUrl, model, apiKey, systemPrompt);
    }

    private String preview(String content) {
        if (content == null || content.length() <= 120) {
            return content;
        }
        return content.substring(0, 120) + "...";
    }
}
