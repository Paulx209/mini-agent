package com.getian.core;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 发给模型的单条消息，role 只能是 user 或 assistant(上一轮的回复)。
 * @Author: sonicge
 * @CreateTime: 2026-07-05
 */

@Data
public class Message {
    private String role;
    private List<ContentBlock> content;

    public Message(String role,List<ContentBlock> content){
        this.role = role;
        this.content = content;
    }

    public static Message user(ContentBlock content) {
        return new Message("user", Collections.singletonList(content));
    }

    //tool_result 也属于 user
    public static Message user(List<ContentBlock> content) {
        return new Message("user", content);
    }

    public static Message toolResults(List<ToolResultBlock> toolResultBlocks){
        return new Message("user",new ArrayList<>(toolResultBlocks));
    }


    //tool_use 也属于 assistant
    public static Message assistant(List<ContentBlock> content){
        return new Message("assistant",content);
    }

}
