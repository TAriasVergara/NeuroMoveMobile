package com.example.tomas.speechprocessingapp;

import android.content.Intent;
//import android.icu.text.SimpleDateFormat;
import android.media.MediaPlayer;
import android.os.Build;
import android.provider.AlarmClock;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.tomas.speechprocessingapp.SpeechProcessing.tools.Plot2D;
import com.example.tomas.speechprocessingapp.SpeechProcessing.Speaker_Adaptation;
import com.example.tomas.speechprocessingapp.SpeechProcessing.features.f0detector;
import com.example.tomas.speechprocessingapp.SpeechProcessing.featureExtraction;
import com.example.tomas.speechprocessingapp.SpeechProcessing.sigproc;

import java.text.SimpleDateFormat;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static java.lang.Math.floor;
import static java.lang.Math.round;
import static java.util.Arrays.copyOfRange;

public class DisplayResults extends AppCompatActivity {
    boolean mStartPlaying = true;
    private MediaPlayer mPlayer = null;
    private static final String LOG_TAG = "AudioRecordTest";
    File file = null;
    File[] FilesRec = null;
    private String WAVspath = null;
    private double Bha = 0;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_results);

        Intent intent = getIntent();
        WAVspath = intent.getStringExtra(AlarmClock.EXTRA_MESSAGE);

        //Get number of samples (infosig[0]) and sampling frequency (infosig[1])
        int infosig[] = getdatainfo(WAVspath), Fs = infosig[1];
        //Read WAV audio file (as a float array)
        float signal[] = readWAV(infosig[0]);

        //Normalize signal
        sigproc SigProc = new sigproc();
        signal = SigProc.normalizesig(signal);

        f0detector F0cont = new f0detector();
        float[] f0 = F0cont.sig_f0(signal,Fs);

        //Voiced
        List VoicedSeg = F0cont.voiced(f0,signal);
/*
        //Unvoiced
        //List UnvoicedSeg = F0cont.unvoiced(f0,signal);

        List<float[]> feats = new ArrayList<float[]>();
        for(int i=0;i<VoicedSeg.size();i++)
        {
            float[] sigVUV = (float[]) VoicedSeg.get(i);

            if (sigVUV.length!=0)
            {
                //Feature extraction
                featureExtraction compute = new featureExtraction();
                List<float[]> temp = compute.feat_ext(sigVUV, Fs);
                for (int j = 0; j < temp.size(); j++)
                {
                    feats.add(temp.get(j));
                }
        }
        }*/

        //Perform MAP adaptation
        //Readfile
        //Speaker_Adaptation spkr = new Speaker_Adaptation(feats);
        //Bha = spkr.spkrdist();//Compute bhattacharyya distance.
        Bha = 0;//Compute bhattacharyya distance.

        //Set sound file info
        setinfo(WAVspath, Fs, infosig);

        //Plot bark
        //float bbenergy[] = bbe;
        //float y[] = bbenergy;
        float[] y = f0;
        //float[] y = signal;
        int l = y.length;
        float xvalues[] = new float[l];
        for (int i = 0; i < l; i++) {
            xvalues[i] = i;
        }
        Plot2D plt = new Plot2D(this, xvalues, y, 1);
        LinearLayout ll = (LinearLayout) findViewById(R.id.plotarea1);
        ll.addView(plt);
    }

    //Get number of samples and sampling frequency from wav file.
    public int[] getdatainfo(String path) {
        BufferedInputStream WAVHeader;
        //file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),"testRecord04112016115819.wav");
        file = new File(path);
        try {
            WAVHeader = new BufferedInputStream(new FileInputStream(file));
            //Get sampling frequency.
            byte[] HeaderWav = new byte[44];
            WAVHeader.read(HeaderWav, 0, 44);
            byte fs[] = copyOfRange(HeaderWav, 24, 28);
            int Fs = ByteBuffer.wrap(fs).order(ByteOrder.LITTLE_ENDIAN).getInt();
            byte size[] = copyOfRange(HeaderWav, 40, 44);
            int Size = ByteBuffer.wrap(size).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int infosig[] = {Size / 2, Fs};
            return infosig;
        } catch (Exception e) {
            e.printStackTrace();
            return new int[0];
        }
    }

    //read .WAV file and convert amplitudes.
    public float[] readWAV(int size) {
        BufferedInputStream bufferedInputStream;
        int samplesPerValue = 1;
        int bf = 2;//Number of bytes that represent the audio data captured with a resolution of 16-bits
        try {
            bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
            bufferedInputStream.skip(44); //Skip header of .wav
            byte[] byteBuffer = new byte[samplesPerValue * bf];
            float maxS = 32767; //Maximum positive value used to represent 16-bit signed data (2 bytes)
            int ind = 0;
            float sig[] = new float[size];
            while ((bufferedInputStream.read(byteBuffer, 0, samplesPerValue * bf)) > 0) {
                ByteBuffer littleEndianBuffer = ByteBuffer.wrap(byteBuffer);
                littleEndianBuffer.order(ByteOrder.LITTLE_ENDIAN);
                sig[ind] = littleEndianBuffer.getShort() / maxS;
                ind = ind + 1;
            }
            return sig;
        } catch (Exception e) {
            e.printStackTrace();
            return new float[0];
        }
    }
    //Resample data to 8kHz
    private float[] resampleTo8kHz(float[] audioData,float Fs) {
        float CONVERSION = Fs/8000f;
        float[] resampled = new float[(int) ((float) audioData.length / CONVERSION)];
        for (int i = 0; i < resampled.length; i++) {
            //Interpolation
            float indexAudioData = (float) i * CONVERSION;
            float index0 = (float) Math.floor(indexAudioData);
            float index1 = (float) Math.ceil(indexAudioData);
            float value = (index1 - indexAudioData) * audioData[(int) index0] + (indexAudioData - index0) * audioData[(int) index0];
            resampled[i] = value;
        }
        return resampled;
    }

    //Set file info on the screen
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void setinfo(String WAVspath, int Fs, int[] infosig) {

        //Display data info
        TextView textView = (TextView) findViewById(R.id.fname_val);
        //File name
        File wavf = new File(WAVspath);
        textView.setText(wavf.getName());

        //Sampling frequency
        textView = (TextView) findViewById(R.id.fs_val);
        textView.setText(Float.toString(Fs / (float) 1000) + "kHz");

        //Set date
        SimpleDateFormat lastm = new SimpleDateFormat("dd");
        String dd = lastm.format(wavf.lastModified());
        SimpleDateFormat lastm2 = new SimpleDateFormat("MM");
        String mm = lastm2.format(wavf.lastModified());
        SimpleDateFormat lastm3 = new SimpleDateFormat("yyyy");
        String yy = lastm3.format(wavf.lastModified());
        textView = (TextView) findViewById(R.id.fdate_val);
        textView.setText(dd + "/" + mm + "/" + yy);


        //Set duration
        int s = (int) Math.ceil(infosig[0] / Fs);
        int m = (int) floor(s / 60);
        s = s - (m * 60);
        String min, sec;
        textView = (TextView) findViewById(R.id.fdur_val);
        if (m >= 10) {
            min = Integer.toString(m);
        } else {
            min = "0" + Integer.toString(m);
        }
        if (s >= 10) {
            sec = Integer.toString(s);
        } else {
            sec = "0" + Integer.toString(s);
        }
        textView.setText(min + ":" + sec);

        //Set model distance
        textView = (TextView) findViewById(R.id.fdist_val);
        textView.setText(Integer.toString(Math.round((float)  Bha)));

    }


    public void plays(View view) {
        Button pb = (Button) findViewById(R.id.playB);
        onPlay(mStartPlaying);
        if (mStartPlaying) {
            pb.setText("Stop");
        } else {
            pb.setText("Start");
        }
        mStartPlaying = !mStartPlaying;
    }

    private void onPlay(boolean start) {
        if (start) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(WAVspath);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaPlayer.stop();
                Button pb = (Button) findViewById(R.id.playB);
                pb.setText("Start");
                mStartPlaying = true;
                if (mediaPlayer != null) {
                    mPlayer.release();
                }

            }
        });

    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
    }

}
