package com.tsuro.remind_me_bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@SpringBootApplication
public class TelegramRemindMeBotApplication {


    public static void main(String[] args) throws TelegramApiException {

        SpringApplication.run(TelegramRemindMeBotApplication.class, args);
    }
}
