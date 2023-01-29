package gr.hua.stapps.android.noisepollutionapp;

import static gr.hua.stapps.android.noisepollutionapp.CalibrationUseCase.GROUP_I;
import static gr.hua.stapps.android.noisepollutionapp.CalibrationUseCase.GROUP_II;
import static gr.hua.stapps.android.noisepollutionapp.CalibrationUseCase.GROUP_III;
import static gr.hua.stapps.android.noisepollutionapp.CalibrationUseCase.GROUP_IV;
import static gr.hua.stapps.android.noisepollutionapp.CalibrationUseCase.calculateAverage;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CalibrationViewModel extends ViewModel {

    private static final String LOG_INTRO = "CalibrationViewModel -> ";
    private final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static String STOP_RECORDING = "STOP";

    //Recording
    private BackgroundRecording task;
    private LiveData<Double> localData;
    private MutableLiveData<Integer> loop;
    private final List<Double> localRecording = new ArrayList<>();
    private static final Integer RECORDING = 1; // 0=not recording, 1 = recording
    private static final Integer NOT_RECORDING = 0;

    //Calibration
    private ConnectionThread connectionThread;
    private NoiseCalibration noiseCalibration;
    private final MutableLiveData<Boolean> isBluetoothEnabled = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isConnectedToESP = new MutableLiveData<>();
    private final MutableLiveData<String> espMessage = new MutableLiveData<>();
    private double remoteRecording = 0.0;
    private String calibrationGroup;
    private final MutableLiveData<Double> calibrationGroupI = new MutableLiveData<>();
    private final MutableLiveData<Double> calibrationGroupII = new MutableLiveData<>();
    private final MutableLiveData<Double> calibrationGroupIII = new MutableLiveData<>();
    private final MutableLiveData<Double> calibrationGroupIV = new MutableLiveData<>();
    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case CONNECTING_STATUS:
                    switch (msg.arg1) {
                        case 1:
                            Logger.getGlobal().log(Level.INFO, LOG_INTRO + "Handler reports: connected to " + "ESP32");
                            isConnectedToESP.postValue(true);
                            break;
                        case -1:
                            Logger.getGlobal().log(Level.INFO, LOG_INTRO + "Handler reports: device fails to connect");
                            isConnectedToESP.postValue(false);
                            break;
                    }
                    break;
                case MESSAGE_READ:
                    espMessage.postValue(msg.obj.toString());  //Read message from Arduino
            }
        }
    };

    public void initializeBackgroundRecording(double calibrationI, double calibrationII, double calibrationIII, double calibrationIV) {
        task = new BackgroundRecording(calibrationI, calibrationII, calibrationIII, calibrationIV);
    }

    public void initNoiseCalibration(Context context) {
        noiseCalibration = new NoiseCalibration(context);
        isBluetoothEnabled.postValue(noiseCalibration.isBluetoothEnabled());
    }

    public void initConnectionThread(String deviceAddress) {
        connectionThread = new ConnectionThread(deviceAddress, handler);
        connectionThread.start();
    }

    public void searchForDevices() {
        noiseCalibration.startDiscovery();
    }

    public MutableLiveData<Boolean> getIsBluetoothEnabled() {
        return isBluetoothEnabled;
    }

    public MutableLiveData<Boolean> getIsConnectedToESP() {
        return isConnectedToESP;
    }

    public void sendCommand(String command) {
        connectionThread.getDataThread().write(command);
        if (command.contains("RECORD")) {
            localRecording.clear();
            calibrationGroup = command;
            startBackgroundRecording();
        }
    }

    public LiveData<String> getEspMessage() {
        return espMessage;
    }

    public void stopRecording() {
        if (loop.getValue().equals(RECORDING)) {
            loop.postValue(NOT_RECORDING);
        }
        sendCommand(STOP_RECORDING);
    }

    private void startBackgroundRecording() {
        Logger.getGlobal().log(Level.INFO, "Starting to record locally");
        loop.postValue(RECORDING);
        task.start();
        localData = task.getData();
    }

    public MutableLiveData<Integer> getLoop() {
        loop = task.getLoop();
        return loop;
    }

    public LiveData<Double> getLocalData() {
        localData = task.getData();
        return localData;
    }

    public MutableLiveData<Double> getCalibrationGroupI() {
        return calibrationGroupI;
    }

    public MutableLiveData<Double> getCalibrationGroupII() {
        return calibrationGroupII;
    }

    public MutableLiveData<Double> getCalibrationGroupIII() {
        return calibrationGroupIII;
    }

    public MutableLiveData<Double> getCalibrationGroupIV() {
        return calibrationGroupIV;
    }

    public void addLocalData(Double localData) {
        //Logger.getGlobal().log(Level.INFO, LOG_INTRO + "local measurement is: " + localData.toString());
        if (!localData.isNaN()) {
            localRecording.add(localData);
        }
    }

    public void addRemoteData(Double remoteData) {
        remoteRecording = remoteData;
    }

    public void printRecordings() {
        Logger.getGlobal().log(Level.INFO, LOG_INTRO + "localRecording: " + localRecording + "and size: " + localRecording.size());
        Logger.getGlobal().log(Level.INFO, LOG_INTRO + "remoteRecording: " + remoteRecording);
        updateCalibrationValues();

    }

    public void updateCalibrationValues() {
        double localAverage = calculateAverage(localRecording);
        Logger.getGlobal().log(Level.INFO, LOG_INTRO + "localAverage is " + localAverage + " of group " + calibrationGroup);
        double boundary = CalibrationUseCase.calculateBoundary(localAverage, remoteRecording);
        double group = CalibrationUseCase.calculateCalibrationGroup(remoteRecording);
        switch (calibrationGroup) {
            case ("RECORD0"):
                if (group == GROUP_I) {
                    calibrationGroupI.postValue(boundary);
                }
                break;
            case ("RECORD1"):
                if (group == GROUP_II) {
                    calibrationGroupII.postValue(boundary);
                }
                break;
            case ("RECORD2"):
                if (group == GROUP_III) {
                    calibrationGroupIII.postValue(boundary);
                }
                break;
            case ("RECORD3"):
                if (group == GROUP_IV) {
                    calibrationGroupIV.postValue(boundary);
                }
                break;
        }
    }
}
