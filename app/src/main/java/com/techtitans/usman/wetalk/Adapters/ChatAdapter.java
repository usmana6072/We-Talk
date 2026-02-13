package com.techtitans.usman.wetalk.Adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.techtitans.usman.wetalk.Models.MessageModel;
import com.techtitans.usman.wetalk.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ChatAdapter extends RecyclerView.Adapter{

    ArrayList<MessageModel> list;
    Context context;
    String receiverId;

    int SENDER_VIEW_TYPE=1;
    int RECEIVER_VIEW_TYPE=2;

    public ChatAdapter(ArrayList<MessageModel> list, Context context) {
        this.list = list;
        this.context = context;
    }

    public ChatAdapter(ArrayList<MessageModel> list, Context context, String receiverId) {
        this.list = list;
        this.context = context;
        this.receiverId = receiverId;
    }

    @Override
    public int getItemViewType(int position) {
        if(list.get(position).getUserId().equals(FirebaseAuth.getInstance().getUid()))
            return SENDER_VIEW_TYPE;
        else
            return RECEIVER_VIEW_TYPE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == RECEIVER_VIEW_TYPE) {
            view = LayoutInflater.from(context).inflate(R.layout.sample_receiver_layout, parent, false);
            return new RecieverViewHolder(view);
        } else{
            view = LayoutInflater.from(context).inflate(R.layout.sample_sender_layout, parent, false);
            return new SenderViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageModel message=list.get(position);

        holder.itemView.setOnLongClickListener(e->{

            new AlertDialog.Builder(context).setTitle("Confirmation")
                    .setMessage("Are You sure You want to delete message")
                    .setPositiveButton("Delete from me", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String chatId=FirebaseAuth.getInstance().getUid()+receiverId;
                            FirebaseDatabase.getInstance().getReference().child("Chats").child(chatId)
                                    .child(message.getMessageId()).removeValue();
                        }
                    })
                    .setNegativeButton("Delete From everyone", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String chatId=FirebaseAuth.getInstance().getUid()+receiverId;
                            FirebaseDatabase.getInstance().getReference().child("Chats").child(chatId)
                                    .child(message.getMessageId()).removeValue();
                            FirebaseDatabase.getInstance().getReference().child("Chats").child(receiverId+FirebaseAuth.getInstance().getUid())
                                    .child(message.getMessageId()).removeValue();
                        }
                    }).setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).show();
            return true;
        });
        if(holder.getClass()==SenderViewHolder.class){
            ((SenderViewHolder) holder).senderMessage.setText(message.getMessageText());
            Date date=new Date(message.getMessageTime());
            SimpleDateFormat dateFormat=new SimpleDateFormat("h:m a");
          ((SenderViewHolder) holder).senderTime.setText(dateFormat.format(date));
        }
        else{
            ((RecieverViewHolder) holder).receiverMessage.setText(message.getMessageText());
            Date date=new Date(message.getMessageTime());
            SimpleDateFormat dateFormat=new SimpleDateFormat("h:m a");
           ((RecieverViewHolder)holder).receiverTime.setText(dateFormat.format(date));
        }
    }


    @Override
    public int getItemCount() {
        return list.size();
    }

    public class RecieverViewHolder extends RecyclerView.ViewHolder{
        TextView receiverMessage;
        TextView receiverTime;
        public RecieverViewHolder(@NonNull View itemView) {
            super(itemView);

            receiverMessage =itemView.findViewById(R.id.receivertext);
            receiverTime=itemView.findViewById(R.id.recieivertime);
        }
    }

    public class SenderViewHolder extends RecyclerView.ViewHolder{
        TextView senderMessage;
        TextView senderTime;
        public SenderViewHolder(@NonNull View itemView) {
            super(itemView);

            senderMessage=itemView.findViewById(R.id.senderText);
            senderTime=itemView.findViewById(R.id.senderTime);
        }
    }
}
