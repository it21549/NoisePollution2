package gr.hua.stapps.android.noisepollutionapp;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NoisePollutionViewModel extends ViewModel {

    private backgroundRecording task = new backgroundRecording();
    private LiveData<Double> data;
    private MutableLiveData<Integer> loop;
    public Recording recording = new Recording();
    public List<Double> recordings = new ArrayList<>();
    private final Integer isRecording = 1; // 0=not recording, 1 = recording


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

    public void setPerception(String perception) {
        switch (perception) {
            case ("absolute silence"):
                recording.setPerception(0);
                break;
            case ("extremely quiet"):
                recording.setPerception(1);
                break;
            case ("very quiet"):
                recording.setPerception(2);
                break;
            case ("quiet"):
                recording.setPerception(3);
                break;
            case ("moderate"):
                recording.setPerception(4);
                break;
            case ("somewhat noisy"):
                recording.setPerception(5);
                break;
            case ("noisy"):
                recording.setPerception(6);
                break;
            case ("very noisy"):
                recording.setPerception(7);
                break;
            case ("extremely noisy"):
                recording.setPerception(8);
                break;
        }
    }

}
