package gr.hua.stapps.android.noisepollutionapp;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NoisePollutionViewModel extends ViewModel {

    private BackgroundRecording task;
    private LiveData<Double> data;
    private MutableLiveData<Integer> loop;
    public Recording recording = new Recording();
    public List<Double> recordings = new ArrayList<>();
    private final Integer isRecording = 1; // 0=not recording, 1 = recording
    private NoiseCalibration noiseCalibration;
    private MutableLiveData<Boolean> isBtEnabled = new MutableLiveData<>();
    private Context context;


    public void initializeBackgroundRecording(double calibrationI, double calibrationII, double calibrationIII, double calibrationIV) {
        task = new BackgroundRecording(calibrationI, calibrationII, calibrationIII, calibrationIV);
    }

    //Start Recording
    public void startBackgroundRecording() {
        loop.postValue(isRecording);
        task.start();
        data = task.getData();
    }

    public Double computeAverageDecibel(Double decibel) {
        recording.setDecibels(decibel);
        return recording.getDecibels();
    }

    public void setLocationData(Double latitude, Double longitude) {
        recording.setLongitude(longitude);
        recording.setLatitude(latitude);
    }

    public void setDateTime() {
        recording.setDate(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
        recording.setTime(new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()));
    }

    public LiveData<Double> getData() {
        data = task.getData();
        return data;
    }

    public MutableLiveData<Integer> getLoop() {
        loop = task.getLoop();
        return loop;
    }

    public MutableLiveData<Boolean> getIsBtEnabled() {
        return isBtEnabled;
    }

    public void setPerception(String perception) {
        switch (perception) {
            case ("quiet"):
                recording.setPerception(0);
                break;
            case ("noisy"):
                recording.setPerception(1);
                break;
            case ("very noisy"):
                recording.setPerception(2);
                break;
            case ("extremely noisy"):
                recording.setPerception(3);
                break;
        }
    }

    public void initializeContext(Context context) {
        this.context = context;
        noiseCalibration = new NoiseCalibration(context);
        isBtEnabled.postValue(noiseCalibration.isBluetoothEnabled());
    }

    public void calibrate() {
        noiseCalibration.startDiscovery();
    }

}
