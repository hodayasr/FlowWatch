package com.example.childwatch;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.childwatch.adapters.NotificationAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ParentActivity extends AppCompatActivity {

    ListView lvChildren;
    ArrayAdapter<String> childrenAdapter;
    ArrayList<String> childrenList = new ArrayList<>();
    ArrayList<String> childrenUids = new ArrayList<>();

    RecyclerView rvNotifs;
    NotificationAdapter notifAdapter;
    ArrayList<com.example.childwatch.adapters.NotificationAdapter.NotiItem> notiItems = new ArrayList<>();

    String currentUid;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_parent_full);

        lvChildren = findViewById(R.id.lvChildren);
        rvNotifs = findViewById(R.id.rvNotifs);

        childrenAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, childrenList);
        lvChildren.setAdapter(childrenAdapter);

        notifAdapter = new NotificationAdapter(notiItems);
        rvNotifs.setLayoutManager(new LinearLayoutManager(this));
        rvNotifs.setAdapter(notifAdapter);

        currentUid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (currentUid != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUid);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(DataSnapshot snapshot) {
                    if (!snapshot.exists()) return;
                    String familyId = snapshot.child("familyId").getValue(String.class);
                    if (familyId == null) return;
                    watchChildren(familyId);
                }
                @Override public void onCancelled(DatabaseError error) { }
            });
        }

        lvChildren.setOnItemClickListener((parent, view, position, id) -> {
            String childUid = childrenUids.get(position);
            openChildNotifications(childUid);
        });
    }

    private void watchChildren(String familyId) {
        DatabaseReference famChildren = FirebaseDatabase.getInstance().getReference("families").child(familyId).child("children");
        famChildren.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snapshot) {
                childrenList.clear(); childrenUids.clear();
                for (DataSnapshot childSnap : snapshot.getChildren()) {
                    String uid = childSnap.getKey();
                    String name = "";
                    if (childSnap.child("profile").child("name").exists()) name = childSnap.child("profile").child("name").getValue(String.class);
                    childrenList.add((name==null||name.isEmpty()?uid:name) + " ("+uid+")");
                    childrenUids.add(uid);
                }
                childrenAdapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(DatabaseError error) { }
        });
    }

    private void openChildNotifications(String childUid) {
        // clear current list
        notiItems.clear();

        // get notifications under families/{familyId}/children/{childUid}/notifications
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("families");
        // find family of current user
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUid);
        userRef.child("familyId").get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) return;
            String fam = snapshot.getValue(String.class);
            DatabaseReference notRef = FirebaseDatabase.getInstance().getReference("families").child(fam).child("children").child(childUid).child("notifications");
            notRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(DataSnapshot snapshot) {
                    for (DataSnapshot n : snapshot.getChildren()) {
                        String pkg = n.child("packageName").getValue(String.class);
                        String title = n.child("title").getValue(String.class);
                        String text = n.child("text").getValue(String.class);
                        long ts = n.child("timestamp").getValue(Long.class) != null ? n.child("timestamp").getValue(Long.class) : 0L;
                        String timeStr = formatTs(ts);
                        notiItems.add(new com.example.childwatch.adapters.NotificationAdapter.NotiItem(pkg, title, text, timeStr));
                    }
                    notifAdapter.notifyDataSetChanged();
                }
                @Override public void onCancelled(DatabaseError error) { }
            });
        }).addOnFailureListener(e -> Toast.makeText(this, "Error fetching family", Toast.LENGTH_SHORT).show());
    }

    private String formatTs(long ts) {
        if (ts<=0) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
        return sdf.format(new Date(ts));
    }
}
