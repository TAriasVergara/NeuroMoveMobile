package com.example.tomas.speechprocessingapp.SpeechProcessing;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.util.Log;
import android.widget.Chronometer;

import com.example.tomas.speechprocessingapp.SpeechProcessing.tools.WavFileWriter;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by TOMAS on 12/05/2017.
 */

public class SpeechRec extends Thread{

    private static final int SAMPLE_RATE_HZ = 16000;//44100;
    private static final int SAMPLE_RATE_8HZ = 8000;
    private static final float CONVERSION = (float) SAMPLE_RATE_HZ / SAMPLE_RATE_8HZ;
    //private AudioRecord audioRecord = null;
    //private DataOutputStream dataOutputStream = null;
    //private File filePcm = null;
    //private File fileWav = null;
    //private String format = null;
    private String pathData = null;
    private boolean recflag;
    //private int minBufferSize = 0;
    private Context context;

    public SpeechRec(Context context,String pathdata)
    {
        this.context = context;
        this.pathData = pathdata;
    }

    @Override
    public void run() {
        recflag = true;
        startSpeechRecording();
    }//End run


    private void startSpeechRecording() {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("ddMMyyyyhhmmss");
            Date date = new Date();
            //String format = simpleDateFormat.format(date);
            String format = simpleDateFormat.format(date);
            File filePcm = new File(pathData + File.separator +"testRecord" + format + ".pcm");
            File fileWav = new File(pathData + File.separator + "WAV" + File.separator +"testRecord" + format + ".wav");
            //dataouputStream is the audio signal converted to bytes

        try {
            filePcm.createNewFile();
            OutputStream outputStream = new FileOutputStream(filePcm);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);

            //Get minimum buffer size
             int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_HZ,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            minBufferSize *= CONVERSION;

            //Create new instance to record audio
            AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
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
                if (suppressor.getEnabled()){
                Log.e("CALLRECORDER", "Set supressor");
            }
            else{
                    Log.e("CALLRECORDER", "Not Set supressor");
                }
            }

            if (canceler != null) {
                canceler.setEnabled(true);
                if (canceler.getEnabled()) {
                    Log.e("CALLRECORDER", "Set canceler");
                }
                else
                {
                    Log.e("CALLRECORDER", "Not Set canceler");
                }
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

            audioRecord.stop();
            audioRecord.release();
            //try {
            dataOutputStream.close();
            //} catch (IOException e) {
              //  Log.d("ERROR", e.getMessage());
               // e.printStackTrace();
            //}

            if (gainControl != null) {
                gainControl.release();
            }

            if (suppressor != null) {
                suppressor.release();
            }

            if (canceler != null) {
                canceler.release();
            }

            //  Toast.makeText(this, "Recording stoped", Toast.LENGTH_LONG).show();
            //Write the captured data into a .wav file
            WavFileWriter wavFileWriter = new WavFileWriter(SAMPLE_RATE_HZ, filePcm, fileWav,context, format);
            wavFileWriter.start();
            //recordingThread.start();
        } catch (IOException e) {
            Log.d("ERROR", e.getMessage());
            e.printStackTrace();
        }//Exception
    }//endStartRecording

    public void stopSpeechRecording() {
        recflag = false;
        Log.e("RECORDER", "OFF");
    }


    private float[] resampleTo8kHz(float[] audioData) {
        float[] resampled = new float[(int) ((float) audioData.length / CONVERSION)];
        for (int i = 0; i < resampled.length; i++) {
            //Interpolation
            float indexAudioData = (float) i * CONVERSION;
            float index0 = (float) Math.floor(indexAudioData);
            float index1 = (float) Math.ceil(indexAudioData);
            float value = (float) ((index1 - indexAudioData) * audioData[(int) index0] + (indexAudioData - index0) * audioData[(int) index0]);
            resampled[i] = value;
        }
        return resampled;
    }
}
