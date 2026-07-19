package com.getian.task;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TaskRecord {
    private String id; //唯一
    private String subject;
    private String description;
    private String status; // pending in_progress completed
    private String owner; // 哪个agent去实现它
    private List<String> blockedBy = new ArrayList<>(); //需要依赖的上游任务

    public TaskRecord() {
    }

    public TaskRecord(String id, String subject, String description, String status, String owner,
                      List<String> blockedBy) {
        this.id = id;
        this.subject = subject;
        this.description = description;
        this.status = status;
        this.owner = owner;
        setBlockedBy(blockedBy);
    }

    public List<String> getBlockedBy() {
        if (blockedBy == null) {
            blockedBy = new ArrayList<>();
        }
        return blockedBy;
    }

    public void setBlockedBy(List<String> blockedBy) {
        this.blockedBy = blockedBy == null ? new ArrayList<>() : new ArrayList<>(blockedBy);
    }


}
