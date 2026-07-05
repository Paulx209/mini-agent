package com.getian.core;

/**
 * Resp中的Content是一个数组 包括不同类型的返回内容
 * @Author: sonicge
 * @CreateTime: 2026-07-05
 */

public abstract class ContentBlock {
    //类型
    private String type;

    protected ContentBlock() {}

    protected ContentBlock(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
