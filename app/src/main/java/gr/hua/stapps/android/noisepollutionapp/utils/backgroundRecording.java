package gr.hua.stapps.android.noisepollutionapp.utils;

import android.media.AudioRecord;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Calendar;

public class backgroundRecording {

    private static final String LOG_TAG = "backRec";
    private MutableLiveData<Double> data;
    private MutableLiveData<Integer> loop;
    final static Integer NOT_REC = 0; // NOT recording
    final static Integer REC = 1; // Recording

    public MutableLiveData<Integer> getLoop() {
        return loop;
    }

    private NoiseRecorder noiseRecorder;

    public LiveData<Double> getData() {
        return data;
    }

    public backgroundRecording() {
        data = new MutableLiveData<>();
        loop = new MutableLiveData<>();
        //noiseRecorder = new NoiseRecorder();
        Log.i(LOG_TAG, "NOT RECORDING");
        loop.postValue(NOT_REC);
    }

    public void start() {
        new Thread(() -> {
            long startTime = Calendar.getInstance().getTimeInMillis();
            long recTime = startTime;
            noiseRecorder = new NoiseRecorder();
            Log.i(LOG_TAG, "loop.getValue()=" + loop.getValue().toString());
            //Start recording until loop is not true
            while(loop.getValue().equals(REC)&& (recTime-startTime)<10000) {
                System.out.println("rec loop started ---------------------");
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                data.postValue(noiseRecorder.startRec());
                System.out.println(data.getValue());
                recTime = Calendar.getInstance().getTimeInMillis();
                if(recTime-startTime>10000){
                    loop.postValue(NOT_REC);
                    Log.i(LOG_TAG + "/START", "STOPPED RECORD ");
                }
            }

            //If there is an instruction to stop recording / if loop is false
            if(loop.getValue().equals(NOT_REC)) {
                if(noiseRecorder.recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    noiseRecorder.stopRec();
                    Log.i(LOG_TAG, "Stopped Recording");
                } else
                    Log.i(LOG_TAG ,"Currently not recording");
            } else
                Log.i(LOG_TAG,"loop = " + loop);

        }).start();
    }
}
