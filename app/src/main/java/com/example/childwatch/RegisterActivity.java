package com.example.childwatch;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RegisterActivity extends AppCompatActivity {
    EditText etName, etEmail, etPass;
    Button btnRegister;
    FirebaseAuth auth;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPass = findViewById(R.id.etPass);
        btnRegister = findViewById(R.id.btnRegister);

        auth = FirebaseAuth.getInstance();

        btnRegister.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String pass = etPass.getText().toString().trim();
            if (name.isEmpty() || email.isEmpty() || pass.length() < 6) {
                Toast.makeText(this, "מלא/י את השדות (סיסמה לפחות 6 תווים)", Toast.LENGTH_SHORT).show();
                return;
            }
            btnRegister.setEnabled(false);
            auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
                btnRegister.setEnabled(true);
                if (task.isSuccessful()) {
                    String uid = auth.getCurrentUser().getUid();
                    String familyId = UUID.randomUUID().toString().substring(0,8);
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(uid);
                    Map<String,Object> user = new HashMap<>();
                    user.put("name", name);
                    user.put("email", email);
                    user.put("role", "parent");
                    user.put("familyId", familyId);
                    ref.setValue(user).addOnCompleteListener(dbTask -> {
                        if (dbTask.isSuccessful()) {
                            FirebaseDatabase.getInstance().getReference("families").child(familyId).child("ownerUid").setValue(uid);
                            Toast.makeText(RegisterActivity.this, "נרשמת, familyId: " + familyId, Toast.LENGTH_LONG).show();
                            startActivity(new Intent(RegisterActivity.this, ParentActivity.class).putExtra("familyId", familyId));
                            finish();
                        } else {
                            Toast.makeText(RegisterActivity.this, "DB error: " + dbTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    Toast.makeText(RegisterActivity.this, "Auth error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}
