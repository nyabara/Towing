package com.example.towing;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistorySetupActivity extends AppCompatActivity implements OnMapReadyCallback, RoutingListener {
    private GoogleMap mMap;
    private SupportMapFragment supportMapFragment;
    TextView location,ridedate,name,phone,distance;
    ImageView imageView;
    private String rideId,currentuserid,customerid,workerid,userworkerorcustomer;
    private LatLng destinationLatLng,customerpickuplocation;
    DatabaseReference historyinfordb;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_setup);
        supportMapFragment=(SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        supportMapFragment.getMapAsync(this);


        location=findViewById(R.id.location);
        ridedate=findViewById(R.id.date);
        name=findViewById(R.id.name);
        phone=findViewById(R.id.phone);
        distance=findViewById(R.id.distance);

        polylines = new ArrayList<>();

        imageView=findViewById(R.id.imageview);

        rideId=getIntent().getExtras().getString("rideId");
        currentuserid= FirebaseAuth.getInstance().getCurrentUser().getUid();
        historyinfordb= FirebaseDatabase.getInstance().getReference().child("History").child(rideId);
        getRideInformation();

    }

    private void getRideInformation() {
        historyinfordb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists())
                {
                    for(DataSnapshot child:dataSnapshot.getChildren())
                    {
                        if (child.getKey().equals("customerideid")) {
                            customerid=child.getValue().toString();
                            if (!customerid.equals(currentuserid)){
                                userworkerorcustomer="workers";
                                getUserInformation("customers",customerid);
                            }
                        }
                        if (child.getKey().equals("driverideid")) {
                            workerid=child.getValue().toString();
                            if (!workerid.equals(currentuserid)){
                                userworkerorcustomer="customers";
                                getUserInformation("workers",workerid);
                            }
                        }
                        if (child.getKey().equals("timestamp")) {
                            ridedate.setText(getDate(Long.valueOf(child.getValue().toString())));
                        }
                        if (child.getKey().equals("destination")) {
                            location.setText(child.getValue().toString());
                        }
                        if (child.getKey().equals("location")) {
                            customerpickuplocation=new LatLng(Double.valueOf(child.child("from").child("lat").getValue().toString()),
                                    Double.valueOf(child.child("from").child("lng").getValue().toString()));
                            destinationLatLng=new LatLng(Double.valueOf(child.child("to").child("lat").getValue().toString()),
                                    Double.valueOf(child.child("to").child("lng").getValue().toString()));
                            if (destinationLatLng!=new LatLng(0.0,0.0))
                            {
                                getRouteToMarker();
                            }

                        }

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getUserInformation(String othercustomerorworker, String otherid) {
        DatabaseReference otherworkerorcustomerdb=FirebaseDatabase.getInstance().getReference().
                child(othercustomerorworker).child(otherid);
        otherworkerorcustomerdb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists())
                {
                    Map<String,Object> map=(Map<String, Object>)dataSnapshot.getValue();
                    if (map.get("name")!=null)
                    {
                        name.setText(map.get("name").toString());
                    }
                    if (map.get("phone")!=null)
                    {
                        phone.setText(map.get("phone").toString());
                    }
                    if (map.get("profileImageUri") != null) {

                        Glide.with(getApplication()).load(map.get("profileImageUri").toString()).into(imageView);

                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private String getDate(Long timestamp) {
        Calendar cal=Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(timestamp*1000);
        String date= DateFormat.format("dd-MM-yyyy hh:mm",cal).toString();
        return date;
    }
    private void getRouteToMarker() {
        Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(customerpickuplocation,destinationLatLng)
                .key("AIzaSyDwBPxwpg7G6DessJkHmZ9Kgrsf8r-Dknk")
                .build();
        routing.execute();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap=googleMap;

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
    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};

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
