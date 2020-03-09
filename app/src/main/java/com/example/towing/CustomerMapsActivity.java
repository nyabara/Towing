package com.example.towing;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;

public class CustomerMapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {
    FirebaseAuth firebaseAuth;
    Button logout,btnRequest;
    LocationManager locationManager;
    String provider;
    double lat,lng;
    LatLng pickupLocation;

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_maps);
        logout=findViewById(R.id.logout);
        btnRequest=findViewById(R.id.btnRequest);
        firebaseAuth=FirebaseAuth.getInstance();
        getActionBar();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        final FirebaseUser firebaseUser=FirebaseAuth.getInstance().getCurrentUser();
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseAuth.getInstance().signOut();
                startActivity(new Intent(CustomerMapsActivity.this,MainActivity.class));
                finish();
                return;

            }
        });
        locationManager=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
        //provide  a criteria for showing your location
        Criteria criteria=new Criteria();
        provider=locationManager.getBestProvider(criteria,false);
        // allow dangerous permission to access location
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CustomerMapsActivity.this, new String[]
                    {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }
        else
        {
            Location location=locationManager.getLastKnownLocation(provider);
            if (location!=null)
            {
                onLocationChanged(location);
            }
        }
        mapFragment.getMapAsync(this);
        btnRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user_id=firebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference reQuestRef= FirebaseDatabase.getInstance().getReference("CustomerRequests");
                GeoFire geoFire=new GeoFire(reQuestRef);
                geoFire.setLocation(user_id, new GeoLocation(lat, lng), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        if (error!=null)
                        {
                            Toast.makeText(CustomerMapsActivity.this, ""+error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            Toast.makeText(CustomerMapsActivity.this, "success", Toast.LENGTH_SHORT).show();
                        }

                    }
                });
                 pickupLocation=new LatLng(lat,lng);
                mMap.addMarker(new MarkerOptions().position(pickupLocation).title("pick up here"));
                btnRequest.setText("request a tow...");
                getClosestWorker();
            }
        });
    }
    private boolean workerFound=false;
    private double radius=1;
    private  String workerFoundId;
     private void getClosestWorker()
    {
        DatabaseReference closetWorkerRef=FirebaseDatabase.getInstance().getReference("WorkersAvailable");
        GeoFire geoFire=new GeoFire(closetWorkerRef);
        GeoQuery geoQuery=geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude,pickupLocation.longitude),radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!workerFound)
                {
                    workerFound=true;
                    workerFoundId=key;
                    String customer_id=FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference workeref=FirebaseDatabase.getInstance().getReference().child("users").child("workers")
                            .child(workerFoundId);
                    HashMap map=new HashMap();
                    map.put("customerid",customer_id);
                    workeref.updateChildren(map);
                    btnRequest.setText("Looking a tow for you");
                    getWorkerLocation();
                }

            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!workerFound)
                {
                    radius++;
                    getClosestWorker();
                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }
    private Marker workermarker;
    private void getWorkerLocation(){
         DatabaseReference workerlocation=FirebaseDatabase.getInstance().getReference().child("Workersworking").child(workerFoundId)
                 .child("l");
         workerlocation.addValueEventListener(new ValueEventListener() {
             @Override
             public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                 if (dataSnapshot.exists())
                 {
                     List<Object> map=(List<Object>) dataSnapshot.getValue();
                     double locationlat=0;
                     double locationlng=0;
                     btnRequest.setText("tower found");
                     if (map.get(0)!=null)
                     {
                         locationlat=Double.parseDouble(map.get(0).toString());
                     }
                     if (map.get(1)!=null)
                     {
                         locationlng=Double.parseDouble(map.get(1).toString());
                     }
                     LatLng worklatlng=new LatLng(locationlat,locationlng);
                     if (workermarker!=null)
                     {
                         workermarker.remove();
                     }
                     workermarker=mMap.addMarker(new MarkerOptions().position(worklatlng).title("your tower"));

                 }

             }

             @Override
             public void onCancelled(@NonNull DatabaseError databaseError) {

             }
         });

    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in your current place and move the camera
        LatLng position = new LatLng(lat, lng);
        mMap.addMarker(new MarkerOptions().position(position));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position,10));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(10), 200, null);
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CustomerMapsActivity.this, new String[]
                    {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        } else {
            mMap.setMyLocationEnabled(true);
        }


    }

    @Override
    public void onLocationChanged(Location location) {
        lat=location.getLatitude();
        lng=location.getLongitude();
        if (mMap!=null) {
            LatLng position = new LatLng(lat, lng);
            mMap.addMarker(new MarkerOptions()
                    .title("your location")
                    .anchor(0.0f, 1.0f)
                    .position(position));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(position));
        }


    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
