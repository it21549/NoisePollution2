package gr.hua.stapps.android.noisepollutionapp;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class CalibrationViewModel extends ViewModel {

    private NoiseCalibration noiseCalibration;
    private MutableLiveData<Boolean> isBluetoothEnabled = new MutableLiveData<>();

    public MutableLiveData<Boolean> getIsBluetoothEnabled() {
        return isBluetoothEnabled;
    }

    public void initNoiseCalibration(Context context) {
        noiseCalibration = new NoiseCalibration(context);
        isBluetoothEnabled.postValue(noiseCalibration.isBluetoothEnabled());
    }

    public void searchForDevices() {
        noiseCalibration.startDiscovery();
    }
}
