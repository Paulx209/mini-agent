package com.getian.memory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.getian.core.AssistantMessage;
import com.getian.core.ContentBlock;
import com.getian.core.Message;
import com.getian.core.TextBlock;
import com.getian.llm.LLMClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 根据当前问题选择相关记忆。
 * 1.调用大模型，根据最近对话历史和记忆目录去选择适当的记忆索引
 * 2.如果失败的话，使用关键词匹配进行兜底
 */
public class MemorySelector {
    private static final int MAX_SELECTED = 5;
    private final LLMClient llmClient;

    public MemorySelector(LLMClient llmClient) {
        this.llmClient = llmClient;
    }

    public List<Memory> select(List<Memory> memoryList, String recentSessionContext) {
        if (memoryList == null || memoryList.isEmpty()) {
            return Collections.emptyList();
        }
        //1.首先让LLM帮我们选择记忆
        List<Memory> selectedMemory = selectWithLLM(memoryList, recentSessionContext);
        if (!selectedMemory.isEmpty()) {
            return selectedMemory;
        }
        //2.关键词选择
        return selectWithKeyWords(memoryList, recentSessionContext);
    }

    private List<Memory> selectWithLLM(List<Memory> memoryList, String recentSessionContext) {
        String prompt = "Given the recent conversation and the memory catalog below,\n"
                + "select the indices of memories that are clearly relevant.\n"
                + "Return ONLY a JSON array of integers, e.g. [0, 3].\n"
                + "If none are relevant, return [].\n\n"
                + "Recent conversation:\n" + recentSessionContext + "\n\n"
                + "Memory catalog:\n" + catalog(memoryList);
        AssistantMessage resp = llmClient.chat(Collections.singletonList(Message.user(prompt)), Collections.emptyList());
        JSONArray indices = fristJsonArray(extractText(resp));
        if (indices == null) {
            return Collections.emptyList();
        }
        List<Memory> selected = new ArrayList<>();
        for (int i = 0; i < indices.size() && selected.size() < MAX_SELECTED; i++) {
            Integer index = indices.getInteger(i);
            if (index != null && index >= 0 && index < memoryList.size()) {
                selected.add(memoryList.get(index));
            }
        }
        return selected;
    }

    private List<Memory> selectWithKeyWords(List<Memory> memoryList, String recentConversation) {
        List<Memory> selected = new ArrayList<>();
        String query = recentConversation == null ? "" : recentConversation.toLowerCase(Locale.ROOT);
        for (Memory memory : memoryList) {
            if (selected.size() >= MAX_SELECTED) {
                break;
            }
            String haystack = (memory.getName() + " " + memory.getDescription()).toLowerCase(Locale.ROOT);
            for (String token : query.split("[^a-z0-9]+")) {
                if (token.length() >= 3 && haystack.contains(token)){
                    selected.add(memory);
                }
            }
        }
        return selected;
    }

    /**
     * 获取到memoryList的索引格式
     */
    private String catalog(List<Memory> memoryList) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < memoryList.size(); i++) {
            Memory memory = memoryList.get(i);
            builder.append(i)
                    .append(": ")
                    .append(memory.getName())
                    .append("- ")
                    .append(memory.getDescription())
                    .append("\n");
        }
        return builder.toString();
    }

    private JSONArray fristJsonArray(String text) {
        if (text == null) {
            return null;
        }
        int start = text.indexOf("[");
        int end = text.indexOf("]");
        if (start < 0 || end < start) {
            return null;
        }
        return JSON.parseArray(text.substring(start, end + 1));
    }

    private String extractText(AssistantMessage resp) {
        StringBuilder sb = new StringBuilder();
        List<ContentBlock> content = resp.getContent();
        for (ContentBlock block : content) {
            if (block instanceof TextBlock) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(((TextBlock) block).getText());
            }
        }
        return sb.toString();
    }

}
