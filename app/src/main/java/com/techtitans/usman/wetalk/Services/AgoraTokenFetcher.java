package com.techtitans.usman.wetalk.Services;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class AgoraTokenFetcher {

    private static final String SERVER_URL = "https://we-talk-agora-tocken-server-production.up.railway.app/getToken?channel=";

    public interface TokenCallback {
        void onSuccess(String token);
        void onError(String error);
    }

    public static void fetchToken(String channelName, TokenCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(SERVER_URL + channelName);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    JSONObject jsonObject = new JSONObject(response.toString());
                    String token = jsonObject.getString("token");
                    Log.d("AGORA_TOKEN", "Fetched Token: " + token);

                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(token));
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("Server returned code: " + responseCode));
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }
}
