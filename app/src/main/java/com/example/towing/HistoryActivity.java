package com.example.towing;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;

import com.example.towing.HistoryReclerView.HistoryAdapter;
import com.example.towing.HistoryReclerView.HistoryObject;
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

public class HistoryActivity extends AppCompatActivity {
    private String customerOrDriver,userId;
    private RecyclerView historyReclerview;
    private RecyclerView.Adapter mHistoryReclerviewAdapter;
    private RecyclerView.LayoutManager mHistoryLayouManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        historyReclerview=findViewById(R.id.historyReclerview);
        historyReclerview.setHasFixedSize(true);
        historyReclerview.setNestedScrollingEnabled(false);
        mHistoryLayouManager=new LinearLayoutManager(HistoryActivity.this);
        historyReclerview.setLayoutManager(mHistoryLayouManager);
        mHistoryReclerviewAdapter=new HistoryAdapter(getDasetHistory(),HistoryActivity.this);
        historyReclerview.setAdapter(mHistoryReclerviewAdapter);
        customerOrDriver=getIntent().getExtras().getString("customerOrDriver");
        userId= FirebaseAuth.getInstance().getUid();
        getUserHistoryIds();




        historyReclerview.setVisibility(View.VISIBLE);

    }

    private void getUserHistoryIds() {
        DatabaseReference userHistoryDatabase= FirebaseDatabase.getInstance().getReference().child("users").child(customerOrDriver)
                .child(userId).child("History");
        userHistoryDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists())
                {
                    for (DataSnapshot history: dataSnapshot.getChildren())
                        FetchRideInformation(history.getKey());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void FetchRideInformation(String ridekey) {
        DatabaseReference userHistoryDatabase= FirebaseDatabase.getInstance().getReference().child("History").child(ridekey);
        userHistoryDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists())
                {
                    String rideId=dataSnapshot.getKey();
                    Long timestamp=0L;
                    for(DataSnapshot child: dataSnapshot.getChildren())
                    {
                        if (child.getKey().equals("timestamp"))
                        {
                            timestamp=Long.valueOf(child.getValue().toString());
                        }
                    }
                    HistoryObject object=new HistoryObject(rideId,getDate(timestamp));
                    resultHistory.add(object);
                    mHistoryReclerviewAdapter.notifyDataSetChanged();

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

    private ArrayList resultHistory=new ArrayList<HistoryObject>();
    private ArrayList<HistoryObject> getDasetHistory() {
        return resultHistory;
    }
}
