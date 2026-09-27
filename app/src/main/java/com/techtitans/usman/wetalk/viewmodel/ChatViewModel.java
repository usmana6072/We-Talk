package com.techtitans.usman.wetalk.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.techtitans.usman.wetalk.Models.MessageModel;
import com.techtitans.usman.wetalk.db.MessageEntity;
import com.techtitans.usman.wetalk.repository.ChatRepository;

import java.util.ArrayList;
import java.util.List;

public class ChatViewModel extends AndroidViewModel {

    private final ChatRepository repository;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private String chatRoomId;
    private String firebasePath;

    private int currentLimit = ChatRepository.RECENT_MESSAGES_LIMIT;
    private final MutableLiveData<Boolean> isLoadingOlderLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> toastMessageLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<MessageModel>> messagesLiveData = new MutableLiveData<>();

    private LiveData<List<MessageEntity>> currentDbLiveData;
    private Observer<List<MessageEntity>> dbObserver;

    public ChatViewModel(@NonNull Application application) {
        super(application);
        this.repository = ChatRepository.getInstance(application);
    }

    public void init(String chatRoomId, String firebasePath) {
        if (this.chatRoomId != null && this.chatRoomId.equals(chatRoomId)) {
            return; // Already initialized
        }

        this.chatRoomId = chatRoomId;
        this.firebasePath = firebasePath;

        observeLocalDb();

        // Start background synchronization
        repository.startSync(chatRoomId, firebasePath);
    }

    private void observeLocalDb() {
        if (chatRoomId == null) return;

        mainHandler.post(() -> {
            if (currentDbLiveData != null && dbObserver != null) {
                currentDbLiveData.removeObserver(dbObserver);
            }

            dbObserver = entities -> {
                List<MessageModel> models = new ArrayList<>();
                if (entities != null) {
                    for (MessageEntity entity : entities) {
                        models.add(MessageModel.fromEntity(entity));
                    }
                }
                messagesLiveData.setValue(models);
            };

            currentDbLiveData = repository.observeMessages(chatRoomId, currentLimit);
            currentDbLiveData.observeForever(dbObserver);
        });
    }

    public LiveData<List<MessageModel>> getMessagesLiveData() {
        return messagesLiveData;
    }

    public LiveData<Boolean> getIsLoadingOlder() {
        return isLoadingOlderLiveData;
    }

    public LiveData<String> getToastMessage() {
        return toastMessageLiveData;
    }

    public void loadOlderMessages() {
        if (Boolean.TRUE.equals(isLoadingOlderLiveData.getValue()) || chatRoomId == null || firebasePath == null) {
            return;
        }

        List<MessageModel> currentList = messagesLiveData.getValue();
        long oldestTimestamp = 0;
        if (currentList != null && !currentList.isEmpty()) {
            oldestTimestamp = currentList.get(0).getMessageTime();
        }

        isLoadingOlderLiveData.setValue(true);

        long finalOldestTimestamp = oldestTimestamp;
        repository.loadOlderMessages(chatRoomId, firebasePath, oldestTimestamp, ChatRepository.OLDER_MESSAGES_PAGE_SIZE, new ChatRepository.PaginationCallback() {
            @Override
            public void onSuccess(int loadedCount) {
                mainHandler.post(() -> {
                    isLoadingOlderLiveData.setValue(false);
                    if (loadedCount > 0) {
                        currentLimit += ChatRepository.OLDER_MESSAGES_PAGE_SIZE;
                        observeLocalDb();
                    } else if (finalOldestTimestamp > 0) {
                        toastMessageLiveData.setValue("No older messages found");
                    }
                });
            }

            @Override
            public void onError(String message) {
                mainHandler.post(() -> {
                    isLoadingOlderLiveData.setValue(false);
                    if (message != null) {
                        toastMessageLiveData.setValue(message);
                    }
                });
            }
        });
    }

    public void saveMessageLocally(MessageModel model) {
        if (model != null && chatRoomId != null) {
            repository.saveMessageLocally(model.toEntity(chatRoomId));
        }
    }

    public void deleteMessageLocally(String messageId) {
        if (messageId != null && chatRoomId != null) {
            repository.deleteMessageLocally(chatRoomId, messageId);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mainHandler.post(() -> {
            if (currentDbLiveData != null && dbObserver != null) {
                currentDbLiveData.removeObserver(dbObserver);
            }
        });
        if (chatRoomId != null && firebasePath != null) {
            repository.stopSync(chatRoomId, firebasePath);
        }
    }
}
