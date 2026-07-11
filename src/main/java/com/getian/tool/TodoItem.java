package com.getian.tool;

import lombok.Data;

/**
 *@Author: sonicge
 *@CreateTime: 2026-07-11
 */

@Data
public class TodoItem {
    private String content;
    private String status;
    public TodoItem(String content,String status){
        this.content = content;
        this.status = status;
    }
}
