package com.techtitans.usman.wetalk;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.techtitans.usman.wetalk.Adapters.ChatAdapter;
import com.techtitans.usman.wetalk.Calls.CallActivity;
import com.techtitans.usman.wetalk.Calls.CallHistoryHelper;
import com.techtitans.usman.wetalk.Interfaces.ApiService;
import com.techtitans.usman.wetalk.Models.MessageModel;
import com.techtitans.usman.wetalk.Services.AgoraTokenFetcher;
import com.techtitans.usman.wetalk.Services.FcmAccessTokenManager;
import com.techtitans.usman.wetalk.Services.NotificationSender;
import com.techtitans.usman.wetalk.databinding.ActivityGroupChatBinding;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.app.DownloadManager;
import android.util.Log;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class GroupChatActivity extends AppCompatActivity {

    private static final int REQUEST_PICK_IMAGE = 101;
    private static final int REQUEST_PICK_VIDEO = 102;
    private static final int REQUEST_PICK_DOC = 103;
    private static final int REQUEST_PREVIEW_MEDIA = 104;

    private static final long MAX_VIDEO_SIZE = 20 * 1024 * 1024;
    private static final long MAX_DOC_SIZE = 5 * 1024 * 1024;

    ActivityGroupChatBinding binding;
    FirebaseDatabase database;
    FirebaseAuth auth;

    ChatAdapter adapter;
    ArrayList<MessageModel> messageList = new ArrayList<>();

    String senderId, groupId, groupName, groupIcon;
    ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityGroupChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
        senderId = auth.getUid();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://fcm.googleapis.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

        groupId = getIntent().getStringExtra("groupId");
        groupName = getIntent().getStringExtra("groupName");
        groupIcon = getIntent().getStringExtra("groupIcon");

        if (groupId == null) {
            // Global Group Chat Mode
            binding.tvUserNameChatDetails.setText("Global Group");
            binding.profileimage.setImageResource(R.drawable.avatar);
            binding.voiceCall.setVisibility(View.GONE);
            binding.videoCall.setVisibility(View.GONE);
        } else {
            // Private Group Chat Mode
            binding.tvUserNameChatDetails.setText(groupName != null ? groupName : "Group");
            if (groupIcon != null && !groupIcon.isEmpty()) {
                Picasso.get().load(groupIcon).placeholder(R.drawable.avatar).into(binding.profileimage);
            } else {
                binding.profileimage.setImageResource(R.drawable.avatar);
            }
            
            binding.toolbar2.setOnClickListener(v -> {
                Intent intent = new Intent(GroupChatActivity.this, GroupInfoActivity.class);
                intent.putExtra("groupId", groupId);
                startActivity(intent);
            });

            binding.voiceCall.setOnClickListener(v -> startGroupCall(false));
            binding.videoCall.setOnClickListener(v -> startGroupCall(true));
        }

        adapter = new ChatAdapter(messageList, this);
        binding.recyclerViewChatDetails.setAdapter(adapter);
        binding.recyclerViewChatDetails.setLayoutManager(new LinearLayoutManager(this));

        binding.imageViewSend.setOnClickListener(e -> sendMessage(null, "text", null, 0));
        binding.imageViewAttachment.setOnClickListener(v -> showAttachmentOptions());

        loadMessages();

        binding.backArrow.setOnClickListener(e -> finish());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(this, onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED);
        } else {
            registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }
    }

    private void startGroupCall(boolean isVideo) {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Starting group call...");
        dialog.setCancelable(false);
        dialog.show();

        String channelId = "group_" + groupId;

        AgoraTokenFetcher.fetchToken(channelId, new AgoraTokenFetcher.TokenCallback() {
            @Override
            public void onSuccess(String token) {
                dialog.dismiss();
                notifyGroupMembersOfCall(channelId, token, isVideo);
                
                String myUid = FirebaseAuth.getInstance().getUid();
                if (myUid != null) {
                    CallHistoryHelper.logCall(myUid, groupId, groupName, groupIcon, isVideo ? "video" : "audio", "outgoing");
                }

                Intent intent = new Intent(GroupChatActivity.this, CallActivity.class);
                intent.putExtra("channelName", channelId);
                intent.putExtra("token", token);
                intent.putExtra("isVideoCall", isVideo);
                intent.putExtra("isGroupCall", true);
                intent.putExtra("participantName", groupName);
                intent.putExtra("participantIcon", groupIcon);
                startActivity(intent);
            }

            @Override
            public void onError(String error) {
                dialog.dismiss();
                Toast.makeText(GroupChatActivity.this, "Failed to start call: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void notifyGroupMembersOfCall(String channelId, String token, boolean isVideo) {
        database.getReference().child("Groups").child(groupId).child("members").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String memberId = ds.getKey();
                    if (memberId != null && !memberId.equals(senderId)) {
                        sendCallSignalToMember(memberId, channelId, token, isVideo);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void sendCallSignalToMember(String memberId, String channelId, String token, boolean isVideo) {
        HashMap<String, Object> callRequest = new HashMap<>();
        callRequest.put("callerId", senderId);
        callRequest.put("callerName", auth.getCurrentUser().getDisplayName());
        callRequest.put("callerIcon", auth.getCurrentUser().getPhotoUrl() != null ? auth.getCurrentUser().getPhotoUrl().toString() : "");
        callRequest.put("channelName", channelId);
        callRequest.put("token", token);
        callRequest.put("groupName", groupName);
        callRequest.put("isVideoCall", isVideo);
        callRequest.put("isGroupCall", true);
        callRequest.put("status", "ringing");

        database.getReference().child("Calls").child(memberId).setValue(callRequest);

        // Also FCM for background
        database.getReference().child("FCM").child(memberId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    try {
                        JSONObject callData = new JSONObject();
                        callData.put("callerId", senderId);
                        callData.put("callerName", auth.getCurrentUser().getDisplayName());
                        callData.put("callerIcon", auth.getCurrentUser().getPhotoUrl() != null ? auth.getCurrentUser().getPhotoUrl().toString() : "");
                        callData.put("channelName", channelId);
                        callData.put("token", token);
                        callData.put("groupName", groupName);
                        callData.put("isVideoCall", String.valueOf(isVideo));
                        callData.put("isGroupCall", "true");
                        callData.put("type", "call");

                        for (DataSnapshot tokenSnapshot : snapshot.getChildren()) {
                            String fcmToken = tokenSnapshot.getValue(String.class);
                            if (fcmToken != null) {
                                sendViaRetrofit(fcmToken, "Group Call", "Incoming call from " + groupName, "call", callData);
                            }
                        }
                    } catch (Exception e) {}
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void sendViaRetrofit(String token, String title, String message, String type, JSONObject extraData) {
        Map<String, String> dataMap = new HashMap<>();
        dataMap.put("title", title);
        dataMap.put("message", message);
        dataMap.put("type", type);
        
        if (extraData != null) {
            Iterator<String> keys = extraData.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                try {
                    dataMap.put(key, extraData.getString(key));
                } catch (Exception ignored) {}
            }
        }

        NotificationSender sender = new NotificationSender(token, dataMap);
        String projectId = FirebaseApp.getInstance().getOptions().getProjectId();

        FcmAccessTokenManager.getAccessToken(this, new FcmAccessTokenManager.TokenCallback() {
            @Override
            public void onToken(String accessToken) {
                apiService.sendNotification(projectId, "Bearer " + accessToken, sender)
                        .enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                if (response.isSuccessful()) {
                                    Log.d("FCM_SEND", "Group notification sent");
                                }
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                Log.e("FCM_SEND", "FCM group error", t);
                            }
                        });
            }

            @Override
            public void onError(Exception e) {
                Log.e("FCM_SEND", "Token error: " + e.getMessage());
            }
        });
    }

    private void loadMessages() {
        String path = (groupId == null) ? "GroupChat" : "Groups/" + groupId + "/messages";
        database.getReference().child(path).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    MessageModel messageModel = dataSnapshot.getValue(MessageModel.class);
                    if (messageModel != null) {
                        messageList.add(messageModel);
                    }
                }
                adapter.notifyDataSetChanged();
                if (messageList.size() > 0) {
                    binding.recyclerViewChatDetails.scrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void sendMessage(String text, String type, String mediaUrl, long fileSize) {
        String messageStr = text;
        if (text == null) {
            messageStr = binding.tvMessage.getText().toString().trim();
        }
        
        if (messageStr.isEmpty() && mediaUrl == null) {
            Toast.makeText(this, "Type a message", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.tvMessage.setText("");
        String senderName = auth.getCurrentUser().getDisplayName();
        if (senderName == null) senderName = "User";
        
        // WhatsApp style: name inside bubble for group chats
        String displayMsg = (groupId == null || "text".equals(type)) ? String.format("\b %s \b\n %s", senderName, messageStr) : messageStr;

        MessageModel model = new MessageModel(senderId, displayMsg);
        model.setMessageTime(new Date().getTime());
        model.setType(type);
        model.setMediaUrl(mediaUrl);
        model.setFileSize(fileSize);
        model.setFileName(mediaUrl != null ? currentFileName : text); 

        String path = (groupId == null) ? "GroupChat" : "Groups/" + groupId + "/messages";
        DatabaseReference msgRef = database.getReference().child(path).push();
        model.setMessageId(msgRef.getKey()); // Fix: Added message ID for download tracking
        msgRef.setValue(model);

        // Instant local copy for sender
        if (mediaUrl != null) {
            try {
                String publicDir = Environment.DIRECTORY_PICTURES;
                if ("video".equalsIgnoreCase(type)) publicDir = Environment.DIRECTORY_MOVIES;
                else if ("document".equalsIgnoreCase(type)) publicDir = Environment.DIRECTORY_DOWNLOADS;
                
                File localFile = ChatAdapter.getLocalFile(publicDir, ChatAdapter.getUniqueFileName(model));
                if (currentFileUri != null) copyFileToLocal(currentFileUri, localFile);
            } catch (Exception e) {}
        }
    }

    private String currentFileName;
    private Uri currentFileUri;

    private void copyFileToLocal(Uri srcUri, File destFile) {
        try (InputStream in = getContentResolver().openInputStream(srcUri);
             OutputStream out = new FileOutputStream(destFile)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        } catch (IOException e) {}
    }

    private void showAttachmentOptions() {
        CharSequence[] options = new CharSequence[]{"Image", "Video", "Document"};
        new AlertDialog.Builder(this)
                .setTitle("Attach")
                .setItems(options, (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    if (which == 0) intent.setType("image/*");
                    else if (which == 1) intent.setType("video/*");
                    else intent.setType("*/*");
                    startActivityForResult(intent, which == 0 ? REQUEST_PICK_IMAGE : (which == 1 ? REQUEST_PICK_VIDEO : REQUEST_PICK_DOC));
                }).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_PREVIEW_MEDIA) {
                Uri uri = data.getParcelableExtra("uri");
                String type = data.getStringExtra("type");
                String caption = data.getStringExtra("caption");
                if (uri != null) {
                    FileMeta meta = getFileMeta(uri);
                    uploadMedia(uri, type, meta.name, meta.size, caption);
                }
                return;
            }

            if (data.getData() != null) {
                Uri fileUri = data.getData();
                FileMeta meta = getFileMeta(fileUri);
                if (requestCode == REQUEST_PICK_IMAGE) {
                    startPreview(fileUri, "image");
                } else if (requestCode == REQUEST_PICK_VIDEO) {
                    if (meta.size > MAX_VIDEO_SIZE) {
                        Toast.makeText(this, "Video too large (Max 20MB)", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    startPreview(fileUri, "video");
                } else if (requestCode == REQUEST_PICK_DOC) {
                    if (meta.size > MAX_DOC_SIZE) {
                        Toast.makeText(this, "Document too large (Max 5MB)", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    uploadMedia(fileUri, "document", meta.name, meta.size, "");
                }
            }
        }
    }

    private void startPreview(Uri uri, String type) {
        Intent intent = new Intent(this, MediaPreviewActivityV2.class);
        intent.putExtra("uri", uri);
        intent.putExtra("type", type);
        startActivityForResult(intent, REQUEST_PREVIEW_MEDIA);
    }

    private void uploadMedia(Uri uri, String type, String name, long size, String caption) {
        this.currentFileUri = uri;
        this.currentFileName = name;
        Toast.makeText(this, "Uploading attachment...", Toast.LENGTH_SHORT).show();
        String resType = "image";
        if ("video".equals(type)) resType = "video";
        else if ("document".equals(type)) resType = "raw";

        MediaManager.get().upload(uri)
                .unsigned("wetalk_profile_images")
                .option("folder", "group_media")
                .option("resource_type", resType)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}
                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String url = resultData.get("secure_url").toString();
                        sendMessage(caption, type, url, size);
                        
                        // Copy to local
                        try {
                            String publicDir = Environment.DIRECTORY_PICTURES;
                            if ("video".equalsIgnoreCase(type)) publicDir = Environment.DIRECTORY_MOVIES;
                            else if ("document".equalsIgnoreCase(type)) publicDir = Environment.DIRECTORY_DOWNLOADS;
                            
                            MessageModel temp = new MessageModel();
                            temp.setMessageId("temp"); // won't match exactly but prevents download icon
                            temp.setFileName(name);
                            File localFile = ChatAdapter.getLocalFile(publicDir, ChatAdapter.getUniqueFileName(temp));
                        } catch (Exception e) {}
                    }
                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(GroupChatActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private FileMeta getFileMeta(Uri uri) {
        String name = "file"; long size = 0;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int ni = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int si = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (ni != -1) name = cursor.getString(ni);
                if (si != -1) size = cursor.getLong(si);
            }
        } catch (Exception e) {}
        return new FileMeta(name, size);
    }

    private static class FileMeta {
        String name; long size;
        FileMeta(String n, long s) { name = n; size = s; }
    }

    private final BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            adapter.notifyDataSetChanged();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(onDownloadComplete); } catch (Exception e) {}
    }
}
