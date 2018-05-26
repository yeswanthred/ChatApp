package com.example.yeswa.lapitchat;

import android.*;
import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mUserDatabase;
    private Toolbar mToolbar;
    private ImageButton mMainIcon;
    private DatabaseReference mRootRef;

    private ViewPager mViewPager;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private TabLayout mTabLayout;

    private FingerprintManager fingerprintManager;
    private KeyguardManager keyguardManager;
    private KeyStore keyStore;
    private Cipher cipher;
    private String KEY_NAME = "AndroidKey";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(mAuth.getCurrentUser().getUid());
        }

        mRootRef = FirebaseDatabase.getInstance().getReference();

        mToolbar = (Toolbar) findViewById(R.id.main_page_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("");
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowCustomEnabled(true);

        //Inflate Custom Bar on Action bar
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View action_bar_view = inflater.inflate(R.layout.main_custom_bar, null);

        actionBar.setCustomView(action_bar_view);

        mMainIcon = (ImageButton) findViewById(R.id.main_custom_image);

        //Tabs
        mTabLayout = (TabLayout) findViewById(R.id.main_tabs);
        mViewPager = (ViewPager) findViewById(R.id.main_tabpager);
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setCurrentItem(1);
        mTabLayout.setupWithViewPager(mViewPager);

        // CHECK 1: Android version should be greater or equal to Marshmallow
        // CHECK 2: Device has fingerprint scanner
        // CHECK 3: Have permission to user fingerprint scanner in the app
        // CHECK 4: Lock screen should be secured with atleast1 1 type of lock
        // CHECK 5: atleast 1 fingerprint is registered

        mMainIcon.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(final View view) {

                fingerprintManager = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);
                keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && fingerprintManager.isHardwareDetected()){


                    if (!fingerprintManager.isHardwareDetected()){
                        Toast.makeText(MainActivity.this, "Fingerprint Scanner not detected", Toast.LENGTH_SHORT).show();
                    }

                    else if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(MainActivity.this, "Permission not granted to use Fingerprint Scanner", Toast.LENGTH_LONG).show();
                    }

                    else if (!keyguardManager.isKeyguardSecure()){
                        Toast.makeText(MainActivity.this, "Add lock to your phone in Settings", Toast.LENGTH_SHORT).show();
                    }

                    else if (!fingerprintManager.hasEnrolledFingerprints()){
                        Toast.makeText(MainActivity.this, "Need atleast one Fingerprint to use this Feature", Toast.LENGTH_SHORT).show();
                    }

                    else {
                        Toast.makeText(MainActivity.this, "Place your finger on the finger print scanner", Toast.LENGTH_SHORT).show();

                        generateKey();

                        if (cipherInit()) {

                            FingerprintManager.CryptoObject cryptoObject = new FingerprintManager.CryptoObject(cipher);
                            FingerprintHandler fingerprintHandler = new FingerprintHandler(MainActivity.this);
                            fingerprintHandler.startAuth(fingerprintManager, cryptoObject);
                        }
                    }

                }

                else {

                    mUserDatabase.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            if (!dataSnapshot.hasChild("pin")) {

                                AlertDialog.Builder alertBuilderSet = new AlertDialog.Builder(MainActivity.this);
                                View viewPinSet = getLayoutInflater().inflate(R.layout.hide_pin_set_dialog, null);

                                alertBuilderSet.setView(viewPinSet);
                                final AlertDialog dialogSet = alertBuilderSet.create();
                                dialogSet.setCanceledOnTouchOutside(true);
                                dialogSet.show();

                                final EditText pinSet = (EditText) viewPinSet.findViewById(R.id.hide_pin_set1);
                                final EditText pinRetrySet = (EditText) viewPinSet.findViewById(R.id.hide_pin_set2);
                                Button pinSetButton = (Button) viewPinSet.findViewById(R.id.hide_pin_set_button);

                                pinSetButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        String pin = pinSet.getText().toString();
                                        String pinRetry = pinRetrySet.getText().toString();
                                        String current_user_id = mAuth.getCurrentUser().getUid();

                                        if (!pin.isEmpty() && !pinRetry.isEmpty()){
                                            if (pin.equals(pinRetry)){

                                                Map pinMap = new HashMap();
                                                pinMap.put("Users/" + current_user_id + "/pin", pin);

                                                mRootRef.updateChildren(pinMap, new DatabaseReference.CompletionListener() {
                                                    @Override
                                                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                                        if (databaseError != null){
                                                            Toast.makeText(MainActivity.this, "Error While updating pin", Toast.LENGTH_SHORT).show();
                                                        }
                                                        else {
                                                            Toast.makeText(MainActivity.this, "All Good", Toast.LENGTH_SHORT).show();
                                                            dialogSet.dismiss();
                                                        }
                                                    }
                                                });
                                            }
                                        }
                                    }
                                });
                            }

                            else {
                                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
                                View viewPin = getLayoutInflater().inflate(R.layout.hide_pin_dialog, null);

                                alertBuilder.setView(viewPin);
                                final AlertDialog dialog = alertBuilder.create();
                                dialog.setCanceledOnTouchOutside(true);
                                dialog.show();

                                final EditText pin = (EditText) viewPin.findViewById(R.id.hide_pin);
                                Button pinButton = (Button) viewPin.findViewById(R.id.hide_pin_button);

                                pinButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        if (!pin.getText().toString().isEmpty()){
                                            Toast.makeText(MainActivity.this, "Everything filled up", Toast.LENGTH_SHORT).show();
                                        }
                                        else {
                                            Toast.makeText(MainActivity.this, "Fill in the pin", Toast.LENGTH_SHORT).show();
                                            dialog.dismiss();
                                        }
                                    }
                                });
                            }

                            }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

                }
            }
        });

    }
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null){
            sendToStart();
        }
        else {
            mUserDatabase.child("online").setValue("true");
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            mUserDatabase.child("online").setValue(ServerValue.TIMESTAMP);
        }
    }

    private void sendToStart() {
        Intent startIntent = new Intent(MainActivity.this, StartActivity.class);
        startActivity(startIntent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.main_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        if (item.getItemId() == R.id.main_logout_btn){
            FirebaseAuth.getInstance().signOut();
            sendToStart();
        }

        if (item.getItemId() == R.id.main_settings_btn){
            Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(settingsIntent);
        }

        if (item.getItemId() == R.id.main_all_btn){
            Intent usersIntent = new Intent(MainActivity.this, UsersActivity.class);
            startActivity(usersIntent);
        }
        return true;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void generateKey() {

        try {

            keyStore = KeyStore.getInstance("AndroidKeyStore");
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

            keyStore.load(null);
            keyGenerator.init(new
                    KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(
                            KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            keyGenerator.generateKey();

        } catch (KeyStoreException | IOException | CertificateException
                | NoSuchAlgorithmException | InvalidAlgorithmParameterException
                | NoSuchProviderException e) {

            e.printStackTrace();

        }

    }

    @TargetApi(Build.VERSION_CODES.M)
    public boolean cipherInit() {
        try {
            cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get Cipher", e);
        }


        try {

            keyStore.load(null);

            SecretKey key = (SecretKey) keyStore.getKey(KEY_NAME,
                    null);

            cipher.init(Cipher.ENCRYPT_MODE, key);

            return true;

        } catch (KeyPermanentlyInvalidatedException e) {
            return false;
        } catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }

    }

}

