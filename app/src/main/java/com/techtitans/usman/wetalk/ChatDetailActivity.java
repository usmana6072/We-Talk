package com.techtitans.usman.wetalk;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.core.content.ContextCompat;

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
import com.techtitans.usman.wetalk.databinding.ActivityChatDetailBinding;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.util.Log;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ChatDetailActivity extends AppCompatActivity {

    private static final int REQUEST_PICK_IMAGE = 101;
    private static final int REQUEST_PICK_VIDEO = 102;
    private static final int REQUEST_PICK_DOC = 103;
    private static final int REQUEST_PREVIEW_MEDIA = 104;

    private static final long MAX_VIDEO_SIZE = 20 * 1024 * 1024; // 20 MB
    private static final long MAX_DOC_SIZE = 5 * 1024 * 1024;    // 5 MB

    FirebaseDatabase database;
    FirebaseAuth auth;
    ActivityChatDetailBinding binding;
    String senderId, receiverId, receiverUserName, receiverProfile, senderRoom, receiverRoom;
    MessageModel model;

    ArrayList<MessageModel> list = new ArrayList<>();
    ChatAdapter adapter;
    ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityChatDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();

        senderId = auth.getUid();
        receiverId = getIntent().getStringExtra("userId");
        receiverProfile = getIntent().getStringExtra("profile");
        receiverUserName = getIntent().getStringExtra("username");

        if (senderId == null || receiverId == null || receiverId.trim().isEmpty()) {
            Toast.makeText(this, "Invalid user", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (receiverUserName == null || receiverUserName.trim().isEmpty()) {
            receiverUserName = "User";
        }

        adapter = new ChatAdapter(list, ChatDetailActivity.this, receiverId);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://fcm.googleapis.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

        binding.tvUserNameChatDetails.setText(receiverUserName);
        if (receiverProfile != null && !receiverProfile.trim().isEmpty()) {
            Picasso.get().load(receiverProfile).placeholder(R.drawable.avatar).error(R.drawable.avatar).into(binding.profileimage);
        } else {
            binding.profileimage.setImageResource(R.drawable.avatar);
        }

        binding.backArrow.setOnClickListener(e -> finish());

        senderRoom = senderId + receiverId;
        receiverRoom = receiverId + senderId;

        binding.recyclerViewChatDetails.setAdapter(adapter);
        binding.recyclerViewChatDetails.setLayoutManager(new LinearLayoutManager(this));

        binding.imageViewSend.setOnClickListener(e -> {
            String message = binding.tvMessage.getText().toString();
            if (!message.isEmpty()) {
                model = new MessageModel(senderId, message);
                model.setMessageTime(new Date().getTime());
                model.setReceiverId(receiverId);
                model.setType("text");
                binding.tvMessage.setText("");

                DatabaseReference messageRef = database
                        .getReference()
                        .child("Chats")
                        .child(senderRoom)
                        .push();

                model.setMessageId(messageRef.getKey());
                messageRef.setValue(model);

                database.getReference().child("Users").child(receiverId)
                        .child("timeStam").setValue(new Date().getTime());

                database.getReference().child("Users").child(senderId)
                        .child("timeStam").setValue(new Date().getTime());

                if (!senderRoom.equals(receiverRoom)) {
                    database.getReference().child("Chats").child(receiverRoom).child(model.getMessageId())
                            .setValue(model);
                }

                // Send Notification
                sendPushNotification(receiverId, "New Message from " + auth.getCurrentUser().getDisplayName(), message, "message", null);
            } else {
                Toast.makeText(this, "Please Type a Message", Toast.LENGTH_SHORT).show();
            }
        });

        binding.imageViewAttachment.setOnClickListener(v -> showAttachmentOptions());

        database.getReference().child("Chats").child(senderRoom).
                addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        list.clear();
                        for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                            MessageModel modele = snapshot1.getValue(MessageModel.class);
                            if (modele != null) {
                                list.add(modele);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        if (list.size() > 0) {
                            binding.recyclerViewChatDetails.post(() -> {
                                binding.recyclerViewChatDetails.scrollToPosition(list.size() - 1);
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        binding.tvUserNameChatDetails.setOnClickListener(e -> {
            Intent intent = new Intent(ChatDetailActivity.this, ProfileViewActivity.class);
            intent.putExtra("receiverId", receiverId);
            startActivity(intent);
        });
        binding.profileimage.setOnClickListener(e -> {
            Intent intent = new Intent(ChatDetailActivity.this, ProfileViewActivity.class);
            intent.putExtra("receiverId", receiverId);
            startActivity(intent);
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(this, onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED);
        } else {
            ContextCompat.registerReceiver(this, onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED);
        }
        binding.videoCall.setOnClickListener(e -> launchCallScreen(true));
        binding.voiceCall.setOnClickListener(e-> launchCallScreen(false));
    }

    private final BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(onDownloadComplete);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAttachmentOptions() {
        CharSequence[] options = new CharSequence[]{"Image (No Limit)", "Video (Max 20MB)", "Document (Max 5MB)"};
        new AlertDialog.Builder(this)
                .setTitle("Select Media Type")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        startActivityForResult(intent, REQUEST_PICK_IMAGE);
                    } else if (which == 1) {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("video/*");
                        startActivityForResult(intent, REQUEST_PICK_VIDEO);
                    } else if (which == 2) {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("*/*");
                        startActivityForResult(intent, REQUEST_PICK_DOC);
                    }
                }).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {

            if (requestCode == REQUEST_PREVIEW_MEDIA) {
                Uri previewUri = data.getParcelableExtra("uri");
                String type = data.getStringExtra("type");
                String caption = data.getStringExtra("caption");
                if (previewUri != null) {
                    FileMeta meta = getFileMeta(previewUri);
                    uploadMediaToCloudinary(previewUri, type, meta.name, meta.size, caption);
                }
                return;
            }

            if (data.getData() != null) {
                Uri fileUri = data.getData();
                FileMeta fileMeta = getFileMeta(fileUri);

                if (requestCode == REQUEST_PICK_IMAGE) {
                    Intent intent = new Intent(this, MediaPreviewActivityV2.class);
                    intent.putExtra("uri", fileUri);
                    intent.putExtra("type", "image");
                    startActivityForResult(intent, REQUEST_PREVIEW_MEDIA);
                } else if (requestCode == REQUEST_PICK_VIDEO) {
                    if (fileMeta.size > MAX_VIDEO_SIZE) {
                        showSizeErrorDialog("Video", "20 MB", fileMeta.size);
                        return;
                    }
                    Intent intent = new Intent(this, MediaPreviewActivityV2.class);
                    intent.putExtra("uri", fileUri);
                    intent.putExtra("type", "video");
                    startActivityForResult(intent, REQUEST_PREVIEW_MEDIA);
                } else if (requestCode == REQUEST_PICK_DOC) {
                    if (fileMeta.size > MAX_DOC_SIZE) {
                        showSizeErrorDialog("Document", "5 MB", fileMeta.size);
                        return;
                    }
                    uploadMediaToCloudinary(fileUri, "document", fileMeta.name, fileMeta.size, "");
                }
            }
        }
    }

    private void showSizeErrorDialog(String fileType, String maxLimit, long actualSize) {
        double sizeInMb = actualSize / (1024.0 * 1024.0);
        String formattedSize = String.format("%.2f MB", sizeInMb);
        new AlertDialog.Builder(this)
                .setTitle("File Size Exceeded")
                .setMessage(fileType + " size limit is " + maxLimit + ".\nYour selected file is " + formattedSize + ".")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void uploadMediaToCloudinary(Uri uri, String type, String fileName, long fileSize, String caption) {
        Toast.makeText(this, "Uploading attachment...", Toast.LENGTH_SHORT).show();
        String resourceType = "auto";
        if ("video".equalsIgnoreCase(type)) {
            resourceType = "video";
        } else if ("document".equalsIgnoreCase(type)) {
            resourceType = "raw";
        } else {
            resourceType = "image";
        }

        MediaManager.get().upload(uri)
                .unsigned("wetalk_profile_images")
                .option("folder", "chat_media")
                .option("resource_type", resourceType)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) { }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) { }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String secureUrl = resultData.get("secure_url").toString();
                        
                        MessageModel mediaModel = new MessageModel(senderId, caption);
                        mediaModel.setType(type);
                        mediaModel.setMediaUrl(secureUrl);
                        mediaModel.setFileName(fileName);
                        mediaModel.setFileSize(fileSize);
                        mediaModel.setMessageTime(new Date().getTime());
                        mediaModel.setReceiverId(receiverId);

                        DatabaseReference messageRef = database.getReference().child("Chats").child(senderRoom).push();
                        mediaModel.setMessageId(messageRef.getKey());
                        messageRef.setValue(mediaModel);

                        database.getReference().child("Users").child(receiverId).child("timeStam").setValue(new Date().getTime());
                        database.getReference().child("Users").child(senderId).child("timeStam").setValue(new Date().getTime());

                        if (!senderRoom.equals(receiverRoom)) {
                            database.getReference().child("Chats").child(receiverRoom).child(mediaModel.getMessageId()).setValue(mediaModel);
                        }

                        // Copy the file to local WeTalk folder so the sender doesn't have to download it
                        try {
                            String publicDir = Environment.DIRECTORY_PICTURES;
                            if ("video".equalsIgnoreCase(type)) publicDir = Environment.DIRECTORY_MOVIES;
                            else if ("document".equalsIgnoreCase(type)) publicDir = Environment.DIRECTORY_DOWNLOADS;

                            File localFile = ChatAdapter.getLocalFile(publicDir, ChatAdapter.getUniqueFileName(mediaModel));
                            copyFileToLocal(uri, localFile);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        Toast.makeText(ChatDetailActivity.this, "Attachment sent successfully", Toast.LENGTH_SHORT).show();
                        
                        // Send Notification for Attachment
                        sendPushNotification(receiverId, "New Attachment from " + auth.getCurrentUser().getDisplayName(), "Sent a " + type, "message", null);
                    }

                    private void copyFileToLocal(Uri srcUri, File destFile) {
                        try (InputStream in = getContentResolver().openInputStream(srcUri);
                             OutputStream out = new FileOutputStream(destFile)) {
                            byte[] buf = new byte[8192];
                            int len;
                            while ((len = in.read(buf)) > 0) {
                                out.write(buf, 0, len);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(ChatDetailActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                    }
                }).dispatch();
    }

    private void launchCallScreen(boolean isVideo) {
        ProgressDialog callDialog = new ProgressDialog(this);
        callDialog.setMessage("Preparing call...");
        callDialog.setCancelable(false);
        callDialog.show();

        String channelId = (senderId.compareTo(receiverId) < 0)
                ? senderId + "_" + receiverId
                : receiverId + "_" + senderId;

        // Fetch valid Token from Railway server
        AgoraTokenFetcher.fetchToken(channelId, new AgoraTokenFetcher.TokenCallback() {
            @Override
            public void onSuccess(String token) {
                callDialog.dismiss();
                initiateCallWithToken(channelId, token, isVideo);
            }

            @Override
            public void onError(String error) {
                callDialog.dismiss();
                Toast.makeText(ChatDetailActivity.this, "Failed to get token: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initiateCallWithToken(String channelId, String token, boolean isVideo) {
        // Signaling: Push call request to receiver's node
        HashMap<String, Object> callRequest = new HashMap<>();
        callRequest.put("callerId", senderId);
        callRequest.put("callerName", auth.getCurrentUser().getDisplayName() != null ? auth.getCurrentUser().getDisplayName() : "Someone");
        callRequest.put("callerIcon", auth.getCurrentUser().getPhotoUrl() != null ? auth.getCurrentUser().getPhotoUrl().toString() : "");
        callRequest.put("channelName", channelId);
        callRequest.put("token", token); 
        callRequest.put("isVideoCall", isVideo);
        callRequest.put("isGroupCall", false);
        callRequest.put("status", "ringing");

        database.getReference().child("Calls").child(receiverId).setValue(callRequest);

        // Send FCM Notification for Background Waking
        try {
            JSONObject callData = new JSONObject();
            callData.put("callerId", senderId);
            callData.put("callerName", auth.getCurrentUser().getDisplayName());
            callData.put("callerIcon", auth.getCurrentUser().getPhotoUrl() != null ? auth.getCurrentUser().getPhotoUrl().toString() : "");
            callData.put("channelName", channelId);
            callData.put("token", token);
            callData.put("isVideoCall", String.valueOf(isVideo));
            callData.put("isGroupCall", "false");
            callData.put("type", "call"); 
            
            sendPushNotification(receiverId, "Incoming Call", "Someone is calling you...", "call", callData);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Log Call History for caller and receiver
        String currentUserName = (auth.getCurrentUser() != null && auth.getCurrentUser().getDisplayName() != null)
                ? auth.getCurrentUser().getDisplayName() : "Someone";
        String currentUserPic = (auth.getCurrentUser() != null && auth.getCurrentUser().getPhotoUrl() != null)
                ? auth.getCurrentUser().getPhotoUrl().toString() : "";
        String callTypeStr = isVideo ? "video" : "audio";

        CallHistoryHelper.logCall(senderId, receiverId, receiverUserName, receiverProfile, callTypeStr, "outgoing", false);
        CallHistoryHelper.logCall(receiverId, senderId, currentUserName, currentUserPic, callTypeStr, "incoming", false);

        Intent intent = new Intent(ChatDetailActivity.this, CallActivity.class);
        intent.putExtra("channelName", channelId);
        intent.putExtra("token", token);
        intent.putExtra("isVideoCall", isVideo);
        intent.putExtra("isGroupCall", false);
        intent.putExtra("receiverId", receiverId);
        intent.putExtra("participantName", receiverUserName);
        intent.putExtra("participantIcon", receiverProfile);
        startActivity(intent);
    }

    private void sendPushNotification(String receiverId, String title, String message, String type, JSONObject extraData) {
        database.getReference().child("FCM").child(receiverId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot tokenSnapshot : snapshot.getChildren()) {
                        String token = tokenSnapshot.getValue(String.class);
                        if (token != null) {
                            sendViaRetrofit(token, title, message, type, extraData);
                        }
                    }
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
                                    Log.d("FCM_SEND", "Notification sent successfully");
                                } else {
                                    Log.e("FCM_SEND", "FCM failed: " + response.code());
                                }
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                Log.e("FCM_SEND", "FCM network error", t);
                            }
                        });
            }

            @Override
            public void onError(Exception e) {
                Log.e("FCM_SEND", "Token error: " + e.getMessage());
            }
        });
    }

    private FileMeta getFileMeta(Uri uri) {
        String name = "file";
        long size = 0;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (nameIndex != -1) name = cursor.getString(nameIndex);
                if (sizeIndex != -1) size = cursor.getLong(sizeIndex);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new FileMeta(name, size);
    }

    private static class FileMeta {
        String name;
        long size;

        FileMeta(String name, long size) {
            this.name = name;
            this.size = size;
        }
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }
}
