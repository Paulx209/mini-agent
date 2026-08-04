package com.getian.permission;

import com.getian.core.ToolUseBlock;

public interface CheckPermissionStrategy {
    PermissionDecision checkPermission(ToolUseBlock toolUseBlock);
}
