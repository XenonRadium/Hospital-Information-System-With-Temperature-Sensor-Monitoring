package com.example.prototype.patient;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Patient implements Parcelable {

    private String patientID;
    private String patientFullName;
    private String patientIC;
    private String patientBloodPressure;
    private String patientBodyTemperatureDevice;
    private String nextOfKinPhoneNumber;    //Not yet implemented

    public Patient(){}

    //Add parameter for arraylist
    public Patient(String patientID, String patientFullName, String patientIC, String patientBloodPressure,
                   String patientBodyTemperatureDevice, String nextOfKinPhoneNumber){
        this.patientID = patientID;
        this.patientFullName = patientFullName;
        this.patientIC = patientIC;
        this.patientBloodPressure = patientBloodPressure;
        this.patientBodyTemperatureDevice = patientBodyTemperatureDevice;
        this.nextOfKinPhoneNumber = nextOfKinPhoneNumber;
    }

    //Add parameter for arraylist
    protected Patient(Parcel in) {
        patientID = in.readString();
        patientFullName = in.readString();
        patientIC = in.readString();
        patientBloodPressure = in.readString();
        patientBodyTemperatureDevice = in.readString();
        nextOfKinPhoneNumber = in.readString(); //Fishy af
    }

    public static final Creator<Patient> CREATOR = new Creator<Patient>() {
        @Override
        public Patient createFromParcel(Parcel in) {
            return new Patient(in);
        }

        @Override
        public Patient[] newArray(int size) {
            return new Patient[size];
        }
    };

    public String getPatientID() {
        return patientID;
    }

    public void setPatientID(String patientID) {
        this.patientID = patientID;
    }

    public String getPatientFullName() {
        return patientFullName;
    }

    public void setPatientFullName(String patientFullName) {
        this.patientFullName = patientFullName;
    }

    public String getPatientIC() {
        return patientIC;
    }

    public void setPatientIC(String patientIC) {
        this.patientIC = patientIC;
    }

    public String getPatientBloodPressure() {
        return patientBloodPressure;
    }

    public void setPatientBloodPressure(String patientBloodPressure) {
        this.patientBloodPressure = patientBloodPressure;
    }

    public String getPatientBodyTemperatureDevice() {
        return patientBodyTemperatureDevice;
    }

    public void setPatientBodyTemperatureDevice(String patientBodyTemperatureDevice) {
        this.patientBodyTemperatureDevice = patientBodyTemperatureDevice;
    }

    public String getNextOfKinPhoneNumber() {
        return nextOfKinPhoneNumber;
    }

    public void setNextOfKinPhoneNumber(String nextOfKinPhoneNumber) {
        this.nextOfKinPhoneNumber = nextOfKinPhoneNumber;
    }


    //Add parameter for arraylist
    public Map<String, Object> toMap(){
        HashMap<String, Object> result = new HashMap<>();
        result.put("patientID", patientID);
        result.put("patientFullName", patientFullName);
        result.put("patientIC", patientIC);
        result.put("patientBloodPressure", patientBloodPressure);
        result.put("patientBodyTemperatureDevice", patientBodyTemperatureDevice);
        result.put("nextOfKinPhoneNumber", nextOfKinPhoneNumber);
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    //Add parameter for arraylist
    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(patientID);
        parcel.writeString(patientFullName);
        parcel.writeString(patientIC);
        parcel.writeString(patientBloodPressure);
        parcel.writeString(patientBodyTemperatureDevice);
        parcel.writeString(nextOfKinPhoneNumber);
    }
}
