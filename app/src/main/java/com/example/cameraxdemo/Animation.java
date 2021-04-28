package com.example.cameraxdemo;

import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class Animation {
    static AlertDialog alertDialog;

    public static void start(AppCompatActivity activity) {
        LayoutInflater inflater = (activity).getLayoutInflater();
        alertDialog = new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
                .setView(inflater.inflate(R.layout.progress_bar, null))
                .setCancelable(false)
                .create();
        alertDialog.show();
    }

    public static void stop(AppCompatActivity activity) {
        alertDialog.dismiss();
    }
}
