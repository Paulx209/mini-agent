package com.getian.mcp;

import com.getian.tool.ConnectMcpTool;
import com.getian.tool.McpTool;
import com.getian.tool.Tool;
import com.getian.tool.ToolRegistry;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *@Author: sonicge
 *@CreateTime: 2026-08-01
 */

public class McpToolPool {
    private final List<Tool> builtinTools;
    private final McpManager manager;
    public McpToolPool(List<Tool> builtinTools,McpManager mcpManager){
        this.builtinTools = builtinTools;
        this.manager = mcpManager;
    }

    /**
     * 重组工具池 tools pool
     */
    public ToolRegistry assemble(){
        ToolRegistry registry = new ToolRegistry();
        Set<String> agentToolNames = new HashSet<>();

        //1.注册agent自身提供的tool工具
        for(Tool tool : builtinTools){
            String name = tool.getDefinition().getName();
            if(agentToolNames.add(name)){
                registry.registry(tool);
            }
        }

        //2.注册ConnectMcpTool
        ConnectMcpTool connectMcpTool = new ConnectMcpTool(manager);
        if(agentToolNames.add(connectMcpTool.getDefinition().getName())){
            registry.registry(connectMcpTool);
        }

        //3.注册mcp提供的tool工具
        Collection<McpClient> mcpClients = manager.connectClients();
        for(McpClient client : mcpClients){
            String mcpServerName = client.getName();
            List<McpToolDefinition> tools = client.getTools();
            for(McpToolDefinition mcpToolDefinition : tools){
                String agentToolName = McpToolName.prefixed(mcpServerName,mcpToolDefinition.getName());
                if(agentToolNames.add(agentToolName)){
                    registry.registry(new McpTool(client,mcpToolDefinition,mcpToolDefinition.getName()));
                }else{
                    //重复注册
                    System.out.println("[mcp] skipped duplicate tool: " + agentToolName);
                }
            }
        }
        return registry;
    }
}
