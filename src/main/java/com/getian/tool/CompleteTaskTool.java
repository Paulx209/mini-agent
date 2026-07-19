package com.getian.tool;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.getian.task.TaskService;

public class CompleteTaskTool implements Tool {
    private final TaskService taskService;

    public CompleteTaskTool(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * {
     *   "name": "complete_task",
     *   "description": "Complete an in-progress task. Reports unblocked downstream tasks.",
     *   "input_schema": {
     *     "type": "object",
     *     "properties": {
     *       "task_id": {"type": "string"}
     *     },
     *     "required": ["task_id"]
     *   }
     * }
     */
    @Override
    public ToolDefinition getDefinition() {
        JSONObject properties = new JSONObject()
                .fluentPut("task_id", new JSONObject().fluentPut("type", "string"));
        JSONObject schema = new JSONObject()
                .fluentPut("type", "object")
                .fluentPut("properties", properties)
                .fluentPut("required", new JSONArray().fluentAdd("task_id"));
        return new ToolDefinition("complete_task",
                "Complete an in-progress task. Reports unblocked downstream tasks.",
                schema);
    }

    @Override
    public ToolResult execute(JSONObject input) {
        try {
            String taskId = input == null ? null : input.getString("task_id");
            return new ToolResult(taskService.completedTask(taskId));
        } catch (Exception e) {
            return  new ToolResult("Error:"+e.getMessage());
        }
    }
}
