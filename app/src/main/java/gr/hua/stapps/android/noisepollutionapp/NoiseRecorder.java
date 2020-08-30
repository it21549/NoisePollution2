package gr.hua.stapps.android.noisepollutionapp;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class NoiseRecorder {
    private static final String LOG_TAG = "NoiseRecorder";

    public void setREC_TIME(long REC_TIME) {
        this.REC_TIME = REC_TIME;
    }

    public void setBUF(int BUF) {
        this.BUF = BUF;
    }

    public int BUF = 120;
    public long REC_TIME = 10000;
    public static double reference = 0.00002;
    public static int MAX_DB = 95;
    public AudioRecord recorder;
    private int minBufferSize;


    public NoiseRecorder() {
        minBufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
        recorder.startRecording();
        Log.i(LOG_TAG, "Recorder created");
    }

    public HashMap<String, Double> startRec() {
        final short[] data = new short[minBufferSize];
        System.out.println("Time started " + Calendar.getInstance().getTimeInMillis());
        int read_error = recorder.read(data, 0, data.length);
        System.out.println("Time stopped " + Calendar.getInstance().getTimeInMillis());
        System.out.println(read_error);
        return computeDecibels(data);
    }

    private HashMap<String, Double> computeDecibels(short[] data) {

        HashMap<String, Double> results = new HashMap<>();
        double average = 0.0;
        int count = 0;
        for (short s : data) {
            if (s!=0) {
                average += Math.abs(s);
                count++;
            }
        }

        System.out.println("Data length = " + data.length + " minbuffer: " + minBufferSize + "|" + average + "|" + count);

        double x = average/count;

        double a = 20*Math.log10(x/32768) + MAX_DB;
        double dba;
        double pressureA = x/51805.5336;
        dba = (20*Math.log10(pressureA/reference));

        results.put("Average", a);
        results.put("Algorithm 1 Average", dba);
        //recorder.stop();
        return results;
    }
    public void stopRec() {
        recorder.stop();
        recorder.release();
        Log.i(LOG_TAG, "Recorder stopped");
    }


    public HashMap getNoiseLevelAverage() {

        //android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

        HashMap<String, Double> results = new HashMap<>();
        int minBufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);

        //Increasing buffer size from 1 to 10 seconds
        int bufferSize = minBufferSize*BUF;
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
        int count = 0;
        for(short s : data) {
            if(s>0) {
                average += Math.abs(s);
            }else {
                bufferSize--;
            }
            //Log.e("short_array", String.valueOf(s));
            if(s> max)
                max = s;
        }
        System.out.println("Data length = " + data.length + "minbuffer: " + minBufferSize + "|" + bufferSize + "|" + count);
        double x = average/bufferSize;
        //double x=average;
        //Stathis Algorithm
        double a = 20*Math.log10(x/32768) + MAX_DB;
        double m = 20*Math.log10(max/32768) + MAX_DB;
        results.put("Average", a);
        results.put("Maximum", m);

        double dba;
        double dbm;
        //Calculating the Pascal pressure based in the idea that the max amplitude (between 0 and 32767) is relative to the pressure

        double pressureA = x/51805.5336; //the value 51805.5336 can be derived from assuming that x=32767 -> 0.6325 Pa and x=1 -> 0.00002 Pa
        double pressureM = max/51805.5336;
        dba = (20*Math.log10(pressureA/reference));
        dbm = (20*Math.log10(pressureM/reference));
        results.put("Algorithm 1 Average", dba);
        results.put("Algorithm 2 Maximum", dbm);
        return results;
    }
}
