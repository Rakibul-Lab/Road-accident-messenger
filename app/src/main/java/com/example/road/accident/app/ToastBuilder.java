package com.example.road.accident.app;

import android.content.Context;
import android.widget.Toast;

public class ToastBuilder {
    public static void build(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
