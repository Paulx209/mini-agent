package com.getian.tool;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.getian.mcp.McpManager;

/**
 *@Author: sonicge
 *@CreateTime: 2026-08-01
 */

public class ConnectMcpTool implements  Tool{
    private final McpManager mcpManager;
    public ConnectMcpTool(McpManager manager){
        this.mcpManager = manager;
    }

    /*
     * {
     *   "name": "connect_mcp",
     *   "description": "Connect to a mock MCP server. Available servers: time, weather.",
     *   "input_schema": {
     *     "type": "object",
     *     "properties": {
     *       "name": {"type": "string", "description": "MCP server name"}
     *     },
     *     "required": ["name"]
     *   }
     * }
     */
    @Override
    public ToolDefinition getDefinition() {
        String name = "connect_mcp";
        //这里是在tool中提前声明有哪些mcp server的 ; 如果提前配置mcp server的话，只需要在创建agentLoop时，提前让他知道mcp server list即可
        String description = "Connect to a mock MCP server. Available servers: time, weather.";
        JSONObject inputSchema = new JSONObject()
                .fluentPut("type","object")
                .fluentPut("properties",new JSONObject()
                        .fluentPut("name",new JSONObject()
                                .fluentPut("type","string")
                                .fluentPut("description","Mcp Server name")))
                .fluentPut("required",new JSONArray()
                        .fluentAdd("name"));
        return new ToolDefinition(name,description,inputSchema);
    }

    @Override
    public ToolResult execute(JSONObject input) {
        String mcpServerName = input == null ? "" : input.getString("name");
        if(mcpServerName == null || mcpServerName.isBlank()){
            return new ToolResult("Error : ,mcp server name is null");
        }
        return new ToolResult(mcpManager.connect(mcpServerName));
    }
}
