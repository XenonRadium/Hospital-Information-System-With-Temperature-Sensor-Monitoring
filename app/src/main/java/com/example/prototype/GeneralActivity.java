package com.example.prototype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.app.Activity;
import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.prototype.deviceMonitor.deviceMonitor;
import com.example.prototype.patient.Patient;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.squareup.picasso.Picasso;

import java.util.concurrent.Executor;

public class GeneralActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    //Navigation Sidebar
    DrawerLayout drawerLayout;
    ActionBarDrawerToggle drawerToggle;
    NavigationView navigationView;
    Toolbar toolbar;

    private FirebaseAuth fAuth;
    private String patientScanID;
    private DatabaseReference databaseReference;
    private FirebaseDatabase db;
    private TextInputEditText patientIDTV, patientFullNameTV, patientICTV,
            patientBloodPressureTV, patientBodyTemperatureTV, nextOfKinPhoneNumberTV;
    private ImageView patientImage;
    private Patient scannedPatient;
    private Button btnViewHistory;

    private final String PATIENT_KEY = "patient";
    private String currentDate;

    Boolean foregroundStatus = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        //Maybe have to change elements ID
        patientIDTV = findViewById(R.id.PatientIDTV);
        patientFullNameTV = findViewById(R.id.PatientFullNameTV);
        patientICTV = findViewById(R.id.PatientICTV);
        patientBloodPressureTV = findViewById(R.id.PatientBloodPressureTV);
        patientImage = findViewById(R.id.patientImg);
        patientBodyTemperatureTV = findViewById(R.id.PatientBodyTemperatureTV);
        nextOfKinPhoneNumberTV = findViewById(R.id.NextOfKinPhoneNumberTV);

        btnViewHistory = findViewById(R.id.btnViewHistory);
        btnViewHistory.setVisibility(View.INVISIBLE);
        btnViewHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CustomDialog cd = new CustomDialog(GeneralActivity.this, scannedPatient);
                cd.show();
                cd.getWindow().setLayout(1100, 1200);
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
                    GeneralActivity.this
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
            Toast.makeText(getApplicationContext(), "Sorry, Please scan again", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.general_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }



    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.logOut:
                Toast.makeText(getApplicationContext(), "User Logged Out", Toast.LENGTH_SHORT).show();
                fAuth.signOut();
                Intent i = new Intent(getApplicationContext(), Login.class);
                startActivity(i);
                finish();
                break;
            case R.id.scan:
                //temporary solution

                IntentIntegrator intentIntegrator = new IntentIntegrator(
                        GeneralActivity.this
                );
                //Set prompt text
                intentIntegrator.setPrompt("For flash use volume up key");
                //Set beep
                intentIntegrator.setBeepEnabled(true);
                //Locked orientation
                intentIntegrator.setOrientationLocked(true);
                //Set Capture Activity
                intentIntegrator.setCaptureActivity(Capture.class);
                //Initiate Scan
                intentIntegrator.initiateScan();


        }
//                retrieveData("005");
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_patientDashboard:
                Toast.makeText(getApplicationContext(), "Testing", Toast.LENGTH_SHORT).show();
                break;

            case R.id.nav_deviceList:
                startActivity(new Intent(getApplicationContext(), deviceMonitor.class));
                this.finish();
        }
        return false;
    }

    private void retrieveData(String PatientID){
        db = FirebaseDatabase.getInstance();
        databaseReference = db.getReference("Patients").child(PatientID);

        if(databaseReference != null){
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    scannedPatient = new Patient(snapshot.child("patientID").getValue(String.class), snapshot.child("patientFullName").getValue(String.class),
                            snapshot.child("patientIC").getValue(String.class), snapshot.child("patientBloodPressure").getValue(String.class),
                            snapshot.child("patientBodyTemperatureDevice").getValue(String.class), snapshot.child("nextOfKinPhoneNumber").getValue(String.class));
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
            Toast.makeText(getApplicationContext(), "This patient does not exist, ", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayButton(){
        if (scannedPatient != null){
            btnViewHistory.setVisibility(View.VISIBLE);
        }else {
            btnViewHistory.setVisibility(View.INVISIBLE);
        }
    }

    private void clear(){
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

    private void secondaryPatientCaller(Patient patient) {this.scannedPatient = patient;}

    private void refresh(){
        String patientID = getIntent().getStringExtra("patientID");
        if (patientID != null){
            retrieveData(patientID);
        }else{
            clear();
        }
    }

    private void getLiveTemperature(String dName){
        currentDate = java.time.LocalDate.now().toString();
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference reference = db.getReference().child("Sensor").child(dName);
        //For Live data
        reference.child(currentDate).limitToLast(1).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()){
                    patientBodyTemperatureTV.setText(data.getValue(Float.class).toString());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
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