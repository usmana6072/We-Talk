package com.techtitans.usman.wetalk.Models;

public class MessageModel {
    String userId,messageText;
    long messageTime;

    String messageId;

    public MessageModel(String userId, String messageText, long messageTime, String messageId) {
        this.userId = userId;
        this.messageText = messageText;
        this.messageTime = messageTime;
        this.messageId = messageId;
    }

    public MessageModel(String userId, String messageText, long messageTime) {
        this.userId = userId;
        this.messageTime = messageTime;
        this.messageText = messageText;
    }

    public MessageModel(String userId, String messageText) {
        this.userId = userId;
        this.messageText = messageText;
    }

    public MessageModel(){}

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public long getMessageTime() {
        return messageTime;
    }

    public void setMessageTime(long messageTime) {
        this.messageTime = messageTime;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
}
