package com.getian.hooks;

import com.getian.core.Message;
import com.getian.core.ToolResultBlock;
import com.getian.core.ToolUseBlock;
import lombok.Data;

import java.util.List;

/**
 *@Author: sonicge
 *@CreateTime: 2026-07-10
 */

@Data
public class HookContext {
    private String event; //hook type
    private String userPrompt;
    private List<Message> messageList; //上下文
    private ToolUseBlock toolUseBlock;
    private ToolResultBlock toolResultBlock;

    public HookContext(String event){
        this.event = event;
    }

}
