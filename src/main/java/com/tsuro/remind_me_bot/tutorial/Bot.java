package com.tsuro.remind_me_bot.tutorial;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.recognizers.datatypes.timex.expression.Resolution;
import com.microsoft.recognizers.datatypes.timex.expression.TimexProperty;
import com.microsoft.recognizers.datatypes.timex.expression.TimexResolver;
import com.microsoft.recognizers.datatypes.timex.expression.TimexValue;
import com.microsoft.recognizers.text.Culture;
import com.microsoft.recognizers.text.ModelResult;
import com.microsoft.recognizers.text.datetime.DateTimeOptions;
import com.microsoft.recognizers.text.datetime.DateTimeRecognizer;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;

@Service
public class Bot extends TelegramLongPollingBot {

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
        if (msg.isCommand()) {
            if (msg.getText().equals("/help"))
                listCommands(user.getId());
            else if (msg.getText().equals("/bye"))
                terminateSession(user);
            else if (msg.getText().equals("/start")) {
                sendText(user.getId(), "Welcome " + user.getFirstName() + " to the rangarira/ remind me Bot, you can now schedule your queries!!");

            } else {
                scheduleQuery(update);
            }
        } else {
            scheduleQuery(update);
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
                System.out.println(sortedMap);

                @SuppressWarnings("unchecked") ArrayList<Object> objects = (ArrayList<Object>) sortedMap.get(sortedMap.lastKey());
                ObjectMapper objectMapper = new ObjectMapper();
                String json = objectMapper.writeValueAsString(objects.get(objects.size() - 1));
                DecodedMessage decodedMessage = objectMapper.readValue(json, DecodedMessage.class);

                String joiner = decodedMessage.getType().equals("time") ? " at " : " on ";
                var resp = "";
                if (decodedMessage.getType().equals("duration")) {
                    LocalDateTime scheduled =  LocalDateTime.now().plusSeconds(Long.parseLong(decodedMessage.getValue()));
                    joiner = " on ";
                    resp = msg.getText().toUpperCase().replace(result.text.toUpperCase(), joiner.concat(scheduled.toString()).toUpperCase());
                } else {
                    resp = msg.getText().toUpperCase().replace(result.text.toUpperCase(), joiner.concat(decodedMessage.getValue()).toUpperCase());
                }

                sendText(userId, "SCHEDULED " + resp);
            } else {
                sendText(userId, "An error occurred decoding your query, kindly rephrase.");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            sendText(userId, "An error occurred decoding your query, kindly rephrase.");
        }
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

    private void sendText(Long who, String what) {
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
