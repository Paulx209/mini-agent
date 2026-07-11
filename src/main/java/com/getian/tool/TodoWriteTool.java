package com.getian.tool;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 制定全局计划工具
 * s05 把“先列计划”做成一个普通工具，而不是写进 AgentLoop。
 * 工具只保存当前 todo 列表，不直接执行任何任务。
 *@Author: sonicge
 *@CreateTime: 2026-07-11
 */

public class TodoWriteTool implements Tool {
    private final List<TodoItem> currentTodos = new ArrayList<>();

    /**
     * {
     *   "name": "todo_write",
     *   "description": "Create or replace the current task list",
     *   "input_schema": {
     *     "type": "object",
     *     "properties": {
     *       "todos": {
     *         "type": "array",
     *         "items": {
     *           "type": "object",
     *           "properties": {
     *             "content": {"type": "string", "description": "Task content"},
     *             "status": {
     *               "type": "string",
     *               "enum": ["pending", "in_progress", "completed"],
     *               "description": "Task status"
     *             }
     *           },
     *           "required": ["content", "status"]
     *         },
     *         "description": "Full current todo list"
     *       }
     *     },
     *     "required": ["todos"]
     *   }
     * }
     */
    @Override
    public ToolDefinition getDefinition() {
        String name = "todo_write";
        String description = "Create or replace the current task list";
        JSONObject inputSchema = new JSONObject();
        JSONObject todos = new JSONObject()
                .fluentPut("type", "array")
                .fluentPut("items", new JSONObject()
                        .fluentPut("type", "object")
                        .fluentPut("properties", new JSONObject()
                                .fluentPut("content", new JSONObject()
                                        .fluentPut("type", "string")
                                        .fluentPut("description", "Task content"))
                                .fluentPut("status", new JSONObject()
                                        .fluentPut("type", "string")
                                        .fluentPut("enum", new JSONArray()
                                                .fluentAdd("pending")
                                                .fluentAdd("in_progress")
                                                .fluentAdd("completed"))
                                        .fluentPut("description", "Task status")))
                        .fluentPut("required", new JSONArray()
                                .fluentAdd("content")
                                .fluentAdd("status")))
                .fluentPut("description", "Full current todo list");
        inputSchema.fluentPut("type", "object")
                .fluentPut("properties", new JSONObject()
                        .fluentPut("todos", todos))
                .fluentPut("required", new JSONArray()
                        .fluentAdd("todos"));
        return new ToolDefinition(name, description, inputSchema);

    }

    @Override
    public ToolResult execute(JSONObject input) {
        JSONArray todos = todosArray(input);
        if (todos == null) return new ToolResult("Error: todos must be an array");
        List<TodoItem> newTools = new ArrayList<>();
        for (int i = 0; i < todos.size(); i++) {
            JSONObject todoItem = todos.getJSONObject(i);
            if (todoItem == null) {
                return new ToolResult("Error: todoItem must be an object");
            }
            String content = todoItem.getString("content");
            if (content == null || content.isBlank()) {
                return new ToolResult("Error : content is null");
            }
            String status = todoItem.getString("status");
            if (!"pending".equals(status) && !"in_progress".equals(status) && !"completed".equals(status)) {
                return new ToolResult("Error: status is invalid");
            }
            newTools.add(new TodoItem(content, status));
        }
        currentTodos.clear();
        currentTodos.addAll(newTools);
        return new ToolResult("Updated " + currentTodos.size() + " tasks\n" + render());
    }

    private String render() {
        if (currentTodos.isEmpty()) {
            return "(no todos)";
        }
        StringBuilder builder = new StringBuilder("Current tasks:");
        for (TodoItem todo : currentTodos) {
            builder.append("\n- [")
                    .append(todo.getStatus())
                    .append("] ")
                    .append(todo.getContent());
        }
        return builder.toString();
    }

    private JSONArray todosArray(JSONObject input) {
        if (input == null || !input.containsKey("todos")) {
            return null;
        }
        Object todos = input.get("todos");
        if (todos instanceof JSONArray) {
            return (JSONArray) todos;
        } else if (todos instanceof String) {
            try {
                return JSON.parseArray((String) todos);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
