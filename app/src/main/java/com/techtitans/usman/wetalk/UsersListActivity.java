package com.techtitans.usman.wetalk;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.techtitans.usman.wetalk.Adapters.ChatAdapter;
import com.techtitans.usman.wetalk.Adapters.UserAdapterChatView;
import com.techtitans.usman.wetalk.Models.Users;
import com.techtitans.usman.wetalk.databinding.ActivityUsersListBinding;

import java.util.ArrayList;

public class UsersListActivity extends AppCompatActivity {

    ActivityUsersListBinding binding;
    FirebaseDatabase database;
    ArrayList<Users> list;

    UserAdapterChatView adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding=ActivityUsersListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        database=FirebaseDatabase.getInstance();

        list=new ArrayList<>();
        adapter=new UserAdapterChatView(list,this);
        binding.recyclerViewChatFragment.setAdapter(adapter);
        binding.recyclerViewChatFragment.setLayoutManager(new LinearLayoutManager(this));

        database.getReference().child("Users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot dataShapshot:snapshot.getChildren()){
                    Users users=dataShapshot.getValue(Users.class);
                    users.setUserId(dataShapshot.getKey());
                    list.add(users);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        binding.backImg.setOnClickListener(e->{
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}