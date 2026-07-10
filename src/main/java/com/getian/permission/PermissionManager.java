package com.getian.permission;

import com.getian.core.ToolUseBlock;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class PermissionManager {
    private final File workDir;
    private final ApprovalPrompter approvalPrompter;
    // 直接拦截的集合 —— 黑名单
    private final List<String> denyList = Arrays.asList("rm -rf /", "sudo", "shutdown", "reboot", "mkfs", "dd if=",
            "> /dev/sda");
    // 需要询问的命令集合
    private final List<String> askList = Arrays.asList("rm ", "> /etc/", "chmod 777");

    public PermissionManager(File workDir, ApprovalPrompter approvalPrompter) {
        this.workDir = workDir;
        this.approvalPrompter = approvalPrompter;
    }

    public PermissionDecision check(ToolUseBlock toolUseBlock) {
        String toolName = toolUseBlock.getName();
        //todo 这里可以使用策略模式来进行修改
        if ("bash".equalsIgnoreCase(toolName)) {
            //bash对应的处理方式
            return checkBash(toolUseBlock);
        } else if ("write_file".equalsIgnoreCase(toolName) || "read_file".equalsIgnoreCase(toolName)) {
            //write_file | read_file对应的处理方式
            //todo 这里我觉得还可以细化
            return checkFileWrite(toolUseBlock);
        }
        return PermissionDecision.allow();
    }

    public PermissionDecision checkBash(ToolUseBlock toolUseBlock) {
        String command = toolUseBlock.getInput() == null ? "" : toolUseBlock.getInput().getString("command");
        if (command == null) command = "";
        for (String pattern : denyList) {
            if (command.contains(pattern)) {
                //第一道关卡
                return PermissionDecision.deny("Permission denied: '" + pattern + "' is on the deny list");
            }
        }
        for (String pattern : askList) {
            if (command.contains(pattern)) {
                // 第二道关卡
                return ask(toolUseBlock, "Potentially destructive command");
            }
        }
        return PermissionDecision.allow();
    }

    public PermissionDecision checkFileWrite(ToolUseBlock toolUseBlock) {
        String path = toolUseBlock.getInput() == null ? "" : toolUseBlock.getInput().getString("path");
        if (path == null || path.isBlank()) {
            return PermissionDecision.allow();
        }
        //判断当前文件写入的路径是否是项目路径？
        try {
            File target = new File(workDir, path).getCanonicalFile();
            File source = workDir.getCanonicalFile();
            if (!target.toPath().startsWith(source.toPath())) {
                return PermissionDecision.deny("Permission denied : path escapes workspace");
            }
            return PermissionDecision.allow();
        } catch (IOException e) {
            return PermissionDecision.deny("Permission denied: " + e.getMessage());
        }
    }


    private PermissionDecision ask(ToolUseBlock toolUseBlock, String reason) {
        return approvalPrompter.approve(toolUseBlock, reason)
                ? PermissionDecision.allow()
                : PermissionDecision.deny("Operation denied,rejection reason: " + reason);
    }
}
