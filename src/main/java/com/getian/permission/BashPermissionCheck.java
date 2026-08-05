package com.getian.permission;

import com.alibaba.fastjson.JSONObject;
import com.getian.core.ToolUseBlock;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 *@Author: sonicge
 *@CreateTime: 2026-08-04
 */

public class BashPermissionCheck implements CheckPermissionStrategy{
    private  ApprovalPrompter approvalPrompter;
    // 直接拦截的集合 —— 黑名单
    private final List<String> denyList = Arrays.asList("rm -rf /", "sudo", "shutdown", "reboot", "mkfs", "dd if=",
            "> /dev/sda");
    // 需要询问的命令集合
    private final List<String> askList = Arrays.asList("rm ", "> /etc/", "chmod 777");

    public BashPermissionCheck(){}

    public BashPermissionCheck(ApprovalPrompter approvalPrompter){
        this.approvalPrompter = approvalPrompter;
    }

    @Override
    public Set<String> supportedTools() {
        return Set.of("bash");
    }

    @Override
    public PermissionDecision checkPermission(ToolUseBlock toolUseBlock) {
        JSONObject input = toolUseBlock.getInput();
        String command = input != null ? input.getString("command") :"";
        if(command == null || command.isBlank()){
            return PermissionDecision.deny("Error : command is blank");
        }
        command = command.toLowerCase();
        //1.判断是否在denyList中
        for(String pattern : denyList){
            if(command.contains(pattern)){
                return PermissionDecision.deny("Permission denied: '" + pattern + "' is on the deny list");
            }
        }
        //2.判断是否在askList中
        for(String pattern : askList){
            if(command.contains(pattern)){
                String reason = "Potentially destructive command";
                return approvalPrompter.approve(toolUseBlock,reason)
                        ? PermissionDecision.allow()
                        : PermissionDecision.deny("Operation denied,rejection reason: " + reason);
            }
        }
        //3.放行
        return PermissionDecision.allow();
    }

    @Override
    public CheckPermissionStrategy create(PermissionContext context) {
        ApprovalPrompter approvalPrompter = context.getApprovalPrompter();
        return new BashPermissionCheck(approvalPrompter);
    }
}
