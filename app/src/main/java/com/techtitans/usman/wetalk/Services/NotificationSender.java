package com.techtitans.usman.wetalk.Services;

import java.util.Map;

public class NotificationSender {

    private Message message;

    public NotificationSender(String token, Map<String, String> data) {
        this.message = new Message(token, data);
    }

    public static class Message {
        private String token;
        private Map<String, String> data;

        public Message(String token, Map<String, String> data) {
            this.token = token;
            this.data = data;
        }
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }
}
