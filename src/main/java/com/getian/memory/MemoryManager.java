package com.getian.memory;

import com.getian.compact.MessageInspector;
import com.getian.core.Message;

import java.util.List;

public class MemoryManager {
    private final MessageInspector inspector = new MessageInspector();
    private final MemoryStore memoryStore;
    private final MemorySelector memorySelector;
    private final MemoryExtractor memoryExtractor;
    private final MemoryConsolidator memoryConsolidator;

    public MemoryManager(MemoryStore store, MemorySelector selector, MemoryExtractor extractor, MemoryConsolidator consolidator) {
        this.memoryStore = store;
        this.memorySelector = selector;
        this.memoryExtractor = extractor;
        this.memoryConsolidator = consolidator;
    }

    /**
     * 返回记忆索引内容
     */
    public String systemMemoryIndex() {
        return memoryStore.indexContent();
    }

    /**
     * 注入相关的记忆 select -> builder -> Message(user)
     */
    public Message injectRelevantMemories(List<Message> history, String userText) {
        List<Memory> selectedMemory = memorySelector.select(memoryStore.list(), recentConversation(history, userText));
        if (selectedMemory.isEmpty()) {
            return Message.user(userText);
        }
        StringBuilder sb = new StringBuilder("<relevant_memories>");
        for (Memory memory : selectedMemory) {
            sb.append("<memory name=\"")
                    .append(memory.getName())
                    .append("\" type=\"")
                    .append(memory.getType())
                    .append("\">\n")
                    .append(memory.getBody())
                    .append("\n</memory>\n");
        }
        sb.append("</relevant_memories>\n\n")
                .append("<user_message>\n")
                .append(userText)
                .append("\n</user_message>");
        return Message.user(sb.toString());
    }

    /**
     * extract -> duplicate -> consolidate
     */
    public void afterTurn(List<Message> preCompactMessages) {
        //目前存在的memory文件
        List<Memory> existing = memoryStore.list();
        //从最近上下文对话中抽取出来的memory文件
        List<Memory> extract = memoryExtractor.extract(preCompactMessages, existing);
        //将抽取出来新的记忆存储到Memory中
        int count = 0;
        for (Memory memory : extract) {
            if (isDuplicate(memory, existing)) {
                continue;
            }
            memoryStore.write(memory);
            existing.add(memory);
            count++;
        }
        if (count > 0) {
            System.out.println("[Memory: extracted " + count + " new memories]");
        }
        memoryConsolidator.consolidate(memoryStore);
    }

    public boolean isDuplicate(Memory memory, List<Memory> existing) {
        String description = normalize(memory.getDescription());
        String body = normalize(memory.getBody());
        for (Memory existed : existing) {
            if (!description.isBlank() && description.equals(normalize(existed.getDescription()))){
                return true;
            }
            if(!body.isBlank() && body.equals(normalize(existed.getBody()))){
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase();
    }


    private String recentConversation(List<Message> history, String userText) {
        StringBuilder builder = new StringBuilder();
        int start = Math.max(0, history.size() - 6);
        for (int i = start; i < history.size(); i++) {
            Message message = history.get(i);
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(message.getRole())
                    .append(": ")
                    .append(inspector.textOf(message));
        }
        builder.append("\n");
        builder.append("user").append(": ").append(userText);
        return builder.toString();
    }

}
