package com.example.prototype.deviceMonitor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.prototype.R;

import java.util.ArrayList;

public class DeviceArrayAdapter extends ArrayAdapter<Device> {

    public DeviceArrayAdapter(@NonNull Context context,ArrayList<Device> deviceArrayList){
        super(context, 0, deviceArrayList);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        //convertView which is recyclable view
        View currentItemView = convertView;

        // if the recyclable view is null then inflate the custom layout for the same
        if (currentItemView == null){
            currentItemView = LayoutInflater.from(getContext()).inflate(R.layout.device_list_item, parent, false);
        }

        Device currentDevicePosition = getItem(position);

        //then according to the position of the view assign the desired Textview 1 and 2 for the same
        TextView textView1 = currentItemView.findViewById(R.id.deviceName);
        textView1.setText(currentDevicePosition.getDeviceID());

        TextView textView2 = currentItemView.findViewById(R.id.deviceTemperature);
        textView2.setText(currentDevicePosition.getTemperature());


        return currentItemView;
    }
}
