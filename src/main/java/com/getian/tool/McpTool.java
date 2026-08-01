package com.getian.tool;

import com.alibaba.fastjson.JSONObject;
import com.getian.mcp.McpClient;
import com.getian.mcp.McpToolDefinition;
import com.getian.mcp.McpToolName;

/**
 *@Author: sonicge
 *@CreateTime: 2026-08-01
 * 一个Mcp中包含很多个Tool， 一个Tool对应一个McpTool
 */

public class McpTool implements  Tool{
    private final McpClient client;
    private final McpToolDefinition mcpToolDefinition;
    private final String toolName;

    public McpTool(McpClient client,McpToolDefinition mcpToolDefinition,String toolName){
        this.client = client;
        this.mcpToolDefinition = mcpToolDefinition;
        this.toolName = toolName;
    }
    /*
     * {
     *   "name": "mcp__{server}__{tool}",
     *   "description": "MCP tool description from server",
     *   "input_schema": {
     *     "type": "object",
     *     "properties": {},
     *     "required": []
     *   }
     * }
     */
    @Override
    public ToolDefinition getDefinition() {
        String mcpServerName = client.getName();

        String agentToolName = McpToolName.prefixed(mcpServerName, toolName);
        String agentDescription = mcpToolDefinition.getDescription();
        JSONObject agentInputSchema = mcpToolDefinition.getInputSchema();

        return new ToolDefinition(agentToolName,agentDescription,agentInputSchema);
    }

    @Override
    public ToolResult execute(JSONObject input) {
        return new ToolResult(client.callTool(toolName,input));
    }
}
