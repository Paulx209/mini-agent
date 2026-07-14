package com.getian.tool;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.getian.skill.Skill;
import com.getian.skill.SkillRegistry;

public class LoadSkillTool implements Tool {
    private SkillRegistry skillRegistry;
    public LoadSkillTool(SkillRegistry registry){
        this.skillRegistry = registry;
    }

    /**
     *   "name": "load_skill",
     *   "description": "Load specialized knowledge by name.",
     *   "input_schema": {
     *     "type": "object",
     *     "properties": {
     *       "name": {"type": "string", "description": "Skill name to load"}
     *     },
     *     "required": ["name"]
     *   }
     */
    @Override
    public ToolDefinition getDefinition() {
        String name = "load_skill";
        String description = "Load specialized knowledge by name";
        JSONObject inputSchema = new JSONObject()
                .fluentPut("type", "object")
                .fluentPut("properties", new JSONObject()
                        .fluentPut("name", new JSONObject()
                                .fluentPut("type", "string")
                                .fluentPut("description", "Skill name to load")))
                .fluentPut("required", new JSONArray().fluentAdd("name"));

        return new ToolDefinition(name,description,inputSchema);
    }

    @Override
    public ToolResult execute(JSONObject input) {
        String skillName = input == null ? "" : input.getString("name");
        if(skillName == null || skillName.isBlank()){
            return new ToolResult("Error: No skill name provided");
        }
        Skill skill = skillRegistry.find(skillName);
        if(skill == null){
            return new ToolResult("Skill not found: " + skillName);
        }
        // 用显式标签包住正文，让模型能区分“工具返回的技能内容”和普通对话文本。
        return new ToolResult("<skill name=\"" + skill.getName() + "\">\n"
                + skill.getBody() + "\n</skill>");
    }
}
