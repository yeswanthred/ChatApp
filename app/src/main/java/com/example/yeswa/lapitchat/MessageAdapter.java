package com.example.yeswa.lapitchat;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by yeswa on 12-03-2018.
 */

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder>{

    private List<Messages> mMessageList;
    private FirebaseAuth mAuth;
    private DatabaseReference mUserDatabase;


    public MessageAdapter(List<Messages> mMessageList){
        this.mMessageList = mMessageList;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_single_layout, parent, false);
        return new MessageViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final MessageViewHolder holder, int position) {

        mAuth = FirebaseAuth.getInstance();
        String current_user_id = mAuth.getCurrentUser().getUid();

        Messages c = mMessageList.get(position);
        String from_user = c.getFrom();
        String message_type = c.getType();


        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(from_user);

        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                String image = dataSnapshot.child("thumb_image").getValue().toString();

                Picasso.with(holder.profileImage.getContext()).load(image).placeholder(R.drawable.default_avatar).into(holder.profileImage);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        holder.messageImage.setVisibility(View.VISIBLE);
        holder.messageText.setVisibility(View.VISIBLE);
        holder.messageText.setGravity(Gravity.START);


        if (message_type.equals("text")){

            holder.messageText.setBackgroundResource(R.drawable.message_text_backgroup);
            GradientDrawable drawable = (GradientDrawable) holder.messageText.getBackground();
            holder.messageText.setText(c.getMessage());
            holder.messageImage.setVisibility(View.GONE);

            if (from_user.equals(current_user_id)){

                holder.profileImage.setVisibility(View.INVISIBLE);
                holder.layout.setGravity(Gravity.END);
                drawable.setColor(Color.WHITE);

//                holder.messageText.setBackgroundColor(Color.WHITE);
                holder.messageText.setTextColor(Color.BLACK);
            }

            else {
                drawable.setColor(Color.parseColor("#355C7D"));
                holder.messageText.setTextColor(Color.WHITE);
            }
        }

        else if (message_type.equals("image")){

            Picasso.with(holder.profileImage.getContext()).load(c.getMessage()).placeholder(R.drawable.default_avatar).into(holder.messageImage);
            holder.messageText.setVisibility(View.GONE);

            if (from_user.equals(current_user_id)){

                holder.profileImage.setVisibility(View.INVISIBLE);
                holder.layout.setGravity(Gravity.END);
            }

        }

    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }


    public class MessageViewHolder extends RecyclerView.ViewHolder{

        public TextView messageText;
        public CircleImageView profileImage;
        public RelativeLayout layout;
        public ImageView messageImage;

        public MessageViewHolder(View itemView) {
            super(itemView);

            messageText = (TextView) itemView.findViewById(R.id.message_text_layout);
            profileImage = (CircleImageView) itemView.findViewById(R.id.message_profile_layout);
            layout = (RelativeLayout)itemView.findViewById(R.id.message_single_layout);
            messageImage = (ImageView) itemView.findViewById(R.id.message_image_layout);

        }
    }
}
