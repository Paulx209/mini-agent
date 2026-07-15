package com.getian.compact;

import com.getian.core.ToolResultBlock;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

//当tool_result内容超级大时，将内容写到文件中，只需要在上下文中声明对应的文件路径即可
//索引目录的写法
public class ToolResultStore {
    private static final int PREVIEW_CHARS = 2000;

    private final File outputDir;

    public ToolResultStore(File workDir){
        this.outputDir = new File(workDir,".task_outputs/tool-results");
    }

    public String persist(ToolResultBlock block){
        //check directory exist
        if(!outputDir.exists() && !outputDir.mkdirs()){
            throw new IllegalStateException("Failed to create directory: " + outputDir);
        }
        if(!outputDir.isDirectory()){
            throw new IllegalStateException("Not a directory : " + outputDir);
        }
        String safeId = safeId(block.getToolUseId());
        File file = new File(outputDir,safeId + ".txt");
        try {
            Files.writeString(file.toPath(),block.getContent(), StandardCharsets.UTF_8);
            return "<persisted-output>\n"
                    + "Full output: " + relativePath(file) + "\n"
                    + "Preview:\n"
                    + preview(block.getContent())
                    + "\n</persisted-output>";
        } catch (IOException e) {
            throw new IllegalStateException("Failed to persist tool result: " + file, e);
        }
    }

    private String relativePath(File file) {
        return ".task_outputs/tool-results/" + file.getName();
    }

    /**
     *  对文件内容进行截断 (2000)
     */
    private String preview(String content){
        if(content.length() < PREVIEW_CHARS){
            return content;
        }
        return content.substring(0,PREVIEW_CHARS) + "\n...[truncated preview]";
    }

    //将非大小写字母,数字, _, -,等符号的都替换成 _
    private String safeId(String id) {
        if (id == null || id.isBlank()) {
            return "tool_result";
        }
        return id.replaceAll("[^A-Za-z0-9_-]", "_");
    }
}
