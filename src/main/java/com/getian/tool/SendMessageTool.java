package com.getian.tool;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.getian.team.MessageBus;

/**
 * @Author: sonicge
 * @CreateTime: 2026-07-27
 * 给谁注册该Tool from就是谁
 */
public class SendMessageTool implements  Tool{
    private final MessageBus messageBus;

    private final String fromAgentName;

    public SendMessageTool(MessageBus messageBus, String fromAgentName) {
        this.fromAgentName = fromAgentName;
        this.messageBus = messageBus;
    }

    /**
     * {
     *   "name": "send_message",
     *   "description": "Send a message to another agent via MessageBus.",
     *   "input_schema": {
     *     "type": "object",
     *     "properties": {
     *       "to": {"type": "string"},
     *       "content": {"type": "string"}
     *     },
     *     "required": ["to", "content"]
     *   }
     * }
     */
    @Override
    public ToolDefinition getDefinition() {
        JSONObject inputSchema = new JSONObject()
                .fluentPut("type", "object")
                .fluentPut("properties", new JSONObject()
                        .fluentPut("to", new JSONObject()
                                .fluentPut("type", "string"))
                        .fluentPut("content", new JSONObject()
                                .fluentPut("type", "string")))
                .fluentPut("required", new JSONArray()
                        .fluentAdd("to")
                        .fluentAdd("content"));
        return new ToolDefinition("send_message","Send a message to another agent via MessageBus.",inputSchema);
    }

    @Override
    public ToolResult execute(JSONObject input) {
        String to = input !=null ? input.getString("to") : "";
        String content = input !=null ? input.getString("content") : "";
        if (to == null || to.isBlank()) {
            return new ToolResult("Error: to is required");
        }
        if (content == null || content.isBlank()) {
            return new ToolResult("Error: content is required");
        }
        try {
            messageBus.send(fromAgentName,to.trim(),content.trim());
        } catch (Exception e) {
            return new ToolResult("Error : "+e.getMessage());
        }
        return new ToolResult("Sent to " + to.trim());
    }

}
