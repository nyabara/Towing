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
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WorkerSettingActivity extends AppCompatActivity {

    EditText namefield,phonefield,tow;
    Button btnconfirm,btnback;
    ImageView workerprofile;
    FirebaseAuth mAuth;
    DatabaseReference workerefernce;
    String userid,mservice;
    RadioGroup radiogroup;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_setting);

        namefield=findViewById(R.id.name);
        phonefield=findViewById(R.id.phone);
        tow=findViewById(R.id.tow);

        btnconfirm=findViewById(R.id.confirm);
        btnback=findViewById(R.id.back);

        workerprofile=findViewById(R.id.workerprofile);

        radiogroup=findViewById(R.id.radiogroup);

        mAuth=FirebaseAuth.getInstance();
        userid=mAuth.getCurrentUser().getUid();
        workerefernce= FirebaseDatabase.getInstance().getReference().child("users").child("workers").child(userid);
        getWorkerInfo();

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
        workerprofile.setOnClickListener(new View.OnClickListener() {
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
        mtow=tow.getText().toString().trim();

        int selectedid=radiogroup.getCheckedRadioButtonId();
        final RadioButton radioButton=findViewById(selectedid);

        if (radioButton.getText()==null)
        {
            return;
        }
        mservice=radioButton.getText().toString();

        Map workerInfo=new HashMap();
        workerInfo.put("name",mname);
        workerInfo.put("tow",mtow);
        workerInfo.put("phone",mphone);
        workerInfo.put("service",mservice);
        workerefernce.updateChildren(workerInfo);
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
                    workerefernce.updateChildren(newImage);
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
    String mtow;
    String mname;
    String mphone;
    String mprofileImageUri;
    private void getWorkerInfo()
    {
        workerefernce.addValueEventListener(new ValueEventListener() {
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
                    if (map.get("tow")!=null)
                    {
                        mtow=map.get("tow").toString();
                        tow.setText(mtow);
                    }
                    if (map.get("phone")!=null)
                    {
                        mphone=map.get("phone").toString();
                        phonefield.setText(mphone);
                    }
                    if (map.get("service")!=null)
                    {
                        mservice=map.get("service").toString();
                        switch (mservice)
                        {
                            case "special":
                                radiogroup.check(R.id.special);
                                break;
                            case "medium":
                                radiogroup.check(R.id.medium);
                                break;
                            case "cheap":
                                radiogroup.check(R.id.cheap);
                                break;
                        }
                    }
                    if (map.get("profileImageUri")!=null)
                    {
                        mprofileImageUri=map.get("profileImageUri").toString();
                        Glide.with(getApplication()).load(mprofileImageUri).into(workerprofile);

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
            workerprofile.setImageURI(resulturi);
        }
    }

}
