package com.techtitans.usman.wetalk.Models;

import java.util.ArrayList;
import java.util.List;

public class GroupModel {
    private String groupId;
    private String groupName;
    private String groupDescription;
    private String groupIcon;
    private String createdBy;
    private boolean hideMembers;
    private long timestamp;

    public GroupModel() {}

    public GroupModel(String groupId, String groupName, String groupDescription, String groupIcon, String createdBy, boolean hideMembers, long timestamp) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.groupDescription = groupDescription;
        this.groupIcon = groupIcon;
        this.createdBy = createdBy;
        this.hideMembers = hideMembers;
        this.timestamp = timestamp;
    }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String getGroupDescription() { return groupDescription; }
    public void setGroupDescription(String groupDescription) { this.groupDescription = groupDescription; }

    public String getGroupIcon() { return groupIcon; }
    public void setGroupIcon(String groupIcon) { this.groupIcon = groupIcon; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public boolean isHideMembers() { return hideMembers; }
    public void setHideMembers(boolean hideMembers) { this.hideMembers = hideMembers; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
