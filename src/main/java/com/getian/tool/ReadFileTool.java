package com.getian.tool;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.*;

public class ReadFileTool implements Tool {
    private final File workDir;

    private final PathGuard pathGuard;

    public ReadFileTool(File workDir) {
        this.workDir = workDir;
        this.pathGuard = new PathGuard(workDir);
    }

    /**
     * {
     * "name": "read_file",
     * "description": "Read a UTF-8 text file from the workdir",
     * "input_schema": {
     * "type": "object",
     * "properties": {
     * "path": {"type": "string", "description": "File path relative to the workdir"},
     * "limit": {"type": "integer", "description": "Optional max number of lines to return"}
     * },
     * "required": ["path"]
     * }
     * }
     */
    @Override
    public ToolDefinition getDefinition() {
        String name = "read_file";
        String description = "Read a UTF-8 text file from the workdir";
        JSONObject properties = new JSONObject()
                .fluentPut("path", new JSONObject()
                        .fluentPut("type", "string")
                        .fluentPut("description", "File path relative to the workdir"))
                .fluentPut("limit", new JSONObject()
                        .fluentPut("type", "integer")
                        .fluentPut("description", "Optional max number of lines to return"));
        JSONObject inputSchema = new JSONObject()
                .fluentPut("type", "object")
                .fluentPut("properties", properties)
                .fluentPut("required", new JSONArray().fluentAdd("path"));
        return new ToolDefinition(name, description, inputSchema);
    }

    @Override
    public ToolResult execute(JSONObject input) {
        String path = input.getString("path");
        if (path == null || path.isEmpty()) {
            return new ToolResult("Error: No path provided");
        }
        Integer limitLines = input.getInteger("limit");
        StringBuilder content = new StringBuilder();
        int lineCount = 0;
        try {
            File finalPath = pathGuard.resolve(path);
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(finalPath)));
            String line;
            while ((line = reader.readLine()) != null) {
                if (limitLines != null && limitLines > 0 && lineCount > limitLines) {
                    content.append("... output truncated by limit=").append(limitLines);
                    break;
                }
                content.append(line).append("\n");
                lineCount++;
            }
        } catch (IOException e) {
            return new ToolResult("Error: " + e.getMessage());
        }
        return new ToolResult(content.toString());
    }
}
