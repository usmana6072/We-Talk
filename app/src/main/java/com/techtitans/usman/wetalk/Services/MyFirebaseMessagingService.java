package com.techtitans.usman.wetalk.Services;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.techtitans.usman.wetalk.Calls.IncomingCallActivity;
import com.techtitans.usman.wetalk.MainActivity;
import com.techtitans.usman.wetalk.R;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "wetalk_chat_notifications";
    private static final String CALL_CHANNEL_ID = "wetalk_call_notifications";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        if (FirebaseAuth.getInstance().getUid() != null) {
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("FCM").child(FirebaseAuth.getInstance().getUid()).child(token);
            reference.setValue(token);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        
        Map<String, String> data = remoteMessage.getData();
        if (!data.isEmpty()) {
            String type = data.get("type");
            Log.d("FCM_SERVICE", "Data Type: " + type);
            
            if ("call".equals(type)) {
                handleIncomingCall(data);
            } else {
                String title = data.get("title") != null ? data.get("title") : "New Message";
                String message = data.get("message") != null ? data.get("message") : "";
                showNotification(title, message);
            }
        }
    }

    private void handleIncomingCall(Map<String, String> data) {
        String callerName = data.get("callerName");
        String groupName = data.get("groupName");
        String title = "Incoming Call";
        String body = (groupName != null) ? "Group call from " + groupName : "Call from " + (callerName != null ? callerName : "Someone");

        Intent intent = new Intent(this, IncomingCallActivity.class);
        intent.putExtra("channelName", data.get("channelName"));
        intent.putExtra("token", data.get("token"));
        intent.putExtra("callerId", data.get("callerId"));
        intent.putExtra("callerName", callerName);
        intent.putExtra("callerIcon", data.get("callerIcon"));
        intent.putExtra("groupName", groupName);
        intent.putExtra("isVideoCall", Boolean.parseBoolean(data.get("isVideoCall")));
        intent.putExtra("isGroupCall", Boolean.parseBoolean(data.get("isGroupCall")));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent fullScreenIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(),
                intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CALL_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setAutoCancel(true)
                .setOngoing(true)
                .setSound(ringtoneUri)
                .setFullScreenIntent(fullScreenIntent, true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setVibrate(new long[]{1000, 1000, 1000, 1000, 1000});

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        notificationManager.notify(10, builder.build());
    }

    private void showNotification(String title, String messageBody) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(messageBody))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) return;

            // Chat Channel
            NotificationChannel chatChannel = new NotificationChannel(CHANNEL_ID, "WeTalk Chat", NotificationManager.IMPORTANCE_HIGH);
            chatChannel.setDescription("Notifications for messages");
            manager.createNotificationChannel(chatChannel);

            // Call Channel (High priority + Ringtone)
            NotificationChannel callChannel = new NotificationChannel(CALL_CHANNEL_ID, "WeTalk Calls", NotificationManager.IMPORTANCE_HIGH);
            callChannel.setDescription("Notifications for incoming calls");
            
            Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .build();
            callChannel.setSound(ringtoneUri, audioAttributes);
            callChannel.enableVibration(true);
            
            manager.createNotificationChannel(callChannel);
        }
    }
}
