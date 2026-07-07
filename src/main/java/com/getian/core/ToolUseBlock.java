package com.getian.core;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 用来承载 Tool Use 的内容
 *@Author: sonicge
 *@CreateTime: 2026-07-05
 */
@Data
public class ToolUseBlock extends ContentBlock{
    private String id;
    //tool_name
    private String name;
    //tool_params
    private JSONObject input;

    public  ToolUseBlock() {
        super("tool_use");
    }
    public ToolUseBlock(String id,String name,JSONObject input){
        super("tool_use");
        this.id = id;
        this.name = name;
        this.input = input;
    }
}
