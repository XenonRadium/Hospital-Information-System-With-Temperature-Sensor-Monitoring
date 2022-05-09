package com.example.prototype;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import androidx.annotation.NonNull;

import com.example.prototype.patient.Patient;
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
import com.google.protobuf.Value;

import java.util.ArrayList;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class CustomDialog extends Dialog implements View.OnClickListener {


    public Dialog d;
    public Button closeBtn;

    public Patient scannedPatient;

    private String currentDate;

    private ArrayList<Entry> yValues = new ArrayList<>();
    private ArrayList<String> xAxisTimestamp;

    public LineChart temperatureChart;
    private LineDataSet lineDataSet;
    private ArrayList<ILineDataSet> dataSets = new ArrayList<>();
    private LimitLine upper_limit;
    private Legend legend;
    private XAxis xAxis;



    public CustomDialog(Activity a, Patient pt) {
        super(a);
        this.scannedPatient = pt;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.linechart_popup);
        closeBtn = findViewById(R.id.closeButton);
        closeBtn.setOnClickListener(this);
        temperatureChart = findViewById(R.id.temperatureChart);

        setDataArraylist();
        setLineChart();
        setLimitLine();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.closeButton:
                dismiss();
            default:
                break;
        }
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
        DatabaseReference reference = db.getReference().child("Sensor").child(scannedPatient.getPatientBodyTemperatureDevice());
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
        leftAxis.setAxisMaximum(40f);
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


}
