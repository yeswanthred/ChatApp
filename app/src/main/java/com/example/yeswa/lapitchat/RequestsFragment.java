package com.example.yeswa.lapitchat;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class RequestsFragment extends Fragment {

    private View mView;

    private RecyclerView mReqList;

    private FirebaseAuth mAuth;
    private DatabaseReference mReqDatabase;
    private DatabaseReference mUserDatabase;

    private String mCurrent_user;


    public RequestsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mView = inflater.inflate(R.layout.fragment_requests, container, false);

        mReqList = (RecyclerView) mView.findViewById(R.id.req_list);

        mAuth = FirebaseAuth.getInstance();
        mCurrent_user = mAuth.getCurrentUser().getUid();

        mReqDatabase = FirebaseDatabase.getInstance().getReference().child("Friends_req").child(mCurrent_user);
        //mReqDatabase.keepSynced(true);
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
        //mUserDatabase.keepSynced(true);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);

        mReqList.setHasFixedSize(true);
        mReqList.setLayoutManager(linearLayoutManager);

        // Inflate the layout for this fragment
        return mView;
    }

    @Override
    public void onStart() {
        super.onStart();

        Query requestQuery = mReqDatabase.orderByKey();

        final FirebaseRecyclerAdapter<Requests, ReqViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Requests, ReqViewHolder>(
                Requests.class,
                R.layout.users_single_layout,
                ReqViewHolder.class,
                requestQuery
        ) {
            @Override
            protected void populateViewHolder(final ReqViewHolder viewHolder, final Requests model, int position) {

                final String list_user_id = getRef(position).getKey();

                mReqDatabase.child(list_user_id).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        if (dataSnapshot.hasChild("request_type")) {

                            if (dataSnapshot.child("request_type").getValue().toString().equals("received")) {

                                String request = "Request Pending";
                                viewHolder.setStatus(request);

                            } else {
                                String request = "Request Awaiting";
                                viewHolder.setStatus(request);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                mUserDatabase.child(list_user_id).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        String userName = dataSnapshot.child("name").getValue().toString();
                        String userImage = dataSnapshot.child("thumb_image").getValue().toString();

                        if (dataSnapshot.hasChild("online")){

                            String userOnline = dataSnapshot.child("online").getValue().toString();
                            viewHolder.setUserOnline(userOnline);
                        }

                        viewHolder.setName(userName);
                        viewHolder.setUserImage(userImage, getContext());

                        viewHolder.mView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {

                                Intent profileIntent = new Intent(getContext(), ProfileActivity.class);
                                profileIntent.putExtra("user_id", list_user_id);
                                startActivity(profileIntent);
                            }
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }
        };
        mReqList.setAdapter(firebaseRecyclerAdapter);
    }

    public static class ReqViewHolder extends RecyclerView.ViewHolder {

        View mView;

        public ReqViewHolder(View itemView) {
            super(itemView);

            mView = itemView;
        }

        public void setStatus(String status) {

            TextView requestStatus = (TextView) mView.findViewById(R.id.user_single_status);
            requestStatus.setText(status);
        }

        public void setUserOnline(String userOnline) {
            ImageView userOnlineView = (ImageView) mView.findViewById(R.id.user_single_online_icon);

            if (userOnline.equals("online")){
                userOnlineView.setVisibility(View.VISIBLE);
            }
            else {
                userOnlineView.setVisibility(View.INVISIBLE);
            }
        }

        public void setName(String name) {

            TextView userNameView = (TextView) mView.findViewById(R.id.user_single_name);
            userNameView.setText(name);
        }

        public void setUserImage(String userImage, Context context) {

            CircleImageView userImageView = (CircleImageView) mView.findViewById(R.id.user_single_image);
            Picasso.with(context).load(userImage).placeholder(R.drawable.default_avatar).into(userImageView);
        }
    }
}
