package com.getian.mcp;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.*;

/**
 *@Author: sonicge
 *@CreateTime: 2026-08-01
 */

@Data
public class McpClient {
    public interface Handler{
        String handler(JSONObject input);
    }

    private String name;
    private List<McpToolDefinition> tools = new ArrayList<>();
    private Map<String,Handler> handlerMap =new LinkedHashMap<>();

    public McpClient(String name){
        this.name = name;
    }

    public void register(List<McpToolDefinition> tools,Map<String,Handler> handlerMap){
        this.tools = tools == null ? Collections.emptyList() : tools;
        this.handlerMap = handlerMap == null ? Collections.emptyMap() : handlerMap;
    }

    public String callTool(String toolName,JSONObject inputSchema){
        if(toolName == null || toolName.isBlank() || !handlerMap.containsKey(toolName)){
            return "Mcp Error : toolName is invalid";
        }
        Handler handler = handlerMap.get(toolName);
        if(handler == null){
            return "Mcp Error: unknown tool: " + toolName;
        }
        try {
            return handler.handler(inputSchema == null ? new JSONObject() : inputSchema);
        } catch (Exception e) {
            return "MCP Error: " + e.getMessage();
        }
    }

}
