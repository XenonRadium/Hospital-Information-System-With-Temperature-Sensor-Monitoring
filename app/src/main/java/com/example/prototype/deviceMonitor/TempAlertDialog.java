package com.example.prototype.deviceMonitor;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;

import com.example.prototype.MyPlayService;
import com.example.prototype.R;
import com.example.prototype.TemperatureReading;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class TempAlertDialog extends Dialog{
    //First implementation, to be implemented/improved
    private String DeviceID;
    private Activity a;

    private Button closeBtn;
    private String currentDate;

    private ArrayList<Entry> yValues = new ArrayList<>();
    private ArrayList<String> xAxisTimestamp;

    private LineChart temperatureChart;
    private LineDataSet lineDataSet;
    private ArrayList<ILineDataSet> dataSets = new ArrayList<>();
    private LimitLine upper_limit;
    private Legend legend;
    private XAxis xAxis;

    private SwitchCompat alarmSwitch;
    private boolean switchCheck = false;
    private boolean musicPlaying = false;
    private boolean emergency;  //check if dialog is triggered by spike in temperature
    private Intent serviceIntent;

    OnMyDialogResult mDialogResult;

    public TempAlertDialog(Activity a, String id, Boolean check) {
        super(a);
        this.a = a;
        DeviceID = id;
        emergency = check;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.monitor_linechart_popup);

        serviceIntent = new Intent(a, MyPlayService.class);

        temperatureChart = findViewById(R.id.temperatureChart);
        alarmSwitch = findViewById(R.id.alarmSwitch);

        setDataArraylist();
        setLineChart();
        setLimitLine();

        closeBtn = findViewById(R.id.closeButton);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!emergency) {dismiss();}
                else {closeDialog();}
            }
        });

        alarmSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b)
                {
                    switchCheck = true;
                }else if(!b && musicPlaying == true)
                {
                    musicPlaying = false;
                    stopPlayService();
                    switchCheck = false;
                }
            }
        });

        raiseAlert();
    }

    public void setLineChart(){
        temperatureChart.setNoDataText("No Data");
        temperatureChart.setNoDataTextColor(Color.RED);

        //Set and customize borders
        temperatureChart.setDrawGridBackground(true);
        temperatureChart.setDrawBorders(true);
        temperatureChart.setBorderColor(Color.RED);
        temperatureChart.setBorderWidth(2);

        legend = temperatureChart.getLegend();
        legend.setEnabled(false);

        Description description = new Description();
        description.setText("Patient Temperature Graph");
        description.setTextColor(Color.BLACK);
        description.setTextSize(10);
        temperatureChart.setDescription(description);

    }

    public void setDataArraylist(){
        currentDate = java.time.LocalDate.now().toString();

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference reference = db.getReference().child("Sensor").child(DeviceID);
        reference.child(currentDate).limitToLast(7).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int counter = 0;
                xAxisTimestamp = new ArrayList<>();
                for (DataSnapshot data : snapshot.getChildren()){
                    TemperatureReading newReading = new TemperatureReading(data.getKey(), data.getValue(Float.class));
                    while(yValues.size() >= 7){
                        yValues.remove(0);
                    }
                    yValues.add(new Entry(counter, newReading.getTemperature()));
                    xAxisTimestamp.add(newReading.getTimeStamp());
                    counter++;

                    if(newReading.getTemperature() > 37.5 && alarmSwitch.isChecked() && !musicPlaying && switchCheck)
                    {
                        playAudio();
                        musicPlaying = true;
                    }
                    else if ((newReading.getTemperature() <= 37.5 || !alarmSwitch.isChecked() || !switchCheck) && musicPlaying)
                    {
                        stopPlayService();
                        musicPlaying = false;
                    }
                }
                lineDataSet = new LineDataSet(yValues, "Temperature Reading");
                formatDataValues();
                setXAxisLabel(xAxisTimestamp);
                dataSets.add(lineDataSet);
                fillInChart();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void fillInChart(){
        LineData data = new LineData(dataSets);
        data.setValueFormatter(new MyValueFormatter());

        temperatureChart.setData(data);
        temperatureChart.notifyDataSetChanged();
        temperatureChart.invalidate();
    }

    private void setXAxisLabel(ArrayList<String> xAxisTimestamp1){
        xAxis = temperatureChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return xAxisTimestamp1.get((int)value);
            }
        });
    }

    private void setLimitLine(){
        upper_limit = new LimitLine(37.5f, "danger");
        upper_limit.setLineWidth(4);;
        upper_limit.enableDashedLine(10f, 10f, 0f);
        upper_limit.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        upper_limit.setTextSize(15);

        YAxis leftAxis = temperatureChart.getAxisLeft();
        leftAxis.removeAllLimitLines();
        leftAxis.addLimitLine(upper_limit);
        leftAxis.setAxisMaximum(50f);
        leftAxis.setAxisMinimum(30f);
        leftAxis.enableGridDashedLine(10f, 10f, 0);
        leftAxis.setDrawLimitLinesBehindData(true);

        temperatureChart.getAxisRight().setEnabled(false);
    }

    private void formatDataValues(){
        lineDataSet.setFillAlpha(110);
        lineDataSet.setLineWidth(3f);
        lineDataSet.setColors(Color.BLUE);
        lineDataSet.setValueTextSize(10);
        lineDataSet.setValueTextColor(Color.BLUE);

        //Set Circle
        lineDataSet.setLineWidth(3);
        lineDataSet.setDrawCircles(true);
        lineDataSet.setDrawCircleHole(true);
        lineDataSet.setCircleColor(Color.GRAY);
        lineDataSet.setCircleHoleColor(Color.GREEN);
        lineDataSet.setCircleRadius(10);
        lineDataSet.setCircleHoleRadius(5);
    }


    private class MyValueFormatter extends ValueFormatter{
        @Override
        public String getFormattedValue(float value) {
            return value + "°C";
        }
    }


    @Override
    public void dismiss() {
        super.dismiss();
    }

    private void playAudio(){
        try {
            a.startService(serviceIntent);
        }catch (SecurityException e)
        {
            Toast.makeText(a, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopPlayService(){
        try{
            a.stopService(serviceIntent);
        }catch (SecurityException e)
        {
            Toast.makeText(a, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void raiseAlert(){
        musicPlaying = true;
        alarmSwitch.setChecked(true);
        playAudio();
    }

    private void closeDialog(){
        if(mDialogResult != null){
            mDialogResult.finish("false");
        }
        emergency = false;
        dismiss();
    }

    public void setDialogResult(OnMyDialogResult dialogResult){
        mDialogResult = dialogResult;
    }

    public interface OnMyDialogResult{
        void finish(String result);
    }

}
