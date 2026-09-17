package com.techtitans.usman.wetalk.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.techtitans.usman.wetalk.Adapters.GroupListAdapter;
import com.techtitans.usman.wetalk.Models.GroupModel;
import com.techtitans.usman.wetalk.databinding.FragmentGroupBinding;

import java.util.ArrayList;

public class GroupFragment extends Fragment {

    FragmentGroupBinding binding;
    FirebaseDatabase database;
    FirebaseAuth auth;
    ArrayList<GroupModel> list = new ArrayList<>();
    GroupListAdapter adapter;

    public GroupFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentGroupBinding.inflate(inflater, container, false);
        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();

        adapter = new GroupListAdapter(list, getContext());
        binding.rvGroupList.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvGroupList.setAdapter(adapter);

        loadGroups();

        return binding.getRoot();
    }

    private void loadGroups() {
        String uid = auth.getUid();
        if (uid == null) return;

        database.getReference().child("Users").child(uid).child("groups").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                adapter.notifyDataSetChanged(); // Clear UI immediately
                
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String groupId = ds.getKey();
                    if (groupId == null) continue;
                    
                    database.getReference().child("Groups").child(groupId).child("details").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot groupSnapshot) {
                            GroupModel group = groupSnapshot.getValue(GroupModel.class);
                            if (group != null) {
                                // Prevent duplicates if listener fires while loading
                                boolean exists = false;
                                for (GroupModel g : list) {
                                    if (g.getGroupId().equals(group.getGroupId())) { exists = true; break; }
                                }
                                if (!exists) {
                                    list.add(group);
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
}
