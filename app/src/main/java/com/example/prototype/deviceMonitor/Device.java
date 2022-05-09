package com.example.prototype.deviceMonitor;

public class Device {

    private String deviceID;

    private String temperature;

    //Create constructor to set the value for all the parameters of the each single view
    public Device(String deviceID, String temperature){
        this.setDeviceID(deviceID);
        this.setTemperature(temperature);
    }


    public String getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }
}
