package com.techtitans.usman.wetalk.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.techtitans.usman.wetalk.Adapters.CallHistoryAdapter;
import com.techtitans.usman.wetalk.Models.CallHistoryModel;
import com.techtitans.usman.wetalk.databinding.FragmentCallsBinding;

import java.util.ArrayList;

public class CallsFragment extends Fragment {

    private FragmentCallsBinding binding;
    private ArrayList<CallHistoryModel> callList;
    private CallHistoryAdapter adapter;
    private DatabaseReference callHistoryRef;
    private ValueEventListener historyListener;

    public CallsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCallsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        callList = new ArrayList<>();
        adapter = new CallHistoryAdapter(callList, getContext());

        binding.recyclerViewCalls.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewCalls.setAdapter(adapter);

        loadCallHistory();
    }

    private void loadCallHistory() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            updateEmptyState();
            return;
        }

        callHistoryRef = FirebaseDatabase.getInstance().getReference().child("CallHistory").child(uid);
        historyListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                callList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        CallHistoryModel model = dataSnapshot.getValue(CallHistoryModel.class);
                        if (model != null) {
                            callList.add(model);
                        }
                    }
                    // Sort by timestamp descending (newest calls first)
                    callList.sort((c1, c2) -> Long.compare(c2.getTimestamp(), c1.getTimestamp()));
                }
                adapter.notifyDataSetChanged();
                updateEmptyState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                updateEmptyState();
            }
        };

        callHistoryRef.addValueEventListener(historyListener);
    }

    private void updateEmptyState() {
        if (binding == null) return;
        if (callList.isEmpty()) {
            binding.layoutEmptyCalls.setVisibility(View.VISIBLE);
            binding.recyclerViewCalls.setVisibility(View.GONE);
        } else {
            binding.layoutEmptyCalls.setVisibility(View.GONE);
            binding.recyclerViewCalls.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (callHistoryRef != null && historyListener != null) {
            callHistoryRef.removeEventListener(historyListener);
        }
        binding = null;
    }
}
