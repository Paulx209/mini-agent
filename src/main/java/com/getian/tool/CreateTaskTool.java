package com.getian.tool;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.getian.task.TaskRecord;
import com.getian.task.TaskService;

import java.util.ArrayList;
import java.util.List;

public class CreateTaskTool implements Tool {
    private final TaskService taskService;

    public CreateTaskTool(TaskService taskService) {
        this.taskService = taskService;
    }

    /*
     * {
     *   "name": "create_task",
     *   "description": "Create a new task with optional blockedBy dependencies.",
     *   "input_schema": {
     *     "type": "object",
     *     "properties": {
     *       "subject": {"type": "string"},
     *       "description": {"type": "string"},
     *       "blockedBy": {"type": "array", "items": {"type": "string"}}
     *     },
     *     "required": ["subject"]
     *   }
     * }
     */
    @Override
    public ToolDefinition getDefinition() {
        JSONObject properties = new JSONObject()
                .fluentPut("subject", new JSONObject().fluentPut("type", "string"))
                .fluentPut("description", new JSONObject().fluentPut("type", "string"))
                .fluentPut("blockedBy", new JSONObject()
                        .fluentPut("type", "array")
                        .fluentPut("items", new JSONObject().fluentPut("type", "string")));
        JSONObject schema = new JSONObject()
                .fluentPut("type", "object")
                .fluentPut("properties", properties)
                .fluentPut("required", new JSONArray().fluentAdd("subject"));
        return new ToolDefinition("create_task",
                "Create a new task with optional blockedBy dependencies.",
                schema);
    }

    @Override
    public ToolResult execute(JSONObject input) {
        try {
            String subject = input == null ? null : input.getString("subject");
            String description = input == null ? null : input.getString("description");
            JSONArray blockedBy = input == null ? null : input.getJSONArray("blockedBy");
            TaskRecord task = taskService.createTask(subject, description, toStringList(blockedBy));
            String deps = task.getBlockedBy().isEmpty() ? "" : " (blockedBy: " + String.join(", ", task.getBlockedBy()) + ")";
            return new ToolResult("Created " + task.getId() + ": " + task.getSubject() + deps);
        } catch (Exception e) {
            return new ToolResult("Error: " + e.getMessage());
        }

    }

    private List<String> toStringList(JSONArray array) {
        List<String> taskIds = new ArrayList<>();
        if (array == null) {
            return taskIds;
        }
        for (int i = 0; i < array.size(); i++) {
            String taskId = array.getString(i);
            taskIds.add(taskId);
        }
        return taskIds;
    }

}
