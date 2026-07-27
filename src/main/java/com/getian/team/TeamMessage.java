package com.getian.team;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 *@Author: sonicge
 *@CreateTime: 2026-07-27
 */

@Data
@AllArgsConstructor
public class TeamMessage {
    private String from; //发送者
    private String to; //接收者
    private String content;
    private String type;
    private long timestamp;
}
