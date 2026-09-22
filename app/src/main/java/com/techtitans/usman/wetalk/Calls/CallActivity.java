package com.techtitans.usman.wetalk.Calls;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.techtitans.usman.wetalk.R;
import com.techtitans.usman.wetalk.databinding.ActivityCallBinding;

import java.util.Locale;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;

public class CallActivity extends AppCompatActivity {

    private static final long CALL_TIMEOUT_MS = 30000; // 30 seconds

    private ActivityCallBinding binding;
    private RtcEngine mRtcEngine;
    private String channelName, token, receiverId, participantName, participantIcon;
    private boolean isVideoCall, isGroupCall;
    private boolean isMuted = false;
    private boolean isSpeakerOn = true;
    private boolean isCallAccepted = false;

    private long startTime = 0;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            binding.tvCallTimer.setText(String.format(Locale.US, "%02d:%02d", minutes, seconds));
            timerHandler.postDelayed(this, 1000);
        }
    };

    private Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable callTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isCallAccepted && !isFinishing() && !isDestroyed()) {
                Toast.makeText(CallActivity.this, "Call not answered", Toast.LENGTH_SHORT).show();
                if (receiverId != null && !isGroupCall) {
                    FirebaseDatabase.getInstance().getReference().child("Calls").child(receiverId).child("status").setValue("cancelled");
                    FirebaseDatabase.getInstance().getReference().child("Calls").child(receiverId).removeValue();
                }
                finish();
            }
        }
    };

    private static final int PERMISSION_REQ_ID = 22;
    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        channelName = getIntent().getStringExtra("channelName");
        token = getIntent().getStringExtra("token");
        isVideoCall = getIntent().getBooleanExtra("isVideoCall", true);
        isGroupCall = getIntent().getBooleanExtra("isGroupCall", false);
        receiverId = getIntent().getStringExtra("receiverId");
        participantName = getIntent().getStringExtra("participantName");
        participantIcon = getIntent().getStringExtra("participantIcon");

        setupInitialUI();

        if (!checkPermissions()) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID);
        } else {
            initAgoraAndJoin();
        }

        initControls();
        listenForCallStatus();

        // Start 30-second timeout timer for non-group calls
        if (!isGroupCall) {
            timeoutHandler.postDelayed(callTimeoutRunnable, CALL_TIMEOUT_MS);
        }
    }

    private void setupInitialUI() {
        binding.tvCallParticipantName.setText(participantName != null ? participantName : "WeTalk User");
        if (participantIcon != null && !participantIcon.trim().isEmpty()) {
            Picasso.get().load(participantIcon).placeholder(R.drawable.avatar).error(R.drawable.avatar).into(binding.ivCallerAvatar);
        } else {
            binding.ivCallerAvatar.setImageResource(R.drawable.avatar);
        }

        if (isVideoCall) {
            binding.audioCallUI.setVisibility(View.GONE);
            binding.remoteVideoViewContainer.setVisibility(View.VISIBLE);
            binding.localVideoViewCard.setVisibility(View.VISIBLE);
            binding.btnSwitchCamera.setVisibility(View.VISIBLE);
        } else {
            binding.audioCallUI.setVisibility(View.VISIBLE);
            binding.remoteVideoViewContainer.setVisibility(View.GONE);
            binding.localVideoViewCard.setVisibility(View.GONE);
            binding.btnSwitchCamera.setVisibility(View.GONE);
        }
    }

    private void listenForCallStatus() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || isGroupCall) return;

        FirebaseDatabase.getInstance().getReference().child("Calls").child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String status = snapshot.child("status").getValue(String.class);
                    if ("declined".equals(status) || "cancelled".equals(status)) {
                        timeoutHandler.removeCallbacks(callTimeoutRunnable);
                        Toast.makeText(CallActivity.this, "Call " + status, Toast.LENGTH_SHORT).show();
                        FirebaseDatabase.getInstance().getReference().child("Calls").child(uid).removeValue();
                        finish();
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            runOnUiThread(() -> {
                binding.tvCallStatus.setText("Ringing...");
            });
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            isCallAccepted = true;
            timeoutHandler.removeCallbacks(callTimeoutRunnable);
            runOnUiThread(() -> {
                binding.tvCallStatus.setText(isVideoCall ? "Video Active" : "On Call");
                binding.tvCallTimer.setVisibility(View.VISIBLE);
                if (startTime == 0) {
                    startTime = System.currentTimeMillis();
                    timerHandler.postDelayed(timerRunnable, 0);
                }
                if (isVideoCall) {
                    setupRemoteVideo(uid);
                }
            });
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            runOnUiThread(() -> {
                if (!isGroupCall) {
                    binding.tvCallStatus.setText("Disconnected");
                    finish();
                }
            });
        }

        @Override
        public void onError(int err) {
            runOnUiThread(() -> {
                if (err != 0) {
                    Toast.makeText(CallActivity.this, "Agora Error: " + err, Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }
    };

    private boolean checkPermissions() {
        for (String perm : REQUESTED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void initAgoraAndJoin() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getApplicationContext();
            config.mAppId = getString(R.string.agora_app_id);
            config.mEventHandler = mRtcEventHandler;
            mRtcEngine = RtcEngine.create(config);

            if (isVideoCall) {
                mRtcEngine.enableVideo();
                mRtcEngine.startPreview();
                setupLocalVideo();
            } else {
                mRtcEngine.disableVideo();
                mRtcEngine.enableAudio();
            }

            ChannelMediaOptions options = new ChannelMediaOptions();
            options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION;
            options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
            options.autoSubscribeAudio = true;
            options.autoSubscribeVideo = isVideoCall;

            mRtcEngine.joinChannel(token, channelName, 0, options);

        } catch (Exception e) {
            finish();
        }
    }

    private void setupLocalVideo() {
        SurfaceView surfaceView = new SurfaceView(this);
        surfaceView.setZOrderMediaOverlay(true);
        binding.localVideoViewContainer.addView(surfaceView);
        mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
    }

    private void setupRemoteVideo(int uid) {
        if (binding.remoteVideoViewContainer.getChildCount() > 0 && !isGroupCall) {
            binding.remoteVideoViewContainer.removeAllViews();
        }
        SurfaceView surfaceView = new SurfaceView(this);
        binding.remoteVideoViewContainer.addView(surfaceView);
        mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
    }

    private void initControls() {
        binding.btnBackFromCall.setOnClickListener(v -> finish());
        
        binding.btnEndCall.setOnClickListener(v -> {
            timeoutHandler.removeCallbacks(callTimeoutRunnable);
            if (receiverId != null && !isGroupCall) {
                FirebaseDatabase.getInstance().getReference().child("Calls").child(receiverId).child("status").setValue("cancelled");
            }
            finish();
        });

        binding.btnMute.setOnClickListener(v -> {
            isMuted = !isMuted;
            mRtcEngine.muteLocalAudioStream(isMuted);
            binding.btnMute.setAlpha(isMuted ? 0.4f : 1.0f);
        });

        binding.btnSwitchCamera.setOnClickListener(v -> {
            if (mRtcEngine != null) mRtcEngine.switchCamera();
        });

        binding.btnSpeaker.setOnClickListener(v -> {
            isSpeakerOn = !isSpeakerOn;
            mRtcEngine.setEnableSpeakerphone(isSpeakerOn);
            binding.btnSpeaker.setAlpha(isSpeakerOn ? 1.0f : 0.4f);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_ID && checkPermissions()) {
            initAgoraAndJoin();
        } else {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(timerRunnable);
        timeoutHandler.removeCallbacks(callTimeoutRunnable);
        if (mRtcEngine != null) {
            mRtcEngine.stopPreview();
            mRtcEngine.leaveChannel();
            RtcEngine.destroy();
            mRtcEngine = null;
        }
    }
}
