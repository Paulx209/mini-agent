package com.getian.mcp;

import java.util.regex.Pattern;

/**
 *@Author: sonicge
 *@CreateTime: 2026-08-01
 * get.current.weather -> get_current_weather
 * mcp__weather__get_current_weather
 */
public class McpToolName {
    //[^a-zA-z0-9_-] ^取反 匹配所有不是大小写字母 数字 - _ 的字符
    private static final Pattern DISALLOWED = Pattern.compile("[^a-zA-Z0-9_-]");

    public static String prefixed(String mcpServerName,String toolName){
        return "mcp__" + normalize(mcpServerName) + "__" +normalize(toolName);
    }

    private static String normalize(String name){
        if(name == null || name.isBlank()){
            return "unnamed";
        }
        return DISALLOWED.matcher(name).replaceAll("_");
    }
}
