package com.getian.tool;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 *@Author: sonicge
 *@CreateTime: 2026-07-05
 */

@Data
@AllArgsConstructor
public class ToolDefinition {
    private String name;
    private String description;
    //执行Tool时，需要传入哪些参数 | 参数的类型是什么
    private JSONObject inputSchema;
}
