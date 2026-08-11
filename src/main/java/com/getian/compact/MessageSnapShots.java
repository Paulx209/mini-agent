package com.getian.compact;


import com.alibaba.fastjson.JSONObject;
import com.getian.core.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 工具类 负责创建上下文的快照
 *@Author: sonicge
 *@CreateTime: 2026-08-11
 */

public class MessageSnapShots {
    public static Message copy(Message message){
        List<ContentBlock> content = message.getContent();
        List<ContentBlock> newContent = new ArrayList<>();
        for(ContentBlock block : content){
            newContent.add(copyBlock(block));
        }
        return new Message(message.getRole(),newContent);
    }


    public static List<Message> copy(List<Message> messages){
        return messages.stream()
                .map(MessageSnapShots::copy)
                .collect(Collectors.toList());
    }

    private static  ContentBlock copyBlock(ContentBlock block) {
        if (block instanceof TextBlock) {
            return new TextBlock(((TextBlock) block).getText());
        }
        if (block instanceof ThinkingBlock) {
            ThinkingBlock thinking = (ThinkingBlock) block;
            return new ThinkingBlock(thinking.getThinking(), thinking.getSignature());
        }
        if (block instanceof ToolUseBlock) {
            ToolUseBlock toolUse = (ToolUseBlock) block;
            JSONObject input = toolUse.getInput() == null ? new JSONObject() : new JSONObject(toolUse.getInput());
            return new ToolUseBlock(toolUse.getId(), toolUse.getName(), input);
        }
        if (block instanceof ToolResultBlock) {
            ToolResultBlock result = (ToolResultBlock) block;
            return new ToolResultBlock(result.getToolUseId(), result.getContent());
        }
        if (block instanceof UnknownBlock) {
            UnknownBlock unknown = (UnknownBlock) block;
            JSONObject raw = unknown.getRaw() == null ? new JSONObject() : new JSONObject(unknown.getRaw());
            return new UnknownBlock(unknown.getType(), raw);
        }
        return block;
    }
}
