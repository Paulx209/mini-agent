package com.getian.core;

import lombok.Data;

/**
 * 用来承载 模型思考的 内容
 *@Author: sonicge
 *@CreateTime: 2026-07-05
 */
@Data
public class ThinkingBlock extends ContentBlock {
    private String thinking;
    private String signature;

    public ThinkingBlock() {
        super("thinking");
    }

    public ThinkingBlock(String thinking, String signature) {
        super("thinking");
        this.thinking = thinking;
        this.signature = signature;
    }

}
