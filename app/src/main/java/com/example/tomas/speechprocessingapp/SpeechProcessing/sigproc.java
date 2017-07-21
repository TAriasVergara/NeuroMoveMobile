package com.example.tomas.speechprocessingapp.SpeechProcessing;

import com.example.tomas.speechprocessingapp.SpeechProcessing.features.FFT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.abs;
import static java.util.Arrays.copyOfRange;

/**
 * Created by TOMAS on 18/02/2017.
 */

public class sigproc {
    public sigproc(){
    }

    public float[] normalizesig(float sig[])
    {
        float msig = meanval(sig);
        //Remove DC
        float[] Nsig = new float[sig.length];
        for (int i=0;i<sig.length;i++)
        {
            Nsig[i] = sig[i]-msig;
        }
        Nsig = sig;

        //Scale between -1 and 1
        float[] temp = Arrays.copyOf(Nsig,Nsig.length);;
           Arrays.sort(temp);
        float max = temp[temp.length-1];
        max = Math.abs(max);
        float min = temp[0];
        min = Math.abs(min);
        if (max<min)
        {
           max = min;
        }

        for (int i=0;i<Nsig.length;i++)
        {
            Nsig[i] = Nsig[i]/max;
        }
        return Nsig;
    }

    public float meanval(float[] sig)
    {
        //Find DC level
        float msig=0;
        for (int i=0;i<sig.length;i++)
        {
            msig = msig+sig[i];
        }
        msig = msig/sig.length;
        return msig;
    }

    //Apply windowing
    public float[] makeWindow(float sig[],int selwin) {
    /*    // Make a blackman window:
        // w(n)=0.42-0.5cos{(2*PI*n)/(N-1)}+0.08cos{(4*PI*n)/(N-1)};
        window = new double[n];
        for(int i = 0; i < window.length; i++)
            window[i] = 0.42 - 0.5 * Math.cos(2*Math.PI*i/(n-1))
                    + 0.08 * Math.cos(4*Math.PI*i/(n-1));
    */
        float[] window = new float[sig.length];
        for(int i = 0; i < window.length; i++)
            if (selwin==0) {//Make a hamming window
                window[i] = sig[i] * ((float) (0.54 - 0.46 * Math.cos(2 * Math.PI * i / (sig.length - 1))));
            }
            else if(selwin==1){//Make hanning window
                window[i] = sig[i] * ((float) (0.5 - 0.5* Math.cos(2 * Math.PI * i / (sig.length - 1))));
        }
        return window;
    }


    //Get signal spectrum
    public float[] signal_fft(float[] win_sig,int Fs){
       //signal frame with window function applied
       FFT fft = new FFT(win_sig.length,Fs);
       fft.forward(win_sig);
        float[] sig_spec = new float[fft.specSize()];
        //Get spectrum of the signal (amplitudes)
        for (int i =0;i<fft.specSize();i++) {
            sig_spec[i] = fft.getBand(i);
        }
    //---------------------------------------------------------
    //Find frequency (only to test the FFT class)
/*       int idxmax =0;
        float ampmax = 0;
        //Get maximum frequency and position in the spectrum
        for (int counter = 1; counter < sig_spec.length; counter++)
        {
            if (sig_spec[counter] > ampmax)
            {
                ampmax = sig_spec[counter];
                idxmax = counter;
            }
        }
        //Get frequency
        float freq = fft.indexToFreq(idxmax);
        Log.e("RECORDER", String.valueOf(round(freq)));*/
    //--------------------------------------------------------------------------
    return sig_spec;
}
    //Calculate difference between elements of array
    public float[] diff(float[] x)
    {
        float[] diffV = new float[x.length-1];
        for (int i=0;i< (x.length-1);i++)
        {
            diffV[i] = x[i+1]-x[i];
        }
        return diffV;
    }

    //Absolute value of array elements
    public float[] absArr(float[] x)
    {
        float[] y = new float[x.length];
        for(int i=0;i<x.length;i++)
        {
            y[i]=Math.abs(x[i]);
        }
        return y;
    }

    //Find indices in array with condition opt
    public List find(float[] x,float val,int opt)
    {
        List<Integer> res = new ArrayList<>();
        switch (opt)
        {
            //Less than
            case 0: {
                for (int i = 0; i < x.length;i++) {
                    if (x[i] < val) {
                        res.add(i);
                    }
                }
                break;
            }
            //Less or equal than
            case 1:{
                for (int i = 0; i < x.length;i++) {
                    if (x[i] <= val) {
                        res.add(i);
                    }
                }
                break;
            }
            //Greater than
            case 2:{
                for (int i = 0; i < x.length;i++) {
                    if (x[i] > val) {
                        res.add(i);
                    }
                }
                break;
            }
            //Greater or equal than
            case 3:{
                for (int i = 0; i < x.length;i++) {
                    if (x[i] >= val) {
                        res.add(i);
                    }
                }
                break;
            }
            //Equal to
            case 4:{
                for (int i = 0; i < x.length;i++) {
                    if (x[i] == val) {
                        res.add(i);
                    }
                }
                break;
            }
            //Different than
            case 5:{
                for (int i = 0; i < x.length;i++) {
                    if (x[i] != val) {
                        res.add(i);
                    }
                }
                break;
            }

        }
        return res;
    }

    /***
     * Interpolating method
     * @param start start of the interval
     * @param end end of the interval
     * @param count count of output interpolated numbers
     * @return array of interpolated number with specified count
     */
    public static float[] interpolate(float start, float end, int count) {
        if (count < 2) {
            throw new IllegalArgumentException("interpolate: illegal count!");
        }
        float[] array = new float[count + 1];
        for (int i = 0; i <= count; ++ i) {
            array[i] = start + i * (end - start) / count;
        }
        return array;
    }


    /***
     * Frame speech signal
     * @param signal speech signal
     * @param fs sampling frequency of the speech signal
     * @param winlen size of the analysis window measured in SECONDS (example:0.03)
     * @param winstep size of the steps of the analysis windows measured in SECONDS
     * @return List with the framed speech signal
     */
    public List<float[]> sigframe(float[] signal,int fs,float winlen, float winstep) {
        int numberOfsamples = (int) Math.ceil(winlen * fs);
        //Window step
        int windowshift = (int) Math.ceil(winstep * fs);
        int ini_frame = 0, end_frame = numberOfsamples;
        //Number of frames to analyze
        int numberOfframes;
        if (windowshift == 0) { //if there is no window shift
            numberOfframes = (int) (Math.floor(signal.length / numberOfsamples));
            windowshift = numberOfframes;
        } else {
            float temp = winstep / winlen;
            numberOfframes = (int) (Math.floor((signal.length / numberOfsamples) / temp));
        }
        List<float[]> frames = new ArrayList<float[]>();
        for (int i = 0; i < numberOfframes; i++) {
            float sig_frame[] = copyOfRange(signal, ini_frame, end_frame);
            //new frame
            frames.add(sig_frame);
            ini_frame = ini_frame + windowshift;
            end_frame = end_frame + windowshift;
        }
        return frames;
    }

}
