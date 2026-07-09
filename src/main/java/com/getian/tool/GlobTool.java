package com.getian.tool;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class GlobTool implements Tool {
    private final File workDir;

    public GlobTool(File workDir) {
        this.workDir = workDir;
    }

    /*
     * {
     *   "name": "glob",
     *   "description": "Find files matching a glob pattern in the workdir",
     *   "input_schema": {
     *     "type": "object",
     *     "properties": {
     *       "pattern": {"type": "string", "description": "Glob pattern relative to the workdir"}
     *     },
     *     "required": ["pattern"]
     *   }
     * }
     */
    @Override
    public ToolDefinition getDefinition() {
        JSONObject properties = new JSONObject()
                .fluentPut("pattern", new JSONObject()
                        .fluentPut("type", "string")
                        .fluentPut("description", "Glob pattern relative to the workdir"));
        JSONObject schema = new JSONObject()
                .fluentPut("type", "object")
                .fluentPut("properties", properties)
                .fluentPut("required", new JSONArray().fluentAdd("pattern"));
        return new ToolDefinition("glob", "Find files matching a glob pattern in the workdir", schema);
    }

    @Override
    public ToolResult execute(JSONObject input) {
        String pattern = input == null ? "" : input.getString("pattern");
        if (pattern == null || pattern.isBlank()) {
            return new ToolResult("Error: No pattern provided");
        }
        try {
            Path root = workDir.getCanonicalFile().toPath();
            PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
            //matches只用来收集对应的文件名
            List<String> matches = new ArrayList<>();
            //搜索整个项目库中的文件 —— 是否有必要做剪枝操作
            try (Stream<Path> stream = Files.walk(root)) {
                stream.filter(Files::isRegularFile)
                        .map(root::relativize)
                        .filter(pathMatcher::matches)
                        .map(Path::toString)
                        .sorted()
                        .forEach(matches::add);
            }
            return new ToolResult(matches.isEmpty() ? "(no matches)" : String.join("\n", matches));
        } catch (IOException e) {
            return new ToolResult("Error: " + e.getMessage());
        }
    }
}
