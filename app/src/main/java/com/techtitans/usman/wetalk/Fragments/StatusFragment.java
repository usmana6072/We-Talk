package com.techtitans.usman.wetalk.Fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.techtitans.usman.wetalk.Adapters.StatusViewAdapter;
import com.techtitans.usman.wetalk.Models.StatusModel;
import com.techtitans.usman.wetalk.Models.Users;
import com.techtitans.usman.wetalk.R;
import com.techtitans.usman.wetalk.databinding.FragmentStatusBinding;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

public class StatusFragment extends Fragment {

    FirebaseDatabase database;
    FirebaseAuth auth;

    ArrayList<StatusModel> myStatus=new ArrayList<>();
    ArrayList<StatusModel> othersStatus=new ArrayList<>();

    StatusViewAdapter myStatusAdapter,otherStatusAdapter;
    Uri uri;

    public StatusFragment() {
        // Required empty public constructor
    }

    FragmentStatusBinding binding;
    private static final long STATUS_EXPIRATION_TIME = 24 * 60 * 60 * 1000; // 24 hours in milliseconds

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding=FragmentStatusBinding.inflate(inflater, container, false);
        database=FirebaseDatabase.getInstance();
        auth=FirebaseAuth.getInstance();

        myStatusAdapter=new StatusViewAdapter(myStatus,getContext());
        binding.recyclarViewYourStatus.setAdapter(myStatusAdapter);
        binding.recyclarViewYourStatus.setLayoutManager(new LinearLayoutManager(getContext()));

        otherStatusAdapter=new StatusViewAdapter(othersStatus,getContext());
        binding.recyclarViewOthersStatus.setAdapter(otherStatusAdapter);
        binding.recyclarViewOthersStatus.setLayoutManager(new LinearLayoutManager(getContext()));

        database.getReference().child("Users").child(auth.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                myStatus.clear();
                if(snapshot.child("status").exists()) {
                    StatusModel statusModel = snapshot.child("status").getValue(StatusModel.class);
                    if (statusModel != null) {
                        if (System.currentTimeMillis() - statusModel.getTimeStamp() < STATUS_EXPIRATION_TIME) {
                            myStatus.add(statusModel);
                        } else {
                            // Automatically delete expired status from database
                            database.getReference().child("Users").child(auth.getUid()).child("status").removeValue();


                        }
                    }
                }
                myStatusAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        database.getReference().child("Users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                othersStatus.clear();
                long currentTime = System.currentTimeMillis();
                for(DataSnapshot snapshot1:snapshot.getChildren()){
                    String uid = snapshot1.getKey();
                    if(uid != null && uid.equals(auth.getUid()))
                        continue;
                        
                    if(snapshot1.child("status").exists()){
                        StatusModel statusModel = snapshot1.child("status").getValue(StatusModel.class);
                        if (statusModel != null) {
                            if (currentTime - statusModel.getTimeStamp() < STATUS_EXPIRATION_TIME) {
                                othersStatus.add(statusModel);
                            } else if (uid != null) {
                                // Automatically delete expired status of other users from database when viewed
                                database.getReference().child("Users").child(uid).child("status").removeValue();
                            }
                        }
                    }
                }
                otherStatusAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        binding.addImageView.setOnClickListener(e->{
            Intent intent=new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent,33);
        });

        return binding.getRoot();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(data!=null){
            uri=data.getData();
            MediaManager.get().upload(uri).unsigned("wetalk_profile_images")
                    .option("folder","profile_images")
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {

                        }

        