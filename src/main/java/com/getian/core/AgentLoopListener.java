package com.getian.core;

import com.getian.tool.ToolResult;

public interface AgentLoopListener {
    /**
     * 触发时机：LLM响应之后（非结束）
     */
    default void onAssistantMessage(AssistantMessage message) {
    }

    /**
     * 触发时机：LLM响应之后（结束）
     */
    default void onStop(AssistantMessage message) {
    }

    /**
     * 触发时机：使用工具之前
     */
    default void beforeToolUse(ToolUseBlock toolUseBlck) {
    }

    /**
     * 使用工具之后
     */
    default void afterToolUse(ToolUseBlock toolUse, ToolResult result) {
    }

}
