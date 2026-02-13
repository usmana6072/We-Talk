package com.techtitans.usman.wetalk.Fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.techtitans.usman.wetalk.Adapters.UserAdapterChatView;
import com.techtitans.usman.wetalk.Models.Users;
import com.techtitans.usman.wetalk.UsersListActivity;
import com.techtitans.usman.wetalk.databinding.FragmentChatBinding;

import java.util.ArrayList;
import java.util.Collection;

public class ChatFragment extends Fragment {

    public ChatFragment() {
        // Required empty public constructor
    }

    FirebaseDatabase database;
    FragmentChatBinding binding;
    ArrayList<Users> list;
    ArrayList<String> receiverIdList=new ArrayList<>();
    DatabaseReference chatReference;
    DatabaseReference usersRef;
    ValueEventListener chatListener;
    UserAdapterChatView adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding=FragmentChatBinding.inflate(inflater,container,false);
        database=FirebaseDatabase.getInstance();
        list=new ArrayList<>();


        adapter=new UserAdapterChatView(list,getContext());
        LinearLayoutManager linearLayoutManager=new LinearLayoutManager(getContext());

        binding.recyclerViewChatFragment.setLayoutManager(linearLayoutManager);
        binding.recyclerViewChatFragment.setAdapter(adapter);

        //loading users from firebase to the list

        String senderId = FirebaseAuth.getInstance().getUid();

        chatReference=database.getReference().child("Chats").orderByChild("messageTime").getRef();

        usersRef=database.getReference().child("Users").orderByChild("timeStam").getRef();
        chatListener=new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                for(DataSnapshot snapshot1:snapshot.getChildren()){
                    Users users=snapshot1.getValue(Users.class);
                    users.setUserId(snapshot1.getKey());
                    database.getReference().child("Chats").child(""+FirebaseAuth.getInstance().getUid()+users.getUserId())
                            .addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot2) {
                                    if(snapshot2.exists()){
                                        if(!receiverIdList.contains(users.getUserId())) {
                                            list.add(users);
                                            list.sort(null);
                                            adapter.notifyDataSetChanged();
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
//        chatListener=new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//
//                list.clear();
//                receiverIdList.clear();
//
//                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
//
//                    String chatKey = chatSnapshot.getKey();
//
//                    if (chatKey.contains(senderId)) {
//
//                        String otherUserId =
//                                chatKey.replace(senderId, "");
//
//                        if (receiverIdList.contains(otherUserId))
//                            continue;
//                        receiverIdList.add(otherUserId);
//
//                        database.getReference().child("Users")
//                                .child(otherUserId)
//                                .addListenerForSingleValueEvent(new ValueEventListener() {
//                                    @Override
//                                    public void onDataChange(@NonNull DataSnapshot userSnap) {
//
//                                        Users user =
//                                                userSnap.getValue(Users.class);
//
//                                        if (user != null) {
//                                            user.setUserId(userSnap.getKey());
//                                            list.add(user);
//                                            adapter.notifyDataSetChanged();
//                                        }
//                                    }
//
//                                    @Override
//                                    public void onCancelled(@NonNull DatabaseError error) {
//                                    }
//                                });
//                        list.sort(null);
//                        adapter.notifyDataSetChanged();
//                    }
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//            }
//        };
        usersRef.addValueEventListener(chatListener);

        binding.addBtn.setOnClickListener(e->{
            Intent intent=new Intent(getContext(), UsersListActivity.class);
            startActivity(intent);
        });

        return binding.getRoot();
    }

    @Override
    public void onStop() {
        usersRef.removeEventListener(chatListener);
        super.onStop();
    }

    @Override
    public void onStart() {
        list.sort(null);
        adapter.notifyDataSetChanged();
        super.onStart();
    }
}