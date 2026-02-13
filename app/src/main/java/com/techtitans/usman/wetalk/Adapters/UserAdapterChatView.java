package com.techtitans.usman.wetalk.Adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.techtitans.usman.wetalk.ChatDetailActivity;
import com.techtitans.usman.wetalk.Models.Users;
import com.techtitans.usman.wetalk.R;

import java.util.ArrayList;

public class UserAdapterChatView extends RecyclerView.Adapter<UserAdapterChatView.ViewHolder> {

    ArrayList<Users> list;
    Context context;

    UserAdapterChatView adapterChatView;


    public UserAdapterChatView(ArrayList<Users> list, Context context) {
        this.list = list;
        this.context = context;
    }

    public UserAdapterChatView(ArrayList<Users> list, Context context, UserAdapterChatView adapterChatView) {
        this.list = list;
        this.context = context;
        this.adapterChatView = adapterChatView;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(context).inflate(R.layout.sample_show_user_in_chat,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Users users=list.get(position);
        Picasso.get().load(users.getProfilePic()).placeholder(R.drawable.avatar).into(holder.profile);
        holder.tvUserName.setText(users.getUserName());

        holder.itemView.setOnLongClickListener(e->{
            return applyAction(holder,users);
        });

        String message;
        FirebaseDatabase.getInstance().getReference().child("Chats")
                .child(FirebaseAuth.getInstance().getUid()+users.getUserId())
                .orderByChild("messageTime").limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.hasChildren()){
                            for(DataSnapshot snapshot1:snapshot.getChildren() ){
                                String message =snapshot1.child("messageText").getValue().toString();
                                if(message.length()>30)
                                    message=message.substring(0,30);
                                holder.tvLastMessage.setText(message);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
        if(users.getLastMessage()!=null)
            holder.tvLastMessage.setText(users.getLastMessage());
        else
            holder.tvLastMessage.setText("No Chat");
        holder.profile.setOnClickListener(e->{
            Intent intent=new Intent(context, ChatDetailActivity.class);
            intent.putExtra("userId",users.getUserId());
            intent.putExtra("username",users.getUserName());
            intent.putExtra("profile",users.getProfilePic());
            context.startActivity(intent);
        });

        holder.tvLastMessage.setOnClickListener(e->{
            Intent intent=new Intent(context, ChatDetailActivity.class);
            intent.putExtra("userId",users.getUserId());
            intent.putExtra("username",users.getUserName());
            intent.putExtra("profile",users.getProfilePic());
            context.startActivity(intent);
        });

        holder.tvUserName.setOnClickListener(e->{
            Intent intent=new Intent(context, ChatDetailActivity.class);
            intent.putExtra("userId",users.getUserId());
            intent.putExtra("username",users.getUserName());
            intent.putExtra("profile",users.getProfilePic());
            context.startActivity(intent);
        });

        holder.profile.setOnLongClickListener(e->{
            return applyAction(holder,users);
        });

        holder.tvUserName.setOnLongClickListener(e->{
            return applyAction(holder,users);
        });

        holder.tvLastMessage.setOnLongClickListener(e->{
            return applyAction(holder,users);
        });
    }

    private boolean applyAction(ViewHolder holder, Users users) {
        int positions = holder.getAdapterPosition();
        if (positions == RecyclerView.NO_POSITION) return true;

        new AlertDialog.Builder(context).setTitle("Conformation")
                .setMessage("Do you want to delete this chat")
                .setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String senderId=FirebaseAuth.getInstance().getUid()+users.getUserId();
                        String receiverId=users.getUserId()+FirebaseAuth.getInstance().getUid();
                        FirebaseDatabase.getInstance().getReference().child("Chats")
                                .child(senderId).removeValue();
                        FirebaseDatabase.getInstance().getReference().child("Chats")
                                .child(receiverId).removeValue();
                        list.remove(users);
//                        UserAdapterChatView.this.notifyDataSetChanged();
                    }
                }).show();
        return true;
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        ImageView profile;
        TextView tvUserName;
        TextView tvLastMessage;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            profile= itemView.findViewById(R.id.profileimage);
            tvUserName=itemView.findViewById(R.id.tvUserName);
            tvLastMessage=itemView.findViewById(R.id.tvLastMessage);
        }
    }
}
