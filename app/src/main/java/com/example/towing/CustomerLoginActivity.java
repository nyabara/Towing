package com.example.towing;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CustomerLoginActivity extends AppCompatActivity implements View.OnClickListener {
    Button btnSignup,btnSignin;
    EditText txtemail,txtpass;
    TextView txtResetPass;
    ProgressDialog progressDialog;
    FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_login);
        btnSignup=findViewById(R.id.btnSignup);
        btnSignin=findViewById(R.id.btnSignin);
        txtemail=findViewById(R.id.txtemail);
        txtpass=findViewById(R.id.txtpass);
        txtResetPass=findViewById(R.id.txtResetPass);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("Customer");
        firebaseAuth=FirebaseAuth.getInstance();
        FirebaseUser firebaseUser=FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser!=null&&firebaseUser.isEmailVerified())
        {
            //finish();
            //startActivity(new Intent(CustomerLoginActivity.this,CustomerMapsActivity.class));
        }

        progressDialog=new ProgressDialog(this);
        btnSignin.setOnClickListener(this);
        btnSignup.setOnClickListener(this);
        txtResetPass.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v==btnSignin)
        {
            SignIn();
        }
        if(v==btnSignup)
        {
            SignUp();
        }
        if (v==txtResetPass)
        {
            ResetPassword();
        }
    }
    //signing up a customer to firebase
    private void SignUp() {
        String email=txtemail.getText().toString().trim();
        String password=txtpass.getText().toString().trim();
        if (TextUtils.isEmpty(email)&&TextUtils.isEmpty(password))
        {
            Toast.makeText(this, "please make sure you have filled in both email and password",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        progressDialog.setMessage("signing you up please wait....");
        progressDialog.show();
        firebaseAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener
                (new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful())
                {
                    firebaseAuth.getCurrentUser().sendEmailVerification().addOnCompleteListener
                            (new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful())
                            {
                                String user_id=firebaseAuth.getCurrentUser().getUid();
                                DatabaseReference databaseReference= FirebaseDatabase.getInstance().getReference()
                                        .child("users").child("customers").child(user_id);
                                databaseReference.setValue(true);
                                Toast.makeText(CustomerLoginActivity.this
                                        , "registered successfully, check your email for verification",
                                        Toast.LENGTH_SHORT).show();


                            }
                            else
                            {
                                Toast.makeText(CustomerLoginActivity.this, ""+task.getException().getMessage()
                                        , Toast.LENGTH_SHORT).show();
                            }

                        }
                    });
                }
                else
                {
                    Toast.makeText(CustomerLoginActivity.this, ""+task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
                progressDialog.dismiss();
            }
        });

    }
    //LOGIN  A CUSTOMER
    private void SignIn() {
        String email=txtemail.getText().toString().trim();
        String password=txtpass.getText().toString().trim();
        if (TextUtils.isEmpty(email)&&TextUtils.isEmpty(password))
        {
            Toast.makeText(this, "please make sure you have filled in both email and password",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        progressDialog.setMessage("signing you in please wait....");
        progressDialog.show();
        firebaseAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful())
                {
                    if (firebaseAuth.getCurrentUser().isEmailVerified())
                    {
                        String user_id=firebaseAuth.getCurrentUser().getUid();
                        DatabaseReference databaseReference= FirebaseDatabase.getInstance().getReference()
                                .child("users").child("customers").child(user_id);
                        databaseReference.setValue(true);
                        Toast.makeText(CustomerLoginActivity.this
                                , "successfully logged in",Toast.LENGTH_SHORT).show();

                        finish();
                        startActivity(new Intent(CustomerLoginActivity.this,CustomerMapsActivity.class));
                    }
                    else
                    {
                        Toast.makeText(CustomerLoginActivity.this, ""+task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
                else
                {
                    Toast.makeText(CustomerLoginActivity.this, ""+task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
                progressDialog.dismiss();

            }
        });

    }

    private void ResetPassword() {
        String email=txtemail.getText().toString().trim();
        if (TextUtils.isEmpty(email))
        {
            Toast.makeText(this, "please enter email", Toast.LENGTH_SHORT).show();
            return;
        }
        progressDialog.setMessage("loading....");
        progressDialog.show();
        firebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful())
                {
                    Toast.makeText(CustomerLoginActivity.this, "check your email for password reset", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    Toast.makeText(CustomerLoginActivity.this, ""+task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
                progressDialog.dismiss();

            }
        });

    }
}
