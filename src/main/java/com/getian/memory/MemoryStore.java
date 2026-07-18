package com.getian.memory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MemoryStore {
    //memory文件夹路径
    private final File memoryDir;

    public MemoryStore(File workDir) {
        this.memoryDir = new File(workDir, ".memory");
    }

    public File getMemoryDir() {
        return this.memoryDir;
    }

    /**
     * 从memory文件夹中解析所有的Memory
     */
    public List<Memory> list() {
        List<Memory> memoryList = new ArrayList<>();
        File[] files = memoryDir.listFiles(file -> file.isFile()
                && file.getName().endsWith(".md")
                && !"MEMORY.md".equals(file.getName()));
        Arrays.sort(files);
        for (File file : files) {
            try {
                //真正的解析逻辑 -> parse
                memoryList.add(parse(file));
            } catch (Exception e) {
                System.out.println("Skip memory " + file.getName() + ": " + e.getMessage());
            }
        }
        return memoryList;
    }

    public Memory findByFileName(String filename) {
        if (filename == null || filename.isBlank() || filename.contains("..") || filename.contains("/")) {
            return null;
        }
        File file = new File(memoryDir, filename);
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        try {
            return parse(file);
        } catch (IOException e) {
            return null;
        }
    }

    public Memory write(Memory memory) {
        if (memory == null) {
            return null;
        }
        String name = memory.getName();
        String filename = uniqueFilename(name);
        File writeFile = new File(memoryDir, filename);
        if (!writeFile.exists() || !writeFile.isFile()) {
            return null;
        }
        try {
            Files.writeString(writeFile.toPath(), render(memory), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write memory: " + writeFile, e);
        }
        //重置索引
        rebuildIndex();
        return memory;
    }

    /**
     * memory有变动 重置索引
     */
    public void rebuildIndex() {
        ensureMemoryDir();
        List<Memory> list = list();
        StringBuilder sb = new StringBuilder();
        for (Memory memory : list) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append("- [")
                    .append(memory.getName())
                    .append("](")
                    .append(memory.getFilename())
                    .append(") - ")
                    .append(memory.getDescription());
        }
        try {
            Files.writeString(new File(memoryDir, "MEMORY.md").toPath(), sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write MEMORY.md", e);
        }
    }

    public void deleteMemoryFiles(){
        ensureMemoryDir();
        File[] files = memoryDir.listFiles(file -> file.isFile()
                && file.getName().endsWith("md")
                && !"MEMORY.md".equals(file.getName()));
        if(files == null || files.length == 0){
            return ;
        }
        for(File file : files){
            if (!file.delete()) {
                throw new IllegalStateException("Failed to delete memory: " + file);
            }
        }
        rebuildIndex();
    }

    public String indexContent(){
        File memoryIndex = new File(memoryDir,"MEMORY.md");
        if(!memoryIndex.exists() || !memoryIndex.isFile()){
            return "(no memories yet)";
        }
        try {
            String content = Files.readString(memoryIndex.toPath(), StandardCharsets.UTF_8);
            return content.isBlank() ? "(no memories yet)" : content;
        } catch (IOException e) {
            return "(failed to read memory index: " + e.getMessage() + ")";
        }
    }


    private void ensureMemoryDir() {
        if (!memoryDir.exists() && !memoryDir.mkdirs()) {
            throw new IllegalStateException("Failed to create directory: " + memoryDir);
        }
    }

    /**
     * 用于writeFile
     * 将Memory写入file文件中
     */
    private String render(Memory memory) {
        return "---\n"
                + "name: " + nullToEmpty(memory.getName()) + "\n"
                + "description: " + nullToEmpty(memory.getDescription()) + "\n"
                + "type: " + safeType(memory.getType()) + "\n"
                + "---\n\n"
                + nullToEmpty(memory.getBody()).trim()
                + "\n";
    }

    private String nullToEmpty(String str) {
        return str == null ? "" : str;
    }

    private String uniqueFilename(String slug) {
        String name = slug == null || slug.isBlank() ? "memory-" + System.currentTimeMillis() : slug;
        String filename = name + ".md";
        int index = 2;
        while (new File(filename).exists()) {
            filename = name + "-" + index + ".md";
            index++;
        }
        return filename;
    }

    /**
     * 将xxx.md文件解析成Memory 用于list()函数
     */
    private Memory parse(File file) throws IOException {
        String raw = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        String filename = file.getName();
        String name = getFileNameWithoutSuffix(filename);
        String description = "";
        String type = "user";
        String body = raw;
        if (raw.startsWith("---")) {
            String[] contextArr = raw.split("---", 3);
            String[] summary = contextArr[1].split("\\R");
            for (String line : summary) {
                if (line.trim().startsWith("name")) {
                    name = line.substring("name:".length()).trim();
                }
                if (line.trim().startsWith("description")) {
                    description = line.substring("description:".length()).trim();
                }
                if (line.trim().startsWith("type")) {
                    type = line.substring("type:".length()).trim();
                    type = safeType(type);
                }
            }
            body = contextArr[2];
        }
        return new Memory(filename, name, description, type, body);
    }


    public String getFileNameWithoutSuffix(String filename) {
        if (filename.endsWith(".md")) {
            return filename.substring(0, filename.length() - 3);
        }
        return filename;
    }

    public String safeType(String type) {
        if ("feedback".equals(type) || "project".equals(type) || "reference".equals(type)) {
            return type;
        }
        return "user";
    }
}
