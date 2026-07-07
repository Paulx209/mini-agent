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

    //user -> agent  TextBlock
    public static Message user(String content) {
        return new Message("user", Collections.singletonList(new TextBlock(content)));
    }

    public static Message user(List<ContentBlock> content) {
        return new Message("user", content);
    }

    //toolResults  role -> user
    public static Message toolResults(List<ToolResultBlock> toolResultBlocks){
        return new Message("user",new ArrayList<>(toolResultBlocks));
    }


    //将模型返回的内容 转换成下一轮发给模型的历史信息
    public static Message assistant(List<ContentBlock> content){
        return new Message("assistant",content);
    }

}
