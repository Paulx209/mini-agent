package com.getian.tool;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.util.prefs.PreferenceChangeEvent;

/**
 *@Author: sonicge
 *@CreateTime: 2026-07-05
 */

public class BashTool implements Tool {
    private final File workdir;

    public BashTool(File workdir) {
        this.workdir = workdir;
    }

    /**
     * {
     *   "name": "bash",
     *   "description": "Run a shell command and return stdout/stderr",
     *   "input_schema": {
     *     "type": "object",
     *     "properties": {
     *       "command": {"type": "string", "description": "Shell command to run"}
     *     },
     *     "required": ["command"]
     *   }
     * }
     */
    @Override
    public ToolDefinition getDefinition() {
        String name = "bash";
        String description = "Run a shell command and return stdout/stderr";
        JSONObject properties = new JSONObject()
                .fluentPut("command", new JSONObject()
                        .fluentPut("type", "string")
                        .fluentPut("description", "Shell command to run "));
        JSONObject inputSchema = new JSONObject()
                .fluentPut("type", "object")
                .fluentPut("properties", properties)
                .fluentPut("required", new JSONArray().fluentAdd("command"));
        return new ToolDefinition(name, description, inputSchema);
    }

    @Override
    public ToolResult execute(JSONObject input) {
        String command = input == null ? "" : input.getString("command");
        if (command == null || command.isEmpty()) {
            return new ToolResult("No command provided");
        }

        try {
            //通过ProcessBuilder启动一个本地Shell子进程 执行自定义命令  sh(sh 解释器) -c command
            //mac -> sh ; windows -> cmd/c
            Process process = new ProcessBuilder("cmd", "/c", command)
                    .directory(workdir) //执行命令的工作目录  一般是项目目录
                    .redirectErrorStream(true) //将错误标准流合并到标准输出流
                    .start();
            String resp = readOutput(process);
            //阻塞等待返回exitCode (0 | >0 )
            int exitCode = process.waitFor();
            return new ToolResult("exit_code = " + exitCode + "\n" + resp);
        } catch (IOException e) {
            return new ToolResult("Command failed to start: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ToolResult("Command interrupted");
        }
    }

    private String readOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        //字节 -> 字符流
        InputStreamReader isr = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8);
        //字符 -> 字符缓冲流
        try (BufferedReader bufferedReader = new BufferedReader(isr)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                output.append(line).append('\n');
            }
        }
        return output.toString();
    }
}
