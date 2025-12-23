package com.example.childwatch.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class BatteryReceiver extends BroadcastReceiver {
    private static final String TAG = "BatteryReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int pct;
        if (level >= 0 && scale > 0) pct = (int)((level / (float)scale) * 100);
        else {
            pct = -1;
        }
        long time = System.currentTimeMillis();

        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference("users").child(uid).child("familyId")
                .get().addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) return;
                    String familyId = snapshot.getValue(String.class);
                    FirebaseDatabase.getInstance().getReference("families")
                            .child(familyId).child("children").child(uid)
                            .child("battery").child(String.valueOf(time)).setValue(pct);
                });

        Log.i(TAG, "Battery " + pct + "% at " + time);
    }
}
