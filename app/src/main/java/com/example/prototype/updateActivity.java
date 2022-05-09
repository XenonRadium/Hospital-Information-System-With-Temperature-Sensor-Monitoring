package com.example.prototype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.prototype.patient.AddPatient;
import com.example.prototype.patient.Patient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.concurrent.Executor;

public class updateActivity extends AppCompatActivity{

    private TextInputEditText patientIDUpdate, patientFullNameUpdate, patientICUpdate, patientBloodPressureUpdate,
            patientBodyTemperatureUpdate, nextOfKinPhoneNumberUpdate;
    private Button confirmUpdate, btnAddImgUpdate, confirmDelete, changeSensor;
    private ImageView patientAddImageUpdate;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;

    private FirebaseFirestore fStore;
    private FirebaseAuth fAuth;
    private Uri ImageUri;

    private Patient scannedPatient;
    private final String PATIENT_KEY = "patient";

    //Create Alert Dialog
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;
    private ListView deviceList;
    private Button popupCancel;

    private ArrayAdapter<String> adapter;
    ArrayList<String> list = new ArrayList<>();

    Boolean foregroundStatus = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();

        patientIDUpdate = findViewById(R.id.edtPatientIDUpdate);
        patientFullNameUpdate = findViewById(R.id.edtPatientFullNameUpdate);
        patientICUpdate = findViewById(R.id.edtPatientICUpdate);
        patientBloodPressureUpdate = findViewById(R.id.edtPatientBloodPressureUpdate);
        patientBodyTemperatureUpdate = findViewById(R.id.edtPatientBodyTemperatureTV);
        nextOfKinPhoneNumberUpdate = findViewById(R.id.edtNextOfKinPhoneNumber);

        confirmUpdate = findViewById(R.id.btnConfirmUpdate);
        btnAddImgUpdate = findViewById(R.id.btnAddImgUpdate);
        changeSensor = findViewById(R.id.btnEdtSensorDevice);

        patientAddImageUpdate = findViewById(R.id.patientAddImgUpdate);

        btnAddImgUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImg();
            }
        });

        AddPatient ap = new AddPatient();
        confirmUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Patient pt = new Patient(patientIDUpdate.getText().toString(), patientFullNameUpdate.getText().toString()
                        , patientICUpdate.getText().toString(), patientBloodPressureUpdate.getText().toString(),
                        patientBodyTemperatureUpdate.getText().toString(), nextOfKinPhoneNumberUpdate.getText().toString());
                ap.update(pt).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        if(ImageUri != null) {
                            uploadImg(ImageUri, patientIDUpdate.getText().toString());
                            Toast.makeText(getApplicationContext(),"Patient Details updated", Toast.LENGTH_SHORT).show();
                        } else{
                            checkUserAccessLevelUpdate(fAuth.getCurrentUser().getUid());
                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                });


            }
        });

        changeSensor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createNewDialog();
            }
        });

        scannedPatient = getIntent().getParcelableExtra(PATIENT_KEY);
        FillInViews();

        confirmDelete = findViewById(R.id.btnConfirmDelete);
        confirmDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Patient pt = new Patient(patientIDUpdate.getText().toString(), patientFullNameUpdate.getText().toString()
                        , patientICUpdate.getText().toString(), patientBloodPressureUpdate.getText().toString()
                        , patientBodyTemperatureUpdate.getText().toString(), nextOfKinPhoneNumberUpdate.getText().toString());
                ap.delete(pt).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(getApplicationContext(),"Patient Deleted", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                });
                checkUserAccessLevel(fAuth.getCurrentUser().getUid());
            }
        });
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

    private void checkUserAccessLevelUpdate(String uid){
        DocumentReference df = fStore.collection("Users").document(uid);
        //extract the data from the document
        df.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                Log.d("TAG", "onSuccess: " + documentSnapshot.getData());

                //Identify User access level
                if(documentSnapshot.getString("isAdmin").equals("1")){
                    Intent i = new Intent(getApplicationContext(), AdminActivity.class);
                    i.putExtra("patientID", scannedPatient.getPatientID());
                    startActivity(i);
                    finish();
                }else{
                    startActivity(new Intent(getApplicationContext(), GeneralActivity.class));
                    finish();
                }

            }
        });
    }

    private void selectImg(){
        Intent intent =  new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, 100);
    }

    private void uploadImg(Uri uri, String child){
        StorageReference fileRef = storageReference.child(System.currentTimeMillis() + "." + getFileExtension(uri));
        fileRef.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                fileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        imgModel model = new imgModel(uri.toString());
                        firebaseDatabase.getReference("Patients").child(child).child("patientImg").setValue(model);
                        Toast.makeText(getApplicationContext(), "Uploaded Successfully", Toast.LENGTH_SHORT).show();
                        checkUserAccessLevelUpdate(fAuth.getCurrentUser().getUid());
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), "Uploading Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getFileExtension(Uri mUri){
        ContentResolver cr = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cr.getType(mUri));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 100 && data != null){
            ImageUri = data.getData();
            patientAddImageUpdate.setImageURI(ImageUri);


        }
    }

    private void FillInViews (){
        firebaseDatabase.getReference("Patients").child(scannedPatient.getPatientID()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                Picasso.get().load(snapshot.child("patientImg").child("imgUrl").getValue(String.class)).resize(400,500).into(patientAddImageUpdate);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        patientIDUpdate.setText(scannedPatient.getPatientID());
        patientFullNameUpdate.setText(scannedPatient.getPatientFullName());
        patientICUpdate.setText(scannedPatient.getPatientIC());
        patientBloodPressureUpdate.setText(scannedPatient.getPatientBloodPressure());
        patientBodyTemperatureUpdate.setText(scannedPatient.getPatientBodyTemperatureDevice());
        nextOfKinPhoneNumberUpdate.setText(scannedPatient.getNextOfKinPhoneNumber());
    }


    private void createNewDialog(){
        dialogBuilder = new AlertDialog.Builder(this);

        //Dialog Box Element
        final View popup = getLayoutInflater().inflate(R.layout.popup, null);
        deviceList = popup.findViewById(R.id.deviceList);
        popupCancel = popup.findViewById(R.id.popupCancel);

        //insert device names into List

        adapter = new ArrayAdapter<>(this, R.layout.list_item, list);
        deviceList.setAdapter(adapter);

        databaseReference = FirebaseDatabase.getInstance().getReference().child("Sensor");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot datasnapshot) {
                adapter.clear();
                list.clear();
                for (DataSnapshot snapshot : datasnapshot.getChildren()){
                    String deviceName = snapshot.getKey();
                    list.add(deviceName);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });



        dialogBuilder.setView(popup);
        dialogBuilder.setTitle("Available Devices");
        dialog = dialogBuilder.create();
        dialog.show();
        dialog.getWindow().setLayout(1000,1200);

        deviceList.setClickable(true);
        deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                patientBodyTemperatureUpdate.setText(deviceList.getItemAtPosition(i).toString());
                dialog.dismiss();
            }
        });

        popupCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
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