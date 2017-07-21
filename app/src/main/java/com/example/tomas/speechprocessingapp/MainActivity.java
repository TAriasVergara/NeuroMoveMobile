package com.example.tomas.speechprocessingapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Chronometer;
import android.widget.Toast;

import com.example.tomas.speechprocessingapp.SpeechProcessing.SpeechRec;
import com.example.tomas.speechprocessingapp.TremorProcessing.ActivateAcc;

import java.io.File;

import static android.provider.AlarmClock.EXTRA_MESSAGE;
import static java.util.Arrays.copyOfRange;

public class MainActivity extends AppCompatActivity{
    private boolean recflag = false;
    String pathData = null;
    private Chronometer rectimer= null;
    private int facc=1,fspee = 1;
    private SensorManager mSensorManager;
    public ActivateAcc acc;
    public SpeechRec spe;

    //private int time = (int) System.currentTimeMillis();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Create app folders to store data
        pathData = Environment.getExternalStorageDirectory() + File.separator + "AppSpeechData";
        File datafolder = new File(pathData);
        boolean checkF = datafolder.exists();
        if (checkF == false) {
            datafolder.mkdirs();
            datafolder = new File(pathData + File.separator + "WAV");
            datafolder.mkdirs();
            datafolder = new File(pathData + File.separator + "UBM");
            datafolder.mkdirs();
            datafolder = new File(pathData + File.separator + "ACC");//Folder to save data form acceler
            datafolder.mkdirs();
        }


        // Get an instance of the SensorManager. ACCELEROMETER
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //acc = new SetAcc(this);
        acc =  new ActivateAcc(this,mSensorManager);

        //Chronometer
        rectimer = (Chronometer) findViewById(R.id.chrono);
    }

    public  void RecList(View view){
        Intent intent = new Intent(this, RecordingsList.class);
        intent.putExtra(EXTRA_MESSAGE, pathData+File.separator+"WAV");
        startActivity(intent);
    }

    public void onCheckboxClicked(View view) {
        // Is the view now checked?
        boolean checked = ((CheckBox) view).isChecked();

        // Check which checkbox was clicked
        switch(view.getId()) {
            case R.id.checkbox_motion:
                if (checked==true) facc = 1;
                else facc=0;
                break;
            case R.id.checkbox_speech:
                if (checked==true) fspee = 1;
                else fspee = 0;
                break;
        }
    }

    //Start recording when the "Record" button is clicked
    public void Startrec(View view) {
        //Request permissions to record and audio files
        int record_perm = RequestPermissions();
        Button pb = (Button) findViewById(R.id.rec);
        rectimer.setBase(SystemClock.elapsedRealtime());
        if (record_perm == PackageManager.PERMISSION_GRANTED) {

            recflag = !recflag;
            if (recflag) {
                pb.setText("Stop");
                rectimer.start();
                //Microphone
                if (fspee==1) {
                  //  setMicrophone();
                    //startSpeechRecording();
                    spe = new SpeechRec(this,pathData);
                    spe.start();
                }
                //Accelerometer
                if (facc==1) {
                    acc.startAcc(pathData);
                }

            } else {
                pb.setText("Start");
                rectimer.stop();
                rectimer.setBase(SystemClock.elapsedRealtime());
                if (fspee==1) {
                    //stopSpeechRecording();//Microphone

                   spe.stopSpeechRecording();
                }
                if (facc==1) {
                    acc.stopAcc();
                }
            }
        } else {
            Toast.makeText(this, "You need to grant permission to record and store audio files", Toast.LENGTH_LONG).show();
        }
    }

    //Request for permission Android >= 6
    private static final int REQUEST_PERMISSIONS = 0;
    private int RequestPermissions() {
        //----------------------------------------------------------------
        //Request permission to RECORD AUDIO and STORE DATA on the phone
        //----------------------------------------------------------------
        //The app had permission to record audio?
        int audio_perm = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        //int storage_per = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (audio_perm != PackageManager.PERMISSION_GRANTED) {
            //If there is not permission to record and store audio files, then ask to the user for it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.MODIFY_AUDIO_SETTINGS}, REQUEST_PERMISSIONS);
        }
        return audio_perm;
    }//END REQUEST PERMISSION

}
