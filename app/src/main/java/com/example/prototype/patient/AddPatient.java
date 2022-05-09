package com.example.prototype.patient;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class AddPatient {

    private DatabaseReference databaseReference;
    private FirebaseDatabase db;

    public AddPatient()
    {
        db = FirebaseDatabase.getInstance();

        databaseReference = db.getReference("Patients");
    }

    public Task<Void> add(Patient pat){
        return databaseReference.child(pat.getPatientID()).setValue(pat);
    }

    public Task<Void> update(Patient pat){
        Map<String, Object> childUpdates = pat.toMap();
        return databaseReference.child(pat.getPatientID()).updateChildren(childUpdates);
    }

    public Task<Void> delete(Patient pat){
        return databaseReference.child(pat.getPatientID()).removeValue();
    }
}
