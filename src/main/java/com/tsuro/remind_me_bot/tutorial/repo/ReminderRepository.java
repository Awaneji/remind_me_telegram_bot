package com.tsuro.remind_me_bot.tutorial.repo;

import com.tsuro.remind_me_bot.tutorial.model.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    List<Reminder> findAllByTelegramUserId(String userId);
}
