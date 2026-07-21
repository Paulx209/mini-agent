package com.getian.cron;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *@Author: sonicge
 *@CreateTime: 2026-07-21
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CronJob {
    private String id;
    private String cron;
    private String prompt;
    private boolean recurring;// 一次性 or 循环
    private boolean durable; //是否持久化到文件中
}
