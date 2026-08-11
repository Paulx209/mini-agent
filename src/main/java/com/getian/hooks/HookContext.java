package com.getian.hooks;

import com.getian.core.Message;
import com.getian.core.ToolResultBlock;
import com.getian.core.ToolUseBlock;
import com.getian.tool.ToolResult;
import lombok.Data;

import java.util.List;

/**
 *@Author: sonicge
 *@CreateTime: 2026-07-10
 */

@Data
public class HookContext {
    private String event; //hook type  [required]
    private String userPrompt;// ->  for userPromptSubmit hook
    private List<Message> messageList; // for stop hook
    private ToolUseBlock toolUseBlock;//for preToolUSe hook
    private ToolResult toolResult;//for afterToolUse hook

    public HookContext(String event){
        this.event = event;
    }

}
