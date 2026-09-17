package com.techtitans.usman.wetalk.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.techtitans.usman.wetalk.Models.Users;
import com.techtitans.usman.wetalk.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SelectMemberAdapter extends RecyclerView.Adapter<SelectMemberAdapter.ViewHolder> {

    ArrayList<Users> list;
    Context context;
    Set<String> selectedUserIds = new HashSet<>();

    public SelectMemberAdapter(ArrayList<Users> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.sample_select_member, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Users user = list.get(position);
        holder.tvUserName.setText(user.getUserName());
        holder.tvAbout.setText(user.getLastMessage() != null ? user.getLastMessage() : "Available");
        
        String pic = user.getProfilePic();
        if (pic != null && !pic.isEmpty()) {
            Picasso.get().load(pic).placeholder(R.drawable.avatar).into(holder.profile);
        } else {
            holder.profile.setImageResource(R.drawable.avatar);
        }

        holder.checkBox.setChecked(selectedUserIds.contains(user.getUserId()));

        holder.itemView.setOnClickListener(v -> {
            if (selectedUserIds.contains(user.getUserId())) {
                selectedUserIds.remove(user.getUserId());
            } else {
                selectedUserIds.add(user.getUserId());
            }
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public Set<String> getSelectedUserIds() {
        return selectedUserIds;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView profile;
        TextView tvUserName, tvAbout;
        CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            profile = itemView.findViewById(R.id.profileimage);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvAbout = itemView.findViewById(R.id.tvAbout);
            checkBox = itemView.findViewById(R.id.checkbox);
        }
    }
}
