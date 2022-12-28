package gr.hua.stapps.android.noisepollutionapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import gr.hua.stapps.android.noisepollutionapp.databinding.ActivityCalibrationBinding;

public class CalibrationActivity extends AppCompatActivity {

    private ActivityCalibrationBinding binding;
    private CalibrationViewModel calibrationViewModel;
    private static final String LOG_INTRO = "CalibrationActivity -> ";

    private String deviceName = null;
    public static Handler handler;
    private final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private static final int REQUEST_ENABLE_BLUETOOTH = 100;
    public static BluetoothSocket mmSocket;
    public static ConnectedDataThread connectedThread;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Logger.getGlobal().log(Level.INFO, LOG_INTRO + "action is: " + action);
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
                    System.out.println(LOG_INTRO + "deviceName= " + deviceName + " and mac= " + deviceHardwareAddress + " and UUID= " + Arrays.toString(device.getUuids()));

                    if (Objects.equals(deviceName, "ESP32test")) {
                        String deviceAddress = device.getAddress();
                        System.out.println(LOG_INTRO + "connecting to " + deviceAddress);
                        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                        Thread createConnectThread = new CreateConnectionThread(bluetoothAdapter, deviceAddress);
                        createConnectThread.start();
                    }
                    return;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCalibrationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Register for broadcasts when a device is discovered.
        registerReceivers();

        //Provide ViewModel
        calibrationViewModel = new ViewModelProvider(this).get(CalibrationViewModel.class);

        // Get values from previous activity
        Intent intent = getIntent();
        String key = intent.getStringExtra("Key");
        System.out.println(LOG_INTRO + "value passed is " + key);

        setListeners();
        setObservers();
    }

    @SuppressLint("MissingPermission")
    public void setObservers() {
        final Observer<Boolean> calibration_observer = isBluetoothEnabled -> {
            if (isBluetoothEnabled != null) {
                if (isBluetoothEnabled) {
                    Logger.getGlobal().log(Level.INFO, LOG_INTRO + "bluetooth is enabled");
                    calibrationViewModel.calibrate();
                } else {
                    Logger.getGlobal().log(Level.INFO, LOG_INTRO + "asking to enable bluetooth");
                    Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH);
                }
            } else
                Logger.getGlobal().log(Level.INFO, LOG_INTRO + "isBluetoothEnabled is false");
        };
        calibrationViewModel.getIsBluetoothEnabled().observe(this, calibration_observer);
    }

    public void setListeners() {
        binding.connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Use this check to determine whether Bluetooth classic is supported on the device.
                // Then you can selectively disable BLE-related features.
                if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                    Toast.makeText(CalibrationActivity.this, "R.string.bluetooth_not_supported", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    if (ActivityCompat.checkSelfPermission(CalibrationActivity.this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {
                        calibrationViewModel.initializeContext(CalibrationActivity.this);
                    } else
                        Toast.makeText(CalibrationActivity.this, "permission for bluetooth not granted", Toast.LENGTH_SHORT).show();
                }
            }
        });
        binding.buttonCalibrationGroupI.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectedThread.write("RECORD0");
            }
        });
        binding.buttonCalibrationGroupII.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectedThread.write("RECORD1");
            }
        });
        binding.buttonCalibrationGroupIII.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectedThread.write("RECORD2");
            }
        });
        binding.buttonCalibrationGroupIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectedThread.write("STOP");
            }
        });
    }

    public void registerReceivers() {
        IntentFilter actionFoundFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        IntentFilter actionDiscoveryStartedFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(receiver, actionFoundFilter);
        registerReceiver(receiver, actionDiscoveryStartedFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver);
    }
    @SuppressLint("MissingPermission")
    private class CreateConnectionThread extends Thread {
        public CreateConnectionThread(BluetoothAdapter bluetoothAdapter, String address) {
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
            connectedThread = new ConnectedDataThread(mmSocket);
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

    //Thread for data transfer
    public class ConnectedDataThread extends Thread {
        private BluetoothSocket mmSocket;
        private InputStream mmInStream;
        private OutputStream mmOutStream;

        public ConnectedDataThread(BluetoothSocket socket) {
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