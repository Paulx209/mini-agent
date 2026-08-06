package com.getian.hooks;

import com.getian.core.ToolUseBlock;
import com.getian.permission.PermissionDecision;
import com.getian.permission.PermissionManager;

/**
 *@Author: sonicge
 *@CreateTime: 2026-08-06
 */

public class MyPreToolUseHook implements Hook{
    private final PermissionManager permissionManager;
    public MyPreToolUseHook(PermissionManager  permissionManager){
        this.permissionManager = permissionManager;
    }
    @Override
    public HookDecision execute(HookContext context) {
        //调用PermissionManager
        ToolUseBlock toolUseBlock = context.getToolUseBlock();
        if(toolUseBlock == null){
            return HookDecision.pass();
        }
        PermissionDecision decision = permissionManager.check(toolUseBlock);
        if(decision.isAllowed()){
            return HookDecision.pass();
        }
        return HookDecision.deny(decision.getMessage());
    }
}
