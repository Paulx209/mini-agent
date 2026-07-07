package com.getian.llm;

import com.getian.core.AssistantMessage;
import com.getian.core.Message;
import com.getian.tool.ToolDefinition;

import java.util.List;

public interface LLMClient {
    /**
     * @param messageList 不同role提供的prompt
     * @param tools       所有工具
     * @return AssistantMessage
     */
    AssistantMessage chat(List<Message> messageList, List<ToolDefinition> tools);
}
