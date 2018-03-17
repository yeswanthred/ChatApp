package com.example.yeswa.lapitchat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class StatusActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private TextInputLayout mStatus;
    private Button mSavebtn;

    //Firebase
    private DatabaseReference mStatusDatabase;
    private FirebaseUser mCurrentUser;

    //ProgressBar
    private ProgressDialog mProgress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        mToolbar = (Toolbar) findViewById(R.id.status_appbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Account Status");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mStatus = (TextInputLayout) findViewById(R.id.status_input);
        mSavebtn = (Button) findViewById(R.id.status_btn);

        String status_value = getIntent().getStringExtra("status_value");
        mStatus.getEditText().setText(status_value);

        //Firebase
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        String current_uid = mCurrentUser.getUid();
        mStatusDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(current_uid);

        mSavebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mProgress = new ProgressDialog(StatusActivity.this);
                mProgress.setTitle("Saving Changes");
                mProgress.setMessage("Please wait while we save the changes");
                mProgress.show();

                String status = mStatus.getEditText().getText().toString();
                mStatusDatabase.child("status").setValue(status).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){

                            mProgress.dismiss();
                            Intent settings_intent = new Intent(StatusActivity.this, SettingsActivity.class);
                            startActivity(settings_intent);
                            finish();
                        }
                        else {

                            mProgress.hide();
                            Toast.makeText(StatusActivity.this, "There was some error in saving the changes", Toast.LENGTH_LONG).show();

                        }
                    }
                });


            }
        });
    }
}
