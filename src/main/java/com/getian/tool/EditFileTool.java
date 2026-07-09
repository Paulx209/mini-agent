package com.getian.tool;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditFileTool implements Tool {
    private File workDir;
    private final PathGuard pathGuard;

    public EditFileTool(File workDir) {
        this.pathGuard = new PathGuard(workDir);
    }

    /**
     * {
     * "name": "edit_file",
     * "description": "Replace exact text in a file once",
     * "input_schema": {
     * "type": "object",
     * "properties": {
     * "path": {"type": "string", "description": "File path relative to the workdir"},
     * "old_text": {"type": "string", "description": "Exact text to replace once"},
     * "new_text": {"type": "string", "description": "Replacement text"}
     * },
     * "required": ["path", "old_text", "new_text"]
     * }
     * }
     */
    @Override
    public ToolDefinition getDefinition() {
        String name = "edit_file";
        String description = "Replace exact text in a file once";
        JSONObject properties = new JSONObject()
                .fluentPut("path", new JSONObject()
                        .fluentPut("type", "string")
                        .fluentPut("description", "File path relative to the workdir"))
                .fluentPut("old_text", new JSONObject()
                        .fluentPut("type", "string")
                        .fluentPut("description", "Exact text to replace once"))
                .fluentPut("new_text", new JSONObject()
                        .fluentPut("type", "string")
                        .fluentPut("description", "Replacement text"));
        JSONObject inputSchema = new JSONObject()
                .fluentPut("type", "object")
                .fluentPut("properties", properties)
                .fluentPut("required", new JSONArray().
                        fluentAdd("path")
                        .fluentAdd("old_text")
                        .fluentAdd("new_text"));
        return new ToolDefinition(name, description, inputSchema);
    }

    @Override
    public ToolResult execute(JSONObject input) {
        String path = input == null ? "" : input.getString("path");
        String oldText = input == null ? null : input.getString("old_text");
        String newText = input == null ? null : input.getString("new_text");
        if (path == null || path.isBlank()) {
            return new ToolResult("Error: No path provided");
        }
        if (oldText == null || newText == null) {
            return new ToolResult("Error: old_text and new_text are required");
        }

        try {
            File file = pathGuard.resolve(path);
            String text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            if (!text.contains(oldText)) {
                return new ToolResult("Error: text not found in " + path);
            }
            //开始匹配替换 只替代第一次出现的
            String context = text.replaceFirst(Pattern.quote(oldText), Matcher.quoteReplacement(newText));
            Files.writeString(file.toPath(),context,StandardCharsets.UTF_8);
            return new ToolResult("Edited " + path);
        } catch (IOException e) {
            return new ToolResult("Error: " + e.getMessage());
        }
    }
}
