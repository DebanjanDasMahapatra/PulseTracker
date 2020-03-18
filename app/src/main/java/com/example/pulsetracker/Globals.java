package com.example.pulsetracker;

import android.app.NotificationChannel;
import android.content.Context;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Locale;

public class Globals {
    static String email;
    static int maxPulse = 100, minPulse = 60, normalCount = 0, abnormalCount = 0;
    static NotificationChannel channel;
    static final int NOTIFICATION_ID = 100, BACKGROUND_ID = 200;
    static Context context;
    static FirebaseAuth mAuth;
    static PulseAdapter adapter;
    static GoogleSignInClient mGoogleSignInClient;
    static TextView normal, abnormal;
    static ArrayList<Integer> pulses = new ArrayList<>();

    static void refreshCounts() {
        normalCount = 0;
        abnormalCount = 0;
        for (int pulse: pulses) {
            if (pulse < minPulse || pulse > maxPulse)
                abnormalCount++;
            else
                normalCount++;
        }
        normal.setText(String.format(Locale.getDefault(),"%d",normalCount));
        abnormal.setText(String.format(Locale.getDefault(),"%d",abnormalCount));
    }
}
