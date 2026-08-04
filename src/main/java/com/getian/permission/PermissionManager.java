package com.getian.permission;

import com.getian.core.ToolUseBlock;
import com.getian.tool.PathGuard;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PermissionManager {
    private final Map<String,CheckPermissionStrategy> checkStrategyMap = new HashMap<>();


    public PermissionDecision check(ToolUseBlock toolUseBlock) {
        String toolName = toolUseBlock.getName();
        if(checkStrategyMap.containsKey(toolName)){
            CheckPermissionStrategy checkStrategy = checkStrategyMap.get(toolName);
            return checkStrategy.checkPermission(toolUseBlock);
        }
        return PermissionDecision.allow();
    }
}
