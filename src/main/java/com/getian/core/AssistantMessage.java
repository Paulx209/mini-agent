package com.getian.core;

import lombok.Data;

import java.util.List;

/**
 * 模型返回的信息 List<ContentBlock> 和 stop_reason(控制停止)
 *@Author: sonicge
 *@CreateTime: 2026-07-05
 */

@Data
public class AssistantMessage {
    private List<ContentBlock> content;
    private String stopReason;

    public AssistantMessage(List<ContentBlock> content, String stopReason) {
        this.content = content;
        this.stopReason = stopReason;
    }
}
