package com.getian.tool;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.getian.protocol.ProtocolService;
import com.getian.team.MessageBus;
import com.getian.team.TeamMessage;

import java.util.List;

/**
 *@Author: sonicge
 *@CreateTime: 2026-07-29
 */

public class ProtocolCheckInboxTool implements Tool{
    private final ProtocolService service;
    private final MessageBus bus;
    public ProtocolCheckInboxTool(ProtocolService service,MessageBus bus){
        this.service = service;
        this.bus = bus;
    }


    /**
     * {
     *   "name": "check_inbox",
     *   "description": "Check Lead's inbox. Routes protocol responses automatically.",
     *   "input_schema": {"type": "object", "properties": {}, "required": []}
     * }
     */
    @Override
    public ToolDefinition getDefinition() {
        JSONObject schema = new JSONObject()
                .fluentPut("type", "object")
                .fluentPut("properties", new JSONObject())
                .fluentPut("required", new JSONArray());
        return new ToolDefinition("check_inbox",
                "Check Lead's inbox. Routes protocol responses automatically.", schema);
    }

    @Override
    public ToolResult execute(JSONObject input) {
        List<TeamMessage> messageList = service.consumeLeadInBox();
        if(messageList.isEmpty()){
            return new ToolResult("(inbox empty)");
        }
        return new ToolResult(bus.formatInbox(messageList));
    }
}
