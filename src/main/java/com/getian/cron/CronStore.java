package com.getian.cron;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *@Author: sonicge
 *@CreateTime: 2026-07-21
 * cronJob(durable is true)持久化存储到文件中
 */

public class CronStore {
    private final File file;

    public CronStore(File workDir) {
        this.file = new File(workDir, ".scheduled_tasks.json");
    }

    /**
     * 将cronJobs集合写到file文件中
     */
    public void save(List<CronJob> cronJobs) {
        List<CronJob> durable = cronJobs.stream().filter(CronJob::isDurable).collect(Collectors.toList());
        try {
            String jsonString = JSON.toJSONString(durable, true);
            Files.writeString(file.toPath(), jsonString, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("  [cron] failed to save durable jobs: " + e.getMessage());
        }
    }

    /**
     * 从file文件中加载cronJob
     */
    public List<CronJob> load() {
        List<CronJob> res = new ArrayList<>();
        try {
            String jsonString = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            JSONArray array = JSON.parseArray(jsonString);
            for (int i = 0; i < array.size(); i++) {
                CronJob job = array.getObject(i, CronJob.class);
                if (job.getId() != null && job.getCron() != null && job.getPrompt() != null){
                    res.add(job);
                }
            }
        } catch (IOException e) {
            System.err.println("  [cron] failed to load durable jobs: " + e.getMessage());
        }
        return res;
    }

}
