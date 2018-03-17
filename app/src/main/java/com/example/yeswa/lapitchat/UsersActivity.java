package com.example.yeswa.lapitchat;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class UsersActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private RecyclerView mUsersList;

    //Firebase
    private DatabaseReference mUsersDatabase;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        mToolbar = (Toolbar) findViewById(R.id.users_appbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("All Users");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
        mAuth = FirebaseAuth.getInstance();

        mUsersList = (RecyclerView) findViewById(R.id.users_List);
        mUsersList.setHasFixedSize(true);
        mUsersList.setLayoutManager(new LinearLayoutManager(UsersActivity.this));
    }

    @Override
    protected void onStart() {
        super.onStart();


        FirebaseRecyclerAdapter<Users, UsersViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Users, UsersViewHolder>(
                Users.class,
                R.layout.users_single_layout,
                UsersViewHolder.class,
                mUsersDatabase
        ) {
            @Override
            protected void populateViewHolder(UsersViewHolder viewHolder, Users model, int position) {

                viewHolder.setDisplayName(model.getName());
                viewHolder.setDisplayStatus(model.getStatus());
                viewHolder.setDisplayImg(model.getThumb_image(), getApplicationContext());

                final String user_id = getRef(position).getKey();

                viewHolder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        Intent profileIntent = new Intent(UsersActivity.this, ProfileActivity.class);
                        profileIntent.putExtra("user_id", user_id);
                        startActivity(profileIntent);
                    }
                });

            }
        };

        mUsersList.setAdapter(firebaseRecyclerAdapter);

    }


    public static class UsersViewHolder extends RecyclerView.ViewHolder {
        View mView;

        public UsersViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
        }

        public void setDisplayName(String name){

            TextView user_name = mView.findViewById(R.id.user_single_name);
            user_name.setText(name);
        }

        public void setDisplayStatus(String status) {

            TextView user_status = mView.findViewById(R.id.user_single_status);
            user_status.setText(status);
        }


        public void setDisplayImg(String thumb_image, Context ctx) {

            CircleImageView user_img = mView.findViewById(R.id.user_single_image);
            Picasso.with(ctx).load(thumb_image).placeholder(R.drawable.default_avatar).into(user_img);
        }

    }
}
