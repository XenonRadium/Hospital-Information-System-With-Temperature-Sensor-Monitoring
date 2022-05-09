package com.example.prototype;

public class TemperatureReading {
    private String TimeStamp;
    private float Temperature;

    public TemperatureReading(String timeStamp, float temperature) {
        TimeStamp = timeStamp;
        Temperature = temperature;
    }

    public String getTimeStamp() {
        return TimeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        TimeStamp = timeStamp;
    }

    public float getTemperature() {
        return Temperature;
    }

    public void setTemperature(float temperature) {
        Temperature = temperature;
    }
}
