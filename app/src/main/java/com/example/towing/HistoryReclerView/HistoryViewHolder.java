package com.example.towing.HistoryReclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.towing.HistorySetupActivity;
import com.example.towing.R;

public class HistoryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    TextView rideId,time;
    public HistoryViewHolder(@NonNull View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);
        rideId=itemView.findViewById(R.id.rideId);
        time=itemView.findViewById(R.id.time);
    }

    @Override
    public void onClick(View v) {
        Intent intent=new Intent(v.getContext(),HistorySetupActivity.class);
        Bundle b=new Bundle();
        b.putString("rideId",rideId.getText().toString());
        intent.putExtras(b);
        v.getContext().startActivity(intent);
    }
}
