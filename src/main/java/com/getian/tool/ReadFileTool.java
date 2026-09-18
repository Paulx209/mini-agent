package com.getian.tool;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.*;

public class ReadFileTool implements Tool {
    //单次返回的硬上限（字符数，非字节），模型无法通过参数突破这个天花板。
    private static final int MAX_CHARS = 4000;
    private static final int DEFAULT_OFFSET = 1;

    private final PathGuard pathGuard;

    public ReadFileTool(File workDir) {
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
     * "offset": {"type": "integer", "description": "1-based line number to start reading from"},
     * "limit": {"type": "integer", "description": "Optional max number of lines to return"}
     * },
     * "required": ["path"]
     * }
     * }
     */
    @Override
    public ToolDefinition getDefinition() {
        String name = "read_file";
        String description = "Read a UTF-8 text file from the workdir. Each call returns at most "
                + MAX_CHARS + " characters; for longer files continue with the 'next offset' "
                + "reported in the truncation notice.";
        JSONObject properties = new JSONObject()
                .fluentPut("path", new JSONObject()
                        .fluentPut("type", "string")
                        .fluentPut("description", "File path relative to the workdir"))
                .fluentPut("offset", new JSONObject()
                        .fluentPut("type", "integer")
                        .fluentPut("description", "1-based line number to start reading from (default 1). "
                                + "Pass the 'next offset' from a truncated result to keep reading."))
                .fluentPut("limit", new JSONObject()
                        .fluentPut("type", "integer")
                        .fluentPut("description", "Optional max number of lines to return "
                                + "(default: until the " + MAX_CHARS + "-character cap)"));
        JSONObject inputSchema = new JSONObject()
                .fluentPut("type", "object")
                .fluentPut("properties", properties)
                .fluentPut("required", new JSONArray().fluentAdd("path"));
        return new ToolDefinition(name, description, inputSchema);
    }

    @Override
    public ToolResult execute(JSONObject input) {
        String path = input == null ? "" : input.getString("path");
        if (path == null || path.isBlank()) {
            return new ToolResult("Error: No path provided");
        }
        int offsetLines = readInt(input, "offset", DEFAULT_OFFSET, 1);
        int limitLines = readInt(input, "limit", 0, 0); // 0 表示不限行数，由 MAX_CHARS 兜底

        StringBuilder content = new StringBuilder();
        int currentLine = 0;       // 文件内的 1-based 行号
        int shownLines = 0;        // 已写入的行数（供 limit 判断）
        int lastLine = offsetLines - 1; // 最后一次写入内容对应的行号
        boolean cutMidLine = false;
        boolean truncated = false;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(pathGuard.resolve(path))))) {
            String line;
            while ((line = reader.readLine()) != null) {
                //遍历到offset行
                currentLine++;
                if (currentLine < offsetLines) {
                    continue;
                }
                //1.行数：已经遍历的行数 大于 限制的行数 -> 停止
                if (limitLines > 0 && shownLines >= limitLines) {
                    truncated = true;
                    break;
                }
                //2.字符：如果拼接的字符 > 4000 -> 停止
                int remainingSpace = MAX_CHARS - content.length();
                if (remainingSpace <= 0) {
                    truncated = true;
                    break;
                }
                if (line.length() >= remainingSpace) {
                    //单行长度如果>剩余额度，只需要拼接剩余额度的字符
                    content.append(line, 0, remainingSpace);
                    cutMidLine = true;
                    truncated = true;
                    lastLine = currentLine;
                    break;
                }
                content.append(line).append('\n');
                shownLines++;
                lastLine = currentLine;
            }
        } catch (IOException e) {
            return new ToolResult("Error: " + e.getMessage());
        }

        if (truncated) {
            content.append("\n...[truncated: showing lines ").append(offsetLines).append('-').append(lastLine);
            if (cutMidLine) {
                content.append(" (line ").append(lastLine).append(" cut mid-line)");
            }
            content.append("; next offset=").append(lastLine + 1).append(']');
        }
        if (content.length() == 0) {
            return new ToolResult(offsetLines > DEFAULT_OFFSET
                    ? "Error: offset " + offsetLines + " is past the end of the file"
                    : "");
        }
        return new ToolResult(content.toString());
    }

    /**
     * 读取整型参数，为null时使用defaultValue兜底，小于 min 时返回 min
     */
    private int readInt(JSONObject input, String key, int defaultValue, int min) {
        if (input == null) {
            return defaultValue;
        }
        Integer value = input.getInteger(key);
        return value == null ? defaultValue : Math.max(min, value);
    }
}
