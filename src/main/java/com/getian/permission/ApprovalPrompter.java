package com.getian.permission;

import com.getian.core.ToolUseBlock;

public interface ApprovalPrompter {
    boolean approve(ToolUseBlock toolUse,String reason);
}
