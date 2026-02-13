package com.techtitans.usman.wetalk;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.techtitans.usman.wetalk.databinding.ActivityProfileViewBinding;

public class ProfileViewActivity extends AppCompatActivity {

    ActivityProfileViewBinding binding;
    FirebaseDatabase database;

    String userId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding=ActivityProfileViewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        database=FirebaseDatabase.getInstance();

        userId=getIntent().getStringExtra("receiverId");
        loadData();

        binding.backArrowProfileActivity.setOnClickListener(e->{
            finish();
        });

    }

    private void loadData(){
        String profileImage,userName,about;
        database.getReference().child("Users").child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.hasChildren()){
                    for(DataSnapshot snapshot1:snapshot.getChildren()){
                        if(snapshot1.getKey().equals("userName"))
                            binding.tvUserName.setText(snapshot1.getValue().toString());
                        else if (snapshot1.getKey().equals("profilePic")) {
                            Picasso.get().load(snapshot1.getValue().toString()).placeholder(R.drawable.avatar).into(binding.profileimage);
                        }
                        else if (snapshot1.getKey().equals("lastMessage")) {
                            binding.tvAbout.setText(snapshot1.getValue().toString());
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }
}