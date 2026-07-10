package com.getian.hooks;

/**
 *@Author: sonicge
 *@CreateTime: 2026-07-10
 */

public class HookEvent {
    public static final String USER_PROMPT_SUBMIT = "UserPromptSubmit"; //用户输入提交后触发
    public static final String PRE_TOOL_USE = "PreToolUse"; //工具使用前触发
    public static final String POST_TOOL_USE = "PostToolUse"; //工具使用后触发
    public static final String STOP = "Stop"; // Agent 循环停止时
    private HookEvent(){};
}
