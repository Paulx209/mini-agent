package com.getian.tool;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ToolRegistry {
    private final Map<String, Tool> tools = new LinkedHashMap<>();

    //register new tool
    public ToolRegistry registry(Tool tool) {
        tools.put(tool.getDefinition().getName(), tool);
        return this;
    }

    //find tool by name
    public Tool find(String name) {
        if (tools.containsKey(name)) {
            return tools.get(name);
        }
        return null;
    }

    public List<ToolDefinition> definitions() {
        return tools.values().stream()
                .map(Tool::getDefinition)
                .collect(Collectors.toList());
    }
}
