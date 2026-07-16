package com.getian.memory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *@Author: sonicge
 *@CreateTime: 2026-07-16
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Memory {
    private String filename;
    private String name;
    private String description;
    private String body;
    private String type;
}
