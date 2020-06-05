package ru.Artak.telegram.modul;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import ru.Artak.telegram.modul.Chat;
import ru.Artak.telegram.modul.From;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {

    @JsonProperty("message_id")
    private Integer messageId;
    private From from;
    private Chat chat;
    private Integer date;
    private String text;

    public Message() {
    }

    public From getFrom() {
        return from;
    }


    public Chat getChat() {
        return chat;
    }


    public Integer getDate() {
        return date;
    }


    public String getText() {
        return text;
    }


    public Integer getMessageId() {
        return messageId;
    }


}