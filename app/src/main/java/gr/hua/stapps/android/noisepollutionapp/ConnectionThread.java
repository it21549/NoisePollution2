package gr.hua.stapps.android.noisepollutionapp;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressLint("MissingPermission")
public class ConnectionThread extends Thread {
    private static final String LOG_INTRO = "ConnectionThread -> ";
    private BluetoothSocket mmSocket;
    private Handler handler;
    private DataThread dataThread;

    private final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status

    public ConnectionThread(String address, Handler handler) {
        this.handler = handler;
        /*
            Use a temporary object that is later assigned to mmSocket
            because mmSocket is final.
             */
        BluetoothDevice bluetoothDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
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
            Logger.getGlobal().log(Level.INFO, LOG_INTRO + "Socket's create() method failed:" + e);
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
            Logger.getGlobal().log(Level.INFO, LOG_INTRO + "Status: " + "Device connected");
            handler.obtainMessage(CONNECTING_STATUS, 1, -1).sendToTarget();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            try {
                mmSocket.close();
                Logger.getGlobal().log(Level.INFO, "Status: " + "Cannot connect to device");
                handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();
            } catch (IOException closeException) {
                Logger.getGlobal().log(Level.INFO, LOG_INTRO + "Could not close the client socket:" + closeException);
            }
            return;
        }

        // The connection attempt succeeded. Perform work associated with
        // the connection in a separate thread.
        dataThread = new DataThread(mmSocket, handler);
        dataThread.run();
    }

    public DataThread getDataThread() {
        return dataThread;
    }

    // Closes the client socket and causes the thread to finish.
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Logger.getGlobal().log(Level.INFO, LOG_INTRO + "Could not close the client socket:" + e);
        }
    }
}
