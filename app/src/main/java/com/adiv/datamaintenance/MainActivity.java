package com.adiv.datamaintenance;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.adiv.DataMaintenance;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import static java.util.jar.Pack200.Packer.ERROR;

public class MainActivity extends AppCompatActivity {
    TextView availableRam, totalRam, availableIntMem, availableExtMem, totalIntMem, totalExtMem;
    TextView isCing, usbCing, acCing, batteryTemp;
    float batTemp;
    String totalInternMem, availInternMem, totalExternMem, availExternMem;
    boolean isCharging, usbCharge, acCharge;
    long totalMemory, availMemory;
    MyAsyncTask mAsync = null;
    Timer timer = null;
    TimerTask task = null;

    @SuppressLint("StaticFieldLeak")
    private class MyAsyncTask extends AsyncTask<String, Void, String> {
        MyAsyncTask(){
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        protected String doInBackground(String... params) {
            //Background operation in a separate thread
            //Write here your code to run in the background thread

            //calculate here whatever you like
            totalInternMem = getTotalInternalMemorySize();
            totalExternMem = getTotalExternalMemorySize();
            availInternMem = getAvailableInternalMemorySize();
            availExternMem = getAvailableExternalMemorySize();

            ActivityManager actManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            actManager.getMemoryInfo(memInfo);
            totalMemory = memInfo.totalMem;
            availMemory = memInfo.availMem;

            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = DataMaintenance.getAppContext().registerReceiver(null, ifilter);

            // Are we charging / charged?
            assert batteryStatus != null;
            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;

            // How are we charging?
            int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
            acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

            batTemp = (float)(batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0))/10;

            return null;
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(String result) {
            //Called on Main UI Thread. Executed after the Background operation, allows you to have access to the UI
            availableRam.setText(formatSize(availMemory));
            totalRam.setText(formatSize(totalMemory));
            availableIntMem.setText(availInternMem);
            totalIntMem.setText(totalInternMem);
            availableExtMem.setText(availExternMem);
            totalExtMem.setText(totalExternMem);
            isCing.setText(String.valueOf(isCharging));
            usbCing.setText(String.valueOf(usbCharge));
            acCing.setText(String.valueOf(acCharge));
            batteryTemp.setText(batTemp +" "+ (char) 0x00B0 +"C");
        }

        @Override
        protected void onPreExecute() {
            //Called on Main UI Thread. Executed before the Background operation, allows you to have access to the UI
        }
    }

    public static boolean externalMemoryAvailable() {
        return android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static String getAvailableInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        return formatSize(availableBlocks * blockSize);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static String getTotalInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        return formatSize(totalBlocks * blockSize);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static String getAvailableExternalMemorySize() {
        if (externalMemoryAvailable()) {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSizeLong();
            long availableBlocks = stat.getAvailableBlocksLong();
            return formatSize(availableBlocks * blockSize);
        } else {
            return ERROR;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static String getTotalExternalMemorySize() {
        if (externalMemoryAvailable()) {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSizeLong();
            long totalBlocks = stat.getBlockCountLong();
            return formatSize(totalBlocks * blockSize);
        } else {
            return ERROR;
        }
    }

    public static String formatSize(long size) {
        String suffix = null;

        if (size >= 1024) {
            suffix = "KB";
            size /= 1024;
            if (size >= 1024) {
                suffix = "MB";
                size /= 1024;
            }
        }

        StringBuilder resultBuffer = new StringBuilder(Long.toString(size));

        int commaOffset = resultBuffer.length() - 3;
        while (commaOffset > 0) {
            resultBuffer.insert(commaOffset, ',');
            commaOffset -= 3;
        }

        if (suffix != null) resultBuffer.append(suffix);
        return resultBuffer.toString();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Handler handler = new Handler();
        timer = new Timer();
        task = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        MyAsyncTask mAsync = new MyAsyncTask();
                        mAsync.execute();
                    }
                });
            }
        };
        timer.schedule(task, 0, 1000); //Every 1 second

        availableIntMem = findViewById(R.id.textView3);
        totalIntMem = findViewById(R.id.textView6);
        availableExtMem = findViewById(R.id.textView7);
        totalExtMem = findViewById(R.id.textView9);
        availableRam = findViewById(R.id.textView13);
        totalRam = findViewById(R.id.textView2);
        isCing = findViewById(R.id.textView19);
        usbCing = findViewById(R.id.textView20);
        acCing = findViewById(R.id.textView21);
        batteryTemp = findViewById(R.id.textView26);

        SensorManager sensorManager =
                (SensorManager) getSystemService(SENSOR_SERVICE);

        final Sensor proximitySensor =
                sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        final ProgressBar prox = findViewById(R.id.progressBar);
        final ProgressBar prox2 = findViewById(R.id.progressBar2);
        // Create listener
        SensorEventListener proximitySensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if(sensorEvent.values[0] < proximitySensor.getMaximumRange()) {
                    // Detected something nearby
                   // getWindow().getDecorView().setBackgroundColor(Color.RED);
                    prox.setProgress(100);
                    prox2.setProgress(100);
                } else {
                    // Nothing is nearby
                    //getWindow().getDecorView().setBackgroundColor(Color.GREEN);
                    prox.setProgress(0);
                    prox2.setProgress(0);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };

        // Register it, specifying the polling interval in
        // microseconds
        sensorManager.registerListener(proximitySensorListener,
                proximitySensor, 2 * 1000 * 1000);
    }
}
