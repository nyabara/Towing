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
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkersMapsActivity extends FragmentActivity implements OnMapReadyCallback ,LocationListener, RoutingListener {

    LinearLayout customerInfo;
    TextView customername, customerphone, customerdestination;
    ImageView mcustomerprofile;
    Button logout, setting, ridestatus;
    private GoogleMap mMap;
    LocationManager locationManager;
    GoogleApiClient googleApiClient;
    LocationRequest locationRequest;
    String provider;
    private int status = 0;
    double lat, lng;
    FirebaseAuth firebaseAuth;
    private Boolean isloggingout = false;
    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};
    private LatLng destinationLatLng;
    private String destination;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workers_maps);

        logout = findViewById(R.id.logout);
        setting = findViewById(R.id.setting);
        ridestatus = findViewById(R.id.ridestatus);

        customerInfo = findViewById(R.id.customerInfo);

        customername = findViewById(R.id.customername);
        customerphone = findViewById(R.id.customerphone);
        customerdestination = findViewById(R.id.customerdestination);

        mcustomerprofile = findViewById(R.id.mcustomerprofile);

        polylines = new ArrayList<>();

        firebaseAuth = FirebaseAuth.getInstance();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isloggingout = true;
                disconnect();
                firebaseAuth.getInstance().signOut();
                startActivity(new Intent(WorkersMapsActivity.this, MainActivity.class));
                finish();
                return;
            }
        });
        setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WorkersMapsActivity.this, WorkerSettingActivity.class));
            }
        });
        ridestatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (status) {
                    case 1:
                        status = 2;
                        erasepolylines();
                        if (destinationLatLng.latitude != 0.0 && destinationLatLng.longitude != 0.0) {
                            getRouteToMarker(destinationLatLng);
                        }
                        ridestatus.setText("drive completed");
                        break;
                    case 2:
                        recordRide();
                        endRide();
                        break;
                }
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
            return;
        }else {
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
                onLocationChanged(location);
            }
        }


        mapFragment.getMapAsync(this);
        getAssignedCustomer();
    }

    private void recordRide() {
        String user_id = firebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference workeref = FirebaseDatabase.getInstance().getReference().child("users").child("workers")
                .child(user_id).child("History");
        DatabaseReference customerref = FirebaseDatabase.getInstance().getReference().child("users").child("customers")
                .child(customer_id).child("History");
        DatabaseReference historyref=FirebaseDatabase.getInstance().getReference().child("History");
        String resultRideId=historyref.push().getKey();
        workeref.child(resultRideId).setValue(true);
        customerref.child(resultRideId).setValue(true);
        HashMap map=new HashMap();
        map.put("driverideid",user_id);
        map.put("customerideid",customer_id);
        map.put("rating",0);
        map.put("timestamp",getCurrentTime());
        map.put("destination",destination);
        map.put("location/from/lat",customerpickuplocation.latitude);
        map.put("location/from/lng",customerpickuplocation.longitude);
        map.put("location/to/lat",destinationLatLng.latitude);
        map.put("location/to/lng",destinationLatLng.longitude);
        historyref.child(resultRideId).updateChildren(map);
    }

    private Long getCurrentTime() {
        Long timestamp=System.currentTimeMillis()/1000;
        return timestamp;
    }

    private void endRide() {
        ridestatus.setText("Pick Customer");
        erasepolylines();
        String user_id = firebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference workeref = FirebaseDatabase.getInstance().getReference().child("users").child("workers")
                .child(user_id).child("CustomerRequests");
        workeref.removeValue();
        DatabaseReference reQuestRef = FirebaseDatabase.getInstance().getReference("CustomerRequests");
        GeoFire geoFire = new GeoFire(reQuestRef);
        geoFire.removeLocation(customer_id, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                if (error != null) {
                    Toast.makeText(WorkersMapsActivity.this, "" + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        customer_id = "";

        if (pickupMarker != null) {
            pickupMarker.remove();
        }
        if (assignedPickupLocationRefListener != null) {
            assignedPickupLocationRef.removeEventListener(assignedPickupLocationRefListener);
        }
        customerInfo.setVisibility(View.GONE);
        customername.setText(" ");
        customerphone.setText(" ");
        customerdestination.setText("Destination---");
        // mcustomerprofile.setImageResource(R.mipmap.user_foreground);

    }

    String customer_id = "";

    private void getAssignedCustomer() {
        String worker_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
        final DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("users").child("workers")
                .child(worker_id).child("CustomerRequests").child("customerid");
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    status = 1;
                    customer_id = dataSnapshot.getValue().toString();
                    getAssignedCustomerPickupLocation();
                    getAssignedCustomerDestination();
                    getAssignedCustomerInfo();

                } else {
                   endRide();
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
                .child(worker_id).child("CustomerRequests");
        assignedCustomerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("destination") != null) {
                        destination = map.get("destination").toString();
                        customerdestination.setText("Destination:" + destination);
                    } else {
                        customerdestination.setText("Destination...");
                    }
                    Double destinationLat = 0.0;
                    Double destinationLng = 0.0;
                    if (map.get("destinationLat") != null) {
                        destinationLat = Double.valueOf(map.get("destinationLat").toString());
                    }
                    if (map.get("destinationLng") != null) {
                        destinationLng = Double.valueOf(map.get("destinationLng").toString());
                    }
                    destinationLatLng = new LatLng(destinationLat, destinationLng);


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    Marker pickupMarker;
    private LatLng customerpickuplocation;
    DatabaseReference assignedPickupLocationRef;
    ValueEventListener assignedPickupLocationRefListener;

    private void getAssignedCustomerPickupLocation() {
        assignedPickupLocationRef = FirebaseDatabase.getInstance().getReference().child("CustomerRequests")
                .child(customer_id).child("l");
        assignedPickupLocationRefListener = assignedPickupLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && !customer_id.equals(" ")) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationlat = 0;
                    double locationlng = 0;
                    if (map.get(0) != null) {
                        locationlat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null) {
                        locationlng = Double.parseDouble(map.get(1).toString());

                    }
                     customerpickuplocation = new LatLng(locationlat, locationlng);
                    pickupMarker = mMap.addMarker(new MarkerOptions().position(customerpickuplocation).title("pick up location")
                            .icon(BitmapDescriptorFactory.fromResource(R.mipmap.customer_foreground)));
                    getRouteToMarker(customerpickuplocation);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getRouteToMarker(LatLng customerpickuplocation) {
        Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(new LatLng(lat, lng), customerpickuplocation)
                .key("AIzaSyDwBPxwpg7G6DessJkHmZ9Kgrsf8r-Dknk")
                .build();
        routing.execute();
    }

    private void getAssignedCustomerInfo() {
        customerInfo.setVisibility(View.VISIBLE);
        DatabaseReference mcustomerreference = FirebaseDatabase.getInstance().getReference().child("users").child("customers").child(customer_id);
        mcustomerreference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("name") != null) {
                        customername.setText(map.get("name").toString());
                    }
                    if (map.get("phone") != null) {
                        customerphone.setText(map.get("phone").toString());

                    }
                    if (map.get("profileImageUri") != null) {

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
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 10));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(10), 200, null);
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(WorkersMapsActivity.this, new String[]
                    {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        return;
        }
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
            switch (customer_id) {
                case "":
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
    /*
    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(this);

    }

     */

    private void disconnect() {
        String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference workersAvailable = FirebaseDatabase.getInstance().getReference("WorkersAvailable");
        GeoFire geoFire = new GeoFire(workersAvailable);
        geoFire.removeLocation(user_id, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                if (error != null) {
                    Toast.makeText(WorkersMapsActivity.this, "" + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isloggingout) {
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
        return;
        }
        locationManager.requestLocationUpdates(provider, 180000, 50, this);

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

    @Override
    public void onRoutingFailure(RouteException e) {
        if (e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
        if (polylines.size() > 0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i < route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);
            Toast.makeText(getApplicationContext(), "Route " + (i + 1) + ": distance - " + route.get(i).getDistanceValue() + ": duration - " + route.get(i).getDurationValue(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingCancelled() {

    }

    private void erasepolylines() {
        for (Polyline line : polylines) {
            line.remove();
        }
        polylines.clear();
    }
}
