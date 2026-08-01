package com.getian.mcp;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *@Author: sonicge
 *@CreateTime: 2026-08-01
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class McpToolDefinition {
    private String name;
    private String description;
    private JSONObject inputSchema;
    private boolean readonly;
}
