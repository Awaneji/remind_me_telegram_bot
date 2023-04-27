package com.tsuro.remind_me_bot.tutorial.service;

import com.tsuro.remind_me_bot.tutorial.model.Reminder;
import com.tsuro.remind_me_bot.tutorial.repo.ReminderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class ReminderServiceImpl implements ReminderService{

    private final ReminderRepository reminderRepository;

    @Autowired
    public ReminderServiceImpl(ReminderRepository reminderRepository) {
        this.reminderRepository = reminderRepository;
    }

    @Override
    public Reminder createReminder(Reminder reminder) {
        Objects.requireNonNull(reminder,"Reminder required");
        return reminderRepository.save(reminder);
    }

    @Override
    public Reminder retrieveReminder(Long id) {
        Objects.requireNonNull(id,"Reminder id required");
        return reminderRepository.findById(id).orElseThrow(() -> new RuntimeException("Reminder not found"));
    }

    @Override
    public List<Reminder> getAllTelegramUserReminders(String userId) {
        return reminderRepository.findAllByTelegramUserId(userId);
    }

    @Override
    public List<Reminder> getAllReminders() {
        return reminderRepository.findAll();
    }

    @Override
    public Reminder updateReminder(Long id, Reminder reminder) {
        Objects.requireNonNull(id, "Id required");

        if(id.equals(reminder.getId())){
           return reminderRepository.save(reminder);
        }
        throw new RuntimeException("Id mismatch");
    }
}
