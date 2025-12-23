package com.example.childwatch.services;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;

public class MyNotificationListener extends NotificationListenerService {
    private static final String TAG = "MyNotificationListener";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String pkg = sbn.getPackageName();
        CharSequence title = sbn.getNotification().extras.getCharSequence("android.title");
        CharSequence text = sbn.getNotification().extras.getCharSequence("android.text");
        long time = sbn.getPostTime();

        Log.i(TAG, "Notif from " + pkg + ": " + title + " / " + text);

        try {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
                userRef.child("familyId").get().addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) return;
                    String familyId = snapshot.getValue(String.class);
                    DatabaseReference ref = FirebaseDatabase.getInstance()
                            .getReference("families").child(familyId).child("children").child(uid).child("notifications").push();
                    NotificationModel nm = new NotificationModel(pkg,
                            title != null ? title.toString() : "",
                            text != null ? text.toString() : "",
                            time);
                    ref.setValue(nm);
                });
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed push: " + e.getMessage());
        }
    }

    public static class NotificationModel {
        public String packageName;
        public String title;
        public String text;
        public long timestamp;
        public NotificationModel() {}
        public NotificationModel(String p, String t, String txt, long ts) { packageName = p; title = t; text = txt; timestamp = ts; }
    }
}
