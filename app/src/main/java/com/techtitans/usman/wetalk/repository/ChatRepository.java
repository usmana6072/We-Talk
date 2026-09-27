package com.techtitans.usman.wetalk.repository;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.techtitans.usman.wetalk.Models.MessageModel;
import com.techtitans.usman.wetalk.db.AppDatabase;
import com.techtitans.usman.wetalk.db.MessageDao;
import com.techtitans.usman.wetalk.db.MessageEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatRepository {

    public static final int RECENT_MESSAGES_LIMIT = 30;
    public static final int OLDER_MESSAGES_PAGE_SIZE = 30;

    private static volatile ChatRepository INSTANCE;

    private final MessageDao messageDao;
    private final FirebaseDatabase firebaseDatabase;
    private final ExecutorService executor;
    private final Context context;

    private final Map<String, ChildEventListener> activeSyncListeners = new HashMap<>();

    public interface PaginationCallback {
        void onSuccess(int loadedCount);
        void onError(String message);
    }

    private ChatRepository(Context context) {
        this.context = context.getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(this.context);
        this.messageDao = db.messageDao();
        this.firebaseDatabase = FirebaseDatabase.getInstance();
        this.executor = Executors.newFixedThreadPool(4);
    }

    public static ChatRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ChatRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = ChatRepository.getInstanceUnsafe(context);
                }
            }
        }
        return INSTANCE;
    }

    private static synchronized ChatRepository getInstanceUnsafe(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new ChatRepository(context);
        }
        return INSTANCE;
    }

    public LiveData<List<MessageEntity>> observeMessages(String chatRoomId, int limit) {
        return messageDao.observeLatestMessages(chatRoomId, limit);
    }

    public void startSync(String chatRoomId, String firebasePath) {
        if (activeSyncListeners.containsKey(chatRoomId)) {
            return; // Already syncing
        }

        DatabaseReference ref = firebaseDatabase.getReference().child(firebasePath);
        try {
            ref.keepSynced(true);
        } catch (Exception ignored) {}

        ChildEventListener listener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                processAndSaveMessage(snapshot, chatRoomId);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                processAndSaveMessage(snapshot, chatRoomId);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String messageId = snapshot.getKey();
                if (messageId != null) {
                    executor.execute(() -> messageDao.deleteMessage(chatRoomId, messageId));
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };

        activeSyncListeners.put(chatRoomId, listener);
        ref.addChildEventListener(listener);
    }

    public void stopSync(String chatRoomId, String firebasePath) {
        ChildEventListener listener = activeSyncListeners.remove(chatRoomId);
        if (listener != null) {
            firebaseDatabase.getReference().child(firebasePath).removeEventListener(listener);
        }
    }

    private void processAndSaveMessage(DataSnapshot snapshot, String chatRoomId) {
        MessageModel model = snapshot.getValue(MessageModel.class);
        if (model != null) {
            String msgId = snapshot.getKey();
            if (msgId != null) {
                model.setMessageId(msgId);
                MessageEntity entity = model.toEntity(chatRoomId);
                executor.execute(() -> messageDao.insertMessage(entity));
            }
        }
    }

    public void loadOlderMessages(String chatRoomId, String firebasePath, long oldestTimestamp, int pageSize, PaginationCallback callback) {
        executor.execute(() -> {
            int localOlderCount = messageDao.getOlderMessageCount(chatRoomId, oldestTimestamp);
            if (localOlderCount >= pageSize) {
                // We already have enough older messages in local DB
                if (callback != null) callback.onSuccess(localOlderCount);
                return;
            }

            if (!isNetworkAvailable()) {
                if (callback != null) callback.onError("Older messages unavailable offline");
                return;
            }

            Query query;
            if (oldestTimestamp > 0) {
                query = firebaseDatabase.getReference().child(firebasePath)
                        .orderByChild("messageTime")
                        .endBefore((double) oldestTimestamp)
                        .limitToLast(pageSize);
            } else {
                query = firebaseDatabase.getReference().child(firebasePath)
                        .orderByChild("messageTime")
                        .limitToLast(pageSize);
            }

            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<MessageEntity> entities = new ArrayList<>();
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        MessageModel model = ds.getValue(MessageModel.class);
                        if (model != null) {
                            model.setMessageId(ds.getKey());
                            entities.add(model.toEntity(chatRoomId));
                        }
                    }

                    if (!entities.isEmpty()) {
                        executor.execute(() -> {
                            messageDao.insertMessages(entities);
                            if (callback != null) callback.onSuccess(entities.size());
                        });
                    } else {
                        if (callback != null) callback.onSuccess(0);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (callback != null) callback.onError(error.getMessage());
                }
            });
        });
    }

    public void saveMessageLocally(MessageEntity entity) {
        executor.execute(() -> messageDao.insertMessage(entity));
    }

    public void deleteMessageLocally(String chatRoomId, String messageId) {
        executor.execute(() -> messageDao.deleteMessage(chatRoomId, messageId));
    }

    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            }
        } catch (Exception ignored) {}
        return false;
    }
}
