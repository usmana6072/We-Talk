package com.techtitans.usman.wetalk;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.MediaController;

import androidx.appcompat.app.AppCompatActivity;

import com.techtitans.usman.wetalk.databinding.ActivityMediaPreviewV2Binding;

public class MediaPreviewActivityV2 extends AppCompatActivity {

    ActivityMediaPreviewV2Binding binding;
    Uri fileUri;
    String type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMediaPreviewV2Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        fileUri = getIntent().getParcelableExtra("uri");
        type = getIntent().getStringExtra("type");

        if (fileUri == null || type == null) {
            finish();
            return;
        }

        setupPreview();

        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnSend.setOnClickListener(v -> {
            String caption = binding.etCaption.getText().toString();
            Intent resultIntent = new Intent();
            resultIntent.putExtra("uri", fileUri);
            resultIntent.putExtra("type", type);
            resultIntent.putExtra("caption", caption);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    private void setupPreview() {
        if ("image".equalsIgnoreCase(type)) {
            binding.imagePreview.setVisibility(View.VISIBLE);
            binding.videoPreview.setVisibility(View.GONE);
            binding.btnPlayVideo.setVisibility(View.GONE);
            binding.imagePreview.setImageURI(fileUri);
            binding.tvTitle.setText("Image Preview");
        } else if ("video".equalsIgnoreCase(type)) {
            binding.imagePreview.setVisibility(View.GONE);
            binding.videoPreview.setVisibility(View.VISIBLE);
            binding.btnPlayVideo.setVisibility(View.VISIBLE);
            binding.tvTitle.setText("Video Preview");

            binding.videoPreview.setVideoURI(fileUri);
            
            MediaController mediaController = new MediaController(this);
            mediaController.setAnchorView(binding.videoPreview);
            binding.videoPreview.setMediaController(mediaController);

            binding.btnPlayVideo.setOnClickListener(v -> {
                binding.btnPlayVideo.setVisibility(View.GONE);
                binding.videoPreview.start();
            });

            binding.videoPreview.setOnCompletionListener(mp -> {
                binding.btnPlayVideo.setVisibility(View.VISIBLE);
            });
            
            binding.videoPreview.setOnClickListener(v -> {
                if (binding.videoPreview.isPlaying()) {
                    binding.videoPreview.pause();
                    binding.btnPlayVideo.setVisibility(View.VISIBLE);
                } else {
                    binding.videoPreview.start();
                    binding.btnPlayVideo.setVisibility(View.GONE);
                }
            });
        }
    }
}
