package com.techtitans.usman.wetalk;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.techtitans.usman.wetalk.databinding.ActivityNotificationSettingsBinding;

public class NotificationSettingsActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "wetalk_prefs";
    public static final String KEY_MSG_NOTIF = "pref_msg_notif";
    public static final String KEY_CALL_NOTIF = "pref_call_notif";
    public static final String KEY_SOUND_VIBRATE = "pref_sound_vibrate";

    private ActivityNotificationSettingsBinding binding;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityNotificationSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        binding.btnBackNotificationSettings.setOnClickListener(v -> finish());

        // Load saved preferences
        boolean msgNotif = preferences.getBoolean(KEY_MSG_NOTIF, true);
        boolean callNotif = preferences.getBoolean(KEY_CALL_NOTIF, true);
        boolean soundVibrate = preferences.getBoolean(KEY_SOUND_VIBRATE, true);

        binding.switchMessageNotif.setChecked(msgNotif);
        binding.switchCallNotif.setChecked(callNotif);
        binding.switchSoundVibrate.setChecked(soundVibrate);

        binding.switchMessageNotif.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(KEY_MSG_NOTIF, isChecked).apply();
        });

        binding.switchCallNotif.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(KEY_CALL_NOTIF, isChecked).apply();
        });

        binding.switchSoundVibrate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(KEY_SOUND_VIBRATE, isChecked).apply();
        });
    }
}
