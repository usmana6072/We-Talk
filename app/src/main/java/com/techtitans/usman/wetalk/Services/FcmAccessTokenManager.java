package com.techtitans.usman.wetalk.Services;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FcmAccessTokenManager {

    private static final String TAG = "FcmAccessTokenManager";
    private static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final OkHttpClient httpClient = new OkHttpClient();

    private static String cachedToken;
    private static long cachedTokenExpiryMillis = 0;

    public interface TokenCallback {
        void onToken(String accessToken);
        void onError(Exception e);
    }

    public static void getAccessToken(Context context, TokenCallback callback) {
        if (cachedToken != null && System.currentTimeMillis() < cachedTokenExpiryMillis - 60_000) {
            callback.onToken(cachedToken);
            return;
        }

        executor.execute(() -> {
            try {
                String token = fetchNewAccessToken(context);
                mainHandler.post(() -> callback.onToken(token));
            } catch (Exception e) {
                Log.e(TAG, "Failed to fetch FCM access token", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    private static String fetchNewAccessToken(Context context) throws Exception {
        JSONObject serviceAccount = readServiceAccountJson(context);
        String clientEmail = serviceAccount.getString("client_email");
        String privateKeyPem = serviceAccount.getString("private_key");

        String jwt = buildSignedJwt(clientEmail, privateKeyPem);

        RequestBody formBody = new FormBody.Builder()
                .add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                .add("assertion", jwt)
                .build();

        Request request = new Request.Builder()
                .url(TOKEN_URL)
                .post(formBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new RuntimeException("Token exchange failed: " + response.code() + " " + responseBody);
            }
            JSONObject json = new JSONObject(responseBody);
            String accessToken = json.getString("access_token");
            int expiresIn = json.optInt("expires_in", 3600);

            cachedToken = accessToken;
            cachedTokenExpiryMillis = System.currentTimeMillis() + (expiresIn * 1000L);

            return accessToken;
        }
    }

    private static JSONObject readServiceAccountJson(Context context) throws Exception {
        // Updated to read from assets
        InputStream is = context.getAssets().open("service-account.json");
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return new JSONObject(sb.toString());
    }

    private static String buildSignedJwt(String clientEmail, String privateKeyPem) throws Exception {
        long nowSeconds = System.currentTimeMillis() / 1000L;
        long expSeconds = nowSeconds + 3600;

        JSONObject header = new JSONObject();
        header.put("alg", "RS256");
        header.put("typ", "JWT");

        JSONObject claims = new JSONObject();
        claims.put("iss", clientEmail);
        claims.put("scope", SCOPE);
        claims.put("aud", TOKEN_URL);
        claims.put("iat", nowSeconds);
        claims.put("exp", expSeconds);

        String headerEncoded = base64UrlEncode(header.toString().getBytes(StandardCharsets.UTF_8));
        String claimsEncoded = base64UrlEncode(claims.toString().getBytes(StandardCharsets.UTF_8));
        String unsignedToken = headerEncoded + "." + claimsEncoded;

        PrivateKey privateKey = parsePrivateKey(privateKeyPem);
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(unsignedToken.getBytes(StandardCharsets.UTF_8));
        byte[] signedBytes = signature.sign();

        return unsignedToken + "." + base64UrlEncode(signedBytes);
    }

    private static PrivateKey parsePrivateKey(String pem) throws Exception {
        String cleaned = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\n", "")
                .trim();
        byte[] decoded = Base64.decode(cleaned, Base64.DEFAULT);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    private static String base64UrlEncode(byte[] data) {
        return Base64.encodeToString(data, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }
}
