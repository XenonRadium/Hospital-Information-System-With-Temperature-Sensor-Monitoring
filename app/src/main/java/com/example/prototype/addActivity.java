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

import java.util.ArrayList;
import java.util.concurrent.Executor;

public class addActivity extends AppCompatActivity{

    private TextInputEditText patientID, patientFullName, patientIC, patientBloodPressure,
            patientBodyTemperature, nextOfKinPhoneNumber;
    private Button addPatientBtn, btnAddImg, btnAddSensor;
    private ImageView patientAddImage;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;

    private FirebaseFirestore fStore;
    private FirebaseAuth fAuth;
    private Uri ImageUri;

    //Create DialogBox
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;
    private ListView deviceList;
    private Button popupCancel;

    Boolean foregroundStatus = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();

        patientID = findViewById(R.id.edtPatientID);
        patientFullName = findViewById(R.id.edtPatientFullName);
        patientIC = findViewById(R.id.edtPatientIC);
        nextOfKinPhoneNumber = findViewById(R.id.edtNextOfKinPhoneNumber);
        patientBloodPressure = findViewById(R.id.edtPatientBloodPressure);
        patientBodyTemperature = findViewById(R.id.addPatientBodyTemperatureTV);

        addPatientBtn = findViewById(R.id.btnAddCourse);
        btnAddImg = findViewById(R.id.btnAddImg);
        btnAddSensor = findViewById(R.id.btnAddSensorDevice);

        patientAddImage = findViewById(R.id.patientAddImg);

        btnAddImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImg();
            }
        });

        AddPatient ap = new AddPatient();
        addPatientBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Patient pt = new Patient(patientID.getText().toString(), patientFullName.getText().toString()
                , patientIC.getText().toString(), patientBloodPressure.getText().toString(), patientBodyTemperature.getText().toString()
                , nextOfKinPhoneNumber.getText().toString());
                ap.add(pt).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        uploadImg(ImageUri, patientID.getText().toString());
                        Toast.makeText(getApplicationContext(),"New Patient Created", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });


        btnAddSensor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createNewDialog();
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
                    Intent i = new Intent(getApplicationContext(), AdminActivity.class);
                    i.putExtra("patientID", patientID.getText().toString());
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
                        checkUserAccessLevel(fAuth.getCurrentUser().getUid());
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
            patientAddImage.setImageURI(ImageUri);
        }
    }

    //Create Dialog Box Function
    public void createNewDialog(){
        dialogBuilder = new AlertDialog.Builder(this);

        //Dialog Box Element
        final View popup = getLayoutInflater().inflate(R.layout.popup, null);
        deviceList = popup.findViewById(R.id.deviceList);
        popupCancel = popup.findViewById(R.id.popupCancel);

        //insert device names into List
        ArrayList<String> list = new ArrayList<>();
        ArrayAdapter adapter = new ArrayAdapter<String>(this, R.layout.list_item, list);
        deviceList.setAdapter(adapter);

        databaseReference = FirebaseDatabase.getInstance().getReference().child("Sensor");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot datasnapshot) {
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
                patientBodyTemperature.setText(deviceList.getItemAtPosition(i).toString());
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