package com.techtitans.usman.wetalk;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.techtitans.usman.wetalk.Adapters.UserAdapterChatView;
import com.techtitans.usman.wetalk.Models.Users;
import com.techtitans.usman.wetalk.databinding.ActivityUsersListBinding;

import java.util.ArrayList;

public class UsersListActivity extends AppCompatActivity {

    private ActivityUsersListBinding binding;
    private FirebaseDatabase database;
    private ArrayList<Users> originalList;
    private ArrayList<Users> displayList;
    private UserAdapterChatView adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityUsersListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        database = FirebaseDatabase.getInstance();
        originalList = new ArrayList<>();
        displayList = new ArrayList<>();

        adapter = new UserAdapterChatView(displayList, this);
        binding.recyclerViewChatFragment.setAdapter(adapter);
        binding.recyclerViewChatFragment.setLayoutManager(new LinearLayoutManager(this));

        binding.backImg.setOnClickListener(e -> finish());

        loadAllUsers();
        setupSearchListener();
    }

    private void loadAllUsers() {
        String currentUid = FirebaseAuth.getInstance().getUid();

        database.getReference().child("Users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                originalList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Users users = dataSnapshot.getValue(Users.class);
                    if (users != null) {
                        users.setUserId(dataSnapshot.getKey());
                        // Exclude current logged in user from list
                        if (currentUid != null && currentUid.equals(users.getUserId())) {
                            continue;
                        }
                        originalList.add(users);
                    }
                }
                filterUsers(binding.etSearchUser.getText() != null ? binding.etSearchUser.getText().toString() : "");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void setupSearchListener() {
        binding.etSearchUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void filterUsers(String query) {
        String trimmedQuery = query.trim().toLowerCase();
        displayList.clear();

        if (trimmedQuery.isEmpty()) {
            displayList.addAll(originalList);
        } else {
            for (Users user : originalList) {
                String email = user.getMail() != null ? user.getMail().toLowerCase() : "";
                String name = user.getUserName() != null ? user.getUserName().toLowerCase() : "";

                if (email.contains(trimmedQuery) || name.contains(trimmedQuery)) {
                    displayList.add(user);
                }
            }
        }

        adapter.notifyDataSetChanged();

        if (displayList.isEmpty()) {
            binding.layoutNoUserFound.setVisibility(View.VISIBLE);
            binding.recyclerViewChatFragment.setVisibility(View.GONE);
        } else {
            binding.layoutNoUserFound.setVisibility(View.GONE);
            binding.recyclerViewChatFragment.setVisibility(View.VISIBLE);
        }
    }
}
