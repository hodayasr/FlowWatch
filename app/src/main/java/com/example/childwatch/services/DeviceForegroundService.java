package com.example.childwatch.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class DeviceForegroundService extends Service {
    private static final String TAG = "DeviceFGService";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        Intent notificationIntent = new Intent(this, com.example.childwatch.ChildSetupActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notif = new NotificationCompat.Builder(this, "childwatch_chan")
                .setContentTitle("ChildWatch running")
                .setContentText("Collecting location & battery updates")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1337, notif);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        startLocationUpdates();

        // register battery receiver
        registerReceiver(new com.example.childwatch.receivers.BatteryReceiver(), new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    private void createNotificationChannel(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel("childwatch_chan","ChildWatch channel", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    private void startLocationUpdates(){
        LocationRequest req = LocationRequest.create();
        req.setInterval(60_000); // 1 minute
        req.setFastestInterval(30_000);
        req.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location loc : locationResult.getLocations()) {
                    sendLocationToFirebase(loc);
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(req, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.w(TAG, "Location permission missing: " + e.getMessage());
        }
    }

    private void sendLocationToFirebase(Location loc) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference("users").child(uid).child("familyId")
                .get().addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) return;
                    String familyId = snapshot.getValue(String.class);
                    long ts = System.currentTimeMillis();
                    FirebaseDatabase.getInstance().getReference("families")
                            .child(familyId).child("children").child(uid).child("location")
                            .child(String.valueOf(ts))
                            .setValue(new LocModel(loc.getLatitude(), loc.getLongitude(), loc.getAccuracy(), ts));
                });
    }

    public static class LocModel {
        public double lat, lng;
        public float accuracy;
        public long timestamp;
        public LocModel() {}
        public LocModel(double lat, double lng, float accuracy, long ts) { this.lat = lat; this.lng = lng; this.accuracy = accuracy; this.timestamp = ts; }
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
