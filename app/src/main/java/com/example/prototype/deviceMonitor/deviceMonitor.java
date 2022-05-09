package com.example.prototype.deviceMonitor;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.prototype.*;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

public class deviceMonitor extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    //Navigation Sidebar
    DrawerLayout drawerLayout;
    ActionBarDrawerToggle drawerToggle;
    NavigationView navigationView;
    Toolbar toolbar;

    ListView deviceListView;


    String temperature;
    Map<String, String> newTemp = new HashMap<String, String>();

    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference db;

    private String currentDate;

    private boolean existingEmergencyCheck = false;

    Boolean foregroundStatus = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_monitor);

        fStore = FirebaseFirestore.getInstance();

        //Create the instance of the ListView to set the numbersViewAdapter
        deviceListView = findViewById(R.id.monitorDeviceList);



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

        updateList();
        deviceListView.setClickable(true);
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                TextView text = view.findViewById(R.id.deviceName);
                String deviceID = text.getText().toString();
                TempAlertDialog newDialog = new TempAlertDialog(deviceMonitor.this, deviceID, false);
                newDialog.show();
                newDialog.getWindow().setLayout(1100,1200);
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            //Add validation to redirect to admin if authorized, general if not
            case R.id.nav_patientDashboard:
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                checkUserAccessLevel(user.getUid());
                this.finish();
                break;
            case R.id.nav_deviceList:
                Toast.makeText(getApplicationContext(), "Testing", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private void checkUserAccessLevel(String uid){
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

    private void updateList(){
        currentDate = java.time.LocalDate.now().toString();

        //create a arraylist of the type NumbersView
        final ArrayList<Device> deviceList = new ArrayList<Device>();

        //Now create the instance of the DeviceArrayAdapter and pass the context
        //and arraylist created above
        DeviceArrayAdapter deviceArrayAdapter = new DeviceArrayAdapter(this, deviceList);

        //set the numbersViewAdapter for ListView
        deviceListView.setAdapter(deviceArrayAdapter);

        firebaseDatabase = FirebaseDatabase.getInstance();
        db = firebaseDatabase.getReference().child("Sensor");
        db.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //can be removed and modified if wanted to get live history with timestamp
                deviceList.clear();
                for (DataSnapshot datasnapshot : snapshot.getChildren()){
                    String deviceName = datasnapshot.getKey();
                    //add date to reference
                    getLiveTemperature(deviceName, currentDate);
                }
                //create Device object to add into deviceList
                for (Map.Entry<String,String> set: newTemp.entrySet()){
                    Device newDevice = new Device(set.getKey(), set.getValue());
                    deviceList.add(newDevice);
                }
                deviceListView.invalidate();
                deviceArrayAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getLiveTemperature(String deviceName, String currentDate){

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference reference = db.getReference().child("Sensor").child(deviceName);
        reference.child(currentDate).limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot!=null) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        temperature = data.getValue(Float.class).toString();
                        newTemp.put(deviceName, temperature);

                        if(!existingEmergencyCheck) {
                            if (Float.parseFloat(temperature) > 37.5) {
                                raiseAlert(deviceName);
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void raiseAlert(String id){
        String deviceID = id;
        existingEmergencyCheck = true;
        TempAlertDialog newDialog = new TempAlertDialog(deviceMonitor.this, deviceID, true);
        newDialog.show();
        newDialog.getWindow().setLayout(1100,1200);
        newDialog.setDialogResult(new TempAlertDialog.OnMyDialogResult() {
            @Override
            public void finish(String result) {
                existingEmergencyCheck = Boolean.valueOf(result);
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