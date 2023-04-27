package com.tsuro.remind_me_bot.tutorial;

import com.tsuro.remind_me_bot.tutorial.configuration.QuartzConfig;
import com.tsuro.remind_me_bot.tutorial.model.Reminder;
import com.tsuro.remind_me_bot.tutorial.schedule.RemindersJob;
import com.tsuro.remind_me_bot.tutorial.service.ReminderService;
import com.tsuro.remind_me_bot.tutorial.telegram.BotInitializer;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;

import static java.time.ZoneId.systemDefault;

@Component
@Slf4j
public class AppStartupRunner { //implements CommandLineRunner {

    @Autowired
    private BotInitializer botInitializer;

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private SchedulerFactoryBean schedulerFactoryBean;
    // @Override
    public void run(String... args) throws Exception {
//        botInitializer.init();

        Reminder reminder = new Reminder();
        reminder.setActive(true);
        reminder.setCreated(LocalDateTime.now());
        reminder.setReminderTime(LocalDateTime.now().plusMinutes(5));
        reminder.setModified(LocalDateTime.now());
        reminder.setTelegramUserId(String.valueOf(85006L));
        reminder.setReminderMessage("Testing Quartz Scheduling");

        reminder = reminderService.createReminder(reminder);

        // Creating JobDetail instance
        String id = String.valueOf(reminder.getId());
        JobDetail jobDetail = JobBuilder.newJob(RemindersJob.class).withIdentity(id).build();

        // Adding JobDataMap to jobDetail
        jobDetail.getJobDataMap().put("messageId", id);

        System.out.println("ZoneOffset = "+ZoneOffset.ofHours(2));
//        System.out.println("ZoneOffset = "+ systemDefault());
        ZoneId.getAvailableZoneIds().stream().forEach(System.out::println);


        // Scheduling time to run job
        Date triggerJobAt = Date.from(reminder.getReminderTime().toInstant(ZoneOffset.ofHours(2)));

        jobDetail.getJobDataMap().put("startAt", triggerJobAt);

        SimpleTrigger trigger = TriggerBuilder.newTrigger().withIdentity(id)
                .startAt(triggerJobAt).withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withMisfireHandlingInstructionFireNow())
                .build();
        // Getting scheduler instance
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        scheduler.scheduleJob(jobDetail, trigger);
        scheduler.start();

    }


}
