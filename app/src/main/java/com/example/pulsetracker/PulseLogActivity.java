package com.example.pulsetracker;

import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class PulseLogActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private ArrayList<Date> dates = new ArrayList<>();
    private ProgressDialog progressDialog;
    private NotificationCompat.Builder mBuilder;
    private NotificationManager manager;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pulse_log);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Pulse Log");
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        initiateViews();
        progressDialog.setMessage("Loading Pulse Data...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        mBuilder = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            mBuilder = new NotificationCompat.Builder(PulseLogActivity.this,Globals.channel.getId())
                    .setSmallIcon(R.drawable.icon_warning)
                    .setContentTitle("ALERT")
                    .setContentText("Abnormal Pulse BPM detected!!!")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        }
        mDatabase.child(Globals.email.split("@")[0]).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Globals.pulses.add(dataSnapshot.child("pulse").getValue(Integer.class));
                if(Globals.pulses.get(Globals.pulses.size()-1) > Globals.maxPulse || Globals.pulses.get(Globals.pulses.size()-1) < Globals.minPulse) {
                    if (manager != null && mBuilder != null)
                        manager.notify(Globals.NOTIFICATION_ID, mBuilder.build());
                }
                try {
                    dates.add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(dataSnapshot.child("timestamp").getValue(String.class)));
                } catch (ParseException e) {
                    dates.add(new Date());
                }
                Globals.adapter.notifyDataSetChanged();
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                int pos = Globals.pulses.indexOf(dataSnapshot.child("pulse").getValue(Integer.class));
                try {
                    dates.remove(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(dataSnapshot.child("timestamp").getValue(String.class)));
                    Globals.pulses.remove(pos);
                    Globals.adapter.notifyDataSetChanged();
                } catch (ParseException e) {
                    Toast.makeText(PulseLogActivity.this,"Sync Error !!! Close the App and Open Again",Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        mDatabase.child(Globals.email.split("@")[0]).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                progressDialog.hide();
                Globals.refreshCounts();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void initiateViews() {
        Globals.context = PulseLogActivity.this;
        Globals.normal = ((TextView) findViewById(R.id.normal));
        Globals.abnormal = ((TextView) findViewById(R.id.abnormal));
        progressDialog = new ProgressDialog(PulseLogActivity.this);
        recyclerView = findViewById(R.id.pulseLogNew);
        recyclerView.setLayoutManager(new LinearLayoutManager(PulseLogActivity.this));
        mDatabase = FirebaseDatabase.getInstance().getReference();

        SharedPreferences preferences = getSharedPreferences("PULSE_TRACKER",MODE_PRIVATE);
        Globals.minPulse = preferences.getInt("MIN_PULSE",60);
        Globals.maxPulse = preferences.getInt("MAX_PULSE",100);

        manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Globals.adapter = new PulseAdapter(Globals.pulses,dates, PulseLogActivity.this);
        recyclerView.setAdapter(Globals.adapter);
    }

    boolean isUserClickedBackButton = false;
    @Override
    public void onBackPressed() {
        if (!isUserClickedBackButton) {
            Toast.makeText(PulseLogActivity.this, "Press Back Again to Exit", Toast.LENGTH_LONG).show();
            isUserClickedBackButton = true;
        } else
            finishAffinity();
        PulseLogActivity.class.getDeclaredMethods();
        new CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                isUserClickedBackButton = false;
            }
        }.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.setings:
                startActivity(new Intent(PulseLogActivity.this,SettingsActivity.class));
                break;
        }
        return true;
    }
}
