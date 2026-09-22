package com.techtitans.usman.wetalk.Adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.techtitans.usman.wetalk.ChatDetailActivity;
import com.techtitans.usman.wetalk.GroupChatActivity;
import com.techtitans.usman.wetalk.Models.CallHistoryModel;
import com.techtitans.usman.wetalk.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class CallHistoryAdapter extends RecyclerView.Adapter<CallHistoryAdapter.ViewHolder> {

    private final ArrayList<CallHistoryModel> list;
    private final Context context;

    public CallHistoryAdapter(ArrayList<CallHistoryModel> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.sample_call_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CallHistoryModel model = list.get(position);

        String userName = (model.getOtherUserName() != null && !model.getOtherUserName().isEmpty())
                ? model.getOtherUserName() : "User";
        holder.tvUserName.setText(userName);

        String userPic = model.getOtherUserPic();
        if (userPic != null && !userPic.trim().isEmpty()) {
            Picasso.get().load(userPic).placeholder(R.drawable.avatar).error(R.drawable.avatar).into(holder.ivUserPic);
        } else {
            holder.ivUserPic.setImageResource(R.drawable.avatar);
        }

        // Format Date Time
        String formattedDate = getFormattedTime(model.getTimestamp());
        holder.tvTime.setText(formattedDate);

        // Call type icon
        if ("video".equalsIgnoreCase(model.getCallType())) {
            holder.ivCallAction.setImageResource(R.drawable.videocall);
        } else {
            holder.ivCallAction.setImageResource(R.drawable.voicecall);
        }

        // Direction & color filter
        String direction = model.getDirection();
        if ("missed".equalsIgnoreCase(direction) || "declined".equalsIgnoreCase(direction)) {
            holder.ivDirection.setImageResource(R.drawable.voicecall);
            holder.ivDirection.setColorFilter(Color.parseColor("#F44336")); // Red for missed/declined
        } else {
            holder.ivDirection.setImageResource(R.drawable.voicecall);
            holder.ivDirection.setColorFilter(Color.parseColor("#00A884")); // Green for completed incoming/outgoing
        }

        // Click listener to navigate safely
        View.OnClickListener clickListener = v -> {
            String otherUserId = model.getOtherUserId();
            if (otherUserId == null || otherUserId.trim().isEmpty()) {
                Toast.makeText(context, "User details unavailable", Toast.LENGTH_SHORT).show();
                return;
            }

            if (model.isGroupCall() || otherUserId.startsWith("group_")) {
                Intent intent = new Intent(context, GroupChatActivity.class);
                intent.putExtra("groupId", otherUserId.replace("group_", ""));
                intent.putExtra("groupName", userName);
                intent.putExtra("groupIcon", userPic != null ? userPic : "");
                context.startActivity(intent);
            } else {
                Intent intent = new Intent(context, ChatDetailActivity.class);
                intent.putExtra("userId", otherUserId);
                intent.putExtra("username", userName);
                intent.putExtra("profile", userPic != null ? userPic : "");
                context.startActivity(intent);
            }
        };

        holder.itemView.setOnClickListener(clickListener);
        holder.ivCallAction.setOnClickListener(clickListener);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private String getFormattedTime(long timestamp) {
        if (timestamp <= 0) return "Recent";
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(timestamp);
        return DateFormat.format("MMM dd, hh:mm a", cal).toString();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUserPic, ivDirection, ivCallAction;
        TextView tvUserName, tvTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserPic = itemView.findViewById(R.id.ivCallUserPic);
            ivDirection = itemView.findViewById(R.id.ivCallDirection);
            ivCallAction = itemView.findViewById(R.id.ivCallAction);
            tvUserName = itemView.findViewById(R.id.tvCallUserName);
            tvTime = itemView.findViewById(R.id.tvCallTime);
        }
    }
}
