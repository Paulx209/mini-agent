package com.getian.memory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.getian.compact.MessageInspector;
import com.getian.core.AssistantMessage;
import com.getian.core.ContentBlock;
import com.getian.core.Message;
import com.getian.core.TextBlock;
import com.getian.llm.LLMClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 每一轮对话结束之后，通过调用LLM的方式总结最近对话，提取最新以及，更新对应的索引
 */
public class MemoryExtractor {
    private final MessageInspector inspector = new MessageInspector();
    private final LLMClient llmClient;
    private final int SKIP_MESSAGE_SIZE = 10;

    public MemoryExtractor(LLMClient llmClient) {
        this.llmClient = llmClient;
    }

    /**
     * 从最近的会话中提取memory记忆（用户偏好，项目约束，项目反馈，项目事实，参考信息等）
     */
    public List<Memory> extract(List<Message> messageList, List<Memory> existingMemory) {
        //1.对messageList进行压缩 只抽取最近的消息
        String dialogue = recentDialogue(messageList);
        //2.定义prompt
        String prompt = "Extract user preferences, constraints, feedback, project facts, or references from this dialogue.\n"
                + "Return ONLY a JSON array. Each item must contain:\n"
                + "name, type, description, body.\n"
                + "type must be one of user, feedback, project, reference.\n"
                + "If nothing new or already covered by existing memories, return [].\n\n"
                + "Existing memories:\n" + existingCatalog(existingMemory) + "\n\n"
                + "Dialogue:\n" + dialogue;
        try {
            //3.调用 总结记忆
            AssistantMessage resp = llmClient.chat(Collections.singletonList(Message.user(prompt)), Collections.emptyList());
            List<Memory> extracted = parseMemories(firstJsonArray(extractText(resp)));

            if (!extracted.isEmpty()) {
                return extracted;
            }
        } catch (Exception e) {
            //解析失败走兜底 异常不处理
        }
        return explicitRememberFallback(currentUserText(messageList));

    }

    /**
     * 提取最近的会话
     */
    private String recentDialogue(List<Message> messageList) {
        int start = Math.max(0, messageList.size() - SKIP_MESSAGE_SIZE);
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < messageList.size(); i++) {
            Message message = messageList.get(i);
            if (message != null) {
                sb.append(message.getRole()).append(": ");
                sb.append(inspector.textOf(message)).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 最近记忆的索引信息
     */
    private String existingCatalog(List<Memory> existingMemories) {
        StringBuilder sb = new StringBuilder();
        if (existingMemories == null) {
            return sb.toString();
        }
        for (Memory memory : existingMemories) {
            sb.append("- ");
            sb.append(memory.getName());
            sb.append(": ");
            sb.append(memory.getDescription());
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * 兜底策略：将user prompt中和"请记住"在同一行的prompt提取出来作为记忆
     */
    private List<Memory> explicitRememberFallback(String dialogue) {
        int index = dialogue.indexOf("请记住");
        if (index < 0) {
            return Collections.emptyList();
        }
        String remembered = dialogue.substring(index)
                .replaceFirst("^请记住[:：]?", "")
                .split("\\R", 2)[0]
                .trim();
        if (remembered.isBlank()) {
            return Collections.emptyList();
        }
        List<Memory> memories = new ArrayList<>();
        memories.add(new Memory(null,
                "user-remember-" + System.currentTimeMillis(),
                remembered,
                "user",
                remembered));
        return memories;
    }

    /**
     * 获取上下文消息中最后一次用户消息
     */
    private String currentUserText(List<Message> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if (!"user".equals(message.getRole())) {
                continue;
            }
            String text = inspector.textOf(message);
            int open = text.lastIndexOf("<user_message>");
            int close = text.lastIndexOf("</user_message>");
            if (open >= 0 && close > open) {
                return text.substring(open + "<user_message>".length(), close).trim();
            }
            return text;
        }
        return "";
    }

    /**
     * 从jsonArray中解析memory
     */
    private List<Memory> parseMemories(JSONArray jsonArray) {
        List<Memory> memoryList = new ArrayList<>();
        if (jsonArray == null || jsonArray.isEmpty()) {
            return memoryList;
        }
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject object = jsonArray.getJSONObject(i);
            if (object == null) {
                continue;
            }
            String name = blankToDefault(object.getString("name"),"memory-"+System.currentTimeMillis()+"-"+i);
            String type = safeType(object.getString("type"));
            String description = object.getString("description");
            String body = object.getString("body");
            if (description == null || description.isBlank() || body == null || body.isBlank()) {
                continue;
            }
            Memory memory = new Memory(null,name,description,type,body);
            memoryList.add(memory);
        }
        return memoryList;
    }
    /**
     * 对extractText中获取的字符串解析
     */
    private JSONArray firstJsonArray(String jsonStr) {
        if (jsonStr == null) {
            return null;
        }
        int start = jsonStr.indexOf("[");
        int end = jsonStr.indexOf("]");
        if (start < 0 || end < start) {
            return null;
        }
        return JSON.parseArray(jsonStr.substring(start, end + 1));
    }

    /**
     * 将大模型返回的textBlock中的内容拼接起来
     */
    private String extractText(AssistantMessage resp) {
        StringBuilder sb = new StringBuilder();
        for (ContentBlock block : resp.getContent()) {
            if (block instanceof TextBlock) {
                TextBlock textBlock = (TextBlock) block;
                sb.append(textBlock.getText());
            }
        }
        return sb.toString();
    }



    /**
     * 如果memoryName是blank的话  默认返回defaultValue
     */
    private String blankToDefault(String value,String defaultValue){
        if(value == null || value.isBlank()){
            return defaultValue;
        }
        return value;
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

}
