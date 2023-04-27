package com.tsuro.remind_me_bot.tutorial.service;

import com.tsuro.remind_me_bot.tutorial.model.Reminder;

import java.util.List;

public interface ReminderService {
    Reminder createReminder(Reminder reminder);

    Reminder retrieveReminder(Long id);

    List<Reminder> getAllTelegramUserReminders(String userId);

    List<Reminder> getAllReminders();

    Reminder updateReminder(Long id, Reminder reminder);
}
