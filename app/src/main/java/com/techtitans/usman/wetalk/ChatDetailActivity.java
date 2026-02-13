package com.techtitans.usman.wetalk;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

public class ChatDetailActivity extends AppCompatActivity {

    FirebaseDatabase database;
    FirebaseAuth auth;
    ActivityChatDetailBinding binding;
    String senderId,receiverId,receiverUserName,receiverProfile,senderRoom,receiverRoom;
    MessageModel model;

    ArrayList<MessageModel> list=new ArrayList<>();
    ChatAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding=ActivityChatDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        database=FirebaseDatabase.getInstance();
        auth=FirebaseAuth.getInstance();

        senderId=auth.getUid();
        receiverId=getIntent().getStringExtra("userId");
        receiverProfile=getIntent().getStringExtra("profile");
        receiverUserName=getIntent().getStringExtra("username");
        adapter=new ChatAdapter(list,ChatDetailActivity.this,receiverId);


        binding.tvUserNameChatDetails.setText(receiverUserName);
        Picasso.get().load(receiverProfile).placeholder(R.drawable.avatar).into(binding.profileimage);

        binding.backArrow.setOnClickListener(e->{
            finish();
        });

        senderRoom=senderId+receiverId;
        receiverRoom=receiverId+senderId;

        binding.recyclerViewChatDetails.setAdapter(adapter);
        binding.recyclerViewChatDetails.setLayoutManager(new LinearLayoutManager(this));


        binding.imageViewSend.setOnClickListener(e->{
            String message=binding.tvMessage.getText().toString();
            if(!message.isEmpty()){
                model=new MessageModel(senderId,message);
                model.setMessageTime(new Date().getTime());
                binding.tvMessage.setText("");

                //saving message to sender chat room
                DatabaseReference messageRef = database
                        .getReference()
                        .child("Chats")
                        .child(senderRoom)
                        .push();

                model.setMessageId(messageRef.getKey());
                messageRef.setValue(model);

                //saving sendTime stam to receiver room to later sort
                // it in chat view details activity

                database.getReference().child("Users").child(receiverId)
                        .child("timeStam").setValue(new Date().getTime());

                //saving time stam to sender room to sort later
                //in chat view details activity
                database.getReference().child("Users").child(senderId)
                        .child("timeStam").setValue(new Date().getTime());

                //saving message model to receiver chat room
                if(!senderRoom.equals(receiverRoom))
                    database.getReference().child("Chats").child(receiverRoom).child(model.getMessageId())
                        .setValue(model).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {

                            }
                        });
            }else
                Toast.makeText(this, "Please Type a Message", Toast.LENGTH_SHORT).show();
        });


        //loading users from database to the list created to create adapter

        database.getReference().child("Chats").child(senderRoom).
                addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        list.clear();
                        for(DataSnapshot snapshot1:snapshot.getChildren()){
                            MessageModel modele=snapshot1.getValue(MessageModel.class);
                            if (modele != null) {
                                list.add(modele);
                            }
                            adapter.notifyDataSetChanged();
                            binding.recyclerViewChatDetails.post(()->{
                                binding.recyclerViewChatDetails.scrollToPosition(list.size()-1);
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        binding.tvUserNameChatDetails.setOnClickListener(e->{
            Intent intent=new Intent(ChatDetailActivity.this, ProfileViewActivity.class);
            intent.putExtra("receiverId",receiverId);
            startActivity(intent);
        });
        binding.profileimage.setOnClickListener(e->{
            Intent intent=new Intent(ChatDetailActivity.this, ProfileViewActivity.class);
            intent.putExtra("receiverId",receiverId);
            startActivity(intent);
        });

    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }
}