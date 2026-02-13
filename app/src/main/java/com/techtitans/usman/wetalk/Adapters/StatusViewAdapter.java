package com.techtitans.usman.wetalk.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.techtitans.usman.wetalk.ChatDetailActivity;
import com.techtitans.usman.wetalk.Models.StatusModel;
import com.techtitans.usman.wetalk.Models.Users;
import com.techtitans.usman.wetalk.R;
import com.techtitans.usman.wetalk.StatusViewActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class StatusViewAdapter extends RecyclerView.Adapter<StatusViewAdapter.ViewHolder> {

    ArrayList<StatusModel> list;
    Context context;

    public StatusViewAdapter(ArrayList<StatusModel> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view=LayoutInflater.from(context).inflate(R.layout.sample_show_user_in_chat,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StatusModel statusModel=list.get(position);
        if(statusModel==null) return;
        Picasso.get().load(statusModel.getStatusPic()).placeholder(R.drawable.avatar).into(holder.status);
        holder.tvUserName.setText(statusModel.getUserName());
        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("h: m a");
        holder.tvTime.setText(simpleDateFormat.format(new Date(statusModel.getTimeStamp())));

        holder.status.setOnClickListener(e->{
            Intent intent=new Intent(context, StatusViewActivity.class);
            intent.putExtra("status",statusModel.getStatusPic());
            intent.putExtra("userName",statusModel.getUserName());
            intent.putExtra("time",holder.tvTime.getText().toString());
            context.startActivity(intent);
        });

        holder.tvUserName.setOnClickListener(e->{
            Intent intent=new Intent(context, StatusViewActivity.class);
            intent.putExtra("status",statusModel.getStatusPic());
            intent.putExtra("userName",statusModel.getUserName());
            intent.putExtra("time",holder.tvTime.getText().toString());
            context.startActivity(intent);
        });
        holder.tvTime.setOnClickListener(e->{
            Intent intent=new Intent(context, StatusViewActivity.class);
            intent.putExtra("status",statusModel.getStatusPic());
            intent.putExtra("userName",statusModel.getUserName());
            intent.putExtra("time",holder.tvTime.getText().toString());
            context.startActivity(intent);
        });

    }

    @Override
    public int getItemCount() {
        return list.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder{
        ImageView status;
        TextView tvUserName;
        TextView tvTime;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            status= itemView.findViewById(R.id.profileimage);
            tvUserName=itemView.findViewById(R.id.tvUserName);
            tvTime=itemView.findViewById(R.id.tvLastMessage);
        }
    }
}
