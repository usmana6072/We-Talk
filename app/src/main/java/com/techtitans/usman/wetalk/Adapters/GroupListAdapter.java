package com.techtitans.usman.wetalk.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.techtitans.usman.wetalk.GroupChatActivity;
import com.techtitans.usman.wetalk.Models.GroupModel;
import com.techtitans.usman.wetalk.R;

import java.util.ArrayList;

public class GroupListAdapter extends RecyclerView.Adapter<GroupListAdapter.ViewHolder> {

    ArrayList<GroupModel> list;
    Context context;

    public GroupListAdapter(ArrayList<GroupModel> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.sample_group_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GroupModel group = list.get(position);
        holder.name.setText(group.getGroupName());
        holder.desc.setText(group.getGroupDescription() != null && !group.getGroupDescription().isEmpty() ? group.getGroupDescription() : "Join the conversation");
        
        String iconUrl = group.getGroupIcon();
        if (iconUrl != null && !iconUrl.isEmpty()) {
            Picasso.get().load(iconUrl).placeholder(R.drawable.avatar).into(holder.icon);
        } else {
            holder.icon.setImageResource(R.drawable.avatar);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, GroupChatActivity.class);
            intent.putExtra("groupId", group.getGroupId());
            intent.putExtra("groupName", group.getGroupName());
            intent.putExtra("groupIcon", group.getGroupIcon());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name, desc;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.groupIcon);
            name = itemView.findViewById(R.id.tvGroupName);
            desc = itemView.findViewById(R.id.tvGroupDesc);
        }
    }
}
