package com.getian.team;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson.JSON;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 *@Author: sonicge
 *@CreateTime: 2026-07-27
 */

public class MessageBus {
    private final File mailboxDir;
    public MessageBus(File workDir){
        this.mailboxDir = new File(workDir,".mailboxes");
    }
    public synchronized void send(String from, String to, String content) throws Exception {
        send(from, to, content, "message");
    }
    /**
     * 往mailBox中发送消息
     */
    public synchronized void send(String from, String to, String content, String type) throws Exception {
        try {
            TeamMessage message = new TeamMessage(from, to, content, type, System.currentTimeMillis());
            File file = mailboxFile(to);
            FileUtil.appendUtf8String(JSON.toJSONString(message) + "\n", file);
            System.out.println("  [bus] " + from + " -> " + to + ": " + preview(content));
        } catch (Exception e) {
            System.out.println("  [teammate] failed to send .  error: " + e.getMessage());
        }
    }

    /**
     * 从mailboxes中读取信息
     */
    public synchronized List<TeamMessage> read(String agentName) {
        //1.判断maiBox是否存在
        File file = mailboxFile(agentName);
        List<TeamMessage> teamMessageList = new ArrayList<>();
        if (!file.exists() || !file.isFile()) {
            return teamMessageList;
        }
        //2.读取文件内容并解析
        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            String[] split = content.split("\\R");
            for (String line : split) {
                if (!line.isBlank()) {
                    teamMessageList.add(JSON.parseObject(line, TeamMessage.class));
                }
            }
            FileUtil.del(file);
            return teamMessageList;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 格式化teamMessage
     */
    public String formatInbox(List<TeamMessage> messages) {
        StringBuilder builder = new StringBuilder();
        if(messages!=null){
            for (TeamMessage message : messages) {
                builder.append("From ").append(message.getFrom())
                        .append(" [").append(message.getType()).append("]: ")
                        .append(message.getContent())
                        .append("\n");
            }
        }
        return builder.toString();
    }


    private String preview(String content) {
        if (content == null || content.length() <= 60) {
            return content;
        }
        return content.substring(0, 60);
    }

    private File mailboxFile(String agentName) {
        String filename = agentName + ".jsonl";
        return new File(mailboxDir, filename);
    }
}
