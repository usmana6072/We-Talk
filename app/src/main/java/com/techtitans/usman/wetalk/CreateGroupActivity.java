package com.techtitans.usman.wetalk;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.techtitans.usman.wetalk.Adapters.SelectMemberAdapter;
import com.techtitans.usman.wetalk.Models.GroupModel;
import com.techtitans.usman.wetalk.Models.Users;
import com.techtitans.usman.wetalk.databinding.ActivityCreateGroupBinding;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CreateGroupActivity extends AppCompatActivity {

    ActivityCreateGroupBinding binding;
    FirebaseDatabase database;
    FirebaseAuth auth;
    SelectMemberAdapter adapter;
    ArrayList<Users> usersList = new ArrayList<>();
    Uri selectedIconUri;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateGroupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Creating Group");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

        adapter = new SelectMemberAdapter(usersList, this);
        binding.rvSelectMembers.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSelectMembers.setAdapter(adapter);

        loadUsers();

        binding.btnChangeIcon.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, 45);
        });

        binding.btnCreateGroup.setOnClickListener(v -> createGroup());
        
        binding.toolbar.setNavigationIcon(R.drawable.backarrow);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadUsers() {
        database.getReference().child("Users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                usersList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Users user = dataSnapshot.getValue(Users.class);
                    if (user != null && !dataSnapshot.getKey().equals(auth.getUid())) {
                        user.setUserId(dataSnapshot.getKey());
                        usersList.add(user);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 45 && resultCode == RESULT_OK && data != null) {
            selectedIconUri = data.getData();
            binding.groupIcon.setImageURI(selectedIconUri);
        }
    }

    private void createGroup() {
        String name = binding.etGroupName.getText().toString().trim();
        String desc = binding.etGroupDesc.getText().toString().trim();
        Set<String> selectedMembers = adapter.getSelectedUserIds();
        boolean hideMembers = binding.cbHideMembers.isChecked();

        if (name.isEmpty()) {
            Toast.makeText(this, "Enter group name", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedMembers.isEmpty()) {
            Toast.makeText(this, "Select at least one member", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();
        String groupId = database.getReference().child("Groups").push().getKey();

        if (selectedIconUri != null) {
            uploadIconAndFinish(groupId, name, desc, hideMembers, selectedMembers);
        } else {
            saveGroupData(groupId, name, desc, "", hideMembers, selectedMembers);
        }
    }

    private void uploadIconAndFinish(String groupId, String name, String desc, boolean hideMembers, Set<String> members) {
        try {
            MediaManager.get().upload(selectedIconUri)
                    .unsigned("wetalk_profile_images")
                    .option("folder", "group_icons")
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {}
                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {}
                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String iconUrl = resultData.get("secure_url").toString();
                            saveGroupData(groupId, name, desc, iconUrl, hideMembers, members);
                        }
                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            progressDialog.dismiss();
                            Toast.makeText(CreateGroupActivity.this, "Icon upload failed", Toast.LENGTH_SHORT).show();
                        }
                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {}
                    }).dispatch();
        } catch (Exception e) {
            progressDialog.dismiss();
            Toast.makeText(this, "Media Manager not ready. Saving without icon.", Toast.LENGTH_SHORT).show();
            saveGroupData(groupId, name, desc, "", hideMembers, members);
        }
    }

    private void saveGroupData(String groupId, String name, String desc, String iconUrl, boolean hideMembers, Set<String> members) {
        long now = new Date().getTime();
        GroupModel group = new GroupModel(groupId, name, desc, iconUrl, auth.getUid(), hideMembers, now);

        Map<String, Object> updates = new HashMap<>();
        updates.put("/Groups/" + groupId + "/details", group);
        
        // Add creator as admin
        updates.put("/Groups/" + groupId + "/members/" + auth.getUid(), "admin");
        updates.put("/Users/" + auth.getUid() + "/groups/" + groupId, true);

        // Add selected members
        for (String userId : members) {
            updates.put("/Groups/" + groupId + "/members/" + userId, "member");
            updates.put("/Users/" + userId + "/groups/" + groupId, true);
        }

        database.getReference().updateChildren(updates).addOnCompleteListener(task -> {
            progressDialog.dismiss();
            if (task.isSuccessful()) {
                Toast.makeText(CreateGroupActivity.this, "Group Created", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(CreateGroupActivity.this, "Failed to create group", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
