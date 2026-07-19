package com.getian.task;

import com.alibaba.fastjson.JSON;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 负责 taskRecord 与 文件系统的读写
 */
public class TaskStore {
    private final File tasksDir;

    public TaskStore(File workdir) {
        this.tasksDir = new File(workdir, ".tasks");
    }

    public TaskRecord save(TaskRecord task) {
        ensureTasksDir();
        if (task == null || task.getId() == null || task.getId().isBlank()) {
            throw new IllegalStateException("task is invalid");
        }
        File file = taskFile(task.getId());
        try {
            Files.writeString(file.toPath(), JSON.toJSONString(task, true), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write task: " + file, e);
        }
        return task;
    }

    public TaskRecord loadByTaskId(String taskId) {
        ensureTasksDir();
        File file = taskFile(taskId);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("Task " + taskId + " not found");
        }
        try {
            String jsonStr = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            return JSON.parseObject(jsonStr, TaskRecord.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read task: " + file, e);
        }
    }

    public boolean exists(String taskId) {
        ensureTasksDir();
        File file = new File(tasksDir, taskId + ".json");
        return !(!file.exists() || !file.isFile());
    }

    public List<TaskRecord> list() {
        List<TaskRecord> taskRecords = new ArrayList<>();
        ensureTasksDir();
        File[] files = tasksDir.listFiles(file -> file.isFile()
                && file.getName().startsWith("task_")
                && file.getName().endsWith(".json"));
        if (files != null) {
            Arrays.sort(files);
            for (File file : files) {
                try {
                    taskRecords.add(JSON.parseObject(Files.readString(file.toPath(), StandardCharsets.UTF_8),
                            TaskRecord.class));
                }
                catch (RuntimeException | IOException e) {
                    System.out.println("Skip task " + file.getName() + ": " + e.getMessage());
                }
            }
        }
        return taskRecords;
    }

    /**
     * 随机生成taskId的方法
     */
    public String nextId() {
        return String.format("task_%d_%04d", System.currentTimeMillis(),
                ThreadLocalRandom.current().nextInt(10000));
    }

    private File taskFile(String taskId) {
        if (!isSafeTaskId(taskId)) {
            throw new IllegalArgumentException("Invalid task id: " + taskId);
        }
        return new File(tasksDir, taskId + ".json");
    }

    private boolean isSafeTaskId(String taskId) {
        return taskId != null && taskId.matches("[A-Za-z0-9_-]+");
    }

    private void ensureTasksDir() {
        if (!tasksDir.exists() && !tasksDir.mkdirs()) {
            throw new IllegalStateException("Failed to create directory: " + tasksDir);
        }
    }

}
