package gr.hua.stapps.android.noisepollutionapp;


import static gr.hua.stapps.android.noisepollutionapp.CalibrationUseCase.GROUP_I;
import static gr.hua.stapps.android.noisepollutionapp.CalibrationUseCase.GROUP_II;
import static gr.hua.stapps.android.noisepollutionapp.CalibrationUseCase.GROUP_III;
import static gr.hua.stapps.android.noisepollutionapp.CalibrationUseCase.GROUP_IV;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import gr.hua.stapps.android.noisepollutionapp.databinding.NoisePollutionActivityBinding;


public class NoisePollutionActivity extends AppCompatActivity {

    private NoisePollutionActivityBinding binding;
    private NoisePollutionViewModel rec_model;
    //Handler in case of app crash
    private Thread.UncaughtExceptionHandler appCrashHandler = new AppCrashHandler(this);

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    // Permission to record
    private Boolean permissionToRecordGranted = false;
    private String deviceName = null;
    private String deviceAddress;
    public static Handler handler;
    public static BluetoothSocket mmSocket;
    public static ConnectedThread connectedThread;
    public static CreateConnectThread createConnectThread;
    private final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private SharedPreferences sharedPreferences;

    public void setPermissionToRecordGranted(Boolean permissionToRecordGranted) {
        this.permissionToRecordGranted = permissionToRecordGranted;
    }

    private static final int RECORDING_PERMISSION = 0;
    private static final int LOCATION_PERMISSION = 1;
    private static final int REQUEST_ENABLE_BLUETOOTH = 100;
    private static final String LOG_INTRO = "NoisePollutionActivity: ";

/*  TODO: DELETE
    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            System.out.println(LOG_INTRO + "action is: " + action);
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(NoisePollutionActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress(); //MAC address
                    System.out.println(LOG_INTRO + "deviceName= " + deviceName + " and mac= " + deviceHardwareAddress + " and UUID= " + Arrays.toString(device.getUuids()));

                    if (Objects.equals(deviceName, "ESP32test")) {
                        String deviceAddress = device.getAddress();
                        System.out.println(LOG_INTRO + "connecting to " + deviceAddress);
                        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                        Thread createConnectThread = new CreateConnectThread(bluetoothAdapter, deviceAddress);
                        createConnectThread.start();
                    }
                    return;
                }
            }
        }
    };*/

    boolean hasBeenUploadedUploaded;

    private static DecimalFormat decimalFormat = new DecimalFormat("#.#");
    final static Integer IS_NOT_RECORDING = 0; //Not recording


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = NoisePollutionActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

/*      TODO: DELETE
        // Register for broadcasts when a device is discovered.
        IntentFilter actionFoundFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        IntentFilter actionDiscoveryStartedFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(receiver, actionFoundFilter);
        registerReceiver(receiver, actionDiscoveryStartedFilter);
*/

        //get preference to save calibration results
        sharedPreferences = getSharedPreferences("gr.hua.stapps.android.noisepollutionapp.calibration_data", MODE_PRIVATE);

        Logger.getGlobal().log(Level.INFO, LOG_INTRO + "previous calibrations show: " + sharedPreferences.getAll().toString());

        //Provide ViewModel
        rec_model = new ViewModelProvider(this).get(NoisePollutionViewModel.class);
        double calibrationGroupI = (double) sharedPreferences.getFloat("RECORD0", (float) GROUP_I);
        double calibrationGroupII = (double) sharedPreferences.getFloat("RECORD1", (float) GROUP_II);
        double calibrationGroupIII = (double) sharedPreferences.getFloat("RECORD2", (float) GROUP_III);
        double calibrationGroupIV = (double) sharedPreferences.getFloat("RECORD3", (float) GROUP_IV);
        rec_model.initializeBackgroundRecording(calibrationGroupI, calibrationGroupII, calibrationGroupIII, calibrationGroupIV);


        //Handle app crashes with email
        Thread.setDefaultUncaughtExceptionHandler(appCrashHandler);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.user_perception, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        binding.spinnerPerception.setAdapter(adapter);
        //Perception set
        binding.spinnerPerception.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                rec_model.setPerception(parent.getItemAtPosition(position).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Toast.makeText(NoisePollutionActivity.this, "Perception cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        //Calibration
        binding.calibrateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Use this check to determine whether Bluetooth classic is supported on the device.
                // Then you can selectively disable BLE-related features.
                if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                    Toast.makeText(NoisePollutionActivity.this, "R.string.bluetooth_not_supported", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    if (ActivityCompat.checkSelfPermission(NoisePollutionActivity.this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {
                        Intent intent = new Intent(NoisePollutionActivity.this, CalibrationActivity.class);
                        intent.putExtra("Key", "Value");
                        startActivity(intent);
                        //TODO: DELETE
                        //rec_model.initializeContext(NoisePollutionActivity.this);
                    } else
                        Toast.makeText(NoisePollutionActivity.this, "permission for bluetooth not granted", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //Location Request Handling
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(NoisePollutionActivity.this);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(2 * 1000); // 2 seconds
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult == null) {
                    Log.wtf("locationCallback", "locationResult = null");
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        rec_model.setLocationData(location.getLatitude(), location.getLongitude());
                        mFusedLocationClient.removeLocationUpdates(locationCallback);
                    }
                }
            }
        };

        //Request permission to record if it is not already granted
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (ActivityCompat.checkSelfPermission(NoisePollutionActivity.this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("Permission to record granted");
                    setPermissionToRecordGranted(true);
                } else {
                    System.out.println("Permission to record not granted");
                    setPermissionToRecordGranted(false);
                    requestAudioPermission();
                }
            }
        }).start();


        final Observer<Double> rec_observer = decibels -> {
            if (!decibels.isNaN()) {
                //Setting live recording value
                binding.liveDecibels.setText(decimalFormat.format(decibels));
                rec_model.recording.setDecibels(decibels);
                rec_model.recordings.add(decibels);
                binding.averageDecibels.setText(decimalFormat.format(Utils.calculateAverage(rec_model.recordings)));
                rec_model.setDateTime();
            } else {
                System.out.println("double value is nan");
            }
        };

        final Observer<Integer> up_observer = integer -> {
            if (integer.equals(IS_NOT_RECORDING)) {
                binding.stopRec.setClickable(false);
                binding.recordButton.setClickable(true);
                rec_model.recordings.clear();
            }
        };
/*      TODO: DELETE
        final Observer<Boolean> calibration_observer = isBtEnabled -> {
            if (isBtEnabled != null) {
                if (isBtEnabled) {
                    System.out.println(LOG_INTRO + "bluetooth is enabled");
                    rec_model.calibrate();
                } else {
                    System.out.println(LOG_INTRO + "will ask for bluetooth to be enabled");
                    Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH);
                }
            } else
                System.out.println(LOG_INTRO + "isBtEnabled is null");
        };*/

        rec_model.getData().observe(this, rec_observer);
        rec_model.getLoop().observe(this, up_observer);
        //TODO: DELETE
        //rec_model.getIsBtEnabled().observe(this, calibration_observer);

        binding.recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: DELETE
                //connectedThread.write("RECORD");
                binding.stopRec.setClickable(true);
                hasBeenUploadedUploaded = false;
                Log.i("MainActivity", "loop is " + rec_model.getLoop().getValue().toString());
                if (rec_model.getLoop().getValue().equals(IS_NOT_RECORDING)) { //If not recording
                    //If permission is granted to record
                    if (permissionToRecordGranted) {
                        Log.d("MainActivity", "starting audio recording");
                        rec_model.startBackgroundRecording();
                        binding.recordButton.setClickable(false);
                    } else {
                        Log.d("DEBUG", permissionToRecordGranted.toString());
                        requestAudioPermission();
                    }
                }
            }
        });

        binding.stopRec.setOnClickListener(v -> {
            //TODO: DELETE
            //connectedThread.write("STOP");
            rec_model.getLoop().postValue(IS_NOT_RECORDING);
            binding.stopRec.setClickable(false);
            binding.recordButton.setClickable(true);
        });

        binding.uploadButton.setOnClickListener(v -> {
            if (!binding.stopRec.isClickable() && !hasBeenUploadedUploaded) { //while not recording
                //Set User Info
                boolean error = false;

                //Gender info
                int checked_gender = binding.radioGroupGender.getCheckedRadioButtonId();
                if (checked_gender == binding.radioMale.getId()) {
                    //Male
                    rec_model.recording.setGender(0);
                } else if (checked_gender == binding.radioFem.getId()) {
                    //Female
                    rec_model.recording.setGender(1);
                } else if (checked_gender == binding.radioOther.getId()) {
                    //Undefined
                    rec_model.recording.setGender(2);
                }

                //Age info
                if (!binding.editTextAge.getText().toString().matches("")) {
                    int age = Integer.parseInt(binding.editTextAge.getText().toString());
                    rec_model.recording.setAge(age);
                    if (age > 100 || age < 0) {
                        Toast.makeText(NoisePollutionActivity.this, "Age must be a reasonable number", Toast.LENGTH_SHORT).show();
                        error = true;
                    }
                } else
                    Toast.makeText(NoisePollutionActivity.this, "Age field is required", Toast.LENGTH_SHORT).show();

                //Noise type info

                //Type Anthropogenic
                int anthrop;
                if (binding.editTextAnthropogenic.getText().toString().matches(""))
                    anthrop = 0;
                else
                    anthrop = Integer.parseInt(binding.editTextAnthropogenic.getText().toString());
                rec_model.recording.setAnthropogenic(anthrop);

                //Type Natural
                int natural;
                if (binding.editTextNatural.getText().toString().matches(""))
                    natural = 0;
                else
                    natural = Integer.parseInt(binding.editTextNatural.getText().toString());
                rec_model.recording.setNatural(natural);
                //Type Technological
                int techno;
                if (binding.editTextTechnological.getText().toString().matches(""))
                    techno = 0;
                else
                    techno = Integer.parseInt(binding.editTextTechnological.getText().toString());
                rec_model.recording.setTechnological(techno);

                int total = anthrop + natural + techno;
                if (total != 10) {
                    Toast.makeText(NoisePollutionActivity.this, "Sum of Anthropogenic, Natural and Technological value must equal 10", Toast.LENGTH_SHORT).show();
                    error = true;
                }

                //If some error exist inform user to re-submit
                if (error)
                    Toast.makeText(NoisePollutionActivity.this, "Please re-submit with correct values", Toast.LENGTH_SHORT).show();
                else {
                    accessLocation();
                }
            } else if (hasBeenUploadedUploaded)
                Toast.makeText(NoisePollutionActivity.this, "You have already uploaded, make a new recording to upload!", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(NoisePollutionActivity.this, "You cannot upload while recording!", Toast.LENGTH_SHORT).show();
        });

        binding.switchAdvanced.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.userInfoTable.setVisibility(View.VISIBLE);
                binding.uploadButton.setVisibility(View.VISIBLE);
            } else {
                binding.userInfoTable.setVisibility(View.INVISIBLE);
                binding.uploadButton.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void requestAudioPermission() {
        //Permission has not been granted and must be requested.
        if (ActivityCompat.shouldShowRequestPermissionRationale(NoisePollutionActivity.this, Manifest.permission.RECORD_AUDIO)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // Display a SnackBar with cda button to request the missing permission.
            Log.d("PERMISSIONS", "requestMicPermission()/if");
            Snackbar.make(binding.getRoot(), "Permission to record audio is required", Snackbar.LENGTH_INDEFINITE).setAction("Audio Permission", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Request the permission
                    ActivityCompat.requestPermissions(NoisePollutionActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORDING_PERMISSION);
                }
            }).show();
        } else {
            Snackbar.make(binding.getRoot(), "Audio recording not available", Snackbar.LENGTH_SHORT).show();
            //Request the permission. The result will be received in onRequestPermissionResult().
            ActivityCompat.requestPermissions(NoisePollutionActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORDING_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.d("PERMISSIONS", "onRequestPermissionsResult()");

        if (requestCode == RECORDING_PERMISSION) {
            //Request for permission.
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Permission has been granted. Start Recording.
                Snackbar.make(binding.getRoot(), "Permission granted", Snackbar.LENGTH_SHORT).show();
                setPermissionToRecordGranted(true);
            } else {
                //Permission request was denied.
                Snackbar.make(binding.getRoot(), "Permission denied", Snackbar.LENGTH_SHORT).show();
                setPermissionToRecordGranted(false);
            }
        }
    }

    public void accessLocation() {
        final LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        System.out.println("in accessLocation()");
        if (ActivityCompat.checkSelfPermission(NoisePollutionActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED /*&& ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED*/) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
//            Toast.makeText(MainActivity.this, "Location permission is required", Toast.LENGTH_SHORT).show();
            Snackbar.make(binding.getRoot(), "Permission to access gps is required", Snackbar.LENGTH_INDEFINITE).setAction("Location Permission", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Request the permission
                    ActivityCompat.requestPermissions(NoisePollutionActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION);
                }
            }).show();
        } else {
            mFusedLocationClient.getLastLocation().addOnSuccessListener(NoisePollutionActivity.this, new OnSuccessListener<Location>() {
                @SuppressLint("MissingPermission")
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        rec_model.setLocationData(location.getLatitude(), location.getLongitude());
                        //Send data to database
                        sendAndRequestResponse();
                    } else
                        Toast.makeText(NoisePollutionActivity.this, "GPS cannot be found", Toast.LENGTH_SHORT).show();
                    mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
                }
            });
        }
    }

    private void sendAndRequestResponse() {
        hasBeenUploadedUploaded = true;
        NetworkHelper networkHelper = new NetworkHelper(NoisePollutionActivity.this);
        networkHelper.uploadToFirebase(rec_model.recording);
        networkHelper.uploadToPostgreSQL(rec_model.recording);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //TODO: DELETE
        //Don't forget to unregister the ACTION_FOUND receiver.
//        unregisterReceiver(receiver);
    }

    //TODO: DELETE
    @SuppressLint("MissingPermission")
    private class CreateConnectThread extends Thread {

        public CreateConnectThread(BluetoothAdapter bluetoothAdapter, String address) {
            deviceName = address;
            handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    switch (msg.what) {
                        case CONNECTING_STATUS:
                            switch (msg.arg1) {
                                case 1:
                                    System.out.println("NoisePollution " + "connected to " + deviceName);
                                    break;
                                case -1:
                                    System.out.println("NoisePollution " + "device fails to connect");
                                    break;
                            }
                            break;
                        case MESSAGE_READ:
                            String arduinoMsg = msg.obj.toString(); //Read message from Arduino
                            System.out.println("NoisePollution " + "the message received is: " + arduinoMsg);
                    }
                }
            };
            /*
            Use a temporary object that is later assigned to mmSocket
            because mmSocket is final.
             */
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            BluetoothSocket tmp = null;
            UUID uuid = bluetoothDevice.getUuids()[0].getUuid();

            try {
                /*
                Get a BluetoothSocket to connect with the given BluetoothDevice.
                Due to Android device varieties,the method below may not work fo different devices.
                You should try using other methods i.e. :
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                 */
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);

            } catch (IOException e) {
                Log.e("NoisePollution", "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.cancelDiscovery();
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                Log.e("Status", "Device connected");
                handler.obtainMessage(CONNECTING_STATUS, 1, -1).sendToTarget();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                    Log.e("Status", "Cannot connect to device");
                    handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();
                } catch (IOException closeException) {
                    Log.e("NoisePollution", "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connectedThread = new ConnectedThread(mmSocket);
            connectedThread.run();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("NoisePollution ", "Could not close the client socket", e);
            }
        }
    }

    //TODO: DELETE
    //Thread for data transfer
    public class ConnectedThread extends Thread {
        private BluetoothSocket mmSocket;
        private InputStream mmInStream;
        private OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024]; //buffer store for the stream
            int bytes = 0; //bytes returned from read()
            //keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    /*
                    Read from the InputStream from Arduino until termination character is reached.
                    Then send the whole String message to GUI Handler.
                     */
                    buffer[bytes] = (byte) mmInStream.read();
                    String readMessage;
                    if (buffer[bytes] == '\n') {
                        readMessage = new String(buffer, 0, bytes);
                        System.out.println("Arduino Message: " + readMessage);
                        handler.obtainMessage(MESSAGE_READ, readMessage).sendToTarget();
                        bytes = 0;
                    } else {
                        bytes++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            Logger.getGlobal().log(Level.INFO, LOG_INTRO + "Sending message: " + input);
            byte[] bytes = input.getBytes(); //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                System.out.println("Send Error ->" + "Unable to send message: " + e);
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }
}