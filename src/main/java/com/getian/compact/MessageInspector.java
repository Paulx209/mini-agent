package com.getian.compact;

import com.alibaba.fastjson.JSON;
import com.getian.core.*;

import java.util.ArrayList;
import java.util.List;

//专用的消息结构检查器
public class MessageInspector {

    /**
     * 估算token的大小长度
     */
    public int estimateSize(List<Message> messages) {
        return JSON.toJSONString(messages).length();
    }

    /**
     * 是否是tool_use类型的消息
     */
    public boolean hasToolUse(Message message) {
        if (message == null || !"assistant".equals(message.getRole()) || message.getContent().isEmpty()) {
            return false;
        }
        List<ContentBlock> content = message.getContent();
        for (ContentBlock block : content) {
            if (block instanceof ToolUseBlock) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否是tool_result类型的消息
     */
    public boolean isToolResultMessage(Message message) {
        if (message == null || !"user".equals(message.getRole())) {
            return false;
        }
        List<ContentBlock> content = message.getContent();
        if (content == null || content.isEmpty()) {
            return false;
        }
        for (ContentBlock block : content) {
            if (block instanceof ToolResultBlock) {
                return true;
            }
        }
        return false;
    }


    public List<ToolResultBlock> toolResults(Message message) {
        List<ToolResultBlock> results = new ArrayList<>();
        if (message == null || message.getContent() == null) {
            return results;
        }
        for (ContentBlock block : message.getContent()) {
            if (block instanceof ToolResultBlock) {
                results.add((ToolResultBlock) block);
            }
        }
        return results;
    }

    public String textOf(Message message) {
        if (message == null || message.getContent() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ContentBlock block : message.getContent()) {
            if (block != null && sb.length() > 0) {
                sb.append("\n");
            }
            if (block instanceof TextBlock) {
                sb.append(((TextBlock) block).getText());
            } else if (block instanceof ToolResultBlock) {
                sb.append(((ToolResultBlock) block).getContent());
            } else if (block instanceof ToolUseBlock) {
                ToolUseBlock tool = ((ToolUseBlock) block);
                sb.append("tool_use:")
                        .append(tool.getName())
                        .append(" ")
                        .append(tool.getInput());
            }
        }
        return sb.toString();
    }

}
