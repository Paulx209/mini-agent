package com.getian.compact;

import com.alibaba.fastjson.JSON;
import com.getian.core.Message;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * 上下文压缩之前，将上下文存储到对应的文件中。防止压缩后丢失一些关键信息
 */
public class TranscriptStore {
    private final File transcriptDir;

    public TranscriptStore(File workDir) {
        this.transcriptDir = new File(workDir,".transcripts");
    }

    /**
     * 将上下文内容写到磁盘 [压缩之前执行]
     */
    public File write(List<Message> messageList) {
        if (!transcriptDir.exists() && !transcriptDir.mkdirs()) {
            throw new IllegalStateException("Failed to create directory: " + transcriptDir);
        }
        if(!transcriptDir.isDirectory()){
            throw new IllegalStateException("Not a directory : " + transcriptDir);
        }
        File file = new File(transcriptDir, "transcript_" + System.currentTimeMillis() + ".jsonl");
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            for (Message message : messageList) {
                bufferedWriter.write(JSON.toJSONString(message.getContent()));
                bufferedWriter.newLine();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write transcript: " + file, e);
        }
        System.out.println("[transcript saved: " + file.getPath() + "]");
        return file;
    }
}
