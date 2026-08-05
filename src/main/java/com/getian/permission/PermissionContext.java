package com.getian.permission;

import com.getian.tool.PathGuard;
import lombok.Data;

import java.io.File;

/**
 *@Author: sonicge
 *@CreateTime: 2026-08-05
 */

@Data
public class PermissionContext {
    private final File workDir;
    private final ApprovalPrompter approvalPrompter;

    public PermissionContext(File workDir,ApprovalPrompter approvalPrompter){
        this.workDir = workDir;
        this.approvalPrompter =  approvalPrompter;
    }
}
