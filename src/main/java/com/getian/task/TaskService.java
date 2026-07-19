package com.getian.task;

import com.alibaba.fastjson.JSON;

import java.util.ArrayList;
import java.util.List;

public class TaskService {
    private final TaskStore taskStore;

    private static final String PENDING = "pending";

    private static final String IN_PROGRESS = "in_progress";

    private static final String COMPLETED = "completed";

    public TaskService(TaskStore taskStore) {
        this.taskStore = taskStore;
    }

    /**
     * 创建TaskRecord
     */
    public TaskRecord createTask(String subject, String description, List<String> blockedBy) {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("subject is required");
        }
        TaskRecord taskRecord = new TaskRecord(
                taskStore.nextId(),
                subject,
                description,
                PENDING,
                null,
                blockedBy
        );
        taskStore.save(taskRecord);
        return taskRecord;
    }

    public String listTasks() {
        List<TaskRecord> list = taskStore.list();
        if (list.isEmpty()) {
            return "No tasks. Use create_task to add some.";
        }
        StringBuilder builder = new StringBuilder();
        for (TaskRecord task : list) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append("[")
                    .append(task.getStatus())
                    .append("] ")
                    .append(task.getId())
                    .append(": ")
                    .append(task.getSubject());
            if (task.getOwner() != null && !task.getOwner().isBlank()) {
                builder.append(" [").append(task.getOwner()).append("]");
            }
            if (!task.getBlockedBy().isEmpty()) {
                builder.append(" (blockedBy: ").append(String.join(", ", task.getBlockedBy())).append(")");
            }

        }
        return builder.toString();
    }

    public String getTask(String taskId) {
        return JSON.toJSONString(taskStore.loadByTaskId(taskId), true);
    }

    public boolean canStart(String taskId) {
        return blockingDependencies(taskStore.loadByTaskId(taskId)).isEmpty();
    }


    /**
     * 判断任务是否能够启动 task_status： pending -> in_progress
     */
    public String claimTask(String taskId, String owner) {
        TaskRecord taskRecord = taskStore.loadByTaskId(taskId);
        String oldStatus = taskRecord.getStatus();
        //1.状态不符合
        if (!PENDING.equals(oldStatus)) {
            return "Task status is not 'pending',cannot claim. [taskId:" + taskId + " status:" + oldStatus + "]";
        }
        //2.是否存在依赖阻塞的任务还没完成
        List<String> blockingDependencies = blockingDependencies(taskRecord);
        if (!blockingDependencies.isEmpty()) {
            return "blocked by " + blockingDependencies;
        }
        taskRecord.setOwner(owner == null || owner.isBlank() ? "agent" : owner);
        taskRecord.setStatus(IN_PROGRESS);
        taskStore.save(taskRecord);
        return "Claimed " + taskRecord.getId() + " (" + taskRecord.getSubject() + ")";
    }

    /**
     * 判断任务是否可以完成（task已经开始执行 不需要再判断是否有依赖项了）
     */
    public String completedTask(String taskId) {
        TaskRecord taskRecord = taskStore.loadByTaskId(taskId);
        String oldStatus = taskRecord.getStatus();
        if (!IN_PROGRESS.equals(oldStatus)) {
            return "Task status is not 'in_progress',cannot claim. [taskId:" + taskId + " status:" + oldStatus + "]";
        }
        taskRecord.setStatus(COMPLETED);
        taskStore.save(taskRecord);
        List<String> unblocked = findUnblockedSubjects();
        String message = "Completed " + taskRecord.getId() + " (" + taskRecord.getSubject() + ")";
        if (!unblocked.isEmpty()) {
            message += "\nUnblocked: " + String.join(", ", unblocked);
        }
        return message;
    }

    /**
     * 找到该时刻没有上游阻塞任务的task
     */
    private List<String> findUnblockedSubjects() {
        List<String> unblocked = new ArrayList<>();
        for (TaskRecord task : taskStore.list()) {
            if (PENDING.equals(task.getStatus()) && !task.getBlockedBy().isEmpty() && blockingDependencies(task).isEmpty()) {
                unblocked.add(task.getId());
            }
        }
        return unblocked;
    }

    /**
     * 找当前task上游的依赖 -> 状态不能为completed
     */
    private List<String> blockingDependencies(TaskRecord task) {
        List<String> blockedTaskIds = new ArrayList<>();
        for (String taskId : task.getBlockedBy()) {
            TaskRecord taskRecord = taskStore.loadByTaskId(taskId);
            if (!"completed".equals(taskRecord.getStatus())) {
                blockedTaskIds.add(taskId);
            }
        }
        return blockedTaskIds;
    }
}
