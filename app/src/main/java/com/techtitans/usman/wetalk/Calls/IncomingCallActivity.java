package com.techtitans.usman.wetalk.Calls;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.techtitans.usman.wetalk.R;
import com.techtitans.usman.wetalk.databinding.ActivityIncomingCallBinding;

public class IncomingCallActivity extends AppCompatActivity {

    private static final long RING_TIMEOUT_MS = 30000; // 30 seconds

    private ActivityIncomingCallBinding binding;
    private MediaPlayer mediaPlayer;
    private String channelName, token, callerId, callerName, callerIcon, groupName;
    private boolean isVideoCall, isGroupCall;
    private ValueEventListener callStatusListener;

    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private final Runnable ringTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isFinishing() && !isDestroyed()) {
                stopRinging();
                removeListener();
                String myUid = FirebaseAuth.getInstance().getUid();
                if (myUid != null) {
                    CallHistoryHelper.logCall(myUid, callerId != null ? callerId : "unknown", callerName != null ? callerName : "User", callerIcon != null ? callerIcon : "", isVideoCall ? "video" : "audio", "missed", isGroupCall);
                    FirebaseDatabase.getInstance().getReference().child("Calls").child(myUid).removeValue();
                }
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Ensure activity shows over lockscreen and wakes the device
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }

        binding = ActivityIncomingCallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Cancel the notification that launched this activity
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(10);
        }

        channelName = getIntent().getStringExtra("channelName");
        token = getIntent().getStringExtra("token");
        callerId = getIntent().getStringExtra("callerId");
        callerName = getIntent().getStringExtra("callerName");
        callerIcon = getIntent().getStringExtra("callerIcon");
        groupName = getIntent().getStringExtra("groupName");
        isVideoCall = getIntent().getBooleanExtra("isVideoCall", true);
        isGroupCall = getIntent().getBooleanExtra("isGroupCall", false);

        setupUI();
        startRinging();
        listenForCancellation();

        // Start 30-second ringing timeout
        timeoutHandler.postDelayed(ringTimeoutRunnable, RING_TIMEOUT_MS);

        binding.btnAcceptCall.setOnClickListener(v -> {
            timeoutHandler.removeCallbacks(ringTimeoutRunnable);
            stopRinging();
            removeListener();
            Intent intent = new Intent(IncomingCallActivity.this, CallActivity.class);
            intent.putExtra("channelName", channelName);
            intent.putExtra("token", token);
            intent.putExtra("isVideoCall", isVideoCall);
            intent.putExtra("isGroupCall", isGroupCall);
            intent.putExtra("participantName", isGroupCall ? groupName : callerName);
            intent.putExtra("participantIcon", isGroupCall ? "" : callerIcon);
            startActivity(intent);
            finish();
        });

        binding.btnDeclineCall.setOnClickListener(v -> {
            timeoutHandler.removeCallbacks(ringTimeoutRunnable);
            stopRinging();
            removeListener();
            String myUid = FirebaseAuth.getInstance().getUid();
            if (!isGroupCall && callerId != null) {
                FirebaseDatabase.getInstance().getReference().child("Calls").child(callerId).child("status").setValue("declined");
            }
            if (myUid != null) {
                CallHistoryHelper.logCall(myUid, callerId != null ? callerId : "unknown", callerName != null ? callerName : "User", callerIcon != null ? callerIcon : "", isVideoCall ? "video" : "audio", "declined", isGroupCall);
                FirebaseDatabase.getInstance().getReference().child("Calls").child(myUid).removeValue();
            }
            finish();
        });
    }

    private void setupUI() {
        binding.tvCallerName.setText(callerName != null ? callerName : "Someone");
        binding.tvCallType.setText(isVideoCall ? "Incoming Video Call" : "Incoming Voice Call");
        
        if (isGroupCall && groupName != null) {
            binding.tvGroupNameTag.setVisibility(View.VISIBLE);
            binding.tvGroupNameTag.setText("Group: " + groupName);
        }

        if (callerIcon != null && !callerIcon.trim().isEmpty()) {
            Picasso.get().load(callerIcon).placeholder(R.drawable.avatar).error(R.drawable.avatar).into(binding.ivCallerIcon);
        } else {
            binding.ivCallerIcon.setImageResource(R.drawable.avatar);
        }
    }

    private void listenForCancellation() {
        String myUid = FirebaseAuth.getInstance().getUid();
        if (myUid == null) return;

        callStatusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String status = snapshot.child("status").getValue(String.class);
                    if ("cancelled".equals(status)) {
                        timeoutHandler.removeCallbacks(ringTimeoutRunnable);
                        stopRinging();
                        CallHistoryHelper.logCall(myUid, callerId != null ? callerId : "unknown", callerName != null ? callerName : "User", callerIcon != null ? callerIcon : "", isVideoCall ? "video" : "audio", "missed", isGroupCall);
                        finish();
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        FirebaseDatabase.getInstance().getReference().child("Calls").child(myUid).addValueEventListener(callStatusListener);
    }

    private void removeListener() {
        String myUid = FirebaseAuth.getInstance().getUid();
        if (myUid != null && callStatusListener != null) {
            FirebaseDatabase.getInstance().getReference().child("Calls").child(myUid).removeEventListener(callStatusListener);
        }
    }

    private void startRinging() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            mediaPlayer = MediaPlayer.create(this, notification);
            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
            }
        } catch (Exception e) {}
    }

    private void stopRinging() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception e) {}
            mediaPlayer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timeoutHandler.removeCallbacks(ringTimeoutRunnable);
        stopRinging();
        removeListener();
    }
}
