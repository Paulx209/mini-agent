package com.getian.core;

import com.alibaba.fastjson.JSONObject;
import com.getian.memory.MemoryManager;

import java.util.ArrayList;
import java.util.List;

public class MemoryAgentLoop {
    private final MemoryManager memoryManager;
    private final CompactingAgentLoop compactingAgentLoop;
    public MemoryAgentLoop(MemoryManager memoryManager,CompactingAgentLoop compactingAgentLoop){
        this.memoryManager = memoryManager;
        this.compactingAgentLoop = compactingAgentLoop;
    }

    public AssistantMessage run(List<Message> messages,List<Message> preCompactSnapShot){
        AssistantMessage resp = compactingAgentLoop.run(messages);
        //把本轮生成的压缩内容加进去
        preCompactSnapShot.add(Message.assistant(resp.getContent()));
        //提取记忆要使用压缩前的上下文，避免摘要把用户偏好、反馈等细节抹掉
        memoryManager.afterTurn(preCompactSnapShot);
        return resp;
    }

    /**
     * history是会一直发生变化的，所以这里必须深拷贝
     * 按理来说ContentBlock一般不会发生改变  -> 防御性编程
     */
    public List<Message> snapshot(List<Message> history) {
        List<Message> copied =new ArrayList<>();
        for(Message message : history){
            List<ContentBlock> blocks = new ArrayList<>();
            List<ContentBlock> content = message.getContent();
            for(ContentBlock block : content){
                blocks.add(copyBlock(block));
            }
            copied.add(new Message(message.getRole(),blocks));
        }
        return copied;
    }

    private ContentBlock copyBlock(ContentBlock block) {
        if (block instanceof TextBlock) {
            return new TextBlock(((TextBlock) block).getText());
        }
        if (block instanceof ThinkingBlock) {
            ThinkingBlock thinking = (ThinkingBlock) block;
            return new ThinkingBlock(thinking.getThinking(), thinking.getSignature());
        }
        if (block instanceof ToolUseBlock) {
            ToolUseBlock toolUse = (ToolUseBlock) block;
            JSONObject input = toolUse.getInput() == null ? new JSONObject() : new JSONObject(toolUse.getInput());
            return new ToolUseBlock(toolUse.getId(), toolUse.getName(), input);
        }
        if (block instanceof ToolResultBlock) {
            ToolResultBlock result = (ToolResultBlock) block;
            return new ToolResultBlock(result.getToolUseId(), result.getContent());
        }
        if (block instanceof UnknownBlock) {
            UnknownBlock unknown = (UnknownBlock) block;
            JSONObject raw = unknown.getRaw() == null ? new JSONObject() : new JSONObject(unknown.getRaw());
            return new UnknownBlock(unknown.getType(), raw);
        }
        return block;
    }


}
