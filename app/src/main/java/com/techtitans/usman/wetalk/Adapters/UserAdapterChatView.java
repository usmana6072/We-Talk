package com.techtitans.usman.wetalk.Adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;
import com.techtitans.usman.wetalk.ChatDetailActivity;
import com.techtitans.usman.wetalk.Models.Users;
import com.techtitans.usman.wetalk.R;

import java.util.ArrayList;

public class UserAdapterChatView extends RecyclerView.Adapter<UserAdapterChatView.ViewHolder> {

    private final ArrayList<Users> list;
    private final Context context;

    public UserAdapterChatView(ArrayList<Users> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.sample_show_user_in_chat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Users users = list.get(position);

        if (users.getProfilePic() != null && !users.getProfilePic().trim().isEmpty()) {
            Picasso.get().load(users.getProfilePic()).placeholder(R.drawable.avatar).error(R.drawable.avatar).into(holder.profile);
        } else {
            holder.profile.setImageResource(R.drawable.avatar);
        }

        String userName = (users.getUserName() != null && !users.getUserName().isEmpty()) ? users.getUserName() : "User";
        holder.tvUserName.setText(userName);

        String lastMsg = users.getLastMessage();
        if (lastMsg != null && !lastMsg.isEmpty()) {
            if (lastMsg.length() > 35) {
                lastMsg = lastMsg.substring(0, 35) + "...";
            }
            holder.tvLastMessage.setText(lastMsg);
        } else if (users.getMail() != null && !users.getMail().isEmpty()) {
            holder.tvLastMessage.setText(users.getMail());
        } else {
            holder.tvLastMessage.setText("No Chat");
        }

        View.OnClickListener openChatListener = v -> {
            Intent intent = new Intent(context, ChatDetailActivity.class);
            intent.putExtra("userId", users.getUserId());
            intent.putExtra("username", userName);
            intent.putExtra("profile", users.getProfilePic() != null ? users.getProfilePic() : "");
            context.startActivity(intent);
        };

        holder.itemView.setOnClickListener(openChatListener);

        View.OnLongClickListener longClickListener = v -> applyAction(holder, users);
        holder.itemView.setOnLongClickListener(longClickListener);
    }

    private boolean applyAction(ViewHolder holder, Users users) {
        int position = holder.getAdapterPosition();
        if (position == RecyclerView.NO_POSITION) return true;

        new AlertDialog.Builder(context)
                .setTitle("Delete Chat")
                .setMessage("Do you want to delete this chat?")
                .setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Yes", (dialog, which) -> {
                    String currentUid = FirebaseAuth.getInstance().getUid();
                    if (currentUid != null && users.getUserId() != null) {
                        String senderRoom = currentUid + users.getUserId();
                        String receiverRoom = users.getUserId() + currentUid;
                        FirebaseDatabase.getInstance().getReference().child("Chats").child(senderRoom).removeValue();
                        FirebaseDatabase.getInstance().getReference().child("Chats").child(receiverRoom).removeValue();
                    }
                    if (position < list.size()) {
                        list.remove(position);
                        notifyItemRemoved(position);
                    }
                })
                .show();
        return true;
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView profile;
        TextView tvUserName;
        TextView tvLastMessage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            profile = itemView.findViewById(R.id.profileimage);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
        }
    }
}
