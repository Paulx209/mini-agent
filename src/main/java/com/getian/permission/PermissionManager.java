package com.getian.permission;

import com.getian.core.ToolUseBlock;
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

    /**
     * 通过 SPI 机制加载 META-INF/services 下CheckPermissionStrategy接口的所有实现类
     * @return
     */
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
