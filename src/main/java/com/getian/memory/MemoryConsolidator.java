package com.getian.memory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.getian.core.AssistantMessage;
import com.getian.core.ContentBlock;
import com.getian.core.Message;
import com.getian.core.TextBlock;
import com.getian.llm.LLMClient;

import java.util.Collections;
import java.util.List;

public class MemoryConsolidator {
    private static final int CONSOLIDATE_THRESHOLD = 10;
    private final LLMClient llmClient;

    public MemoryConsolidator(LLMClient llmClient) {
        this.llmClient = llmClient;
    }

    public void consolidate(MemoryStore memoryStore) {
        //1.获取所有的memory
        List<Memory> allMemories = memoryStore.list();
        if (allMemories.size() < CONSOLIDATE_THRESHOLD) {
            return;
        }
        //2.定义prompt
        String prompt = "Merge and deduplicate these memories.\n"
                + "Return ONLY a JSON array. Each item must contain name, type, description, body.\n"
                + "type must be one of user, feedback, project, reference.\n\n"
                + JSON.toJSONString(allMemories);
        //3.调用大模型
        AssistantMessage resp = llmClient.chat(Collections.singletonList(Message.user(prompt)), Collections.emptyList());
        //4.解析resp
        JSONArray array = firstJSONArray(extractText(resp));
        if (array == null || array.isEmpty()) {
            return;
        }
        //清除当前的memory file
        memoryStore.deleteMemoryFiles();
        for (int i = 0; i < array.size(); i++) {
            JSONObject item = array.getJSONObject(i);
            if(item == null || item.isEmpty()){
                continue;
            }
            Memory memory = new Memory();
            String description = item.getString("description");
            String body = item.getString("body");
            if (description == null || description.isBlank() || body == null || body.isBlank()) {
                continue;
            }
            memoryStore.write(new Memory(null,
                    item.getString("name"),
                    description,
                    item.getString("type"),
                    body));
        }
    }


    /**
     * memory type类型是否正确
     */
    private String safeType(String type) {
        if ("feedback".equals(type) || "project".equals(type) || "reference".equals(type)) {
            return type;
        }
        return "user";
    }


    private JSONArray firstJSONArray(String jsonStr) {
        if (jsonStr == null || jsonStr.isBlank()) {
            return null;
        }
        int start = jsonStr.indexOf("[");
        int end = jsonStr.indexOf("]");
        if (start < 0 || start > end) {
            return null;
        }
        String str = jsonStr.substring(start, end + 1);
        return JSON.parseArray(str);
    }

    private String extractText(AssistantMessage resp) {
        if (resp == null || resp.getContent().isEmpty()) {
            return "";
        }
        List<ContentBlock> content = resp.getContent();
        StringBuilder builder = new StringBuilder();
        for (ContentBlock block : content) {
            if (block instanceof TextBlock) {
                TextBlock textBlock = (TextBlock) block;
                builder.append(textBlock.getText());
            }
        }
        return builder.toString();
    }

}
