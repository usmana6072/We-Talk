package com.techtitans.usman.wetalk;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudinary.android.MediaManager;
import com.google.firebase.auth.FirebaseAuth;
import com.techtitans.usman.wetalk.Adapters.FragmentAdapter;
import com.techtitans.usman.wetalk.databinding.ActivityMainBinding;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    FirebaseAuth auth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding=ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        auth=FirebaseAuth.getInstance();
        setSupportActionBar(binding.toolbar);

        initConfig();

        binding.viewPager.setAdapter(new FragmentAdapter(getSupportFragmentManager()));
        binding.tabLayout.setupWithViewPager(binding.viewPager);
    }

    private void initConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dyfjjuzkv");
        config.put("api_key","385755796768381");
        config.put("api_secret","0I-6iNQBkNMrYq6LlKTsiKZ5ag8");
        MediaManager.init(this, config);
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
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else if (item.getItemId()==R.id.groupChatItem) {
            Intent intent=new Intent(MainActivity.this, GroupChatActivity.class);
           startActivity(intent);
        }
        return true;
    }
}