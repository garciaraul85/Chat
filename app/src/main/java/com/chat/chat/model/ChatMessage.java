package com.chat.chat.model;

import java.util.Date;

public class ChatMessage {
    private String id;
    private String messageText;
    private String messageUser;
    private String idUser;
    private boolean isMessage;
    private long messageTime;

    public ChatMessage() {}

    public ChatMessage(String messageText, String messageUser, String idUser) {
        this.messageText = messageText;
        this.messageUser = messageUser;
        this.idUser      = idUser;
        this.isMessage  = true;
        // Initialize to current time
        messageTime = new Date().getTime();
    }

    public ChatMessage(String messageUser, String idUser) {
        this.messageUser = messageUser;
        this.idUser      = idUser;

        // Initialize to current time
        messageTime = new Date().getTime();
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public String getMessageUser() {
        return messageUser;
    }

    public void setMessageUser(String messageUser) {
        this.messageUser = messageUser;
    }

    public long getMessageTime() {
        return messageTime;
    }

    public void setMessageTime(long messageTime) {
        this.messageTime = messageTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdUser() {
        return idUser;
    }

    public void setIdUser(String idUser) {
        this.idUser = idUser;
    }

    public boolean isMessage() {
        return isMessage;
    }

    public void setMessage(boolean message) {
        isMessage = message;
    }
}