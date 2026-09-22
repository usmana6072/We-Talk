package com.techtitans.usman.wetalk.Models;

public class Users implements Comparable<Users> {
    private String profilePic, userName, mail, password, userId, lastMessage;
    private long timeStam;

    public Users() {
    }

    public Users(String userName, String mail, String password) {
        this.userName = userName;
        this.mail = mail;
        this.password = password;
    }

    public Users(String profilePic, String userName, String mail, String password, String userId, String lastMessage) {
        this.profilePic = profilePic;
        this.userName = userName;
        this.mail = mail;
        this.password = password;
        this.userId = userId;
        this.lastMessage = lastMessage;
    }

    public Users(String profilePic, String userName, String mail, String password, String userId, String lastMessage, long timeStam) {
        this.profilePic = profilePic;
        this.userName = userName;
        this.mail = mail;
        this.password = password;
        this.userId = userId;
        this.lastMessage = lastMessage;
        this.timeStam = timeStam;
    }

    public String getProfilePic() {
        return profilePic;
    }

    public void setProfilePic(String profilePic) {
        this.profilePic = profilePic;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getTimeStam() {
        return timeStam;
    }

    public void setTimeStam(long timeStam) {
        this.timeStam = timeStam;
    }

    @Override
    public int compareTo(Users o) {
        if (o == null) return -1;
        return Long.compare(o.getTimeStam(), this.getTimeStam()); // Descending: newest first
    }
}
