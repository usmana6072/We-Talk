package com.techtitans.usman.wetalk;

import android.os.Bundle;
import android.os.CountDownTimer;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.squareup.picasso.Picasso;
import com.techtitans.usman.wetalk.databinding.ActivityStatusViewBinding;

import java.util.Timer;

public class StatusViewActivity extends AppCompatActivity {

    ActivityStatusViewBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding=ActivityStatusViewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        String userName,time,status;
        userName=getIntent().getStringExtra("userName");
        time=getIntent().getStringExtra("time");
        status=getIntent().getStringExtra("status");
        binding.tvUserNameChatDetails.setText(userName);
        binding.tvTime.setText(time);
        Picasso.get().load(FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl())
                        .placeholder(R.drawable.avatar).into(binding.profileimage);
        Picasso.get().load(status).placeholder(R.drawable.avatar).into(binding.statusImage);

        new CountDownTimer(6000,60){

            @Override
            public void onFinish() {
                finish();
            }

            @Override
            public void onTick(long millisUntilFinished) {
                binding.progressBar.setProgress(binding.progressBar.getProgress()+1);
            }
        }.start();

        binding.backArrow.setOnClickListener(e->{
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }
}