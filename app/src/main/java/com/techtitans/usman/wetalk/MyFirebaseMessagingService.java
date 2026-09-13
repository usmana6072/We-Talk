package com.techtitans.usman.wetalk;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);

        DatabaseReference reference= FirebaseDatabase.getInstance().getReference().child("FCM").child(FirebaseAuth.getInstance().getUid()).child(token);
        reference.setValue(token);
    }
}
