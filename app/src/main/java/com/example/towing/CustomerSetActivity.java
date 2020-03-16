package com.example.towing;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class CustomerSetActivity extends AppCompatActivity {
EditText namefield,phonefield;
Button btnconfirm,btnback;
FirebaseAuth mAuth;
DatabaseReference mcustomerreference;
String userid;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_set);

        namefield=findViewById(R.id.name);
        phonefield=findViewById(R.id.phone);

        btnconfirm=findViewById(R.id.confirm);
        btnback=findViewById(R.id.back);

        mAuth=FirebaseAuth.getInstance();
        userid=mAuth.getCurrentUser().getUid();
        mcustomerreference= FirebaseDatabase.getInstance().getReference().child("users").child("customers").child(userid);
        getCustomerInfo();

        btnconfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savingUserInfo();

            }
        });
        btnback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                return;

            }
        });
    }

    private void savingUserInfo() {
        mname=namefield.getText().toString().trim();
        mphone=phonefield.getText().toString().trim();
        Map customerInfo=new HashMap();
        customerInfo.put("name",mname);
        customerInfo.put("phone",mphone);
        mcustomerreference.updateChildren(customerInfo);
        finish();
    }

    String mname;
    String mphone;
    private void getCustomerInfo()
    {
        mcustomerreference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()&&dataSnapshot.getChildrenCount()>0)
                {
                    Map<String,Object> map=(Map<String, Object>)dataSnapshot.getValue();
                    if (map.get("name")!=null)
                    {
                        mname=map.get("name").toString();
                        namefield.setText(mname);
                    }
                    if (map.get("phone")!=null)
                    {
                        mphone=map.get("phone").toString();
                        phonefield.setText(mphone);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }
}
