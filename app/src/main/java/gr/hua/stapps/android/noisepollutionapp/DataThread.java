package gr.hua.stapps.android.noisepollutionapp;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

//Thread for data transfer
public class DataThread extends Thread {
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private static final String LOG_INTRO = "DataThread -> ";

    private Handler handler;
    private BluetoothSocket mmSocket;
    private InputStream mmInStream;
    private OutputStream mmOutStream;

    public DataThread(BluetoothSocket socket, Handler handler) {
        mmSocket = socket;
        this.handler = handler;
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
                    //System.out.println("Arduino Message: " + readMessage);
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
            Logger.getGlobal().log(Level.INFO, LOG_INTRO + "Send Error:" + "Unable to send message: " + e);
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
