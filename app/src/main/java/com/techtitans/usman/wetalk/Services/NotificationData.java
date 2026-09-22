package com.techtitans.usman.wetalk.Services;

import java.util.HashMap;
import java.util.Map;

public class NotificationData {
    private Map<String, String> data;

    public NotificationData() {
        this.data = new HashMap<>();
    }

    public void put(String key, String value) {
        data.put(key, value);
    }

    public Map<String, String> getData() {
        return data;
    }
}
