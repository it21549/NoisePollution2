package gr.hua.stapps.android.noisepollutionapp;

import static gr.hua.stapps.android.noisepollutionapp.CalibrationUseCase.GROUP_I;
import static gr.hua.stapps.android.noisepollutionapp.CalibrationUseCase.GROUP_II;
import static gr.hua.stapps.android.noisepollutionapp.CalibrationUseCase.GROUP_III;
import static gr.hua.stapps.android.noisepollutionapp.CalibrationUseCase.GROUP_IV;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NoiseRecorder {
    private static final String LOG_TAG = "NoiseRecorder ";
    private static final String BRAND = Build.BRAND;
    public static int MAX_DB = 110;
    public AudioRecord recorder;
    private int minBufferSize;
    private Double calibrationGroupI;
    private Double calibrationGroupII;
    private Double calibrationGroupIII;
    private Double calibrationGroupIV;

    @SuppressLint("MissingPermission")
    public NoiseRecorder() {
        minBufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
        recorder.startRecording();
        Log.i(LOG_TAG, "Recorder created");
        /*if(BRAND.contains("samsung"))
            MAX_DB = 95 + 4;
        else if (BRAND.contains("Xiaomi"))
            MAX_DB = 95 + 10;
        else
            MAX_DB = 95 + 9;*/
    }

    @SuppressLint("MissingPermission")
    public NoiseRecorder(double groupI, double groupII, double groupIII, double groupIV) {
        minBufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
        recorder.startRecording();
        Log.i(LOG_TAG, "Recorder created");
        this.calibrationGroupI = groupI;
        this.calibrationGroupII = groupII;
        this.calibrationGroupIII = groupIII;
        this.calibrationGroupIV = groupIV;
    }

    public Double startRec() {
        final short[] data = new short[minBufferSize];
        //System.out.println("Time started " + Calendar.getInstance().getTimeInMillis());
        int read_error = recorder.read(data, 0, data.length);
        //System.out.println("Time stopped " + Calendar.getInstance().getTimeInMillis());
        //System.out.println("read error:" + read_error);
        return computeDecibels(data);
    }

    private Double computeDecibels(short[] data) {
        //printBufferSamples(data);
        //System.out.println("Data length = " + data.length + " minbuffer: " + minBufferSize);
        //dB calculation
        double a = Utils.calculateAvg(data, 32768, MAX_DB);
        Logger.getGlobal().log(Level.INFO, LOG_TAG + "uncalibrated is " + a);
        if (a < calibrationGroupI && a > 0) {
            a += GROUP_I - calibrationGroupI;
            //Logger.getGlobal().log(Level.INFO, LOG_TAG + "GROUP_I calibrated by " + (GROUP_I - calibrationGroupI));
        } else if (a >= calibrationGroupI && a < calibrationGroupII) {
            a += GROUP_II - calibrationGroupII;
            //Logger.getGlobal().log(Level.INFO, LOG_TAG + "GROUP_II calibrated by " + (GROUP_II - calibrationGroupII));
        } else if (a >= calibrationGroupII && a < calibrationGroupIII) {
            a += GROUP_III - calibrationGroupIII;
            //Logger.getGlobal().log(Level.INFO, LOG_TAG + "GROUP_III calibrated by " + (GROUP_III - calibrationGroupIII));
        } else if (a >= calibrationGroupIII) {
            a += GROUP_IV - calibrationGroupIV;
            //Logger.getGlobal().log(Level.INFO, LOG_TAG + "GROUP_IV calibrated by " + (GROUP_IV - calibrationGroupIV));
        }
        return a;
    }

    private void printBufferSamples(short[] data) {
        for (int i = 0; i < data.length; i++) {
            if (i < 500) {
                System.out.print(data[i] + " ");
                if (i == 499)
                    System.out.println("-------------------1--------------------");
            } else if (i < 1000) {
                System.out.print(data[i] + " ");
                if (i == 999)
                    System.out.println("-------------------2--------------------");
            } else if (i < 1500) {
                System.out.print(data[i] + " ");
                if (i == 1499)
                    System.out.println("-------------------3--------------------");
            } else if (i < 2000) {
                System.out.print(data[i] + " ");
                if (i == 1999)
                    System.out.println("-------------------4--------------------");
            } else if (i < 2500) {
                System.out.print(data[i] + " ");
                if (i == 2499)
                    System.out.println("-------------------5--------------------");
            } else if (i < 3000) {
                System.out.print(data[i] + " ");
                if (i == 2999)
                    System.out.println("-------------------6--------------------");
            } else if (i < 3500) {
                System.out.print(data[i] + " ");
                if (i == 3499)
                    System.out.println("-------------------7--------------------");
            } else if (i < 4000) {
                System.out.print(data[i] + " ");
                if (i > 3580)
                    System.out.println("-------------------8--------------------");
            }
            System.out.println("-------------------9--------------------");
        }
    }

    public void stopRec() {
        recorder.stop();
        recorder.release();
        Log.i(LOG_TAG, "Recorder stopped");
    }
}
