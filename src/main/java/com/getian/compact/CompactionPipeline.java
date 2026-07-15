package com.getian.compact;

import com.getian.core.*;
import com.getian.llm.LLMClient;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 压缩顺序如下：
 * 处理大文件 -> 裁剪旧消息 -> 缩短短工具结果 -> 实在不行调用 LLM 生成概要
 */
public class CompactionPipeline {
    private static final int KEEP_RECENT_TOOL_RESULTS = 3;
    private static final int MAX_MESSAGES = 50;
    private static final int TOOL_RESULT_BUDGET = 200000;
    private static final int AUTO_COMPACT_THRESHOLD = 50000;
    private static final int PREVIEW_CHARS = 2000;
    private static final int MICRO_COMPACT_MIN_LENGTH = 120;

    private final MessageInspector inspector = new MessageInspector();
    private final ToolResultStore toolResultStore;
    private final TranscriptStore transcriptStore;
    private final LLMClient summaryClient;

    public CompactionPipeline(File workDir, LLMClient summaryClient) {
        this.toolResultStore = new ToolResultStore(workDir);
        this.transcriptStore = new TranscriptStore(workDir);
        this.summaryClient = summaryClient;
    }

    /**
     * 调用LLM生成概要之前的处理 按照成本从低往高的顺序
     *
     * @param messageList 上下文
     */
    public void beforeLLM(List<Message> messageList) {
        //第一层处理 -> toolResultBlock
        toolResultBudget(messageList);
        snipCompact(messageList);
        microCompact(messageList);
        if (inspector.estimateSize(messageList) > AUTO_COMPACT_THRESHOLD) {
            //调用llm
            List<Message> autoCompact = compactHistory(messageList, "auto compact");
            messageList.clear();
            messageList.addAll(autoCompact);
        }
    }

    /**
     * 第一层压缩： 专门处理超大工具
     * 1.处理最后一次Message中的所有ToolResultBlock，获取到totalContentLength
     * 2.将ToolResultBlock按照content length 大小排序
     * 3.判断每个content 是否 > 2000，大于的话，进行压缩，将原文写到对应的文件中，然后只保留前2000个内容
     */
    private void toolResultBudget(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        //判断当前消息是否是 toolResult
        Message lastMessage = messages.get(messages.size() - 1);
        if (!inspector.isToolResultMessage(lastMessage)) {
            return;
        }
        //获取lastMessage中的所有toolResultBlock
        List<ToolResultBlock> toolResultBlocks = inspector.toolResults(lastMessage);
        int totalLength = totalContentLength(toolResultBlocks);
        if (totalLength <= TOOL_RESULT_BUDGET) {
            return;
        }
        List<ToolResultBlock> results = new ArrayList<>(toolResultBlocks);
        //按照contentLength 从大到小排序
        results.sort(Comparator.comparingInt(this::contentLength).reversed());
        for (ToolResultBlock result : results) {
            if (totalLength <= TOOL_RESULT_BUDGET) {
                return;
            }
            String original = result.getContent() == null ? "" : result.getContent();
            String fixed = original;
            if (original.length() > PREVIEW_CHARS) {
                fixed = toolResultStore.persist(result);
            } else {
                return;
            }
            result.setContent(fixed);
            totalLength -= (original.length() - fixed.length());
        }
    }

    private int contentLength(ToolResultBlock result) {
        return result.getContent() == null ? 0 : result.getContent().length();
    }


    private int totalContentLength(List<ToolResultBlock> toolResultBlocks) {
        int total = 0;
        for (ToolResultBlock block : toolResultBlocks) {
            total += contentLength(block);
        }
        return total;
    }

    /**
     * 压缩消息，只保留前面和最后面的消息，中间的消息一律删除掉。不论是哪一种消息
     * [0 head-1]  [head,start-1]  [start,end]
     * head不能是use  start不能是result
     */
    private void snipCompact(List<Message> messages) {
        if (messages.size() <= MAX_MESSAGES) {
            return;
        }
        int head = 3;
        int start = messages.size() - (MAX_MESSAGES - head);
        //我们要保证head-1 和 head 是tool_use tool_result
        while (head < start && inspector.hasToolUse(messages.get(head - 1))) {
            head++;
        }
        start = moveStartAwayFromOrphanToolResult(messages, start);
        if (head > start) {
            return;
        }
        int removed = start - head;
        List<Message> next = new ArrayList<>();
        //左闭右开 [from, to)
        next.addAll(messages.subList(0, head));
        next.add(Message.user("[snipped " + removed + " messages]"));
        next.addAll(messages.subList(start, messages.size()));
        //清除掉messages
        messages.clear();
        messages.addAll(next);
    }

    /**
     * tool_use 和 tool_result必须在一块 start作为开头 不能是tool_result
     */
    private int moveStartAwayFromOrphanToolResult(List<Message> messages, int start) {
        int newStart = Math.max(0, start);
        while (newStart < messages.size() && inspector.isToolResultMessage(messages.get(newStart))) {
            newStart++;
        }
        return newStart;
    }

    /**
     * 获取Message中所有的ToolResultBlock 处理旧的消息块，如果他们的长度 > 120的话 直接将内容替换掉
     */
    private void microCompact(List<Message> messages) {
        List<ToolResultBlock> toolResultBlocks = new ArrayList<>();
        for (Message message : messages) {
            List<ToolResultBlock> part = inspector.toolResults(message);
            toolResultBlocks.addAll(part);
        }
        //只保留最近 KEEP_RECENT_TOOL_RESULTS 个消息块
        int fullFrom = Math.max(0, toolResultBlocks.size() - KEEP_RECENT_TOOL_RESULTS);
        for (int i = 0; i < fullFrom; i++) {
            ToolResultBlock replacedBlock = toolResultBlocks.get(i);
            if (replacedBlock.getContent().length() > MICRO_COMPACT_MIN_LENGTH) {
                replacedBlock.setContent("[Earlier tool result compacted. Re-run if needed.]");
            }
        }
    }


    public List<Message> compactHistory(List<Message> messages, String focus) {
        transcriptStore.write(messages);
        String prompt = "Summarize this coding-agent conversation so work can continue.\n"
                + "Preserve:\n"
                + "1. current goal\n"
                + "2. key findings and decisions\n"
                + "3. files read or changed\n"
                + "4. remaining work\n"
                + "5. user constraints\n"
                + "Focus: " + blankToDefault(focus, "compact history") + "\n\n"
                + renderMessages(messages);
        AssistantMessage summary = summaryClient.chat(Collections.singletonList(Message.user(prompt)), Collections.emptyList());
        return Collections.singletonList(Message.user("[Compacted]\n\n" + extractText(summary)));
    }

    private String extractText(AssistantMessage summary) {
        StringBuilder builder = new StringBuilder();
        for (ContentBlock block : summary.getContent()) {
            if (block instanceof TextBlock) {
                if (builder.length() > 0) {
                    builder.append("\n");
                }
                builder.append(((TextBlock) block).getText());
            }
        }
        if (builder.length() == 0) {
            return "Summary unavailable.";
        }
        return builder.toString();
    }

    private String renderMessages(List<Message> messages) {
        StringBuilder sb = new StringBuilder();
        for (Message message : messages) {
            sb.append(message.getRole()).append(": ");
            String text = inspector.textOf(message);
            if (text.length() > 4000) {
                text = text.substring(0, 4000) + "\n...[message truncated for summary prompt]";
            }
            sb.append(text).append("\n\n");
        }
        return sb.toString();
    }

    private String blankToDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    public List<Message> reactiveCompact(List<Message> messages) {
        List<Message> compacted = compactHistory(messages, "reactive prompt too long");
        int start = Math.max(0, messages.size() - 10);
        for (int i = start; i < messages.size(); i++) {
            compacted.add(messages.get(i));
        }
        return compacted;
    }
}
