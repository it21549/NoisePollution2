package gr.hua.stapps.android.noisepollutionapp;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.view.View;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class NoiseRecorder  {
    public static long REC_TIME = 10000;
    public static double reference = 0.00002;
    public static int MAX_DB = 91;

    public HashMap getNoiseLevel() {
        HashMap<String, String> results = new HashMap<>();
        int bufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_DEFAULT,AudioFormat.ENCODING_PCM_16BIT);

        //Increasing buffer size from 1 to 10 seconds
        bufferSize = bufferSize*120;
        final AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

        short[] data = new short[bufferSize];
        double average = 0.0;
        recorder.startRecording();
        System.out.println("Time started " + Calendar.getInstance().getTimeInMillis());
        //Recording data
        recorder.read(data, 0, bufferSize);
        double max = 0.0;
        Timer tm = new Timer();
        tm.schedule(new TimerTask() {
            @Override
            public void run() {
                recorder.stop();
                recorder.release();
            }
        }, REC_TIME);
        System.out.println("Time stopped " + Calendar.getInstance().getTimeInMillis());
        //recorder.stop();
        for(short s : data) {
            if(s>0)
                average +=Math.abs(s);
            else
                bufferSize--;
            if(s> max)
                max = s;
        }
        double x = average/bufferSize;

        //Stathis Algorithm
        double a = 20*Math.log10(x/32768) + MAX_DB;
        double m = 20*Math.log10(max/32768) + MAX_DB;
        results.put("Average", String.format("%.2f", a));
        results.put("Maximum", String.format("%.2f", m));

        double dba=0;
        double dbm=0;
        //Calculating the Pascal pressure based in the idea that the max amplitude (between 0 and 32767) is relative to the pressure

        double pressureA = x/51805.5336; //the value 51805.5336 can be derived from assuming that x=32767 -> 0.6325 Pa and x=1 -> 0.00002 Pa
        double pressureM = max/51805.5336;
        dba = (20*Math.log10(pressureA/reference));
        dbm = (20*Math.log10(pressureM/reference));
        results.put("Algorithm 1 Average", String.format("%.2f", dba));
        results.put("Algorithm 2 Maximum", String.format("%.2f", dbm));
        return results;
    }
}
