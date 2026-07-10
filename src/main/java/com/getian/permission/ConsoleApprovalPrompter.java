package com.getian.permission;

import com.getian.core.ToolUseBlock;

import java.util.Scanner;

public class ConsoleApprovalPrompter implements ApprovalPrompter{
    private  final Scanner scanner;
    public  ConsoleApprovalPrompter(Scanner scanner){
        this.scanner = scanner;
    }
    @Override
    public boolean approve(ToolUseBlock toolUse, String reason) {
        System.out.println();
        System.out.println("Permission> " + reason);
        System.out.println("Tool> " + toolUse.getName() + " " + toolUse.getInput());
        System.out.println("Allow this tool call? [y/N] ");
        if(!scanner.hasNextLine()){
            return false;
        }
        String approve = scanner.nextLine().trim().toLowerCase();
        return "yes".equals(approve) || "y".equals(approve);
    }
}
