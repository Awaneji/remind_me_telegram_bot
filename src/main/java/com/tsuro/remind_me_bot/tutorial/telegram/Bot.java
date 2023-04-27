package com.tsuro.remind_me_bot.tutorial.telegram;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.recognizers.text.Culture;
import com.microsoft.recognizers.text.ModelResult;
import com.microsoft.recognizers.text.datetime.DateTimeOptions;
import com.microsoft.recognizers.text.datetime.DateTimeRecognizer;
import com.tsuro.remind_me_bot.tutorial.dto.DecodedMessage;
import com.tsuro.remind_me_bot.tutorial.model.Reminder;
import com.tsuro.remind_me_bot.tutorial.schedule.RemindersJob;
import com.tsuro.remind_me_bot.tutorial.service.ReminderService;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;

@Component
public class Bot extends TelegramLongPollingBot {

    private final ReminderService reminderService;


    private final SchedulerFactoryBean schedulerFactoryBean;

    @Autowired
    public Bot(ReminderService reminderService, SchedulerFactoryBean schedulerFactoryBean) {
        this.reminderService = reminderService;
        this.schedulerFactoryBean = schedulerFactoryBean;
    }

    @Override
    public String getBotUsername() {
        return "rangarira_bot";
    }

    @Override
    public String getBotToken() {
        return "6087959083:AAGfRznu9Qrjbw_TD1EYhM7wHf4fN514gHc";
    }

    @Override
    public void onUpdateReceived(Update update) {

        var msg = update.getMessage();
        var user = msg.getFrom();
        List<ModelResult> modelResultList = DateTimeRecognizer.recognizeDateTime(msg.getText(), Culture.English, DateTimeOptions.SplitDateAndTime);
        if (msg.isCommand()) {
            if (msg.getText().equals("/help"))
                listCommands(user.getId());
            else if (msg.getText().equals("/bye"))
                terminateSession(user);
            else if (msg.getText().equals("/start")) {
                sendText(user.getId(), "Welcome " + user.getFirstName() + " to the rangarira/ remind me Bot, you can now schedule your queries!!");
            } else if (modelResultList.size() > 0) {
                scheduleQuery(update);
            } else {
                listCommands(user.getId());
            }
        } else if (modelResultList.size() > 0) {
            scheduleQuery(update);
        } else {
            listCommands(user.getId());
        }
    }

    private void scheduleQuery(Update update) {
        var msg = update.getMessage();
        var userId = msg.getFrom().getId();
        try {
            List<ModelResult> modelResultList = DateTimeRecognizer.recognizeDateTime(msg.getText(), Culture.English, DateTimeOptions.SplitDateAndTime);

            if (modelResultList.size() > 0) {
                ModelResult result = modelResultList.get(0);
                SortedMap<String, Object> sortedMap = result.resolution;

                @SuppressWarnings("unchecked") ArrayList<Object> objects = (ArrayList<Object>) sortedMap.get(sortedMap.lastKey());
                ObjectMapper objectMapper = new ObjectMapper();
                String json = objectMapper.writeValueAsString(objects.get(objects.size() - 1));
                DecodedMessage decodedMessage = objectMapper.readValue(json, DecodedMessage.class);

                String joiner = decodedMessage.getType().equals("time") ? " at " : " on ";
                var resp = "";
                var reminderTime = "";
                LocalDateTime scheduleTime;
                if (decodedMessage.getType().equals("duration")) {
                    scheduleTime = LocalDateTime.now().plusSeconds(Long.parseLong(decodedMessage.getValue()));
                    joiner = " on ";
                    resp = msg.getText().toUpperCase().replace(result.text.toUpperCase(), joiner.concat(scheduleTime.toString()).toUpperCase());
                } else {
                    resp = msg.getText().toUpperCase().replace(result.text.toUpperCase(), joiner.concat(decodedMessage.getValue()).toUpperCase());
                    reminderTime = decodedMessage.getValue();

                    try {
                        scheduleTime = LocalDateTime.parse(reminderTime, DateTimeFormatter.ISO_DATE);
                    } catch (Exception e) {

                        // time object
                        String[] timeValues = reminderTime.split(":");
                        scheduleTime = LocalDateTime.of(LocalDateTime.now().getYear(), LocalDateTime.now().getMonth(), LocalDateTime.now().getDayOfMonth(), Integer.parseInt(timeValues[0]), Integer.parseInt(timeValues[1]), Integer.parseInt(timeValues[2]));

                    }
                }

                invokeQuartzScheduling(msg, userId, scheduleTime);

                // send message back to bot
                sendText(userId, "SCHEDULED " + resp);
            } else {
                sendText(userId, "An error occurred decoding your query, kindly rephrase.");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            sendText(userId, "An error occurred decoding your query, kindly rephrase.");
        }
    }

    private void invokeQuartzScheduling(Message msg, Long userId, LocalDateTime scheduleTime) throws SchedulerException {
        // create reminder object
        Reminder reminder = new Reminder();
        reminder.setActive(true);
        reminder.setCreated(LocalDateTime.now());
        reminder.setReminderTime(scheduleTime);
        reminder.setModified(LocalDateTime.now());
        reminder.setTelegramUserId(String.valueOf(userId));
        reminder.setReminderMessage(msg.getText());

        reminder = reminderService.createReminder(reminder);

        // Creating JobDetail instance
        String id = String.valueOf(reminder.getId());
        JobDetail jobDetail = JobBuilder.newJob(RemindersJob.class).withIdentity(id).build();

        // Adding JobDataMap to jobDetail
        jobDetail.getJobDataMap().put("messageId", id);
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

    private void listCommands(Long id) {
        String message = """
                Hello!, Kindly type any of the commands below
                 /start to start interaction
                 /help to get help
                 /bye to exit interaction,
                 ==============================================
                 you can now schedule your queries using text!!
                 ==============================================""";
        sendText(id, message);
    }

    private void terminateSession(User user) {
        sendText(user.getId(), "Good bye! " + user.getFirstName());
    }

    public void sendText(Long who, String what) {
        SendMessage sm = SendMessage.builder()
                .chatId(who.toString())
                .text(what).build();
        try {
            execute(sm);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
