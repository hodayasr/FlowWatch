package com.example.childwatch;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ChildSetupActivity extends AppCompatActivity {
    EditText etChildName, etFamily;
    Button btnJoinFamily, btnOpenNotifSettings;
    ActivityResultLauncher<String> requestPermissionLauncher;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_child_setup);

        etChildName = findViewById(R.id.etChildName);
        etFamily = findViewById(R.id.etFamilyId);
        btnJoinFamily = findViewById(R.id.btnJoinFamily);
        btnOpenNotifSettings = findViewById(R.id.btnOpenNotifSettings);

        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (!isGranted) Toast.makeText(this, "לא ניתן לקבל מיקום ללא הרשאה", Toast.LENGTH_SHORT).show();
        });

        btnJoinFamily.setOnClickListener(v -> {
            String name = etChildName.getText().toString().trim();
            String familyId = etFamily.getText().toString().trim();
            if (name.isEmpty() || familyId.isEmpty()) { Toast.makeText(this, "מלא פרטים", Toast.LENGTH_SHORT).show(); return; }

            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                FirebaseAuth.getInstance().signInAnonymously().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) linkChildToFamily(FirebaseAuth.getInstance().getCurrentUser().getUid(), name, familyId);
                });
            } else {
                linkChildToFamily(FirebaseAuth.getInstance().getCurrentUser().getUid(), name, familyId);
            }
        });

        btnOpenNotifSettings.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));

        // request location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void linkChildToFamily(String uid, String name, String familyId) {
        DatabaseReference childRef = FirebaseDatabase.getInstance().getReference("families").child(familyId).child("children").child(uid);
        childRef.child("profile").child("name").setValue(name);
        childRef.child("profile").child("deviceModel").setValue(android.os.Build.MODEL);
        childRef.child("profile").child("lastSeen").setValue(System.currentTimeMillis());

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
        userRef.child("name").setValue(name);
        userRef.child("role").setValue("child");
        userRef.child("familyId").setValue(familyId);

        Toast.makeText(this, "הילד קושר למשפחה: " + familyId, Toast.LENGTH_SHORT).show();

        // start services (NotificationListener is a service that must be enabled in settings)
        startService(new android.content.Intent(this, com.example.childwatch.services.DeviceForegroundService.class));
    }
}
