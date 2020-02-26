package com.example.towing;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    Button customer,worker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        customer=findViewById(R.id.customer);
        worker=findViewById(R.id.worker);
        //navigating to customer activity
        customer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(MainActivity.this,CustomerLoginActivity.class);
                //intent.putExtra(CustomerLoginActivity.BUTTON_EXTRA, (Parcelable) customer);
                startActivity(intent);

                //startActivity(new Intent(MainActivity.this,CustomerLoginActivity.class));
            }
        });
        worker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }
}
