package com.techtitans.usman.wetalk.Models;


public class StatusModel {
    String senderId,userName,statusPic;
    long timeStamp;

    public StatusModel(String senderId, String userName, String statusPic, long timeStamp) {
        this.senderId = senderId;
        this.userName = userName;
        this.statusPic = statusPic;
        this.timeStamp = timeStamp;
    }

    public StatusModel(){}

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getStatusPic() {
        return statusPic;
    }

    public void setStatusPic(String statusPic) {
        this.statusPic = statusPic;
    }
}

