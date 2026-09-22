package com.techtitans.usman.wetalk;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.techtitans.usman.wetalk.Models.Users;
import com.techtitans.usman.wetalk.databinding.ActivitySignUpBinding;

public class SignUpActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 65;

    FirebaseDatabase database;
    FirebaseAuth auth;
    ProgressDialog progressDialog;

    GoogleSignInOptions gso;
    GoogleSignInClient mGoogleSignInClient;

    ActivitySignUpBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id).trim())
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        progressDialog = new ProgressDialog(SignUpActivity.this);
        progressDialog.setTitle("Creating Account");
        progressDialog.setMessage("We're creating your account");

        binding.btnSignUp.setOnClickListener(e -> {
            String email = binding.editTextTextEmailAddress.getText().toString().trim();
            String password = binding.editTextTextPassword.getText().toString().trim();
            String username = binding.editViewUserName.getText().toString().trim();

            if (username.isEmpty()) {
                binding.editViewUserName.setError("Username required");
                return;
            }
            if (email.isEmpty()) {
                binding.editTextTextEmailAddress.setError("Email required");
                return;
            }
            if (password.isEmpty()) {
                binding.editTextTextPassword.setError("Password required");
                return;
            }

            progressDialog.show();
            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        Users user = new Users(username, email, password);
                        String id = task.getResult().getUser().getUid();
                        user.setUserId(id);
                        database.getReference().child("Users").child(id).setValue(user);
                        storeFCMToken();
                        Toast.makeText(SignUpActivity.this, "Signed up successfully", Toast.LENGTH_SHORT).show();
                        updateUI();
                    } else {
                        String msg = (task.getException() != null) ? task.getException().getMessage() : "Sign up failed";
                        Toast.makeText(SignUpActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });

        binding.btnGoogle.setOnClickListener(e -> {
            progressDialog.show();
            signInWithGoogle();
        });

        binding.tvAlreadyHaveAccount.setOnClickListener(e -> {
            Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
            startActivity(intent);
        });
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                } else {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                }
            } catch (ApiException e) {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                if (task.isSuccessful()) {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user != null) {
                        Users users = new Users();
                        String photoUrl = (user.getPhotoUrl() != null) ? user.getPhotoUrl().toString() : "";
                        String email = (user.getEmail() != null) ? user.getEmail() : "";
                        String name = (user.getDisplayName() != null) ? user.getDisplayName() : "User";

                        users.setProfilePic(photoUrl);
                        users.setMail(email);
                        users.setUserName(name);
                        users.setUserId(user.getUid());

                        database.getReference().child("Users").child(user.getUid()).setValue(users);
                    }
                    storeFCMToken();
                    updateUI();
                } else {
                    String msg = (task.getException() != null) ? task.getException().getMessage() : "Authentication failed";
                    Toast.makeText(SignUpActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateUI() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void storeFCMToken() {
        String id = FirebaseAuth.getInstance().getUid();
        if (id == null) return;
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) return;
                    String token = task.getResult();
                    DatabaseReference reference = database.getReference().child("FCM").child(id).child(token);
                    reference.setValue(token);
                    Log.d("FCM_TOKEN", token);
                });
    }
}
