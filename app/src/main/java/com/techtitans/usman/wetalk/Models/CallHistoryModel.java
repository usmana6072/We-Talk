package com.techtitans.usman.wetalk.Models;

public class CallHistoryModel {
    private String callId;
    private String otherUserId;
    private String otherUserName;
    private String otherUserPic;
    private String callType;   // "audio" or "video"
    private String direction;  // "outgoing", "incoming", "missed", "declined"
    private long timestamp;
    private boolean isGroupCall;

    public CallHistoryModel() {
    }

    public CallHistoryModel(String callId, String otherUserId, String otherUserName, String otherUserPic, String callType, String direction, long timestamp, boolean isGroupCall) {
        this.callId = callId;
        this.otherUserId = otherUserId;
        this.otherUserName = otherUserName;
        this.otherUserPic = otherUserPic;
        this.callType = callType;
        this.direction = direction;
        this.timestamp = timestamp;
        this.isGroupCall = isGroupCall;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getOtherUserId() {
        return otherUserId;
    }

    public void setOtherUserId(String otherUserId) {
        this.otherUserId = otherUserId;
    }

    public String getOtherUserName() {
        return otherUserName;
    }

    public void setOtherUserName(String otherUserName) {
        this.otherUserName = otherUserName;
    }

    public String getOtherUserPic() {
        return otherUserPic;
    }

    public void setOtherUserPic(String otherUserPic) {
        this.otherUserPic = otherUserPic;
    }

    public String getCallType() {
        return callType != null ? callType : "audio";
    }

    public void setCallType(String callType) {
        this.callType = callType;
    }

    public String getDirection() {
        return direction != null ? direction : "outgoing";
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isGroupCall() {
        return isGroupCall;
    }

    public void setGroupCall(boolean groupCall) {
        isGroupCall = groupCall;
    }
}
