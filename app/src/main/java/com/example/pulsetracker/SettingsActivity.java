package com.example.pulsetracker;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;
import java.util.Objects;

import androidx.annotation.NonNull;

public class SettingsActivity extends AppCompatPreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Settings");
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.web_client_id))
                .requestEmail()
                .build();
        Globals.mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        Globals.mAuth = FirebaseAuth.getInstance();

        // load settings fragment
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MainPreferenceFragment()).commit();
    }

    public static class MainPreferenceFragment extends PreferenceFragment {
        ProgressDialog progressDialog;
        AlertDialog.Builder builder;
        ValueEventListener valueEventListener;
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            progressDialog = new ProgressDialog(getActivity());
            builder = new AlertDialog.Builder(getActivity());
            valueEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot appleSnapshot: dataSnapshot.getChildren()) {
                        appleSnapshot.getRef().removeValue();
                    }
                    progressDialog.hide();
                    FirebaseDatabase.getInstance().getReference().removeEventListener(valueEventListener);
                    Toast.makeText(getActivity(),"All data cleared !!!",Toast.LENGTH_LONG).show();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(getActivity(),"Error Occurred !!! Try clearing later",Toast.LENGTH_LONG).show();
                }
            };

            findPreference(getString(R.string.switch_notifications)).setDefaultValue(false);
            findPreference(getString(R.string.bpm1)).setDefaultValue(String.valueOf(Globals.minPulse));
            findPreference(getString(R.string.bpm2)).setDefaultValue(String.valueOf(Globals.maxPulse));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.switch_notifications)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.bpm1)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.bpm2)));

            Preference myPref = findPreference(getString(R.string.signOut));
            myPref.setTitle(String.format(Locale.getDefault(),"Email: %s",Globals.email));
            myPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    signOut(getActivity());
                    return true;
                }
            });
            Preference myPref2 = findPreference(getString(R.string.clearData));
            myPref2.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    builder.setTitle("Confirm").setMessage("Sure to Delete ALL Data from Database? This action can't be undone")
                            .setPositiveButton("YES, DELETE", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    progressDialog.setMessage("Clearing Data, Please Wait...");
                                    progressDialog.setCancelable(false);
                                    progressDialog.show();
                                    clearData(valueEventListener);
                                }
                            }).setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).setCancelable(false);
                    builder.show();
                    return true;
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    private static void bindPreferenceSummaryToValue(Preference preference) {

        SharedPreferences sharedPreferences = Globals.context.getSharedPreferences("PULSE_TRACKER",MODE_PRIVATE);

        if(preference instanceof SwitchPreference) {
            preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener1);
            sBindPreferenceSummaryToValueListener1.onPreferenceChange(preference,
                    PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getBoolean(preference.getKey(),false));
        }
        else if(preference instanceof EditTextPreference) {
            if(preference.getKey().equals(Globals.context.getResources().getString(R.string.bpm1))) {
                preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener2);
                sBindPreferenceSummaryToValueListener2.onPreferenceChange(preference,
                        PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(),String.valueOf(sharedPreferences.getInt("MIN_PULSE",Globals.minPulse))));
            }
            else if(preference.getKey().equals(Globals.context.getResources().getString(R.string.bpm2))) {
                preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener3);
                sBindPreferenceSummaryToValueListener3.onPreferenceChange(preference,
                        PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(),String.valueOf(sharedPreferences.getInt("MAX_PULSE",Globals.minPulse))));
            }
        }
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener1 = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {

                if((boolean)newValue) {
                    preference.setSummary("Receive Alerts even if App is closed");
                    Globals.context.startService(new Intent(Globals.context, Search.class));
                }
                else {
                    preference.setSummary("Do not Receive Alerts when App is closed");
                    Globals.context.stopService(new Intent(Globals.context, Search.class));
                }
            return true;
        }
    };

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener2 = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            try {
                int min = Integer.parseInt(newValue.toString());
                if (min < Globals.maxPulse) {
                    Globals.minPulse = min;
                    preference.setSummary(String.format(Locale.getDefault(), "%d", Globals.minPulse));
                    SharedPreferences.Editor editor = Globals.context.getSharedPreferences("PULSE_TRACKER",MODE_PRIVATE).edit();
                    editor.putInt("MIN_PULSE",Globals.minPulse);
                    editor.apply();
                    Globals.adapter.notifyDataSetChanged();
                    Globals.refreshCounts();
                }
            } catch (Exception e) {
                Toast.makeText(Globals.context,e.getMessage(),Toast.LENGTH_LONG).show();
            }
            return true;
        }
    };

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener3 = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            try {
                int max = Integer.parseInt(newValue.toString());
                if (max > Globals.minPulse) {
                    Globals.maxPulse = max;
                    preference.setSummary(String.format(Locale.getDefault(), "%d", Globals.maxPulse));
                    SharedPreferences.Editor editor = Globals.context.getSharedPreferences("PULSE_TRACKER",MODE_PRIVATE).edit();
                    editor.putInt("MAX_PULSE",Globals.maxPulse);
                    editor.apply();
                    Globals.adapter.notifyDataSetChanged();
                    Globals.refreshCounts();
                }
            } catch (Exception e) {
                Toast.makeText(Globals.context,e.getMessage(),Toast.LENGTH_LONG).show();
            }
            return true;
        }
    };

    private static void signOut(final Context c) {
        Globals.context.stopService(new Intent(Globals.context, Search.class));
        Globals.mAuth.signOut();
        Globals.mGoogleSignInClient.signOut().addOnCompleteListener(
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        c.startActivity(new Intent(c,SignInActivity.class));
                    }
                });
    }

    private static void clearData(ValueEventListener valueEventListener) {
        FirebaseDatabase.getInstance().getReference().addValueEventListener(valueEventListener);
    }
}

