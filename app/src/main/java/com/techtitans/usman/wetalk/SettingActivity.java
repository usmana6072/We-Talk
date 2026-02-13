package com.techtitans.usman.wetalk;

import android.app.ComponentCaller;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.cloudinary.Cloudinary;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.cloudinary.utils.ObjectUtils;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import com.techtitans.usman.wetalk.databinding.ActivitySettingBinding;

import java.io.IOException;
import java.util.Map;

public class SettingActivity extends AppCompatActivity {

    ActivitySettingBinding binding;
    FirebaseAuth auth;
    FirebaseDatabase database;
    FirebaseStorage storage;

    FirebaseUser user;
    String currentProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding=ActivitySettingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        database=FirebaseDatabase.getInstance();
        auth=FirebaseAuth.getInstance();
        storage=FirebaseStorage.getInstance();
        user=auth.getCurrentUser();

        database.getReference().child("Users").child(user.getUid()).child("profilePic").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    currentProfile=snapshot.getValue().toString();
                    Picasso.get().load(currentProfile).placeholder(R.drawable.avatar).into(binding.profileimage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        binding.etUserName.setText(user.getDisplayName());

        database.getReference().child("Users").child(user.getUid()).child("lastMessage").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    binding.etAbout.setText(snapshot.getValue().toString());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        binding.addProfileIV.setOnClickListener(e->{
            Intent intent=new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent,33);
        });

        binding.saveBtn.setOnClickListener(e->{
            String uName=binding.etUserName.getText().toString();
            String uAbout=binding.etAbout.getText().toString();
            if(!uName.isEmpty())
                database.getReference().child("Users").child(user.getUid()).child("userName").setValue(uName);
            if(!uAbout.isEmpty())
                database.getReference().child("Users").child(user.getUid()).child("lastMessage").setValue(uAbout);
            Toast.makeText(this, "Data Updated Successfully", Toast.LENGTH_SHORT).show();
        });

        binding.backArrowSettingActivity.setOnClickListener(e -> finish());

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data!=null){
            Uri sFile=data.getData();
            binding.profileimage.setImageURI(sFile);
            MediaManager.get().upload(sFile)
                    .unsigned("wetalk_profile_images")
                    .option("folder","profile_images")
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) { }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {}

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String url=resultData.get("secure_url").toString();
                            database.getReference().child("Users").child(user.getUid()).child("profilePic").setValue(url).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    Toast.makeText(SettingActivity.this, "Profile image Uploaded Successfully", Toast.LENGTH_SHORT).show();
                                }
                            });

                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            Toast.makeText(SettingActivity.this, error.getDescription(), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {

                        }
                    }).dispatch();
        }
    }
}