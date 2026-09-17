package com.techtitans.usman.wetalk;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.techtitans.usman.wetalk.Adapters.SelectMemberAdapter;
import com.techtitans.usman.wetalk.Adapters.UserAdapterChatView;
import com.techtitans.usman.wetalk.Models.GroupModel;
import com.techtitans.usman.wetalk.Models.Users;
import com.techtitans.usman.wetalk.databinding.ActivityGroupInfoBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GroupInfoActivity extends AppCompatActivity {

    ActivityGroupInfoBinding binding;
    FirebaseDatabase database;
    FirebaseAuth auth;
    String groupId;
    GroupModel group;
    ArrayList<Users> memberList = new ArrayList<>();
    UserAdapterChatView adapter;
    boolean isAdmin = false;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGroupInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
        groupId = getIntent().getStringExtra("groupId");

        if (groupId == null) {
            finish();
            return;
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Updating...");
        progressDialog.setCancelable(false);

        adapter = new UserAdapterChatView(memberList, this);
        binding.rvMembers.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMembers.setAdapter(adapter);

        loadGroupDetails();
        
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        
        binding.btnEditInfo.setOnClickListener(v -> {
            if (group != null) showEditDialog();
        });
        
        binding.switchHideMembers.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (group != null && isAdmin) {
                database.getReference().child("Groups").child(groupId).child("details").child("hideMembers").setValue(isChecked);
            }
        });

        binding.ivGroupIcon.setOnClickListener(v -> {
            if (isAdmin) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, 56);
            }
        });

        binding.btnAddMember.setOnClickListener(v -> {
            if (isAdmin) showAddMemberDialog();
        });
    }

    private void loadGroupDetails() {
        database.getReference().child("Groups").child(groupId).child("details").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                group = snapshot.getValue(GroupModel.class);
                if (group != null) {
                    binding.collapsingToolbar.setTitle(group.getGroupName());
                    binding.tvDescription.setText(group.getGroupDescription() != null ? group.getGroupDescription() : "");
                    
                    String icon = group.getGroupIcon();
                    if (icon != null && !icon.isEmpty()) {
                        Picasso.get().load(icon).placeholder(R.drawable.avatar).into(binding.ivGroupIcon);
                    } else {
                        binding.ivGroupIcon.setImageResource(R.drawable.avatar);
                    }
                    
                    isAdmin = auth.getUid().equals(group.getCreatedBy());
                    binding.adminPanel.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                    binding.btnAddMember.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                    binding.switchHideMembers.setChecked(group.isHideMembers());
                    
                    loadMembers();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadMembers() {
        database.getReference().child("Groups").child(groupId).child("members").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                memberList.clear();
                if (group != null && group.isHideMembers() && !isAdmin) {
                    binding.tvMemberCount.setText("Members Hidden by Admin");
                    adapter.notifyDataSetChanged();
                    return;
                }
                
                binding.tvMemberCount.setText("Members: " + snapshot.getChildrenCount());
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String userId = ds.getKey();
                    if (userId == null) continue;
                    database.getReference().child("Users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userSnap) {
                            Users user = userSnap.getValue(Users.class);
                            if (user != null) {
                                user.setUserId(userSnap.getKey());
                                
                                // Prevent duplicates
                                boolean exists = false;
                                for (Users m : memberList) {
                                    if (m.getUserId().equals(user.getUserId())) { exists = true; break; }
                                }
                                if (!exists) {
                                    memberList.add(user);
                                    adapter.notifyDataSetChanged();
                                }
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showEditDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Group Info");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText nameInput = new EditText(this);
        nameInput.setHint("Group Name");
        nameInput.setText(group.getGroupName());
        layout.addView(nameInput);

        final EditText descInput = new EditText(this);
        descInput.setHint("Description");
        descInput.setText(group.getGroupDescription());
        layout.addView(descInput);

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = nameInput.getText().toString().trim();
            String newDesc = descInput.getText().toString().trim();
            if (!newName.isEmpty()) {
                database.getReference().child("Groups").child(groupId).child("details").child("groupName").setValue(newName);
                database.getReference().child("Groups").child(groupId).child("details").child("groupDescription").setValue(newDesc);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showAddMemberDialog() {
        final ArrayList<Users> allUsers = new ArrayList<>();
        final SelectMemberAdapter selectAdapter = new SelectMemberAdapter(allUsers, this);
        
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_select_members, null);
        RecyclerView rv = dialogView.findViewById(R.id.rvSelectMembers);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(selectAdapter);

        database.getReference().child("Users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allUsers.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String uid = ds.getKey();
                    if (uid == null) continue;
                    
                    boolean alreadyMember = false;
                    for (Users m : memberList) {
                        if (m.getUserId().equals(uid)) { alreadyMember = true; break; }
                    }
                    if (!alreadyMember) {
                        Users user = ds.getValue(Users.class);
                        if (user != null) {
                            user.setUserId(uid);
                            allUsers.add(user);
                        }
                    }
                }
                selectAdapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        new AlertDialog.Builder(this)
                .setTitle("Add Members")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    Set<String> selected = selectAdapter.getSelectedUserIds();
                    if (!selected.isEmpty()) {
                        Map<String, Object> updates = new HashMap<>();
                        for (String uid : selected) {
                            updates.put("/Groups/" + groupId + "/members/" + uid, "member");
                            updates.put("/Users/" + uid + "/groups/" + groupId, true);
                        }
                        database.getReference().updateChildren(updates);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 56 && resultCode == RESULT_OK && data != null) {
            Uri iconUri = data.getData();
            if (iconUri != null) uploadNewIcon(iconUri);
        }
    }

    private void uploadNewIcon(Uri uri) {
        progressDialog.show();
        try {
            MediaManager.get().upload(uri)
                    .unsigned("wetalk_profile_images")
                    .option("folder", "group_icons")
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {}
                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {}
                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            progressDialog.dismiss();
                            String iconUrl = resultData.get("secure_url").toString();
                            database.getReference().child("Groups").child(groupId).child("details").child("groupIcon").setValue(iconUrl);
                        }
                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            progressDialog.dismiss();
                            Toast.makeText(GroupInfoActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                        }
                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {}
                    }).dispatch();
        } catch (Exception e) {
            progressDialog.dismiss();
            Toast.makeText(this, "Media Manager error", Toast.LENGTH_SHORT).show();
        }
    }
}
