package com.adiv;

import android.app.Application;
import android.content.Context;

public class DataMaintenance extends Application {
    private static Context context;

    public void onCreate() {
        super.onCreate();
        DataMaintenance.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return DataMaintenance.context;
    }
}
