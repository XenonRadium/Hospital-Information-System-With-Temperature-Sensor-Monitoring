package com.example.prototype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.Executor;

public class Fingerprint extends AppCompatActivity {
    FirebaseAuth fAuth;
    FirebaseFirestore fStore;
    FirebaseUser currentFirebaseUser;

    private int isAdmin = 0;

    private Boolean returnFromBackground = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        Intent i = getIntent();


        setContentView(R.layout.activity_fingerprint);

        TextView msg_text = findViewById(R.id.txt_msg);
        Button fingerprint_btn = findViewById(R.id.fingerprintBtn);

        //Create BiometricManager to check if user can use the fingerprint scanner
        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate()){
            case BiometricManager.BIOMETRIC_SUCCESS:
                msg_text.setText("You can use the fingerprint sensor to login");
                msg_text.setTextColor(Color.parseColor("#000000"));
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                msg_text.setText("The device don't have a fingerprint sensor");
                msg_text.setTextColor(Color.parseColor("#000000"));
                fingerprint_btn.setVisibility(View.GONE);
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                msg_text.setText("The biometric sensors is currently unavailable");
                msg_text.setTextColor(Color.parseColor("#000000"));
                fingerprint_btn.setVisibility(View.GONE);
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                msg_text.setText("Your device don't have any fingerprint enrolled, " +
                        "please register a fingerprint in the settings");
                msg_text.setTextColor(Color.parseColor("#000000"));
                fingerprint_btn.setVisibility(View.GONE);
                break;
        }


        //Executor
        Executor executor = ContextCompat.getMainExecutor(this);

        //Biomtric prompt callback
        BiometricPrompt biometricPrompt = new BiometricPrompt(Fingerprint.this, executor,
                new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                if(!returnFromBackground) {
                    if (fAuth.getCurrentUser() != null) {
                        checkCurrentUserAccessLevel(fAuth.getUid());
                    } else {
                        startActivity(new Intent(getApplicationContext(), Login.class));
                        finish();
                    }
                }else{
                    Fingerprint.super.onBackPressed();
                }

            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
            }
        });

        //Biometric Dialog
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Login")
                .setDescription("Use your fingerprint to login to your app")
                .setNegativeButtonText("Cancel")
                .build();

        fingerprint_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                biometricPrompt.authenticate(promptInfo);
            }
        });
    }

    private void checkCurrentUserAccessLevel(String uid){

        DocumentReference df = fStore.collection("Users").document(uid);
        //extract the data from the document
        df.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                Log.d("TAG", "onSuccess: " + documentSnapshot.getData());

                //Identify User access level
                if(documentSnapshot.getString("isAdmin").equals("1")){
                    startActivity(new Intent(getApplicationContext(), AdminActivity.class));
                    finish();
                }else{
                    startActivity(new Intent(getApplicationContext(), GeneralActivity.class));
                    finish();
                }

            }
        });
    }

    @Override
    public void onBackPressed() {

    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent i = getIntent();
        returnFromBackground = i.getBooleanExtra("returnFromBackground", false);
    }
}