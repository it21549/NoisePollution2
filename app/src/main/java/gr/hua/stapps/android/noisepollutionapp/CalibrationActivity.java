package gr.hua.stapps.android.noisepollutionapp;

import static gr.hua.stapps.android.noisepollutionapp.CalibrationUseCase.GROUP_I;
import static gr.hua.stapps.android.noisepollutionapp.CalibrationUseCase.GROUP_II;
import static gr.hua.stapps.android.noisepollutionapp.CalibrationUseCase.GROUP_III;
import static gr.hua.stapps.android.noisepollutionapp.CalibrationUseCase.GROUP_IV;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.snackbar.Snackbar;

import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import gr.hua.stapps.android.noisepollutionapp.databinding.ActivityCalibrationBinding;

public class CalibrationActivity extends AppCompatActivity {

    private static final Integer NOT_RECORDING = 0; //Not recording
    private ActivityCalibrationBinding binding;
    private CalibrationViewModel calibrationViewModel;
    private static final String LOG_INTRO = "CalibrationActivity -> ";
    private SharedPreferences.Editor editor;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private static final int REQUEST_ENABLE_BLUETOOTH = 100;
    private static final int RECORDING_PERMISSION = 0;
    private static final int LOCATION_PERMISSION = 1;

    private Boolean permissionToRecord = false;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //Logger.getGlobal().log(Level.INFO, LOG_INTRO + "action is: " + action);
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(CalibrationActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress(); //MAC address
                    Logger.getGlobal().log(Level.INFO, LOG_INTRO + "FOUND: deviceName= " + deviceName + " and mac= " + deviceHardwareAddress + " and UUID= " + Arrays.toString(device.getUuids()));
                    if (Objects.equals(deviceName, "ESP32test")) {
                        Logger.getGlobal().log(Level.INFO, LOG_INTRO + "Found ESP32Test! Connecting to " + deviceHardwareAddress);
                        calibrationViewModel.initConnectionThread(deviceHardwareAddress);
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCalibrationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //get preference to save calibration results
        SharedPreferences sharedPreferences = getSharedPreferences("gr.hua.stapps.android.noisepollutionapp.calibration_data", MODE_PRIVATE);
        editor = sharedPreferences.edit();

        Logger.getGlobal().log(Level.INFO, LOG_INTRO + "previous calibrations show: " + sharedPreferences.getAll().toString());

        //Provide ViewModel
        calibrationViewModel = new ViewModelProvider(this).get(CalibrationViewModel.class);
        calibrationViewModel.initializeBackgroundRecording(GROUP_I, GROUP_II, GROUP_III, GROUP_IV);

        // Register for broadcasts when a device is discovered.
        registerReceivers();

        // Get values from previous activity
        Intent intent = getIntent();
        String key = intent.getStringExtra("Key");
        System.out.println(LOG_INTRO + "value passed is " + key);

        setListeners();
        setObservers();
        requestPermission();

        //Location Request Handling
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(CalibrationActivity.this);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(2 * 1000);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult == null) {
                    Log.wtf("locationCallback", "locationResult = null");
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        Logger.getGlobal().log(Level.INFO, LOG_INTRO + "location result exists");
                        mFusedLocationClient.removeLocationUpdates(locationCallback);
                    }
                }
            }
        };
    }

    public void requestPermission() {
        //Request permission to record if it is not already granted
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (ActivityCompat.checkSelfPermission(CalibrationActivity.this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("Permission to record granted");
                    permissionToRecord = true;
                } else {
                    System.out.println("Permission to record not granted");
                    permissionToRecord = false;
                    requestAudioPermission();
                }
            }
        }).start();
    }

    private void requestAudioPermission() {
        //Permission has not been granted and must be requested.
        if (ActivityCompat.shouldShowRequestPermissionRationale(CalibrationActivity.this, Manifest.permission.RECORD_AUDIO)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // Display a SnackBar with cda button to request the missing permission.
            Log.d("PERMISSIONS", "requestMicPermission()/if");
            Snackbar.make(binding.getRoot(), "Permission to record audio is required", Snackbar.LENGTH_INDEFINITE).setAction("Audio Permission", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Request the permission
                    ActivityCompat.requestPermissions(CalibrationActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORDING_PERMISSION);
                }
            }).show();
        } else {
            Snackbar.make(binding.getRoot(), "Audio recording not available", Snackbar.LENGTH_SHORT).show();
            //Request the permission. The result will be received in onRequestPermissionResult().
            ActivityCompat.requestPermissions(CalibrationActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORDING_PERMISSION);
        }
    }

    @SuppressLint("MissingPermission")
    public void setObservers() {
        final Observer<Boolean> calibration_observer = isBluetoothEnabled -> {
            if (isBluetoothEnabled != null) {
                if (isBluetoothEnabled) {
                    Logger.getGlobal().log(Level.INFO, LOG_INTRO + "bluetooth is enabled");
                    calibrationViewModel.searchForDevices();
                } else {
                    Logger.getGlobal().log(Level.INFO, LOG_INTRO + "asking to enable bluetooth");
                    Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH);
                }
            } else Logger.getGlobal().log(Level.INFO, LOG_INTRO + "isBluetoothEnabled is false");
        };
        final Observer<Boolean> connection_observer = isConnectedToESP -> {
            if (isConnectedToESP != null) {
                if (isConnectedToESP) {
                    Logger.getGlobal().log(Level.INFO, LOG_INTRO + " connected to ESP");
                    binding.connectButton.setClickable(false);
                    binding.connectButton.setText(getResources().getText(R.string.connected_to_device));
                    binding.connectButton.setBackgroundColor(getResources().getColor(R.color.light_turquoise_completed));
                    areCommandButtonsClickable(true);
                } else {
                    Toast.makeText(this, "Could not connect to calibration device, please retry in a few moments.", Toast.LENGTH_LONG).show();
                    binding.connectButton.setClickable(true);
                }
            }
        };
        final Observer<String> espMessage_observer = espMessage -> {
            if (espMessage != null) {
                Logger.getGlobal().log(Level.INFO, LOG_INTRO + "message received is: " + espMessage);
                try {
                    calibrationViewModel.addRemoteData(Double.parseDouble(espMessage));
                } catch (Exception e) {
                    Logger.getGlobal().log(Level.INFO, LOG_INTRO + "Error parsing double: " + e);
                }
                if (calibrationViewModel.getLoop().getValue().equals(NOT_RECORDING)) {
                    calibrationViewModel.printRecordings();
                }
            }
        };
        final Observer<Double> localData_observer = localData -> {
            if (localData != null) {
                calibrationViewModel.addLocalData(localData);
            }
        };
        final Observer<Integer> loop_observer = integer -> {
            if (integer.equals(NOT_RECORDING) && Boolean.TRUE.equals(calibrationViewModel.getIsConnectedToESP().getValue())) {
                Logger.getGlobal().log(Level.INFO, LOG_INTRO + "to Stop recording is called");
                calibrationViewModel.stopRecording();
                areCommandButtonsClickable(true);
            }
        };
        final Observer<Double> calibration_groupI_observer = result -> {
            Logger.getGlobal().log(Level.INFO, LOG_INTRO + "saving.. RECORD0 " + result);
            editor.putFloat("RECORD0", result.floatValue());
            editor.apply();
            binding.buttonCalibrationGroupI.setText(getResources().getText(R.string.calibrated));
            binding.buttonCalibrationGroupI.getBackground().setColorFilter(ContextCompat.getColor(CalibrationActivity.this, R.color.light_turquoise_completed), PorterDuff.Mode.SRC_IN);
        };
        final Observer<Double> calibration_groupII_observer = result -> {
            Logger.getGlobal().log(Level.INFO, LOG_INTRO + "saving.. RECORD1 " + result);
            editor.putFloat("RECORD1", result.floatValue());
            editor.apply();
            binding.buttonCalibrationGroupI.setText(getResources().getText(R.string.calibrated));
            binding.buttonCalibrationGroupI.getBackground().setColorFilter(ContextCompat.getColor(CalibrationActivity.this, R.color.light_turquoise_completed), PorterDuff.Mode.SRC_IN);
        };
        final Observer<Double> calibration_groupIII_observer = result -> {
            Logger.getGlobal().log(Level.INFO, LOG_INTRO + "saving.. RECORD2 " + result);
            editor.putFloat("RECORD2", result.floatValue());
            editor.apply();
            binding.buttonCalibrationGroupI.setText(getResources().getText(R.string.calibrated));
            binding.buttonCalibrationGroupI.getBackground().setColorFilter(ContextCompat.getColor(CalibrationActivity.this, R.color.light_turquoise_completed), PorterDuff.Mode.SRC_IN);
        };
        final Observer<Double> calibration_groupIV_observer = result -> {
            Logger.getGlobal().log(Level.INFO, LOG_INTRO + "saving.. RECORD2 " + result);
            editor.putFloat("RECORD3", result.floatValue());
            editor.apply();
            binding.buttonCalibrationGroupI.setText(getResources().getText(R.string.calibrated));
            binding.buttonCalibrationGroupI.getBackground().setColorFilter(ContextCompat.getColor(CalibrationActivity.this, R.color.light_turquoise_completed), PorterDuff.Mode.SRC_IN);
        };
        final Observer<String> retry_calibration_observer = this::retryCalibration;
        calibrationViewModel.getIsBluetoothEnabled().observe(this, calibration_observer);
        calibrationViewModel.getIsConnectedToESP().observe(this, connection_observer);
        calibrationViewModel.getEspMessage().observe(this, espMessage_observer);
        calibrationViewModel.getLocalData().observe(this, localData_observer);
        calibrationViewModel.getLoop().observe(this, loop_observer);
        calibrationViewModel.getCalibrationGroupI().observe(this, calibration_groupI_observer);
        calibrationViewModel.getCalibrationGroupII().observe(this, calibration_groupII_observer);
        calibrationViewModel.getCalibrationGroupIII().observe(this, calibration_groupIII_observer);
        calibrationViewModel.getCalibrationGroupIV().observe(this, calibration_groupIV_observer);
        calibrationViewModel.getRetryCalibration().observe(this, retry_calibration_observer);
    }

    private void retryCalibration(String group) {
        switch (group) {
            case ("RECORD0"):
                if (binding.buttonCalibrationGroupI.getText() == getResources().getText(R.string.calibrating)) {
                    binding.buttonCalibrationGroupI.setText(getResources().getText(R.string.retry_calibration));
                    binding.buttonCalibrationGroupI.getBackground().setColorFilter(ContextCompat.getColor(CalibrationActivity.this, R.color.red_retry), PorterDuff.Mode.SRC_IN);
                }
            case ("RECORD1"):
                if (binding.buttonCalibrationGroupII.getText() == getResources().getText(R.string.calibrating)) {
                    binding.buttonCalibrationGroupII.setText(getResources().getText(R.string.retry_calibration));
                    binding.buttonCalibrationGroupII.getBackground().setColorFilter(ContextCompat.getColor(CalibrationActivity.this, R.color.red_retry), PorterDuff.Mode.SRC_IN);
                }
            case ("RECORD2"):
                if (binding.buttonCalibrationGroupIII.getText() == getResources().getText(R.string.calibrating)) {
                    binding.buttonCalibrationGroupIII.setText(getResources().getText(R.string.retry_calibration));
                    binding.buttonCalibrationGroupIII.getBackground().setColorFilter(ContextCompat.getColor(CalibrationActivity.this, R.color.red_retry), PorterDuff.Mode.SRC_IN);
                }
            case ("RECORD3"):
                if (binding.buttonCalibrationGroupIV.getText() == getResources().getText(R.string.calibrating)) {
                    binding.buttonCalibrationGroupIV.setText(getResources().getText(R.string.retry_calibration));
                    binding.buttonCalibrationGroupIV.getBackground().setColorFilter(ContextCompat.getColor(CalibrationActivity.this, R.color.red_retry), PorterDuff.Mode.SRC_IN);
                }
        }
    }

    public void setListeners() {
        binding.connectButton.setOnClickListener(view -> {
            binding.connectButton.setClickable(false);
            // Use this check to determine whether Bluetooth classic is supported on the device.
            // Then you can selectively disable BLE-related features.
            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                Toast.makeText(CalibrationActivity.this, "R.string.bluetooth_not_supported", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                if (ActivityCompat.checkSelfPermission(CalibrationActivity.this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {
                    requestLocation();
                    calibrationViewModel.initNoiseCalibration(CalibrationActivity.this);
                } else {
                    Toast.makeText(CalibrationActivity.this, "permission for bluetooth not granted", Toast.LENGTH_SHORT).show();
                    binding.connectButton.setClickable(true);
                }
            }
        });
        binding.buttonCalibrationGroupI.setOnClickListener(view -> {
            calibrationViewModel.sendCommand("RECORD0");
            binding.buttonCalibrationGroupI.setText(getResources().getText(R.string.calibrating));
            binding.buttonCalibrationGroupI.getBackground().setColorFilter(ContextCompat.getColor(CalibrationActivity.this, R.color.pink_user_info_title), PorterDuff.Mode.SRC_IN);
            areCommandButtonsClickable(false);
        });
        binding.buttonCalibrationGroupII.setOnClickListener(view -> {
            calibrationViewModel.sendCommand("RECORD1");
            binding.buttonCalibrationGroupII.setText(getResources().getText(R.string.calibrating));
            binding.buttonCalibrationGroupII.getBackground().setColorFilter(ContextCompat.getColor(CalibrationActivity.this, R.color.pink_user_info_title), PorterDuff.Mode.SRC_IN);
            areCommandButtonsClickable(false);
        });
        binding.buttonCalibrationGroupIII.setOnClickListener(view -> {
            calibrationViewModel.sendCommand("RECORD2");
            binding.buttonCalibrationGroupIII.setText(getResources().getText(R.string.calibrating));
            binding.buttonCalibrationGroupIII.getBackground().setColorFilter(ContextCompat.getColor(CalibrationActivity.this, R.color.pink_user_info_title), PorterDuff.Mode.SRC_IN);
            areCommandButtonsClickable(false);
        });
        binding.buttonCalibrationGroupIV.setOnClickListener(view -> {
            calibrationViewModel.sendCommand("RECORD3");
            binding.buttonCalibrationGroupIV.setText(getResources().getText(R.string.calibrating));
            binding.buttonCalibrationGroupIV.getBackground().setColorFilter(ContextCompat.getColor(CalibrationActivity.this, R.color.pink_user_info_title), PorterDuff.Mode.SRC_IN);
            areCommandButtonsClickable(false);
            //STOP FUNCTIONALITY
            /*            calibrationViewModel.stopRecording();
            areCommandButtonsClickable(true);*/
        });
        areCommandButtonsClickable(false);
    }

    public void areCommandButtonsClickable(Boolean clickable) {
        binding.buttonCalibrationGroupI.setClickable(clickable);
        binding.buttonCalibrationGroupII.setClickable(clickable);
        binding.buttonCalibrationGroupIII.setClickable(clickable);
        binding.buttonCalibrationGroupIV.setClickable(clickable);
    }

    public void registerReceivers() {
        IntentFilter actionFoundFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        IntentFilter actionDiscoveryStartedFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(receiver, actionFoundFilter);
        registerReceiver(receiver, actionDiscoveryStartedFilter);
    }

    private void requestLocation() {
        if (ActivityCompat.checkSelfPermission(CalibrationActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(binding.getRoot(), "Permission to access gps is required", Snackbar.LENGTH_INDEFINITE).setAction("Location Permission", v -> {
                //Request permission
                ActivityCompat.requestPermissions(CalibrationActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION);
            }).show();
            binding.connectButton.setClickable(true);
        } else {
            mFusedLocationClient.getLastLocation().addOnSuccessListener(CalibrationActivity.this, location -> {
                if (location == null) {
                    Toast.makeText(CalibrationActivity.this, "GPS cannot be found", Toast.LENGTH_SHORT).show();
                    binding.connectButton.setClickable(true);
                }
                mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver);
    }
}