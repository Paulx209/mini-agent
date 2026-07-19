package com.getian.tool;

import com.alibaba.fastjson.JSONObject;
import com.getian.task.TaskService;

public class ListTaskTool implements Tool {
    private final TaskService taskService;

    public ListTaskTool(TaskService taskService) {
        this.taskService = taskService;
    }

    /*
     * {
     *   "name": "list_tasks",
     *   "description": "List all tasks with status, owner, and dependencies.",
     *   "input_schema": {"type": "object", "properties": {}}
     * }
     */
    @Override
    public ToolDefinition getDefinition() {
        JSONObject schema = new JSONObject()
                .fluentPut("type", "object")
                .fluentPut("properties", new JSONObject());
        return new ToolDefinition("list_tasks",
                "List all tasks with status, owner, and dependencies.",
                schema);
    }

    @Override
    public ToolResult execute(JSONObject input) {
        return new ToolResult(taskService.listTasks());
    }
}
