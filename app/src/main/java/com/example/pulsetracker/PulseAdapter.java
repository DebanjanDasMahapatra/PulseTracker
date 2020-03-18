package com.example.pulsetracker;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PulseAdapter extends RecyclerView.Adapter<PulseAdapter.MyViewHolder> {

    private ArrayList<Integer> pulses;
    private ArrayList<Date> times;
    private Context context;
    private SimpleDateFormat dateFormat, timeFormat;

    PulseAdapter(ArrayList<Integer> pulses, ArrayList<Date> times, Context context) {
        this.pulses = pulses;
        this.times = times;
        this.context = context;
        dateFormat = new SimpleDateFormat("dd-MM-yyyy",Locale.getDefault());
        timeFormat = new SimpleDateFormat("HH:mm:ss",Locale.getDefault());
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        View view = inflater.inflate(R.layout.pulse_record,viewGroup,false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder viewHolder, int i) {
        viewHolder.date.setText(String.format(Locale.getDefault(),"Date: %s\nTime: %s Hrs",dateFormat.format(times.get(i)),timeFormat.format(times.get(i))));
        viewHolder.pulse.setText(String.format(Locale.getDefault(),"%d",pulses.get(i)));
        if(pulses.get(i) > Globals.maxPulse || pulses.get(i) < Globals.minPulse)
            viewHolder.pulse.setTextColor(Color.rgb(255, 0, 0));
        else
            viewHolder.pulse.setTextColor(Color.rgb(0, 156, 21));
        viewHolder.date.setBackgroundColor(Color.WHITE);
        viewHolder.date.setBackgroundColor(Color.rgb(36, 36, 36));
    }

    @Override
    public int getItemCount() {
        return pulses.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {

        TextView pulse, date;
        RelativeLayout mainLayout;

        MyViewHolder(@NonNull View itemView) {
            super(itemView);
            pulse = (TextView) itemView.findViewById(R.id.pulseHolder);
            date = (TextView) itemView.findViewById(R.id.dateHolder);
            mainLayout = (RelativeLayout) itemView.findViewById(R.id.record);
        }
    }
}
