package com.techtitans.usman.wetalk;

import android.app.ComponentCaller;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
import com.google.firebase.database.FirebaseDatabase;
import com.techtitans.usman.wetalk.Models.Users;
import com.techtitans.usman.wetalk.databinding.ActivitySignInBinding;

public class SignInActivity extends AppCompatActivity {
    int RC_SIGN_IN=65;

    FirebaseAuth auth;
    FirebaseDatabase database;
    GoogleSignInOptions gso;
    GoogleSignInClient mGoogleSignInClient;
    ActivitySignInBinding binding;

    ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding=ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        auth=FirebaseAuth.getInstance();
        database=FirebaseDatabase.getInstance();

        gso=new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build();
        mGoogleSignInClient= GoogleSignIn.getClient(this,gso);

        updateUI();
        progressDialog=new ProgressDialog(SignInActivity.this);
        progressDialog.setTitle("Login");
        progressDialog.setMessage("Logging in your account");

        binding.btnSignIn.setOnClickListener(e->{
            progressDialog.show();
            auth.signInWithEmailAndPassword(binding.editViewEmailPhone.getText().toString(),binding.editTextTextPassword.getText().toString()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    progressDialog.dismiss();
                    if(task.isSuccessful()){
                        updateUI();
                    }else
                        Toast.makeText(SignInActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        binding.btnGoogle.setOnClickListener(e->{
            progressDialog.show();
            signIn();
        });

        binding.tvDontHaveAccount.setOnClickListener(e->{
            Intent intent=new Intent(SignInActivity.this,SignUpActivity.class);
            startActivity(intent);
        });

    }

    private void signIn() {
        Intent signInIntent=mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent,RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode==RC_SIGN_IN){
            Task<GoogleSignInAccount> task=GoogleSignIn.getSignedInAccountFromIntent(data);
            try{
                GoogleSignInAccount account=task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential authCredential= GoogleAuthProvider.getCredential(idToken,null);
        auth.signInWithCredential(authCredential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                progressDialog.dismiss();
                if(task.isSuccessful()){
                    FirebaseUser user=auth.getCurrentUser();
                    Users users=new Users();
                    users.setProfilePic(user.getPhotoUrl().toString());
                    users.setMail(user.getEmail());
                    users.setUserName(user.getDisplayName());
                    database.getReference().child("Users").child(user.getUid()).setValue(users);
                    updateUI();
                }
                else
                    Toast.makeText(SignInActivity.this,task.getException().getMessage(),Toast.LENGTH_SHORT);
            }
        });
    }

    public void updateUI(){
        FirebaseUser user=auth.getCurrentUser();
        if(user!=null){
            Intent intent=new Intent(SignInActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }
}