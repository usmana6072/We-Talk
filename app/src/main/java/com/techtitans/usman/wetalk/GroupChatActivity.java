package com.techtitans.usman.wetalk;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.techtitans.usman.wetalk.Adapters.ChatAdapter;
import com.techtitans.usman.wetalk.Models.MessageModel;
import com.techtitans.usman.wetalk.databinding.ActivityGroupChatBinding;

import java.util.ArrayList;
import java.util.Date;

public class GroupChatActivity extends AppCompatActivity {

    ActivityGroupChatBinding binding;
    FirebaseDatabase database;
    FirebaseAuth auth;

    ChatAdapter adapter;
    ArrayList<MessageModel> messageList;

    String senderId,message;

    MessageModel model;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding=ActivityGroupChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        database=FirebaseDatabase.getInstance();
        auth=FirebaseAuth.getInstance();
        senderId=auth.getUid();
        messageList=new ArrayList<>();
        adapter=new ChatAdapter(messageList,this);
        binding.recyclerViewChatDetails.setAdapter(adapter);
        binding.recyclerViewChatDetails.setLayoutManager(new LinearLayoutManager(this));
        binding.tvUserNameChatDetails.setText("Fine Shits");



        binding.imageViewSend.setOnClickListener(e->{
            message=binding.tvMessage.getText().toString();
            binding.tvMessage.setText("");
            if(!message.isEmpty()){
                String mess=String.format("\b %s \b\n %s",auth.getCurrentUser().getDisplayName(),message);
                model=new MessageModel(senderId,mess,new Date().getTime());
                database.getReference().child("GroupChat").push().setValue(model).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {

                    }
                });
            }else
                Toast.makeText(this, "Type message First", Toast.LENGTH_SHORT).show();

        });

        database.getReference().child("GroupChat").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                for(DataSnapshot dataSnapshot:snapshot.getChildren()){
                    MessageModel messageModel=dataSnapshot.getValue(MessageModel.class);
                    messageList.add(messageModel);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        binding.backArrow.setOnClickListener(e->{
            finish();
        });
    }
}