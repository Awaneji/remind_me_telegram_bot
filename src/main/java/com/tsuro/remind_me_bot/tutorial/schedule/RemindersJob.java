package com.tsuro.remind_me_bot.tutorial.schedule;

import com.tsuro.remind_me_bot.tutorial.model.Reminder;
import com.tsuro.remind_me_bot.tutorial.service.ReminderService;
import com.tsuro.remind_me_bot.tutorial.telegram.Bot;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class RemindersJob implements Job {


    private final ReminderService reminderService;

    private final Bot bot;

    @Autowired
    public RemindersJob(ReminderService reminderService, Bot bot) {

        this.reminderService = reminderService;
        this.bot = bot;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        /* Get message id recorded by scheduler during scheduling */
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();

        String messageId = dataMap.getString("messageId");

//        log.info("Executing job for message id {}", messageId);

        /* Get message from database by id */
        long id = Long.parseLong(messageId);
        Reminder reminder = reminderService.retrieveReminder(id);

        reminder.setActive(false);
        reminderService.updateReminder(id, reminder);

        bot.sendText(reminder.getTelegramUserId(),"********* ALARM FOR ********* : "+reminder.getReminderMessage());
        System.out.println("Job trigger at " + LocalDateTime.now() + " and was scheduled to run at " + reminder.getReminderTime());

        /* unschedule or delete after job gets executed */

        try {
            context.getScheduler().deleteJob(new JobKey(messageId));

            TriggerKey triggerKey = new TriggerKey(messageId);

            context.getScheduler().unscheduleJob(triggerKey);

        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }
}

