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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.techtitans.usman.wetalk.Adapters.ChatAdapter;
import com.techtitans.usman.wetalk.Models.MessageModel;
import com.techtitans.usman.wetalk.databinding.ActivityChatDetailBinding;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

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
    ProgressDialog progressDialog;

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
        adapter = new ChatAdapter(list, ChatDetailActivity.this, receiverId);

        binding.tvUserNameChatDetails.setText(receiverUserName);
        Picasso.get().load(receiverProfile).placeholder(R.drawable.avatar).into(binding.profileimage);

        binding.backArrow.setOnClickListener(e -> finish());

        senderRoom = senderId + receiverId;
        receiverRoom = receiverId + senderId;

        binding.recyclerViewChatDetails.setAdapter(adapter);
        binding.recyclerViewChatDetails.setLayoutManager(new LinearLayoutManager(this));

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Uploading Attachment");
        progressDialog.setMessage("Please wait while file is uploading...");
        progressDialog.setCancelable(false);

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
            registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }
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
