package com.getian.skill;

import lombok.Data;

/**
 *@Author: sonicge
 *@CreateTime: 2026-07-14
 */
@Data
public class Skill {
    private String name;
    private String description;
    private String body;

    public Skill(String name, String description, String body) {
        this.name = name;
        this.description = description;
        this.body = body;
    }
}
