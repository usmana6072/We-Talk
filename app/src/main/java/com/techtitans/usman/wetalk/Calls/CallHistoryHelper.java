package com.techtitans.usman.wetalk.Calls;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.techtitans.usman.wetalk.Models.CallHistoryModel;

public class CallHistoryHelper {

    public static void logCall(String currentUserId, String otherUserId, String otherUserName, String otherUserPic, String callType, String direction) {
        logCall(currentUserId, otherUserId, otherUserName, otherUserPic, callType, direction, false);
    }

    public static void logCall(String currentUserId, String otherUserId, String otherUserName, String otherUserPic, String callType, String direction, boolean isGroupCall) {
        if (currentUserId == null || otherUserId == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("CallHistory").child(currentUserId);
        String key = ref.push().getKey();
        if (key != null) {
            CallHistoryModel model = new CallHistoryModel(
                    key,
                    otherUserId,
                    otherUserName != null ? otherUserName : "User",
                    otherUserPic != null ? otherUserPic : "",
                    callType != null ? callType : "audio",
                    direction != null ? direction : "outgoing",
                    System.currentTimeMillis(),
                    isGroupCall
            );
            ref.child(key).setValue(model);
        }
    }
}
