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
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Map;

public class WorkersMapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    LinearLayout customerInfo;
    TextView customername,customerphone,customerdestination;
    ImageView mcustomerprofile;
    Button logout;
    private GoogleMap mMap;
    LocationManager locationManager;
    String provider;
    double lat, lng;
    FirebaseAuth firebaseAuth;
    private Boolean isloggingout=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workers_maps);

        logout = findViewById(R.id.logout);

        customerInfo=findViewById(R.id.customerInfo);

        customername=findViewById(R.id.customername);
        customerphone=findViewById(R.id.customerphone);
        customerdestination=findViewById(R.id.customerdestination);

        mcustomerprofile=findViewById(R.id.mcustomerprofile);

        firebaseAuth = FirebaseAuth.getInstance();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isloggingout=true;
                disconnect();
                firebaseAuth.getInstance().signOut();
                startActivity(new Intent(WorkersMapsActivity.this, MainActivity.class));
                finish();
                return;
            }
        });

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //provide  a criteria for showing your location
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, false);
        // allow dangerous permission to access location
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(WorkersMapsActivity.this, new String[]
                    {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        } else {
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
                onLocationChanged(location);
            }
        }
        mapFragment.getMapAsync(this);
        getAssignedCustomer();
    }

    String customer_id = "";

    private void getAssignedCustomer() {
        String worker_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
        final DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("users").child("workers")
                .child(worker_id).child("customerid");
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {


                    customer_id = dataSnapshot.getValue().toString();
                    getAssignedCustomerPickupLocation();
                    getAssignedCustomerDestination();
                    getAssignedCustomerInfo();

                }
                else
                {
                    customer_id=" ";
                    if (pickupMarker!=null)
                    {
                        pickupMarker.remove();
                    }
                    if (assignedPickupLocationRefListener!=null)
                    {
                        assignedCustomerRef.removeEventListener(assignedPickupLocationRefListener);
                    }
                    customerInfo.setVisibility(View.GONE);
                    customername.setText(" ");
                    customerphone.setText(" ");
                    customerdestination.setText("Destination---");
                    mcustomerprofile.setImageResource(R.mipmap.user_foreground);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getAssignedCustomerDestination() {
        String worker_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
        final DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("users").child("workers")
                .child(worker_id).child("CustomerRequests").child("destination");
        assignedCustomerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {


                    String destination = dataSnapshot.getValue().toString();
                    customerdestination.setText("Destination:"+destination);

                }
                else
                {
                   customerdestination.setText("Destination...");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    Marker pickupMarker;
    DatabaseReference assignedPickupLocationRef;
    ValueEventListener assignedPickupLocationRefListener;

    private void getAssignedCustomerPickupLocation() {
        assignedPickupLocationRef = FirebaseDatabase.getInstance().getReference().child("CustomerRequests")
                .child(customer_id).child("l");
        assignedPickupLocationRefListener=assignedPickupLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()&&!customer_id.equals("")) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationlat = 0;
                    double locationlng = 0;
                    if (map.get(0) != null) {
                        locationlat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null) {
                        locationlng = Double.parseDouble(map.get(1).toString());

                    }
                    LatLng customerpickuplocation = new LatLng(locationlat, locationlng);
                    pickupMarker=mMap.addMarker(new MarkerOptions().position(customerpickuplocation).title("pick up location")
                            .icon(BitmapDescriptorFactory.fromResource(R.mipmap.customer_foreground)));

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
    private void getAssignedCustomerInfo()
    {
        customerInfo.setVisibility(View.VISIBLE);
        DatabaseReference mcustomerreference= FirebaseDatabase.getInstance().getReference().child("users").child("customers").child(customer_id);
        mcustomerreference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()&&dataSnapshot.getChildrenCount()>0)
                {
                    Map<String,Object> map=(Map<String, Object>)dataSnapshot.getValue();
                    if (map.get("name")!=null)
                    {
                        customername.setText(map.get("name").toString());
                    }
                    if (map.get("phone")!=null)
                    {
                        customerphone.setText(map.get("phone").toString());

                    }
                    if (map.get("profileImageUri")!=null)
                    {

                        Glide.with(getApplication()).load(map.get("profileImageUri").toString()).into(mcustomerprofile);

                    }
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

        // Add a marker in your location
        LatLng position = new LatLng(lat, lng);
        //mMap.addMarker(new MarkerOptions().position(position).title("Marker in Bungoma"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 10));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(10), 200, null);
        mMap.setMyLocationEnabled(true);

    }

    @Override
    public void onLocationChanged(Location location) {
        lat = location.getLatitude();
        lng = location.getLongitude();
        if (mMap != null) {
            LatLng position = new LatLng(lat, lng);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(position));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(10));

                String worker_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference workersAvailable = FirebaseDatabase.getInstance().getReference("WorkersAvailable");

                DatabaseReference workersworking = FirebaseDatabase.getInstance().getReference("WorkersWorking");
                GeoFire geoFireAvailable = new GeoFire(workersAvailable);
                GeoFire geoFireWorking = new GeoFire(workersworking);
                switch(customer_id){
                    case " ":
                        geoFireWorking.removeLocation(worker_id, new GeoFire.CompletionListener() {
                            @Override
                            public void onComplete(String key, DatabaseError error) {
                                if (error != null) {
                                    Toast.makeText(WorkersMapsActivity.this, "" + error.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                        geoFireAvailable.setLocation(worker_id, new GeoLocation(lat, lng), new GeoFire.CompletionListener() {
                            @Override
                            public void onComplete(String key, DatabaseError error) {
                                if (error != null) {
                                    Toast.makeText(WorkersMapsActivity.this, "" + error.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                        break;
                    default:
                        geoFireAvailable.removeLocation(worker_id, new GeoFire.CompletionListener() {
                            @Override
                            public void onComplete(String key, DatabaseError error) {
                                if (error != null) {
                                    Toast.makeText(WorkersMapsActivity.this, "" + error.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                        geoFireWorking.setLocation(worker_id, new GeoLocation(lat, lng), new GeoFire.CompletionListener() {
                            @Override
                            public void onComplete(String key, DatabaseError error) {
                                if (error != null) {
                                    Toast.makeText(WorkersMapsActivity.this, "" + error.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                        break;


            }


        }
    }

    @Override
    protected void onPause() {
        super.onPause();
            locationManager.removeUpdates(this);

    }
    private void disconnect()
    {
        String user_id=FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference workersAvailable = FirebaseDatabase.getInstance().getReference("WorkersAvailable");
        GeoFire geoFire=new GeoFire(workersAvailable);
        geoFire.removeLocation(user_id, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                if (error!=null)
                {
                    Toast.makeText(WorkersMapsActivity.this, ""+error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isloggingout)
        {
            disconnect();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(WorkersMapsActivity.this, new String[]
                    {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        } else {
            locationManager.requestLocationUpdates(provider, 180000, 50, this);
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
