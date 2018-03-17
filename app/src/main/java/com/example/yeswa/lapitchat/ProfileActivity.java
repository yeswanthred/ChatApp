package com.example.yeswa.lapitchat;

import android.app.ProgressDialog;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private ImageView mProfileImage;

    private TextView mProfileName;
    private TextView mProfileStatus;
    private TextView mProfileFriendsCount;

    private Button mProfileSendReqBtn;
    private Button mProfileDeclineBtn;

    private int mRequestStatus;

    //Firebase
    private DatabaseReference mUserDatabase;
    private DatabaseReference mFriendRequestDatabase;
    private DatabaseReference mFriendDatabase;
    private FirebaseUser mCurrentUser;
    private DatabaseReference mRootRef;

    //ProgressDailog
    private ProgressDialog mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        final String user_id = getIntent().getStringExtra("user_id");

        mProfileImage = (ImageView) findViewById(R.id.profile_image);
        mProfileName = (TextView) findViewById(R.id.profile_name);
        mProfileStatus = (TextView) findViewById(R.id.profile_status);
        mProfileFriendsCount = (TextView) findViewById(R.id.profile_friends);
        mProfileSendReqBtn = (Button) findViewById(R.id.profile_friend_request_btn);
        mProfileDeclineBtn = (Button) findViewById(R.id.profile_decline_btn);
        mRequestStatus = 0;

        //Firebase
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(user_id);
        mFriendRequestDatabase = FirebaseDatabase.getInstance().getReference().child("Friends_req");
        mFriendDatabase = FirebaseDatabase.getInstance().getReference().child("Friends");
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mRootRef = FirebaseDatabase.getInstance().getReference();


        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                mProgress = new ProgressDialog(ProfileActivity.this);
                mProgress.setTitle("Loading..");
                mProgress.setMessage("Please wait while we load the content");
                mProgress.setCanceledOnTouchOutside(false);
                mProgress.show();

                mProfileDeclineBtn.setVisibility(View.INVISIBLE);
                mProfileDeclineBtn.setEnabled(false);

                String display_name = dataSnapshot.child("name").getValue().toString();
                String status = dataSnapshot.child("status").getValue().toString();
                String image = dataSnapshot.child("image").getValue().toString();

                mProfileName.setText(display_name);
                mProfileStatus.setText(status);

                //picasso
                Picasso.with(ProfileActivity.this).load(image).placeholder(R.drawable.profile_avatar).into(mProfileImage);


                //Friends Request

                mFriendRequestDatabase.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChild(user_id)){

                            String req_type = dataSnapshot.child(user_id).child("request_type").getValue().toString();

                            if (req_type.equals("received")){

                                mProfileSendReqBtn.setText("ACCEPT FRIEND REQUEST");
                                mProfileDeclineBtn.setVisibility(View.VISIBLE);
                                mProfileDeclineBtn.setEnabled(true);
                                mRequestStatus = 2;

                            }
                            else if (req_type.equals("sent")){

                                mProfileSendReqBtn.setText("CANCEL FRIEND REQUEST");
                                mRequestStatus = 1;
                            }

                        }
                        mProgress.dismiss();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mFriendDatabase.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(user_id)){
                    mProfileSendReqBtn.setText("Unfriend");
                    mRequestStatus = 4;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        mProfileSendReqBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mProfileSendReqBtn.setEnabled(false);
                mProgress.setTitle("Loading..");
                mProgress.setMessage("Please wait while we work on your request");
                mProgress.show();

                //Request No Friends state

                if (mRequestStatus == 0){

                    DatabaseReference newNotificationRef = mRootRef.child("Notification").child(user_id).push();
                    String newNotificationId = newNotificationRef.getKey();

                    HashMap<String, String> notificationData = new HashMap<>();
                    notificationData.put("from", mCurrentUser.getUid());
                    notificationData.put("type", "request");

                    Map requestMap = new HashMap();
                    requestMap.put("Friends_req/" + mCurrentUser.getUid() + "/" + user_id + "/request_type", "sent");
                    requestMap.put("Friends_req/" + user_id + "/" + mCurrentUser.getUid() + "/request_type", "received");
                    requestMap.put("Notification/" + user_id + "/" + newNotificationId, notificationData);

                    mRootRef.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            
                            if (databaseError != null) {
                                Toast.makeText(ProfileActivity.this, "There was some error while sending the request", Toast.LENGTH_SHORT).show();
                            }else {
                                mProfileSendReqBtn.setText("CANCEL FRIEND REQUEST");
                                mProfileSendReqBtn.setEnabled(true);
                                mRequestStatus = 1;
                            }
                        }
                    });
                    mProfileSendReqBtn.setEnabled(true);
                }

                //Request Sent State

                if (mRequestStatus == 1){

                    DatabaseReference newNotificationRef = mRootRef.child("Notification").child(user_id);
                    String newNotificationId = newNotificationRef.getKey();

                    Map cancelFriendMap = new HashMap();
                    cancelFriendMap.put("Friends_req/" + mCurrentUser.getUid() + "/" + user_id, null);
                    cancelFriendMap.put("Friends_req/" + user_id + "/" + mCurrentUser.getUid(), null);
                    //unfriendMap.put("Notification/" + user_id, null);

                    mRootRef.updateChildren(cancelFriendMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                            if (databaseError == null){
                                mProfileSendReqBtn.setEnabled(true);
                                mProfileSendReqBtn.setText("SEND FRIEND REQUEST");
                                mRequestStatus = 0;
                            }else {
                                Toast.makeText(ProfileActivity.this, "Request Failed while revoking", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    mProfileSendReqBtn.setEnabled(true);
                }

                // Request Recieved State

                if (mRequestStatus == 2){
                    final String current_date = DateFormat.getDateTimeInstance().format(new Date());

                    Map friendMap = new HashMap();
                    friendMap.put("Friends/" + mCurrentUser.getUid() + "/" + user_id + "/date", current_date);
                    friendMap.put("Friends/" + user_id + "/" + mCurrentUser.getUid() + "/date", current_date);
                    friendMap.put("Friends_req/" + mCurrentUser.getUid() + "/" + user_id + "/request_type", null);
                    friendMap.put("Friends_req/" + user_id + "/" + mCurrentUser.getUid() + "/request_type", null);

                    mRootRef.updateChildren(friendMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                            if (databaseError == null){
                                mRequestStatus = 4;
                                mProfileDeclineBtn.setVisibility(View.INVISIBLE);
                                mProfileDeclineBtn.setEnabled(false);
                                mProfileSendReqBtn.setText("Unfriend");
                            }else {
                                Toast.makeText(ProfileActivity.this, "Request Failed while making friends", Toast.LENGTH_SHORT).show();
                            }
                            mProfileSendReqBtn.setEnabled(true);
                        }
                    });
                }

                else if (mRequestStatus == 4){

                    Map unfriendMap = new HashMap();
                    unfriendMap.put("Friends/" + mCurrentUser.getUid() + "/" + user_id, null);
                    unfriendMap.put("Friends/" + user_id + "/" + mCurrentUser.getUid(), null);

                    mRootRef.updateChildren(unfriendMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                            if (databaseError == null) {
                                mRequestStatus = 0;
                                mProfileSendReqBtn.setText("SEND FRIEND REQUEST");
                            }
                            else {
                                Toast.makeText(ProfileActivity.this, "Request Failed while unfriending", Toast.LENGTH_SHORT).show();
                            }
                            mProfileSendReqBtn.setEnabled(true);
                        }
                    });
                }
                mProgress.dismiss();
            }
        });

        mProfileDeclineBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                mProgress.setTitle("Loading..");
                mProgress.setMessage("Please wait while we work on your request");
                mProgress.show();

                if (mRequestStatus == 2){

                    Map unfriendMap = new HashMap();
                    unfriendMap.put("Friends_req/" + mCurrentUser.getUid() + "/" + user_id, null);
                    unfriendMap.put("Friends_req/" + user_id + "/" + mCurrentUser.getUid(), null);

                    mRootRef.updateChildren(unfriendMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                            if (databaseError == null){
                                mProfileDeclineBtn.setVisibility(View.INVISIBLE);
                                mProfileSendReqBtn.setText("SEND FRIEND REQUEST");
                                mRequestStatus = 0;
                            }
                            else {
                                Toast.makeText(ProfileActivity.this, "Request Failed while Decline Request", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    mProfileDeclineBtn.setEnabled(false);
                    mProfileSendReqBtn.setEnabled(true);
                }
                mProgress.dismiss();
            }
        });
    }

}
