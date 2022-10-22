package gr.hua.stapps.android.noisepollutionapp.network.sockets;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DataTransfer extends Thread {
    private static final String LOG = "NoisePollution: DataTransfer -> ";
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private Handler handler;
    private static final int MESSAGE_READ = 2;

    public DataTransfer(BluetoothSocket socket, Handler handler) {
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
            System.out.println(LOG + "Exception in IO stream: " + e);
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024]; // buffer store for the stream
        int bytes = 0; //bytes returned from read()
        // Keep listening to the InputStream until an exception occurs
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
                    System.out.println(LOG + "Arduino Message is: " + readMessage);
                    handler.obtainMessage(MESSAGE_READ, readMessage).sendToTarget();
                    bytes = 0;
                } else {
                    bytes++;
                }
            } catch (IOException e) {
                System.out.println(LOG + "Error obtaining message");
                e.printStackTrace();
                break;
            }
        }
    }

    /* Call this from the main activity to send data to the remote device */
    public void write(String input) {
        byte[] bytes = input.getBytes(); //converts entered String into bytes
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) {
            System.out.println(LOG + "Failed to send message with: " + e);
        }
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            System.out.println(LOG + "Error closing connection: " + e);
        }
    }
}
