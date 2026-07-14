package com.getian.tool;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.getian.skill.Skill;
import com.getian.skill.SkillRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * 一次性加载多个Skill
 */
public class LoadSkillsTool implements Tool {
    private final SkillRegistry skillRegistry;

    public LoadSkillsTool(SkillRegistry skillRegistry) {
        this.skillRegistry = skillRegistry;
    }

    /**
     * {
     * "name": "load_skills",
     * "description": "Load the full body specifications and constraints of one or more specialized skills by their registered names. Call this tool to inject specific coding guidelines, UI standards, or workflow checklists into your context before performing tasks.",
     * "input_schema": {
     * "type": "object",
     * "properties": {
     * "names": {
     * "type": "array",
     * "items": {
     * "type": "string",
     * "description": "The exact registered name of a single skill."
     * },
     * "description": "List of skill names to load. You can request multiple skills in a single call to handle complex tasks efficiently."
     * }
     * },
     * "required": ["names"]
     * }
     * }
     */
    @Override
    public ToolDefinition getDefinition() {
        String name = "load_skills";
        String description = "Load the full body specifications and constraints of one or more specialized skills by their registered names. Call this tool to inject specific coding guidelines, UI standards, or workflow checklists into your context before performing tasks.";
        JSONObject inputSchema = new JSONObject()
                .fluentPut("type", "object")
                .fluentPut("properties", new JSONObject()
                        .fluentPut("names", new JSONObject()
                                .fluentPut("type", "array")
                                .fluentPut("items", new JSONObject()
                                        .fluentPut("type", "string")
                                        .fluentPut("minLength", 1)
                                        .fluentPut("description", "The exact registered name of a single skill."))
                                .fluentPut("minItems", 1)
                                .fluentPut("uniqueItems", true)
                                .fluentPut("description", "List of skill names to load. You can request multiple skills in a single call to handle complex tasks efficiently.")))
                .fluentPut("required", new JSONArray().fluentAdd("names"))
                .fluentPut("additionalProperties", false);

        return new ToolDefinition(name, description, inputSchema);
    }

    @Override
    public ToolResult execute(JSONObject input) {
        JSONArray names = input == null ? null : input.getJSONArray("names");
        if (names == null || names.isEmpty()) {
            return new ToolResult("Error: No skill names provided");
        }

        List<Skill> skills = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            Object value = names.get(i);
            if (!(value instanceof String) || ((String) value).isBlank()) {
                return new ToolResult("Error: Skill name at index " + i + " must be a non-blank string");
            }

            String skillName = ((String) value).trim();
            Skill skill = skillRegistry.find(skillName);
            if (skill == null) {
                return new ToolResult("Skill not found: " + skillName);
            }
            skills.add(skill);
        }

        StringBuilder result = new StringBuilder();
        for (Skill skill : skills) {
            if (result.length() > 0) {
                result.append("\n\n");
            }

            result.append("<skill name=\"")
                    .append(skill.getName())
                    .append("\">\n")
                    .append(skill.getBody())
                    .append("\n</skill>");
        }

        return new ToolResult(result.toString());
    }
}
