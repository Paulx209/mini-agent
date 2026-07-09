package com.getian.tool;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class WriteFileTool implements Tool {
    private final File fileDir;
    private final PathGuard pathGuard;

    public WriteFileTool(File fileDir) {
        this.fileDir = fileDir;
        this.pathGuard = new PathGuard(fileDir);
    }

    /**
     * "name": "write_file",
     * "description": "Write content to a UTF-8 file in the workdir",
     * "input_schema": {
     * "type": "object",
     * "properties": {
     * "path": {"type": "string", "description": "File path relative to the workdir"},
     * "content": {"type": "string", "description": "Content to write"}
     * },
     * "required": ["path", "content"]
     * }
     * }
     */
    @Override
    public ToolDefinition getDefinition() {
        String name = "write_file";
        String description = "Write content to a UTF-8 file in the workdir";

        JSONObject properties = new JSONObject()
                .fluentPut("path", new JSONObject()
                        .fluentPut("type", "string")
                        .fluentPut("description", "File path relative to the workdir"))
                .fluentPut("content", new JSONObject()
                        .fluentPut("type", "string")
                        .fluentPut("description", "Content to write"));

        JSONObject inputSchema = new JSONObject()
                .fluentPut("type", "object")
                .fluentPut("properties", properties)
                .fluentPut("required", new JSONArray()
                        .fluentAdd("path")
                        .fluentAdd("content"));

        return new ToolDefinition(name, description, inputSchema);
    }

    @Override
    public ToolResult execute(JSONObject input) {
        String path = input.getString("path");
        if (path == null || path.isBlank()) {
            return new ToolResult("Error: No path provided");
        }
        String content = input.getString("content");
        if (content == null) {
            return new ToolResult("Error: No content provided");
        }

        try {
            File finalPath = pathGuard.resolve(path);
            //如果在当前项目目录下的话 parent一般为null ; 不在的话，要确保这个目录存在 所以要先创建
            File parent = finalPath.getParentFile();
            if (parent != null) {
                Files.createDirectories(parent.toPath());
            }
            Files.writeString(finalPath.toPath(), content, StandardCharsets.UTF_8);
            return new ToolResult("Wrote " + content.getBytes(StandardCharsets.UTF_8).length + " bytes to " + path);
        } catch (IOException e) {
            return new ToolResult("Error: " + e.getMessage());
        }

    }
}
