package gr.hua.stapps.android.noisepollutionapp.network.sockets;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;

import java.io.IOException;
import java.util.UUID;

@SuppressLint("MissingPermission")
public class CreateConnection extends Thread {
    private static final String LOG = "NoisePollution: CreateConnection -> ";
    private static Context context;
    private BluetoothSocket mmSocket;
    private Handler handler;
    private DataTransfer dataTransfer;
    private final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status

    public CreateConnection(BluetoothAdapter bluetoothAdapter, String address, Context context, Handler handler, DataTransfer dataTransfer) {
        this.context = context;
        this.handler = handler;
        /*
            Use a temporary object that is later assigned to mmSocket
            because mmSocket is final.
        */
        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
        BluetoothSocket tmpBluetoothSocket = null;
        UUID uuid = bluetoothDevice.getUuids()[0].getUuid();

        try {
            /*
                Get a BluetoothSocket to connect with the given BluetoothDevice.
                Due to Android device varieties,the method below may not work fo different devices.
                You should try using other methods i.e. :
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                 */
            tmpBluetoothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) {
            System.out.println(LOG + "Socket's create() method failed with: " + e);
        }
        mmSocket = tmpBluetoothSocket;
    }

    public DataTransfer getDataTransfer() {
        return dataTransfer;
    }

    @Override
    public void run() {
        // Cancel discovery because it otherwise slows down the connection.
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothAdapter.cancelDiscovery();
        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            mmSocket.connect();
            System.out.println(LOG + "Device Connected");
            handler.obtainMessage(CONNECTING_STATUS, 1, -1).sendToTarget();
            ;
        } catch (IOException connectionException) {
            // Unable to connect; close the socket and return.
            try {
                mmSocket.close();
                System.out.println(LOG + "Cannot connect to device");
                handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();
            } catch (IOException closeException) {
                System.out.println(LOG + "Could not close the client socket, failed with: " + closeException);
            }
            return;
        }

        //The connection attempt succeeded. Perform work associated with the connection in a separate thread.
        dataTransfer = new DataTransfer(mmSocket, handler);
        dataTransfer.run();
    }

    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            System.out.println(LOG + "Could not close the client socket with: " + e);
        }
    }
}
