package com.getian.mcp;

import java.util.*;
import java.util.stream.Collectors;

/**
 *@Author: sonicge
 *@CreateTime: 2026-08-01
 */

public class McpManager {
    //mcpServerName mcpClient
    private final Map<String,McpClient> mcpClientMap = new LinkedHashMap<>();

    /**
     * 连接某个mcp
     */
    public String connect(String name){
        if(name == null || name.isBlank()){
            return  "Mcp Error : name is null";
        }
        String key = name.trim();
        if(mcpClientMap.containsKey(key)){
            return "Mcp Info : [" + key + "] mcp server is connected";
        }

        McpClient client = MockMcpServers.create(key);
        if(client == null){
            return "Mcp Error : Unknown MCP server '" + key + "'. Available: " + availableNames();
        }
        mcpClientMap.put(key,client);

        String mcpServerName = client.getName();
        List<String> discoveredAgentToolsName = client.getTools().stream().filter(definition -> !definition.getName().isBlank())
                .map(definition -> McpToolName.prefixed(mcpServerName, definition.getName()))
                .collect(Collectors.toList());
//        List<String> discoveredAgentToolsName = client.getHandlerMap().keySet().stream()
//                .filter(mcpToolName -> !mcpToolName.isBlank())
//                .map(mcpToolName -> McpToolName.prefixed(mcpServerName,mcpToolName))
//                .collect(Collectors.toList());
        return "Connected to MCP server '" + key + "'. Discovered tools: "
                + String.join(", ", discoveredAgentToolsName);
    }

    /**
     * 获取所有已经连接的mcp
     */
    public Collection<McpClient> connectClients(){
        return mcpClientMap.values();
    }


    /**
     * 获取所有已经连接的mcp name 集合
     */
    public String connectedServerNames(){
        if (mcpClientMap.isEmpty()) {
            return "(none)";
        }
        return String.join(",",mcpClientMap.keySet());
    }

    private String availableNames(){
        return String.join(", " , MockMcpServers.availableNames());
    }
}
