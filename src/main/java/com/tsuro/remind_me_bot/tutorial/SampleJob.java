package com.tsuro.remind_me_bot.tutorial;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SampleJob implements Job {
    @Autowired
    private SampleJobService jobService;


    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        jobService.executeSampleJob();
    }
}
