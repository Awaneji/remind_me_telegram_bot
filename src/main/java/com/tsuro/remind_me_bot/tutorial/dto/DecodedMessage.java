package com.tsuro.remind_me_bot.tutorial.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DecodedMessage {
    private String timex;
    private String type;
    private String value;
    @JsonProperty("Mod")
    private String mod;

    public DecodedMessage(String timex, String type, String value, String mod) {
        this.timex = timex;
        this.type = type;
        this.value = value;
        this.mod=mod;
    }

    public DecodedMessage() {
    }

    public String getTimex() {
        return timex;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public String getMod() {
        return mod;
    }

    @Override
    public String toString() {
        return "DecodedMessage{" +
                "timex='" + timex + '\'' +
                ", type='" + type + '\'' +
                ", value='" + value + '\'' +
                ", mod='" + mod + '\'' +
                '}';
    }
}
