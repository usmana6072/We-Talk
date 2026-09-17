package com.techtitans.usman.wetalk.Models;

public class MessageModel {
    String userId, messageText;
    long messageTime;

    String messageId;
    String receiverId;

    // Media properties
    String type;      // "text", "image", "video", "document"
    String mediaUrl;  // Cloudinary URL
    String fileName;  // File name
    long fileSize;    // Size in bytes

    public MessageModel(String userId, String receiverId, String messageText, long messageTime, String messageId) {
        this.userId = userId;
        this.receiverId = receiverId;
        this.messageText = messageText;
        this.messageTime = messageTime;
        this.messageId = messageId;
        this.type = "text";
    }

    public MessageModel(String userId, String messageText, long messageTime, String messageId) {
        this.userId = userId;
        this.messageText = messageText;
        this.messageTime = messageTime;
        this.messageId = messageId;
        this.type = "text";
    }

    public MessageModel(String userId, String messageText, long messageTime) {
        this.userId = userId;
        this.messageTime = messageTime;
        this.messageText = messageText;
        this.type = "text";
    }

    public MessageModel(String userId, String messageText) {
        this.userId = userId;
        this.messageText = messageText;
        this.type = "text";
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

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getType() {
        return type != null ? type : "text";
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
}
