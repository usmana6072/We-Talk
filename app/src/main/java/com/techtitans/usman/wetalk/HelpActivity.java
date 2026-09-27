package com.techtitans.usman.wetalk;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.techtitans.usman.wetalk.databinding.ActivityHelpBinding;

public class HelpActivity extends AppCompatActivity {

    private ActivityHelpBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityHelpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBackHelp.setOnClickListener(v -> finish());

        binding.btnContactSupport.setOnClickListener(v -> {
            String email = getString(R.string.support_email);
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + email));
            intent.putExtra(Intent.EXTRA_SUBJECT, "WeTalk Support & Inquiry");
            try {
                startActivity(Intent.createChooser(intent, "Contact Support via"));
            } catch (Exception e) {
                intent.setData(null);
                intent.setType("message/rfc822");
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
                startActivity(Intent.createChooser(intent, "Contact Support via"));
            }
        });
    }
}
