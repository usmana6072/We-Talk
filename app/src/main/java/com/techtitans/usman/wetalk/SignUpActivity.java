package com.techtitans.usman.wetalk;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.techtitans.usman.wetalk.Models.Users;
import com.techtitans.usman.wetalk.databinding.ActivitySignUpBinding;

public class SignUpActivity extends AppCompatActivity {
    FirebaseDatabase database;
    FirebaseAuth auth;
    ProgressDialog progressDialog;

    ActivitySignUpBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding=ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth=FirebaseAuth.getInstance();
        database=FirebaseDatabase.getInstance();

        progressDialog=new ProgressDialog(SignUpActivity.this);
        progressDialog.setTitle("Creating Account");
        progressDialog.setMessage("We're creating your account");

        binding.btnSignUp.setOnClickListener(e->{
            progressDialog.show();
            auth.createUserWithEmailAndPassword(binding.editTextTextEmailAddress.getText().toString(),binding.editTextTextPassword.getText().toString()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    progressDialog.dismiss();
                    if(task.isSuccessful()){
                        Users user=new Users(binding.editViewUserName.getText().toString(),binding.editTextTextEmailAddress.getText().toString(),binding.editTextTextPassword.getText().toString());
                        String id=task.getResult().getUser().getUid();
                        database.getReference().child("Users").child(id).setValue(user);
                        Toast.makeText(SignUpActivity.this, "SignUp successfully", Toast.LENGTH_SHORT).show();
                    }else
                        Toast.makeText(SignUpActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();

                }
            });
        });

        binding.tvAlreadyHaveAccount.setOnClickListener(e->{
            Intent intent=new Intent(SignUpActivity.this, SignInActivity.class);
            startActivity(intent);
        });
    }

}