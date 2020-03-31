package com.example.towing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CustomerSetActivity extends AppCompatActivity {
EditText namefield,phonefield;
Button btnconfirm,btnback;
ImageView customerprofile;
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

        customerprofile=findViewById(R.id.customerprofile);

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
        customerprofile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent,1);
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
        if (resulturi!=null)
        {
            StorageReference storagepath= FirebaseStorage.getInstance().getReference().child("profile_images")
                    .child(userid);
            Bitmap bitmap=null;
            try {
                bitmap= MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(),resulturi);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG,20,baos);
            byte[] data=baos.toByteArray();
            storagepath.putBytes(data);
            storagepath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    Uri downloaduri=uri;
                    Map newImage=new HashMap();
                    newImage.put("profileImageUri",downloaduri.toString());
                    mcustomerreference.updateChildren(newImage);
                     finish();
                    return;

                }
            });
        }
        else
        {
            finish();
        }


    }

    String mname;
    String mphone;
    String mprofileImageUri;
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
                    if (map.get("profileImageUri")!=null)
                    {
                     mprofileImageUri=map.get("profileImageUri").toString();
                    Glide.with(getApplication()).load(mprofileImageUri).into(customerprofile);

                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }
    private Uri resulturi;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==1&&resultCode== Activity.RESULT_OK)
        {
            final Uri imageuri=data.getData();
            resulturi=imageuri;
            customerprofile.setImageURI(resulturi);
        }
    }
}
