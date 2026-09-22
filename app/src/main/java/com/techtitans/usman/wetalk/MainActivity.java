package com.techtitans.usman.wetalk;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.cloudinary.android.MediaManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.techtitans.usman.wetalk.Adapters.FragmentAdapter;
import com.techtitans.usman.wetalk.Calls.IncomingCallActivity;
import com.techtitans.usman.wetalk.databinding.ActivityMainBinding;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    FirebaseAuth auth;
    FirebaseDatabase database;

    private static final int NOTIFICATION_PERMISSION_CODE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding=ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        auth=FirebaseAuth.getInstance();
        database=FirebaseDatabase.getInstance();
        setSupportActionBar(binding.toolbar);

        initConfig();

        binding.viewPager.setAdapter(new FragmentAdapter(getSupportFragmentManager()));
        binding.tabLayout.setupWithViewPager(binding.viewPager);

        checkNotificationPermission();
        listenForCalls();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notifications permission denied. You won't see incoming messages.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void listenForCalls() {
        String uid = auth.getUid();
        if (uid == null) return;

        database.getReference().child("Calls").child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String status = snapshot.child("status").getValue(String.class);
                    if ("ringing".equals(status)) {
                        String channelName = snapshot.child("channelName").getValue(String.class);
                        String token = snapshot.child("token").getValue(String.class);
                        String callerId = snapshot.child("callerId").getValue(String.class);
                        String callerName = snapshot.child("callerName").getValue(String.class);
                        String callerIcon = snapshot.child("callerIcon").getValue(String.class);
                        String groupName = snapshot.child("groupName").getValue(String.class);
                        Boolean isVideo = snapshot.child("isVideoCall").getValue(Boolean.class);
                        Boolean isGroup = snapshot.child("isGroupCall").getValue(Boolean.class);
                        
                        boolean isVideoCall = isVideo != null && isVideo;
                        boolean isGroupCall = isGroup != null && isGroup;

                        // CRITICAL: Set to 'received' immediately to prevent crash loops
                        database.getReference().child("Calls").child(uid).child("status").setValue("received");

                        if (channelName != null && callerId != null) {
                            Intent intent = new Intent(MainActivity.this, IncomingCallActivity.class);
                            intent.putExtra("channelName", channelName);
                            intent.putExtra("token", token);
                            intent.putExtra("callerId", callerId);
                            intent.putExtra("callerName", callerName != null ? callerName : "Unknown");
                            intent.putExtra("callerIcon", callerIcon != null ? callerIcon : "");
                            intent.putExtra("groupName", groupName);
                            intent.putExtra("isVideoCall", isVideoCall);
                            intent.putExtra("isGroupCall", isGroupCall);
                            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void initConfig() {
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "dyfjjuzkv");
            config.put("api_key", "385755796768381");
            config.put("api_secret", "0I-6iNQBkNMrYq6LlKTsiKZ5ag8");
            MediaManager.init(this, config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.toobar_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if(item.getItemId()==R.id.settingItem){
            Intent intent=new Intent(MainActivity.this,SettingActivity.class);
            startActivity(intent);
        } else if (item.getItemId()==R.id.logoutItem) {
            auth.signOut();
            Intent intent=new Intent(MainActivity.this,SignInActivity.class);
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK| FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else if (item.getItemId()==R.id.groupChatItem) {
            Intent intent=new Intent(MainActivity.this, GroupChatActivity.class);
           startActivity(intent);
        } else if (item.getItemId()==R.id.createGroupItem) {
            Intent intent=new Intent(MainActivity.this, CreateGroupActivity.class);
            startActivity(intent);
        }
        return true;
    }
}
