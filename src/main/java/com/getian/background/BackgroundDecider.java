package com.getian.background;

import com.alibaba.fastjson.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 *@Author: sonicge
 *@CreateTime: 2026-07-20
 */

public class BackgroundDecider {
    private static final String[] SLOW_KEYWORDS = {
            "install", "build", "test", "deploy", "compile",
            "docker build", "pip install", "npm install",
            "cargo build", "pytest", "make"
    };

    public static boolean isRunBackGround(String toolName, JSONObject inputSchema) {
        //第一层：从大模型返回的inputSchema中判断
        if (toolName != null && !toolName.isBlank() && !"bash".equals(toolName)){
            return true;
        }
        if(inputSchema !=null && inputSchema.getBoolean("run_in_background") !=null && inputSchema.getBoolean("run_in_background")){
            return true;
        }
        //第二层，兜底逻辑
        return isSlowAction(toolName,inputSchema);
    }

    /**
     * 判断要执行的命令类型是否在 slow_keywords 数组中
     */
    private static boolean isSlowAction(String toolName,JSONObject inputSchema){
        String command = inputSchema.getString("command");
        if(command == null || command.isBlank()){
            return false;
        }
        List<String> list = Arrays.stream(SLOW_KEYWORDS).collect(Collectors.toList());
        return list.contains(command);
    }
}
