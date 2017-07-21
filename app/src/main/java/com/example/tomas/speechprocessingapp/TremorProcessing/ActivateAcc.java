package com.example.tomas.speechprocessingapp.TremorProcessing;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.FrameLayout;

import com.example.tomas.speechprocessingapp.MainActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by TOMAS on 20/04/2017.
 */

public class ActivateAcc  extends Thread implements SensorEventListener {
    private Sensor mAcc;
    private double[] linear_acceleration = new double[3];
    private String data_sensors="";
    private SensorManager mSensorManager;
    private String pathData;
    private String format;
    private  String accx,accy,accz;
    private File f;
    private OutputStreamWriter fout;
    private double[] gravity = {0,0,0};

    //private Context context;

    //Use internal sensors
    public ActivateAcc(Context context, SensorManager mSensorManager)
    {
        //super(context);
        this.mSensorManager = mSensorManager;
        mAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // In this example, alpha is calculated as t / (t + dT),
        // where t is the low-pass filter's time-constant and
        // dT is the event delivery rate.

        final double alpha = 0.8;
        Sensor sensor = event.sensor;

        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            //gravity = {0, 0, 0};
            // Isolate the force of gravity with the low-pass filter.
            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

            // Remove the gravity contribution with the high-pass filter.

            linear_acceleration[0] = event.values[0] - gravity[0];
            linear_acceleration[1] = event.values[1] - gravity[1];
            linear_acceleration[2] = event.values[2] - gravity[2];


            accx = String.valueOf(linear_acceleration[0]);
            accy = String.valueOf(linear_acceleration[1]);
            accz = String.valueOf(linear_acceleration[2]);
            //data_sensors = data_sensors + accx + '\t' + accy + '\t' + accz + "\n\r";
            generateNoteOnSD( accx + '\t' + accy + '\t' + accz + "\n\r");
            //Log.e("Accelerometer", data_sensors);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void generateNoteOnSD(String data_sensors){

        try {
            fout.write(data_sensors);
        } catch (Exception e) {
            e.printStackTrace();
        }
                /*
                outputStream = openFileOutput    (Environment.getExternalStorageDirectory().getAbsolutePath() + path_name+fileName, Context.MODE_PRIVATE);

                outputStream.write(data_sensors.getBytes());
                outputStream.close();*/

    }

    public  void startAcc(String pathData)
    {
        this.pathData = pathData;
        //this.format = format;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("ddMMyyyyhhmmss");
        Date date = new Date();
        //String format = simpleDateFormat.format(date);
        format = simpleDateFormat.format(date);
        f = new File(pathData + File.separator + "ACC", "testRecord" + format + ".txt");
        try {
        fout = new OutputStreamWriter(new FileOutputStream(f));
        } catch (Exception e) {
            e.printStackTrace();
        }
        mSensorManager.registerListener(this, mAcc, SensorManager.SENSOR_DELAY_GAME);
        //generateNoteOnSD(pathData,"testRecord" + format + ".txt",data_sensors);
    }
    public  void stopAcc()
    {
        mSensorManager.unregisterListener(this, mAcc);
        try {
        fout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}