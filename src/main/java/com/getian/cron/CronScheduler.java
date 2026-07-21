package com.getian.cron;

import cn.hutool.cron.CronUtil;
import cn.hutool.cron.pattern.CronPattern;
import cn.hutool.cron.task.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 *@Author: sonicge
 *@CreateTime: 2026-07-21
 */

public class CronScheduler {
    private final CronStore cronStore;
    private final Map<String, CronJob> jobs = new ConcurrentHashMap<>();

    public CronScheduler(CronStore store) {
        this.cronStore = store;
    }

    /**
     * 从磁盘中加载cronJob，注册到Hutool，然后启动cron调度线程
     */
    public void start() {
        //1.加载所有durable cronJob
        List<CronJob> loaded = cronStore.load();
        for (CronJob job : loaded) {
            //2.双层校验cron表达式
            String err = validateCron(job.getCron());
            if (err != null) {
                System.err.println("  [cron] skipping invalid durable job "
                        + job.getId() + ": " + err);
                continue;
            }
            jobs.put(job.getId(), job);
            //3.定时任务注册
            CronUtil.schedule(job.getId(), job.getCron(), new CronTask(job.getId()));
            System.out.println("  [cron] loaded durable job " + job.getId()
                    + " '" + job.getCron() + "'");
        }
        if (!loaded.isEmpty()) {
            System.out.println("  [cron] loaded " + loaded.size() + " durable job(s)");
        }
        CronUtil.setMatchSecond(false); //不精确到秒
        //4. 定时任务启动
        CronUtil.start();
        System.out.println("  [cron] scheduler started");
    }

    /**
     * 停止 cron 调度线程。
     */
    public void stop() {
        CronUtil.stop();
    }

    /**
     * 注册一个新的 cron 任务
     */
    public String schedule(String cron, String prompt, boolean recurring, boolean durable) {
        String res = validateCron(cron);
        if (res != null) {
            return "Error: " + res;
        }
        //随机挑选数字，然后转换为16机制的字符串
        String id = "cron_" + Integer.toHexString(
                ThreadLocalRandom.current().nextInt(0x10000, 0x10000000)
        );
        CronJob job = new CronJob(id, cron, prompt, recurring, durable);
        jobs.put(id, job);
        CronUtil.schedule(id, cron, new CronTask(id));

        if (durable) {
            cronStore.save(new ArrayList<>(jobs.values()));
        }

        System.out.println("  [cron register] " + id + " '" + cron + "' → "
                + (prompt.length() > 40 ? prompt.substring(0, 40) : prompt));
        return "Scheduled " + id + ": '" + cron + "' → " + prompt;
    }

    public String cancel(String jobId){
        if(!jobs.containsKey(jobId)){
            return "Job " + jobId +" is not found";
        }
        CronJob removed = jobs.remove(jobId);
        CronUtil.remove(jobId);
        if(removed.isDurable()){
            cronStore.save(new ArrayList<>(jobs.values()));
        }
        System.out.println("  [cron cancel] " + jobId);
        return "Cancelled " + jobId;
    }

    public List<CronJob> list(){
        return new ArrayList<>(jobs.values());
    }


    private String validateCron(String cron) {
        //1.是否为空
        if (cron == null || cron.isBlank()) {
            return "cron expression is null or empty";
        }
        //2.是否为标准的五段式
        String[] fields = cron.split("\\s+");
        if (fields == null || fields.length != 5) {
            return "Expected 5 fields, got " + fields.length + ": " + cron;
        }
        try {
            //3.解析cron表达式 抛出异常 -> return xx
            CronPattern.of(cron);
        } catch (Exception e) {
            return "Invalid cron expression: " + e.getMessage();
        }
        return null;
    }

    private class CronTask implements Task {
        private String id;

        public CronTask(String id) {
            this.id = id;
        }

        /**
         * 当时间符合cron表达式后 就会触发这里的execute方法
         */
        @Override
        public void execute() {
            //todo
        }
    }


}
