package gr.hua.stapps.android.noisepollutionapp;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.logging.Level;
import java.util.logging.Logger;

public class CalibrationViewModel extends ViewModel {

    private static final String LOG_INTRO = "CalibrationViewModel -> ";
    private final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update

    private ConnectionThread connectionThread;
    private NoiseCalibration noiseCalibration;
    private MutableLiveData<Boolean> isBluetoothEnabled = new MutableLiveData<>();
    private MutableLiveData<Boolean> isConnectedToESP = new MutableLiveData<>();
    private MutableLiveData<String> espMessage = new MutableLiveData<>();
    private Handler handler = new Handler(Looper.getMainLooper()) {
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
                    String arduinoMsg = msg.obj.toString(); //Read message from Arduino
                    System.out.println("NoisePollution " + "the message received is: " + arduinoMsg);
            }
        }
    };

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
    }

    public LiveData<String> getEspMessage() {
        return espMessage;
    }
}
