package gr.hua.stapps.android.noisepollutionapp;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;

public class NoiseCalibration {
    private static final String LOG_INTRO = "CalibratePhone: ";
    private Context context;
    private BluetoothAdapter bluetoothAdapter;

    public NoiseCalibration(Context context) {
        this.context = context;
    }

    public Boolean isBluetoothEnabled() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            BluetoothManager bluetoothManager = context.getSystemService(BluetoothManager.class);
            bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter == null) {
                System.out.println(LOG_INTRO + "Device doesn't support bluetooth");
                return false;
            } else {
                if (!bluetoothAdapter.isEnabled()) {
                    return false;
                } else
                    return true;
            }
        } else
            return false;
    }

    @SuppressLint("MissingPermission")
    public void calibrate() {
        System.out.println(LOG_INTRO + "calibrating now");
        bluetoothAdapter.startDiscovery();
    }
}
