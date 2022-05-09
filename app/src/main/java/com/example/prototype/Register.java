package com.example.prototype;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

public class Register extends AppCompatActivity{
    private EditText fullName,email,password,staffID, phoneNumber;
    private Button registerBtn,goToLogin;
    boolean valid = true;
    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;
    private FirebaseDatabase db;
    private DatabaseReference reference;
    private CheckBox isStaffBox, isUserBox;

    private boolean idCheck = false;
    private boolean phoneNumberCheck = false;
    private ArrayList<String> phoneNumberArrayList = new ArrayList<>();

    private int activityReferences = 0;
    private boolean isActivityChangingConfigurations = false;
    private boolean backFromBackground = false;
    Boolean foregroundStatus = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        db = FirebaseDatabase.getInstance();

        fullName = findViewById(R.id.registerName);
        email = findViewById(R.id.registerEmail);
        password = findViewById(R.id.registerPassword);
        staffID = findViewById(R.id.staffID);
        phoneNumber = findViewById(R.id.phoneNumber);

        registerBtn = findViewById(R.id.registerBtn);
        goToLogin = findViewById(R.id.gotoLogin);

        isStaffBox = findViewById(R.id.isStaff);
        isUserBox = findViewById(R.id.isUser);


        isUserBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(compoundButton.isChecked()){
                    isStaffBox.setChecked(false);
                    staffID.setVisibility(View.GONE);
                    phoneNumber.setVisibility(View.VISIBLE);
                    staffID.setText("");

                }
            }
        });

        isStaffBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(compoundButton.isChecked()){
                    isUserBox.setChecked(false);
                    staffID.setVisibility(View.VISIBLE);
                    phoneNumber.setVisibility(View.GONE);
                    phoneNumber.setText("");
                }
            }
        });


        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkField(fullName);
                checkField(email);
                checkField(password);


                if(!(isStaffBox.isChecked() || isUserBox.isChecked())){
                    Toast.makeText(Register.this, "Select the account type", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(isStaffBox.isChecked()){
                    checkField(staffID);
                }

                if(isUserBox.isChecked()){
                    checkField(phoneNumber);
                }

                if(valid){
                    if (isUserBox.isChecked()){
                        checkPhoneNumber(phoneNumber.getText().toString());
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            public void run() {
                                if(phoneNumberCheck) {
                                    fAuth.createUserWithEmailAndPassword(email.getText().toString(), password.getText().toString())
                                            .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                                                @Override
                                                public void onSuccess(AuthResult authResult) {
                                                    FirebaseUser user = fAuth.getCurrentUser();
                                                    Toast.makeText(Register.this, "Account Created", Toast.LENGTH_SHORT).show();
                                                    DocumentReference df = fStore.collection("Users").document(user.getUid());

                                                    Map<String, Object> userInfo = new HashMap<>();
                                                    userInfo.put("FullName", fullName.getText().toString());
                                                    userInfo.put("UserEmail", email.getText().toString());
                                                    userInfo.put("staffID", staffID.getText().toString());
                                                    userInfo.put("NextOfKinPhoneNumber", phoneNumber.getText().toString());
                                                    userInfo.put("isAdmin", "0");
                                                    startActivity(new Intent(getApplicationContext(), GeneralActivity.class));


                                                    df.set(userInfo);
                                                    finish();
                                                    //can add onSuccess or onFailure listener here

                                                    //checkUserAccessLevel(authResult.getUser().getUid());

                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(Register.this, "Failed to Create Account", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }else{
                                    Toast.makeText(getApplicationContext(), "There is no patient with this next of kin phone number.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        }, 1000);

                    }else if (isStaffBox.isChecked()) {
                        checkStaffID(staffID.getText().toString());
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            public void run() {
                                if (idCheck) {
                                    fAuth.createUserWithEmailAndPassword(email.getText().toString(), password.getText().toString())
                                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                                @Override
                                                public void onComplete(@NonNull Task<AuthResult> task) {
                                                    FirebaseUser user = fAuth.getCurrentUser();
                                                    Toast.makeText(Register.this, "Account Created", Toast.LENGTH_SHORT).show();
                                                    DocumentReference df = fStore.collection("Users").document(user.getUid());

                                                    Map<String, Object> userInfo = new HashMap<>();
                                                    userInfo.put("FullName", fullName.getText().toString());
                                                    userInfo.put("UserEmail", email.getText().toString());
                                                    userInfo.put("staffID", staffID.getText().toString());
                                                    userInfo.put("NextOfKinPhoneNumber", phoneNumber.getText().toString());
                                                    userInfo.put("isAdmin", "1");
                                                    df.set(userInfo);
                                                    startActivity(new Intent(getApplicationContext(), AdminActivity.class));
                                                    finish();
                                                    //can add onSuccess or onFailure listener here

                                                    //checkUserAccessLevel(authResult.getUser().getUid());
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(Register.this, "Failed to Create Account", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }else{
                                    Toast.makeText(getApplicationContext(), "Invalid staff ID", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }, 1000);
                    }
                }
            }
        });

        goToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), Login.class));
            }
        });


    }


    private void checkStaffID(String id){
        fStore.collection("StaffID")
                .whereEqualTo("access", true)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()){
                            for (QueryDocumentSnapshot document: task.getResult()) {
                                Log.d("TAG", document.getId() + " ==> " + document.getString("staffID"));
                                String testID = document.getString("staffID");
                                if (testID.equals(id)) {
                                    //Log.d("TAG", document.getId() + " ==> " + document.getString("staffID"));
                                    idCheck = true;
                                    break;
                                }
                            }
                        }else {
                            Log.d("Tag", "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    private void checkPhoneNumber(String phoneNumber){
        reference = db.getReference("Patients");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String n;
                for (DataSnapshot data : snapshot.getChildren())
                {
                    n = data.child("nextOfKinPhoneNumber").getValue(String.class);
                    if (n != null) {
                        phoneNumberArrayList.add(n);
                    }
                }
                if (phoneNumberArrayList.contains(phoneNumber)){
                    phoneNumberCheck = true;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    public boolean checkField(EditText textField){
        if(textField.getText().toString().isEmpty()){
            textField.setError("Error");
            valid = false;
        }else {
            valid = true;
        }

        return valid;
    }

    private boolean secondaryGetForegroundStatus(){
        Boolean foreground  = ((App)this.getApplication()).getForegroundStatus();
        return foreground;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                foregroundStatus = secondaryGetForegroundStatus();
            }}, 1000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!foregroundStatus){
            Intent i = new Intent(getApplicationContext(), Fingerprint.class);
            i.putExtra("returnFromBackground", true);
            startActivity(i);
        }
    }
}