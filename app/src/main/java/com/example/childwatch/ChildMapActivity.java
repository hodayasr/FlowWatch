package com.example.childwatch;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.firebase.database.*;

public class ChildMapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private String familyId, childUid;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_child_map);

        // Expect extras: familyId, childUid
        familyId = getIntent().getStringExtra("familyId");
        childUid = getIntent().getStringExtra("childUid");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        if (familyId==null || childUid==null) return;

        DatabaseReference locRef = FirebaseDatabase.getInstance().getReference("families").child(familyId)
                .child("children").child(childUid).child("location");
        locRef.limitToLast(1).addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                // find most recent
                for (DataSnapshot s : snapshot.getChildren()) {
                    Double lat = s.child("lat").getValue(Double.class);
                    Double lng = s.child("lng").getValue(Double.class);
                    if (lat != null && lng != null) {
                        LatLng pos = new LatLng(lat, lng);
                        mMap.clear();
                        mMap.addMarker(new MarkerOptions().position(pos).title("Child location"));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f));
                        break;
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }
}
