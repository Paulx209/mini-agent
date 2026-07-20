package com.getian.background;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *@Author: sonicge
 *@CreateTime: 2026-07-20
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BackgroundTask {
    private String bgTaskId;
    private String toolId;
    private String command;
    private String status;  //running completed timeout error
}
