package com.techtitans.usman.wetalk.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.techtitans.usman.wetalk.Adapters.UserAdapterChatView;
import com.techtitans.usman.wetalk.Models.MessageModel;
import com.techtitans.usman.wetalk.Models.Users;
import com.techtitans.usman.wetalk.UsersListActivity;
import com.techtitans.usman.wetalk.databinding.FragmentChatBinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ChatFragment extends Fragment {

    private FirebaseDatabase database;
    private FragmentChatBinding binding;
    private ArrayList<Users> list;
    private DatabaseReference chatsRef;
    private ValueEventListener chatsListener;
    private UserAdapterChatView adapter;

    public ChatFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        database = FirebaseDatabase.getInstance();
        list = new ArrayList<>();

        adapter = new UserAdapterChatView(list, getContext());
        binding.recyclerViewChatFragment.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewChatFragment.setAdapter(adapter);

        binding.addBtn.setOnClickListener(e -> {
            Intent intent = new Intent(getContext(), UsersListActivity.class);
            startActivity(intent);
        });

        setupChatsListener();
    }

    private void setupChatsListener() {
        String currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid == null) return;

        chatsRef = database.getReference().child("Chats");
        chatsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                Set<String> activeChatPartnerIds = new HashSet<>();

                for (DataSnapshot chatRoomSnap : snapshot.getChildren()) {
                    String roomKey = chatRoomSnap.getKey();
                    if (roomKey != null && roomKey.startsWith(currentUid) && !roomKey.equals(currentUid)) {
                        String otherUserId = roomKey.substring(currentUid.length());
                        if (otherUserId.isEmpty()) continue;

                        activeChatPartnerIds.add(otherUserId);

                        // Extract last message in room
                        DataSnapshot lastMsgSnap = null;
                        for (DataSnapshot msgChild : chatRoomSnap.getChildren()) {
                            lastMsgSnap = msgChild;
                        }

                        long lastTime = 0;
                        String lastText = "No Chat";

                        if (lastMsgSnap != null) {
                            MessageModel model = lastMsgSnap.getValue(MessageModel.class);
                            if (model != null) {
                                lastTime = model.getMessageTime();
                                String type = model.getType();
                                if ("image".equalsIgnoreCase(type)) {
                                    lastText = "📷 Photo";
                                } else if ("video".equalsIgnoreCase(type)) {
                                    lastText = "🎥 Video";
                                } else if ("document".equalsIgnoreCase(type)) {
                                    lastText = "📄 Document";
                                } else {
                                    lastText = model.getMessageText() != null ? model.getMessageText() : "";
                                }
                            }
                        }

                        final long finalLastTime = lastTime;
                        final String finalLastText = lastText;

                        database.getReference().child("Users").child(otherUserId)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot userSnap) {
                                        if (!isAdded()) return;
                                        Users user = userSnap.getValue(Users.class);
                                        if (user != null) {
                                            user.setUserId(userSnap.getKey());
                                            user.setTimeStam(finalLastTime);
                                            user.setLastMessage(finalLastText);
                                            addOrUpdateUserInList(user);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {}
                                });
                    }
                }

                if (activeChatPartnerIds.isEmpty()) {
                    list.clear();
                    if (adapter != null) adapter.notifyDataSetChanged();
                } else {
                    list.removeIf(user -> !activeChatPartnerIds.contains(user.getUserId()));
                    list.sort((u1, u2) -> Long.compare(u2.getTimeStam(), u1.getTimeStam()));
                    if (adapter != null) adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        chatsRef.addValueEventListener(chatsListener);
    }

    private synchronized void addOrUpdateUserInList(Users user) {
        if (user == null || user.getUserId() == null) return;

        int index = -1;
        for (int i = 0; i < list.size(); i++) {
            if (user.getUserId().equals(list.get(i).getUserId())) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            list.set(index, user);
        } else {
            list.add(user);
        }

        list.sort((u1, u2) -> Long.compare(u2.getTimeStam(), u1.getTimeStam()));

        if (isAdded() && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (adapter != null) adapter.notifyDataSetChanged();
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (chatsRef != null && chatsListener != null) {
            chatsRef.removeEventListener(chatsListener);
        }
        binding = null;
    }
}
