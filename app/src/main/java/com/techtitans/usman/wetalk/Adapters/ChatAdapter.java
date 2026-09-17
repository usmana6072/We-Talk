package com.techtitans.usman.wetalk.Adapters;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;
import com.techtitans.usman.wetalk.Models.MessageModel;
import com.techtitans.usman.wetalk.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    ArrayList<MessageModel> list;
    Context context;
    String receiverId;

    int SENDER_VIEW_TYPE = 1;
    int RECEIVER_VIEW_TYPE = 2;

    // Track in-progress downloads to prevent multiple requests
    private static final Set<String> inProgressDownloads = new HashSet<>();

    public ChatAdapter(ArrayList<MessageModel> list, Context context) {
        this.list = list;
        this.context = context;
    }

    public ChatAdapter(ArrayList<MessageModel> list, Context context, String receiverId) {
        this.list = list;
        this.context = context;
        this.receiverId = receiverId;
    }

    @Override
    public int getItemViewType(int position) {
        if (list.get(position).getUserId().equals(FirebaseAuth.getInstance().getUid()))
            return SENDER_VIEW_TYPE;
        else
            return RECEIVER_VIEW_TYPE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == RECEIVER_VIEW_TYPE) {
            view = LayoutInflater.from(context).inflate(R.layout.sample_receiver_layout, parent, false);
            return new RecieverViewHolder(view);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.sample_sender_layout, parent, false);
            return new SenderViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageModel message = list.get(position);

        holder.itemView.setOnLongClickListener(e -> {
            new AlertDialog.Builder(context).setTitle("Confirmation")
                    .setMessage("Are you sure you want to delete message?")
                    .setPositiveButton("Delete for me", (dialog, which) -> {
                        String chatId = FirebaseAuth.getInstance().getUid() + receiverId;
                        FirebaseDatabase.getInstance().getReference().child("Chats").child(chatId)
                                .child(message.getMessageId()).removeValue();
                    })
                    .setNegativeButton("Delete for everyone", (dialog, which) -> {
                        String chatId = FirebaseAuth.getInstance().getUid() + receiverId;
                        FirebaseDatabase.getInstance().getReference().child("Chats").child(chatId)
                                .child(message.getMessageId()).removeValue();
                        FirebaseDatabase.getInstance().getReference().child("Chats").child(receiverId + FirebaseAuth.getInstance().getUid())
                                .child(message.getMessageId()).removeValue();
                    }).setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss()).show();
            return true;
        });

        SimpleDateFormat dateFormat = new SimpleDateFormat("h:mm a");
        String formattedTime = dateFormat.format(new Date(message.getMessageTime()));

        if (holder instanceof SenderViewHolder) {
            SenderViewHolder senderHolder = (SenderViewHolder) holder;
            bindMediaAndText(message, senderHolder.senderMessage, senderHolder.senderTime, formattedTime,
                    senderHolder.mediaContainer, senderHolder.imageMediaPreview, senderHolder.playIconOverlay,
                    senderHolder.downloadIconOverlay, senderHolder.documentContainer, senderHolder.tvDocName,
                    senderHolder.tvDocSize, senderHolder.docDownloadIcon);
        } else if (holder instanceof RecieverViewHolder) {
            RecieverViewHolder receiverHolder = (RecieverViewHolder) holder;
            bindMediaAndText(message, receiverHolder.receiverMessage, receiverHolder.receiverTime, formattedTime,
                    receiverHolder.mediaContainer, receiverHolder.imageMediaPreview, receiverHolder.playIconOverlay,
                    receiverHolder.downloadIconOverlay, receiverHolder.documentContainer, receiverHolder.tvDocName,
                    receiverHolder.tvDocSize, receiverHolder.docDownloadIcon);
        }
    }

    private void bindMediaAndText(
            MessageModel message, TextView tvText, TextView tvTime, String formattedTime,
            FrameLayout mediaContainer, ImageView imageMediaPreview, ImageView playIconOverlay,
            ImageView downloadIconOverlay, LinearLayout documentContainer, TextView tvDocName,
            TextView tvDocSize, ImageView docDownloadIcon
    ) {
        tvTime.setText(formattedTime);

        String messageText = message.getMessageText();
        if (messageText != null && !messageText.isEmpty()) {
            tvText.setVisibility(View.VISIBLE);
            tvText.setText(messageText);
        } else {
            tvText.setVisibility(View.GONE);
        }

        String type = message.getType();
        String uniqueFileName = getUniqueFileName(message);

        if ("image".equalsIgnoreCase(type)) {
            documentContainer.setVisibility(View.GONE);
            mediaContainer.setVisibility(View.VISIBLE);
            playIconOverlay.setVisibility(View.GONE);

            File localFile = getLocalFile(Environment.DIRECTORY_PICTURES, uniqueFileName);
            if (localFile.exists()) {
                inProgressDownloads.remove(message.getMessageId());
                downloadIconOverlay.setVisibility(View.GONE);
                Picasso.get().load(localFile).placeholder(R.drawable.avatar).into(imageMediaPreview);
                mediaContainer.setOnClickListener(v -> openFileLocally(localFile, "image/*"));
            } else {
                downloadIconOverlay.setVisibility(View.VISIBLE);
                if (message.getMediaUrl() != null && !message.getMediaUrl().isEmpty()) {
                    Picasso.get().load(message.getMediaUrl()).placeholder(R.drawable.avatar).into(imageMediaPreview);
                }
                mediaContainer.setOnClickListener(v -> downloadMedia(message, Environment.DIRECTORY_PICTURES));
            }

        } else if ("video".equalsIgnoreCase(type)) {
            documentContainer.setVisibility(View.GONE);
            mediaContainer.setVisibility(View.VISIBLE);

            File localFile = getLocalFile(Environment.DIRECTORY_MOVIES, uniqueFileName);
            if (localFile.exists()) {
                inProgressDownloads.remove(message.getMessageId());
                downloadIconOverlay.setVisibility(View.GONE);
                playIconOverlay.setVisibility(View.VISIBLE);
                if (message.getMediaUrl() != null && !message.getMediaUrl().isEmpty()) {
                    Picasso.get().load(message.getMediaUrl()).placeholder(R.drawable.avatar).into(imageMediaPreview);
                }
                mediaContainer.setOnClickListener(v -> openFileLocally(localFile, "video/*"));
            } else {
                playIconOverlay.setVisibility(View.GONE);
                downloadIconOverlay.setVisibility(View.VISIBLE);
                if (message.getMediaUrl() != null && !message.getMediaUrl().isEmpty()) {
                    Picasso.get().load(message.getMediaUrl()).placeholder(R.drawable.avatar).into(imageMediaPreview);
                }
                mediaContainer.setOnClickListener(v -> downloadMedia(message, Environment.DIRECTORY_MOVIES));
            }

        } else if ("document".equalsIgnoreCase(type)) {
            mediaContainer.setVisibility(View.GONE);
            documentContainer.setVisibility(View.VISIBLE);

            tvDocName.setText(message.getFileName() != null ? message.getFileName() : "Document");
            tvDocSize.setText(formatFileSize(message.getFileSize()));

            File localFile = getLocalFile(Environment.DIRECTORY_DOWNLOADS, uniqueFileName);
            if (localFile.exists()) {
                inProgressDownloads.remove(message.getMessageId());
                docDownloadIcon.setVisibility(View.GONE);
                documentContainer.setOnClickListener(v -> openFileLocally(localFile, "*/*"));
            } else {
                docDownloadIcon.setVisibility(View.VISIBLE);
                documentContainer.setOnClickListener(v -> downloadMedia(message, Environment.DIRECTORY_DOWNLOADS));
            }
        } else {
            mediaContainer.setVisibility(View.GONE);
            documentContainer.setVisibility(View.GONE);
        }
    }

    public static String getUniqueFileName(MessageModel message) {
        String originalName = message.getFileName();
        if (originalName == null || originalName.isEmpty()) {
            originalName = "file";
        }
        return "WeTalk_" + message.getMessageId() + "_" + originalName;
    }

    public static File getLocalFile(String publicDirectory, String fileName) {
        File dir = new File(Environment.getExternalStoragePublicDirectory(publicDirectory), "WeTalk");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, fileName);
    }

    private void downloadMedia(MessageModel message, String publicDirectory) {
        String messageId = message.getMessageId();
        if (messageId == null) return;

        if (inProgressDownloads.contains(messageId)) {
            Toast.makeText(context, "Download already in progress...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (message.getMediaUrl() == null || message.getMediaUrl().isEmpty()) {
            Toast.makeText(context, "Invalid media URL", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            inProgressDownloads.add(messageId);
            String name = getUniqueFileName(message);

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(message.getMediaUrl()));
            request.setTitle(message.getFileName() != null ? message.getFileName() : "WeTalk Attachment");
            request.setDescription("Downloading file...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(publicDirectory, "WeTalk/" + name);

            DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager != null) {
                manager.enqueue(request);
                Toast.makeText(context, "Starting download...", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            inProgressDownloads.remove(messageId);
            Toast.makeText(context, "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openFileLocally(File file, String mimeType) {
        try {
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Added for broader compatibility
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String formatFileSize(long sizeInBytes) {
        if (sizeInBytes <= 0) return "";
        double sizeInMb = sizeInBytes / (1024.0 * 1024.0);
        if (sizeInMb >= 1.0) {
            return String.format("%.1f MB", sizeInMb);
        } else {
            double sizeInKb = sizeInBytes / 1024.0;
            return String.format("%.0f KB", sizeInKb);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class RecieverViewHolder extends RecyclerView.ViewHolder {
        TextView receiverMessage, receiverTime, tvDocName, tvDocSize;
        FrameLayout mediaContainer;
        ImageView imageMediaPreview, playIconOverlay, downloadIconOverlay, docDownloadIcon;
        LinearLayout documentContainer;

        public RecieverViewHolder(@NonNull View itemView) {
            super(itemView);
            receiverMessage = itemView.findViewById(R.id.receivertext);
            receiverTime = itemView.findViewById(R.id.recieivertime);
            mediaContainer = itemView.findViewById(R.id.mediaContainer);
            imageMediaPreview = itemView.findViewById(R.id.imageMediaPreview);
            playIconOverlay = itemView.findViewById(R.id.playIconOverlay);
            downloadIconOverlay = itemView.findViewById(R.id.downloadIconOverlay);
            documentContainer = itemView.findViewById(R.id.documentContainer);
            tvDocName = itemView.findViewById(R.id.tvDocName);
            tvDocSize = itemView.findViewById(R.id.tvDocSize);
            docDownloadIcon = itemView.findViewById(R.id.docDownloadIcon);
        }
    }

    public static class SenderViewHolder extends RecyclerView.ViewHolder {
        TextView senderMessage, senderTime, tvDocName, tvDocSize;
        FrameLayout mediaContainer;
        ImageView imageMediaPreview, playIconOverlay, downloadIconOverlay, docDownloadIcon;
        LinearLayout documentContainer;

        public SenderViewHolder(@NonNull View itemView) {
            super(itemView);
            senderMessage = itemView.findViewById(R.id.senderText);
            senderTime = itemView.findViewById(R.id.senderTime);
            mediaContainer = itemView.findViewById(R.id.mediaContainer);
            imageMediaPreview = itemView.findViewById(R.id.imageMediaPreview);
            playIconOverlay = itemView.findViewById(R.id.playIconOverlay);
            downloadIconOverlay = itemView.findViewById(R.id.downloadIconOverlay);
            documentContainer = itemView.findViewById(R.id.documentContainer);
            tvDocName = itemView.findViewById(R.id.tvDocName);
            tvDocSize = itemView.findViewById(R.id.tvDocSize);
            docDownloadIcon = itemView.findViewById(R.id.docDownloadIcon);
        }
    }
}
