package com.example.yeswa.lapitchat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout mLoginEmail;
    private TextInputLayout mLoginPassword;
    private Button mLoginButton;
    private Toolbar mLoginToolbar;

    //Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    //ProgressDialog
    private ProgressDialog mLoginProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mLoginEmail = (TextInputLayout) findViewById(R.id.log_email);
        mLoginPassword = (TextInputLayout) findViewById(R.id.log_password);
        mLoginButton = (Button) findViewById(R.id.log_create_btn);

        mLoginToolbar = (Toolbar) findViewById(R.id.login_toolbar);
        setSupportActionBar(mLoginToolbar);
        getSupportActionBar().setTitle("Login");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Users");

        mLoginProgress = new ProgressDialog(this);

        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = mLoginEmail.getEditText().getText().toString();
                String password = mLoginPassword.getEditText().getText().toString();

                if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)){

                    mLoginProgress.setTitle("Loggin In");
                    mLoginProgress.setMessage("Please wait while we check your credentials");
                    mLoginProgress.setCanceledOnTouchOutside(false);
                    mLoginProgress.show();

                    LoginUser(email, password);
                }
            }
        });
    }

    private void LoginUser(String email, String password) {

      mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
          @Override
          public void onComplete(@NonNull Task<AuthResult> task) {
              if (task.isSuccessful()){

                  String current_user = mAuth.getCurrentUser().getUid();
                  String token_id = FirebaseInstanceId.getInstance().getToken();

                  mDatabase.child(current_user).child("device_token").setValue(token_id).addOnCompleteListener(new OnCompleteListener<Void>() {
                      @Override
                      public void onComplete(@NonNull Task<Void> task) {
                          if (task.isSuccessful()) {
                              mLoginProgress.dismiss();
                              Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
                              mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                              startActivity(mainIntent);
                              finish();
                          } else {
                              Toast.makeText(LoginActivity.this, "Login Failed while creating TokenID", Toast.LENGTH_LONG).show();
                          }
                      }
                  });

              }
              else {
                  mLoginProgress.hide();
                  // If sign in fails, display a message to the user.
                  Toast.makeText(LoginActivity.this, "Cannot Sign in. Please check the form and try agian.",
                          Toast.LENGTH_SHORT).show();
              }
          }
      });

    }
}
