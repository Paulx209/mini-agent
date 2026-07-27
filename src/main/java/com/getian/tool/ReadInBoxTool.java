package com.getian.tool;

import com.alibaba.fastjson.JSONObject;
import com.getian.team.MessageBus;
import com.getian.team.TeamMessage;

import java.util.List;

/**
 * @Author: sonicge
 * @CreateTime: 2026-07-27
 * 谁注册该Tool 就读谁的Inbox
 */

public class ReadInBoxTool implements Tool{
    private final MessageBus messageBus;
    private final String curAgentName;

    public ReadInBoxTool(MessageBus messageBus, String curAgentName) {
        this.messageBus = messageBus;
        this.curAgentName = curAgentName;
    }

    /**
     * {
     *   "name": "check_inbox",
     *   "description": "Check this agent's inbox for teammate messages.",
     *   "input_schema": {"type": "object", "properties": {}}
     * }
     */
    @Override
    public ToolDefinition getDefinition() {
        JSONObject inputSchema = new JSONObject()
                .fluentPut("type", "object")
                .fluentPut("properties", new JSONObject());
        return new ToolDefinition("check_inbox", "Check this agent's inbox for teammate messages.", inputSchema);
    }

    @Override
    public ToolResult execute(JSONObject input) {
        List<TeamMessage> teamMessages = messageBus.read(curAgentName);
        if(teamMessages.isEmpty()) {
            return new ToolResult("(inbox empty)");
        }
        return new ToolResult(messageBus.formatInbox(teamMessages));
    }
}
