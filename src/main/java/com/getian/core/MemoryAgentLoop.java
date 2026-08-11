package com.getian.core;

import com.getian.compact.MessageSnapShots;
import com.getian.memory.MemoryManager;

import java.util.List;

public class MemoryAgentLoop {
    private final MemoryManager memoryManager;
    private final CompactingAgentLoop compactingAgentLoop;
    public MemoryAgentLoop(MemoryManager memoryManager,CompactingAgentLoop compactingAgentLoop){
        this.memoryManager = memoryManager;
        this.compactingAgentLoop = compactingAgentLoop;
    }

    public AssistantMessage run(List<Message> messages){
        //1.保留当前旧的上下文
        List<Message> preCompactSnapShot = MessageSnapShots.copy(messages);
        AssistantMessage resp = compactingAgentLoop.runWithTrace(messages, preCompactSnapShot);
        //提取记忆要使用压缩前的上下文，避免摘要把用户偏好、反馈等细节抹掉
        memoryManager.afterTurn(preCompactSnapShot);
        return resp;
    }

}
