package com.example.yeswa.lapitchat;

import android.content.Intent;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.lang.reflect.GenericArrayType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private String mChatuser;
    private Toolbar mChatToolbar;
    private TextView mTitleView;
    private TextView mLastSeenView;
    private CircleImageView mProfileImage;
    private String mCurrentUserID;

    //Firebase
    private DatabaseReference mRootRef;
    private FirebaseAuth mAuth;
    private StorageReference mImageStorage;

    //Message Edit Linear Layout
    private ImageButton mChatAddBtn;
    private EditText mChatMessage;
    private ImageButton mChatSendBtn;

    //RecyclerView
    private RecyclerView mMessagesList;
    private SwipeRefreshLayout mRefreshLayout;
    private static final int GALLARY_PICK = 1;

    private final List<Messages> messagesList = new ArrayList<>();
    private LinearLayoutManager mLinearLayout;
    private MessageAdapter mAdapter;

    //Pagination
    private static final int TOTAL_ITEMS_TO_LOAD = 10;
    private int mCurrentPage = 1;
    private int itemPos = 0;
    private String mlastKey = "";
    private String mPrevKey = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //Intent getExtra
        mChatuser = getIntent().getStringExtra("user_id");
        String userName = getIntent().getStringExtra("user_name");

        //Firebase
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        mCurrentUserID = mAuth.getCurrentUser().getUid();
        mImageStorage = FirebaseStorage.getInstance().getReference();

        //ActionBar
        mChatToolbar = (Toolbar) findViewById(R.id.chat_app_bar);
        setSupportActionBar(mChatToolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        //InFlate Custom Bar on Action Bar
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View action_bar_view = inflater.inflate(R.layout.chat_custom_bar, null);

        actionBar.setCustomView(action_bar_view);

        //InFlated Action Bar Content
        mTitleView = (TextView) findViewById(R.id.custom_bar_title);
        mLastSeenView = (TextView) findViewById(R.id.custom_bar_seen);
        mProfileImage = (CircleImageView) findViewById(R.id.custom_bar_image);

        //Message Edit Linear Layout
        mChatAddBtn = (ImageButton) findViewById(R.id.chat_add_btn);
        mChatSendBtn = (ImageButton) findViewById(R.id.chat_send_btn);
        mChatMessage = (EditText) findViewById(R.id.chat_message_text);

        //Recycler View Adapter
        mAdapter = new MessageAdapter(messagesList);

        mMessagesList = (RecyclerView) findViewById(R.id.messages_list);
        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.message_swipe_layout);
        mLinearLayout = new LinearLayoutManager(this);
        mMessagesList.setHasFixedSize(true);
        mMessagesList.setLayoutManager(mLinearLayout);

        mMessagesList.setAdapter(mAdapter);

        LoadMessages();

        //Last Seen, Title, Profile Image
        mTitleView.setText(userName);
        mRootRef.child("Users").child(mChatuser).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                String online = dataSnapshot.child("online").getValue().toString();
                String thumb_image = dataSnapshot.child("thumb_image").getValue().toString();
                if (online.equals("true")){

                    mLastSeenView.setText("online");
                }
                else{

                    GetTimeAgo getTimeAgo = new GetTimeAgo();
                    long lasttime = Long.parseLong(online);
                    String Lastseen = getTimeAgo.getTimeAgo(lasttime, getApplicationContext());
                    mLastSeenView.setText(Lastseen);
                }
                Picasso.with(ChatActivity.this).load(thumb_image).placeholder(R.drawable.default_avatar).into(mProfileImage);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        //Chats
        mRootRef.child("Chats").child(mCurrentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (!dataSnapshot.hasChild(mChatuser)){

                    Map chatAddMap =  new HashMap();
                    chatAddMap.put("seen", false);
                    chatAddMap.put("timestamp", ServerValue.TIMESTAMP);

                    Map chatUserMap = new HashMap();
                    chatUserMap.put("Chats/" + mCurrentUserID + "/" + mChatuser, chatAddMap);
                    chatUserMap.put("Chats/" + mChatuser + "/" + mCurrentUserID, chatAddMap);

                    mRootRef.updateChildren(chatUserMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                            if (databaseError != null){
                                Log.d("CHAT_LOG", databaseError.getMessage().toString());
                            }
                        }
                    });
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        //Send Button Database Update
        mChatSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                sendMessage();
            }
        });

        mChatAddBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(galleryIntent, "Select Image"), GALLARY_PICK);
            }
        });

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                mCurrentPage++;
                itemPos = 0;
                LoadMoreMessages();

            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLARY_PICK && resultCode == RESULT_OK){
            Uri imageUri = data.getData();
            final String current_user_ref = "Messages/" + mCurrentUserID + "/" + mChatuser;
            final String chat_user_ref = "Messages/" + mChatuser + "/" + mCurrentUserID;

            DatabaseReference user_message_push = mRootRef.child("Messages").child(mCurrentUserID).child(mChatuser).push();

            final String push_id = user_message_push.getKey();

            StorageReference filepath = mImageStorage.child("message_images").child(push_id + ".jpg");

            filepath.putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                    if (task.isSuccessful()){

                        String download_uri = task.getResult().getDownloadUrl().toString();

                        Map messageMap = new HashMap();
                        messageMap.put("message", download_uri);
                        messageMap.put("seen", false);
                        messageMap.put("type", "image");
                        messageMap.put("time", ServerValue.TIMESTAMP);
                        messageMap.put("from", mCurrentUserID);

                        Map messageUserMap = new HashMap();
                        messageUserMap.put(current_user_ref + "/" + push_id, messageMap);
                        messageUserMap.put(chat_user_ref + "/" + push_id, messageMap);

                        mChatMessage.setText("");
                        mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                if (databaseError != null) {
                                    Log.d("CHAT_LOG", databaseError.getMessage().toString());
                                }
                            }
                        });
                    }
                }
            });
        }
    }

    private void LoadMoreMessages() {

        DatabaseReference messageRef = mRootRef.child("Messages").child(mCurrentUserID).child(mChatuser);
        Query messageQuery = messageRef.orderByKey().endAt(mlastKey).limitToLast(10);

        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                Messages message = dataSnapshot.getValue(Messages.class);
                String messageKey = dataSnapshot.getKey();

                if (!mPrevKey.equals(messageKey)) {
                    messagesList.add(itemPos++, message);
                }
                else {
                    mPrevKey = mlastKey;
                }

                if (itemPos == 1){
                    mlastKey = messageKey;
                }

                mAdapter.notifyDataSetChanged();

                mLinearLayout.scrollToPositionWithOffset(10,0);
                mRefreshLayout.setRefreshing(false);

                }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    //Functions
    private void LoadMessages() {

        DatabaseReference messageRef = mRootRef.child("Messages").child(mCurrentUserID).child(mChatuser);
        Query messageQuery = messageRef.limitToLast(mCurrentPage * TOTAL_ITEMS_TO_LOAD);

        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Messages message = dataSnapshot.getValue(Messages.class);

                itemPos++;

                if (itemPos == 1){
                    String messageKey = dataSnapshot.getKey();

                    mlastKey = messageKey;
                    mPrevKey = messageKey;
                }

                messagesList.add(message);
                mAdapter.notifyDataSetChanged();

                mMessagesList.scrollToPosition(messagesList.size()-1);
                mRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void sendMessage() {

        String message = mChatMessage.getText().toString();
        if (!TextUtils.isEmpty(message)){

            String current_user_ref = "Messages/" + mCurrentUserID + "/" + mChatuser;
            String chat_user_ref = "Messages/" + mChatuser + "/" + mCurrentUserID;

            DatabaseReference userMessagePush = mRootRef.child("messages").child(mCurrentUserID).child(mChatuser).push();

            String pushId = userMessagePush.getKey();

            Map messageMap = new HashMap();
            messageMap.put("message", message);
            messageMap.put("seen", false);
            messageMap.put("type", "text");
            messageMap.put("time", ServerValue.TIMESTAMP);
            messageMap.put("from", mCurrentUserID);

            Map messageUserMap = new HashMap();
            messageUserMap.put(current_user_ref + "/" + pushId, messageMap);
            messageUserMap.put(chat_user_ref + "/" + pushId, messageMap);

            mChatMessage.setText("");

            mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if (databaseError != null){
                        Log.d("CHAT_LOG", databaseError.getMessage().toString());
                    }
                }
            });
        }
    }

}
