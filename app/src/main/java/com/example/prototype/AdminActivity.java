package com.example.prototype;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.example.prototype.deviceMonitor.deviceMonitor;
import com.example.prototype.patient.Patient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import com.squareup.picasso.Picasso;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;


public class AdminActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    //Navigation Sidebar
    DrawerLayout drawerLayout;
    ActionBarDrawerToggle drawerToggle;
    NavigationView navigationView;
    Toolbar toolbar;

    private FirebaseAuth fAuth;
    private FloatingActionButton addFAB;
    private String patientScanID;
    private DatabaseReference databaseReference;
    private FirebaseDatabase db;
    private FirebaseFirestore fStore;

    private TextInputEditText patientIDTV, patientFullNameTV, patientICTV,
            patientBloodPressureTV, patientBodyTemperatureTV, nextOfKinPhoneNumberTV;
    private ImageView patientImage;
    private Patient scannedPatient;
    private Button btnEditPatient, btnViewHistory;

    private final String PATIENT_KEY = "patient";
    private String currentDate;

    Boolean foregroundStatus = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        registerActivityLifecycleCallbacks(this);

        setContentView(R.layout.activity_admin);

        //Navigation Sidebar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.open_drawer, R.string.close_drawer);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.setDrawerIndicatorEnabled(true);
        drawerToggle.syncState();

        navigationView = findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(this);


        fAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();

        patientIDTV = findViewById(R.id.PatientIDTV);
        patientFullNameTV = findViewById(R.id.PatientFullNameTV);
        patientICTV = findViewById(R.id.PatientICTV);
        nextOfKinPhoneNumberTV = findViewById(R.id.NextOfKinPhoneNumberTV);
        patientBloodPressureTV = findViewById(R.id.PatientBloodPressureTV);
        patientImage = findViewById(R.id.patientImg);
        patientBodyTemperatureTV = findViewById(R.id.PatientBodyTemperatureTV);

        addFAB = findViewById(R.id.AddFAB);
        addFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), addActivity.class));
            }
        });

        btnEditPatient = findViewById(R.id.btnEditPatient);
        btnViewHistory = findViewById(R.id.btnViewHistory);

        btnEditPatient.setVisibility(View.INVISIBLE);
        btnEditPatient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), updateActivity.class);
                i.putExtra(PATIENT_KEY, scannedPatient);
                startActivity(i);
            }
        });

        btnViewHistory.setVisibility(View.INVISIBLE);
        btnViewHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                CustomDialog cd = new CustomDialog(AdminActivity.this, scannedPatient);
                cd.show();
                cd.getWindow().setLayout(1100,1200);

            }
        });


        refresh();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Initialize intent result
        IntentResult intentResult = IntentIntegrator.parseActivityResult(
                requestCode, resultCode, data
        );
        //Check condition
        if (intentResult.getContents() != null){
            //When result content is not null, initialize alert dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    AdminActivity.this
            );
            //Set Title
            builder.setTitle("Result");
            //Set Message
            builder.setMessage(intentResult.getContents());
            patientScanID = intentResult.getContents();
            //Set Positive Button
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    retrieveData(patientScanID);
                    dialogInterface.dismiss();
                }
            });
            //show alert dialog
            builder.show();
        }else{
            //When result is null, Display toast
            Toast.makeText(getApplicationContext(), "Sorry, please scan again.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.admin_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.logOut:
                Toast.makeText(getApplicationContext(), "Admin Logged Out", Toast.LENGTH_SHORT).show();
                fAuth.signOut();
                Intent i = new Intent(getApplicationContext(), Login.class);
                startActivity(i);
                finish();
                break;
            case R.id.scan:
//                IntentIntegrator intentIntegrator = new IntentIntegrator(
//                        AdminActivity.this
//                );
//
//                //Set prompt text
//                intentIntegrator.setPrompt("For flash use volume up key");
//                //Set beep
//                intentIntegrator.setBeepEnabled(true);
//                //Locked orientation
//                intentIntegrator.setOrientationLocked(true);
//                //Set Capture Activity
//                intentIntegrator.setCaptureActivity(Capture.class);
//                //Initiate Scan
//                intentIntegrator.initiateScan();
                retrieveData("005");


        }
        return super.onOptionsItemSelected(item);
    }

    //Temporary Bypass
    //retrieveData("005");

    private void retrieveData(String PatientID){
        db = FirebaseDatabase.getInstance();
        databaseReference = db.getReference("Patients").child(PatientID);

        if (databaseReference != null) {
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    scannedPatient = new Patient(snapshot.child("patientID").getValue(String.class), snapshot.child("patientFullName").getValue(String.class),
                            snapshot.child("patientIC").getValue(String.class), snapshot.child("patientBloodPressure").getValue(String.class),
                            snapshot.child("patientBodyTemperatureDevice").getValue(String.class),
                            snapshot.child("nextOfKinPhoneNumber").getValue(String.class));
                    Picasso.get().load(snapshot.child("patientImg").child("imgUrl").getValue(String.class)).resize(400,500).into(patientImage);
                    patientIDTV.setText(scannedPatient.getPatientID());
                    patientFullNameTV.setText(scannedPatient.getPatientFullName());
                    patientICTV.setText(scannedPatient.getPatientIC());
                    nextOfKinPhoneNumberTV.setText(scannedPatient.getNextOfKinPhoneNumber());
                    patientBloodPressureTV.setText(scannedPatient.getPatientBloodPressure());
                    getLiveTemperature(scannedPatient.getPatientBodyTemperatureDevice());
                    secondaryPatientCaller(scannedPatient);
                    displayButton();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getApplicationContext(), "This patient does not exist, " + error.toString(), Toast.LENGTH_SHORT).show();
                }
            });
        }else{
            Toast.makeText(getApplicationContext(), "This patient does not exist.", Toast.LENGTH_SHORT).show();
        }
    }

    public void displayButton(){
        if (scannedPatient != null) {
            btnEditPatient.setVisibility(View.VISIBLE);
            btnViewHistory.setVisibility(View.VISIBLE);
        } else{
            btnEditPatient.setVisibility(View.INVISIBLE);
            btnViewHistory.setVisibility(View.INVISIBLE);
        }
    }

    public void clear(){
        patientIDTV.setText("");
        patientFullNameTV.setText("");
        patientICTV.setText("");
        nextOfKinPhoneNumberTV.setText("");
        patientBloodPressureTV.setText("");
        patientBodyTemperatureTV.setText("");

        String uri = "@drawable/ic_baseline_person_outline_24";
        int imageResource = getResources().getIdentifier(uri, null, getPackageName());
        Drawable res = getResources().getDrawable(imageResource);
        patientImage.setImageDrawable(res);
    }

    private void secondaryPatientCaller(Patient patient){
        this.scannedPatient = patient;
    }

    private void refresh() {
        String patientID = getIntent().getStringExtra("patientID");
        if (patientID != null) {
            retrieveData(patientID);
        }else{
            clear();
        }
    }

    private void getLiveTemperature(String dName){
        currentDate = java.time.LocalDate.now().toString();
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference reference = db.getReference().child("Sensor").child(dName);
        // For Live data
        reference.child(currentDate).limitToLast(1).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot data : snapshot.getChildren()){
                    patientBodyTemperatureTV.setText(data.getValue(Float.class).toString());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_patientDashboard:
                Toast.makeText(getApplicationContext(), "Testing", Toast.LENGTH_SHORT).show();
                break;

            case R.id.nav_deviceList:
                startActivity(new Intent(getApplicationContext(), deviceMonitor.class));
        }
        return false;
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