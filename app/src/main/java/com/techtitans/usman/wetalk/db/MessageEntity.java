package com.techtitans.usman.wetalk.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "messages",
    indices = {
        @Index(value = {"chatRoomId", "messageTime"}),
        @Index(value = {"messageId"}, unique = true)
    }
)
public class MessageEntity {

    @PrimaryKey(autoGenerate = true)
    private long localId;

    @NonNull
    private String messageId;

    @NonNull
    private String chatRoomId;

    private String senderId;
    private String receiverId;
    private String messageText;
    private long messageTime;

    private String type;      // "text", "image", "video", "document"
    private String mediaUrl;  // Cloudinary URL
    private String fileName;  // File name
    private long fileSize;    // Size in bytes
    private String status;    // "sending", "sent", "failed"

    public MessageEntity() {
    }

    public MessageEntity(@NonNull String messageId, @NonNull String chatRoomId, String senderId, String receiverId, String messageText, long messageTime, String type, String mediaUrl, String fileName, long fileSize, String status) {
        this.messageId = messageId;
        this.chatRoomId = chatRoomId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.messageText = messageText;
        this.messageTime = messageTime;
        this.type = type != null ? type : "text";
        this.mediaUrl = mediaUrl;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.status = status != null ? status : "sent";
    }

    public long getLocalId() {
        return localId;
    }

    public void setLocalId(long localId) {
        this.localId = localId;
    }

    @NonNull
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(@NonNull String messageId) {
        this.messageId = messageId;
    }

    @NonNull
    public String getChatRoomId() {
        return chatRoomId;
    }

    public void setChatRoomId(@NonNull String chatRoomId) {
        this.chatRoomId = chatRoomId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
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

    public String getStatus() {
        return status != null ? status : "sent";
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
