package com.example.towing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerMapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {
    FirebaseAuth firebaseAuth;
    Button logout, btnRequest, setting;
    LocationManager locationManager;
    String provider;
    double lat, lng;
    LatLng pickupLocation;
    private Boolean requestBool = false;
    private Marker pickupMarker;
    RadioGroup radiogroup;

    private GoogleMap mMap;
    private String destination,reQuestservice;
    LinearLayout workerInfo;
    TextView workername, workerphone, workertow;
    ImageView workerprofile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_maps);

        firebaseAuth = FirebaseAuth.getInstance();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //provide  a criteria for showing your location
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, false);
        // allow dangerous permission to access location
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CustomerMapsActivity.this, new String[]
                    {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        } else {
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
                onLocationChanged(location);
            }
        }
        mapFragment.getMapAsync(this);

        logout = findViewById(R.id.logout);
        btnRequest = findViewById(R.id.btnRequest);
        setting = findViewById(R.id.setting);

        workerInfo = findViewById(R.id.workerInfo);

        workername = findViewById(R.id.workername);
        workerphone = findViewById(R.id.workerphone);
        workertow = findViewById(R.id.workertow);

        radiogroup = findViewById(R.id.radiogroup);
        radiogroup.check(R.id.special);

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseAuth.getInstance().signOut();
                startActivity(new Intent(CustomerMapsActivity.this, MainActivity.class));
                finish();
                return;

            }
        });

        btnRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (requestBool) {
                   requestBool = false;

                    geoQuery.removeAllListeners();
                    if (workerlocationListener!=null)
                    {
                        workerlocation.removeEventListener(workerlocationListener);
                    }



                    if (workerFoundId != null) {
                        DatabaseReference workeref = FirebaseDatabase.getInstance().getReference().child("users").child("workers")
                                .child(workerFoundId).child("CustomerRequests");
                        workeref.removeValue();
                        workerFoundId = null;
                    }
                    workerFound = false;
                    radius = 1;
                    String user_id = firebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference reQuestRef = FirebaseDatabase.getInstance().getReference("CustomerRequests");
                    GeoFire geoFire = new GeoFire(reQuestRef);
                    geoFire.removeLocation(user_id, new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                            if (error!=null)
                            {
                                Toast.makeText(CustomerMapsActivity.this, ""+error.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    if (pickupMarker != null) {
                        pickupMarker.remove();
                    }
                    if (workermarker!=null)
                    {
                        workermarker.remove();
                    }
                    btnRequest.setText("Call Tow");
                    workerInfo.setVisibility(View.GONE);
                    workername.setText(" ");
                    workerphone.setText(" ");
                    workertow.setText(" ");
                    //workerprofile.setImageResource(R.mipmap.user_foreground);

                } else {
                    requestBool = true;

                    int selectedid=radiogroup.getCheckedRadioButtonId();
                    final RadioButton radioButton=findViewById(selectedid);

                    if (radioButton.getText()==null)
                    {
                        return;
                    }
                    reQuestservice=radioButton.getText().toString();



                    String user_id = firebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference reQuestRef = FirebaseDatabase.getInstance().getReference("CustomerRequests");
                    GeoFire geoFire = new GeoFire(reQuestRef);
                    geoFire.setLocation(user_id, new GeoLocation(lat, lng), new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                            if (error != null) {
                                Toast.makeText(CustomerMapsActivity.this, "" + error.getMessage(), Toast.LENGTH_SHORT).show();
                            }

                        }
                    });
                    pickupLocation = new LatLng(lat, lng);
                    pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLocation).title("pick up here")
                            .icon(BitmapDescriptorFactory.fromResource(R.mipmap.customer_foreground)));
                    btnRequest.setText("Getting you a tower...");
                    getClosestWorker();
                }

            }
        });
        setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startActivity(new Intent(CustomerMapsActivity.this, CustomerSettingActivity.class));
                Intent intent = new Intent(CustomerMapsActivity.this, CustomerSetActivity.class);
                startActivity(intent);
                return;
            }
        });
        //PlacesClient placesClient = Places.createClient(this);
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyDgkF_nJaUiaFOz2RBz3jykP87IfUXwNbM");
        }
        // Initialize the AutocompleteSupportFragment.
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        autocompleteFragment.setCountry("KE");

// Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));

// Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                destination = place.getName();
                //Log.i(destination, "Place: " + place.getName() + ", " + place.getId());
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                //Log.i(destination, "An error occurred: " + status);
            }
        });

    }

    private boolean workerFound = false;
    private double radius = 1;
    private String workerFoundId;
    private GeoQuery geoQuery;

    private void getClosestWorker() {


        DatabaseReference closetWorkerRef = FirebaseDatabase.getInstance().getReference("WorkersAvailable");
        GeoFire geoFire = new GeoFire(closetWorkerRef);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude, pickupLocation.longitude), radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!workerFound && requestBool) {

                    DatabaseReference mCustomerDatabaseReference = FirebaseDatabase.getInstance().getReference()
                            .child("users").child("workers").child(key);
                    mCustomerDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                                Map<Object, String> driverMap = (Map<Object, String>) dataSnapshot.getValue();
                                if (workerFound) {
                                    return;
                                }
                                if (driverMap.get("service").equals(reQuestservice)) {
                                    workerFound = true;
                                    workerFoundId = dataSnapshot.getKey();
                                    String customer_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                    DatabaseReference workeref = FirebaseDatabase.getInstance().getReference().child("users").child("workers")
                                            .child(workerFoundId).child("CustomerRequests");
                                    HashMap map = new HashMap();
                                    map.put("customerid", customer_id);
                                    map.put("destination", destination);
                                    workeref.updateChildren(map);
                                    getWorkerLocation();
                                    getWorkerInfo();
                                    btnRequest.setText("Looking a tow for you");

                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });


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
                if (!workerFound) {
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
    private DatabaseReference workerlocation;
    private ValueEventListener workerlocationListener;

    private void getWorkerLocation() {
        workerlocation = FirebaseDatabase.getInstance().getReference().child("WorkersWorking").child(workerFoundId)
                .child("l");
        workerlocationListener = workerlocation.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && requestBool) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationlat = 0;
                    double locationlng = 0;
                    btnRequest.setText("tower found");
                    if (map.get(0) != null) {
                        locationlat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null) {
                        locationlng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng worklatlng = new LatLng(locationlat, locationlng);
                    if (workermarker != null) {
                        workermarker.remove();
                    }
                    Location loc1 = new Location("");
                    loc1.setLatitude(pickupLocation.latitude);
                    loc1.setLongitude(pickupLocation.longitude);

                    Location loc2 = new Location("");
                    loc2.setLatitude(worklatlng.latitude);
                    loc2.setLongitude(worklatlng.longitude);

                    float distance = loc1.distanceTo(loc2);

                    if (distance <= 100) {
                        btnRequest.setText("tower is hear");
                    } else {
                        btnRequest.setText("worker found" + String.valueOf(distance));
                    }

                    workermarker = mMap.addMarker(new MarkerOptions().position(worklatlng).title("your tower")
                            .icon(BitmapDescriptorFactory.fromResource(R.mipmap.tow_foreground)));

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void getWorkerInfo() {
        workerInfo.setVisibility(View.VISIBLE);
        DatabaseReference mworkerreference = FirebaseDatabase.getInstance().getReference().child("users").child("workers").child(workerFoundId);
        mworkerreference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("name") != null) {
                        workername.setText(map.get("name").toString());
                    }
                    if (map.get("phone") != null) {
                        workerphone.setText(map.get("phone").toString());

                    }
                    if (map.get("tow") != null) {
                        workertow.setText(map.get("tow").toString());

                    }
                    if (map.get("profileImageUri") != null) {

                        Glide.with(getApplication()).load(map.get("profileImageUri").toString()).into(workerprofile);

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

        // Add a marker in your current place and move the camera
        LatLng position = new LatLng(lat, lng);
        //mMap.addMarker(new MarkerOptions().position(position));
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
