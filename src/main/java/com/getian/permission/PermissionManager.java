package com.getian.permission;

import com.getian.core.ToolUseBlock;
import com.getian.tool.PathGuard;

import javax.naming.Context;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class PermissionManager {
    private final Map<String,CheckPermissionStrategy> checkStrategyMap;
    private final PermissionContext context;

    public PermissionManager(PermissionContext context){
        this.context = context;
        checkStrategyMap = loadCheckStrategies();
    }

    public PermissionDecision check(ToolUseBlock toolUseBlock) {
        String toolName = toolUseBlock.getName();
        if(checkStrategyMap.containsKey(toolName)){
            CheckPermissionStrategy checkStrategy = checkStrategyMap.get(toolName);
            return checkStrategy.checkPermission(toolUseBlock);
        }
        return PermissionDecision.allow();
    }

    private Map<String,CheckPermissionStrategy> loadCheckStrategies(){
        Map<String,CheckPermissionStrategy> map = new HashMap<>();
        ServiceLoader<CheckPermissionStrategy> strategies = ServiceLoader.load(CheckPermissionStrategy.class);
        for(CheckPermissionStrategy strategy : strategies){
            CheckPermissionStrategy realStrategy  = strategy.create(context);
            Set<String> toolNames = realStrategy.supportedTools();
            for(String name : toolNames){
                if(map.containsKey(name.toLowerCase())){
                    continue;
                }
                map.put(name,realStrategy);
            }
        }
        return map;
    }
}
