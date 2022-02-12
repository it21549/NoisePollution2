package gr.hua.stapps.android.noisepollutionapp;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class NoisePollutionViewModel extends ViewModel {

    private backgroundRecording task = new backgroundRecording();
    private LiveData<Double> data;
    private MutableLiveData<Integer> loop;


    //Start recording
    public void start() {
        System.out.println("ViewModel started");
        task.start();
        data = task.getData();
        System.out.println("ViewModel finished");
    }

    public LiveData<Double> getData() {
        data = task.getData();
        return data;
    }
    public MutableLiveData<Integer> getLoop() {
        loop = task.getLoop();
        return loop;
    }
}
