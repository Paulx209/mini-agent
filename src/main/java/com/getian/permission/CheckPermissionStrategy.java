package com.getian.permission;

import com.getian.core.ToolUseBlock;

import java.util.Set;

public interface CheckPermissionStrategy {
    //支持的工具
    Set<String> supportedTools();

    //检查permission权限
    PermissionDecision checkPermission(ToolUseBlock toolUseBlock);

    CheckPermissionStrategy create(PermissionContext context);
}
