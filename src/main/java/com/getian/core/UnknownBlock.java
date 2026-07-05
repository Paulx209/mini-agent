package com.getian.core;

import com.alibaba.fastjson.JSONObject;;
import lombok.Data;

/**
 * 兼容未来或非标准 content block，避免解析时直接丢掉未知数据。
 *@Author: sonicge
 *@CreateTime: 2026-07-05
 */
@Data
public class UnknownBlock extends ContentBlock {
    private JSONObject raw;

    protected UnknownBlock(String type, JSONObject raw) {
        super(type);
        this.raw = raw;
    }
}
