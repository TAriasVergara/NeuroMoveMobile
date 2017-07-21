package com.example.tomas.speechprocessingapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.example.tomas.speechprocessingapp.SpeechProcessing.tools.WavFileWriter;
import com.example.tomas.speechprocessingapp.TremorProcessing.ActivateAcc;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.os.SystemClock.sleep;
import static android.provider.AlarmClock.EXTRA_MESSAGE;
import static java.util.Arrays.copyOfRange;

public class MainActivity extends AppCompatActivity{
    //private static final int SAMPLE_RATE_HZ = 16000;//44100;
    //private static final int SAMPLE_RATE_8HZ = 8000;
    //private static final double CONVERSION = (double) SAMPLE_RATE_HZ / SAMPLE_RATE_8HZ;
    //private AudioRecord audioRecord = null;
    //private DataOutputStream dataOutputStream = null;
    //private File filePcm = null;
    //private File fileWav = null;
    //File file = null;
    //private String format = null;
    //Thread recordingThread = null;
    private boolean recflag = false;
    //private int minBufferSize = 0;
    String pathData = null;
    private Chronometer rectimer= null;
    private int facc=1,fspee = 1;
    //Context context;
    private SensorManager mSensorManager;
//    public Sensor mSensorAcc;
 //   public boolean internal_sensor = true;
 //   private double[] linear_acceleration = new double[3];
 //   public String data_sensors, data_sensorsext;
    public ActivateAcc acc;
    public SpeechRec spe;
    //public SetAcc acc;

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

    /*
    //Use mic to Record Speech
    private void setMicrophone()
    {


    }

    public void startSpeechRecording() {
        try {
            //dataouputStream is the audio signal converted to bytes
            OutputStream outputStream = new FileOutputStream(filePcm);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            dataOutputStream = new DataOutputStream(bufferedOutputStream);

            //int times = 0;

            //Toast.makeText(this, "Recording", Toast.LENGTH_LONG).show();
            //This thread is necessary so the button remains unblocked
            recordingThread = new Thread(new Runnable() {
                @Override
                public void run() {

                    //Get minimum buffer size
                    minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_HZ,
                            AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT);
                    minBufferSize *= CONVERSION;

                    //Create new instance to record audio
                    audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                            SAMPLE_RATE_HZ,
                            AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            minBufferSize);

                    AcousticEchoCanceler canceler = AcousticEchoCanceler.create(audioRecord.getAudioSessionId());
                    NoiseSuppressor suppressor = NoiseSuppressor.create(audioRecord.getAudioSessionId());
                    AutomaticGainControl gainControl = AutomaticGainControl.create(audioRecord.getAudioSessionId());

                    if (gainControl != null) {
                        gainControl.setEnabled(true);
                        Log.e("CALLRECORDER", "Set gainControl");
                    }

                    if (suppressor != null) {
                        suppressor.setEnabled(true);
                        Log.e("CALLRECORDER", "Set supressor");
                    }

                    if (canceler != null) {
                        canceler.setEnabled(true);
                        Log.e("CALLRECORDER", "Set canceler");
                    }

                    audioRecord.startRecording();
                    //Variable in which data will be captured
                    short[] audioData = new short[minBufferSize];
                    Log.e("RECORDER", "ON");
                    int var = 0;
                    while (recflag) {
                       // var = var + 1;
                        //Log.e("RECORDER", String.valueOf(var));
                        //time = (int) (System.currentTimeMillis() - time);
                        //times = time / 1000;
                        //"Pull" the data from the AudioRecorder object (audioRecord). Read audio data from
                        //the selected audio source on audioRecord (Phone's Microphone)
                        int numrec = audioRecord.read(audioData, 0, minBufferSize);
                        try {
                            for (int i = 0; i < numrec; i++)
                                dataOutputStream.writeShort(audioData[i]);
                        } catch (IOException e) {
                            Log.d("ERROR", e.getMessage());
                            e.printStackTrace();
                        }//endException
                    }//EndWhile

                    if (gainControl != null) {
                        gainControl.release();
                    }

                    if (suppressor != null) {
                        suppressor.release();
                    }

                    if (canceler != null) {
                        canceler.release();
                    }
                }//End run
            });//endThread
            recordingThread.start();
        } catch (IOException e) {
            Log.d("ERROR", e.getMessage());
            e.printStackTrace();
        }//Exception
    }//endStartRecording

    public void stopSpeechRecording() {
        audioRecord.stop();
        audioRecord.release();
        try {
            dataOutputStream.close();
        } catch (IOException e) {
            Log.d("ERROR", e.getMessage());
            e.printStackTrace();
        }
        Log.e("RECORDER", "OFF");
      //  Toast.makeText(this, "Recording stoped", Toast.LENGTH_LONG).show();
        //Write the captured data into a .wav file
        WavFileWriter wavFileWriter = new WavFileWriter(SAMPLE_RATE_HZ, filePcm, fileWav, this, format);
        wavFileWriter.start();
    }
*/
}
