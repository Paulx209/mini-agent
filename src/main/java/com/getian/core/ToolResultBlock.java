package com.getian.core;

import lombok.Data;

/**
 * 用来承载 Tool Result 的类型
 *@Author: sonicge
 *@CreateTime: 2026-07-05
 */
@Data
public class ToolResultBlock extends ContentBlock{
    private String toolUseId;
    private String content;

    public ToolResultBlock() {
        super("tool_result");
    }
    public ToolResultBlock(String toolUseId,String content){
        super("tool_result");
        this.toolUseId = toolUseId;
        this.content = content;
    }

}
