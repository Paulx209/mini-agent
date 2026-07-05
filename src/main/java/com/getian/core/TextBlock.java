package com.getian.core;

import lombok.Data;

/**
 *@Author: sonicge
 *@CreateTime: 2026-07-05
 */

@Data
public class TextBlock extends ContentBlock {
    private String text;

    public TextBlock() {
        super("text");
    }

    public TextBlock(String text) {
        super("text");
        this.text = text;
    }
}
