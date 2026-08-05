package com.getian.permission;

import com.alibaba.fastjson.JSONObject;
import com.getian.core.ToolUseBlock;
import com.getian.tool.PathGuard;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 *@Author: sonicge
 *@CreateTime: 2026-08-04
 */

public class WriteAndReadPermissionCheck implements CheckPermissionStrategy{
    private  PathGuard pathGuard;
    public WriteAndReadPermissionCheck(){}

    public WriteAndReadPermissionCheck(File workDir){
        this.pathGuard = new PathGuard(workDir);
    }

    @Override
    public Set<String> supportedTools() {
        return Set.of("write_file","read_file","edit_file");
    }

    @Override
    public PermissionDecision checkPermission(ToolUseBlock toolUseBlock) {
        JSONObject input = toolUseBlock.getInput();
        String path = input != null ? input.getString("path") : "";
        if(path == null || path.isBlank()){
            return PermissionDecision.deny("path is invalid");
        }
        try {
            pathGuard.resolve(path);
            return PermissionDecision.allow();
        } catch (IOException e) {
            return PermissionDecision.deny("Error: " + e.getMessage());
        }
    }

    @Override
    public CheckPermissionStrategy create(PermissionContext context){
        File workDir = context.getWorkDir();
        return new WriteAndReadPermissionCheck(workDir);
    }
}
