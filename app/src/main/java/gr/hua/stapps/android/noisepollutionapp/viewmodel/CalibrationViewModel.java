package gr.hua.stapps.android.noisepollutionapp.viewmodel;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import gr.hua.stapps.android.noisepollutionapp.utils.NoiseCalibration;

public class CalibrationViewModel extends ViewModel {

    private NoiseCalibration noiseCalibration;
    private MutableLiveData<Boolean> isBtEnabled = new MutableLiveData<>();
    private Context context;

    public MutableLiveData<Boolean> getIsBtEnabled() {
        return isBtEnabled;
    }

    public void initializeContext(Context context) {
        this.context = context;
        noiseCalibration = new NoiseCalibration(context);
        isBtEnabled.postValue(noiseCalibration.isBluetoothEnabled());
    }

    public void calibrate() {
        noiseCalibration.calibrate();
    }
}
