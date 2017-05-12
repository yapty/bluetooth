package com.example.tze.bluetooth;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

/**
 * Created by NAPL on 5/12/2017.
 */

public class BluetoothConnectionService {
    private static final String TAG = "BluetoothConnectionServ";

    private static final String appName = "MYAPP";

    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    private final BluetoothAdapter mBluetoothAdapter;
    Context mContext;

    private AcceptThread mInsecureAcceptThread;

    private ConnectThread mConnectThread;
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;
    ProgressDialog mProgressDialog;

    private ConnectedThread mConnectedThread;

    @RequiresApi(api = Build.VERSION_CODES.GINGERBREAD_MR1)
    public BluetoothConnectionService(Context context) {
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {

        //The local server socket
        private final BluetoothServerSocket mmServerSocket;

        @RequiresApi(api = Build.VERSION_CODES.GINGERBREAD_MR1)
        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            //Create a new listening server socket
            try {
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, MY_UUID_INSECURE);

                Log.d(TAG, "AcceptThread: Setting up Server Using: " + MY_UUID_INSECURE);
            } catch (IOException e) {
                Log.e(TAG, "AccceptThread: IOException: " + e.getMessage());
            }

            mmServerSocket = tmp;

        }

        public void run() {
            Log.d(TAG, "run: AcceptThread Running.");

            BluetoothSocket socket = null;

            try {
                //This is a blocking call and will only return on a
                //successful connection or an exception
                Log.d(TAG, "run: RFCOM server socket start.....");

                socket = mmServerSocket.accept();

                Log.d(TAG, "run: RFCOM server socket accepted connection.");

            } catch (IOException e) {
                Log.e(TAG, "AccceptThread: IOException: " + e.getMessage());
            }

            if (socket != null) {
                connected(socket, mmDevice);
            }

            Log.i(TAG, "END mAcceptThread ");
        }

        public void cancel() {
            Log.d(TAG, "cancel: Canceling AcceptThread.");
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: Close of AcceptThread Server Socket failed. " + e.getMessage());
            }
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the conection either
     * succceeds or fails.
     */
    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            Log.d(TAG, "ConnectionThread: started.");
            mmDevice = device;
            deviceUUID = uuid;
        }

        public void run() {
            BluetoothSocket tmp = null;
            Log.i(TAG, "RUN mConnectedThread ");

            //Get a BluetoothSocket for a connection with the
            //given BluetoothDevice
            try {
                Log.d(TAG, "ConnectThread: Trying to create InsecureRfcommSocket using UUID: "
                        + MY_UUID_INSECURE);
                tmp = mmDevice.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread: Could not create InsecureRfcommSocket " + e.getMessage());
            }

            mmSocket = tmp;


            //Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();

            //Make a connection to the BluetoothSocket

            try {
                //This is a blocking call and will only return on a
                //successful connection or an exception
                mmSocket.connect();
                Log.d(TAG, "run: ConnectThread connected.");
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                    Log.d(TAG, "run: Closed Socket.");
                } catch (IOException e1) {
                    Log.e(TAG, "mConnectThread: run: Unable to close connecction in socket. " + e1.getMessage());
                }
                Log.d(TAG, "run: ConnectedThread: Could not connect to UUID: " +) MY_UUID_INSECURE);
            }

            //
            connected(mmSocket, mmDevice);
        }
        public void cancel(){
            try {
                Log.d(TAG, "cancel: Closing Client Socket.");
                mmSocket.close();
            } catch (IOException e){
                Log.e(TAG, "cancel: close() of mmSocket in Connectthread failed." + e.getMessage());
            }
        }
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    @RequiresApi(api = Build.VERSION_CODES.GINGERBREAD_MR1)
    public synchronized void start() {
        Log.d(TAG, "start");

        //Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread();
            mInsecureAcceptThread.start();
        }
    }

    /**
     * AcceptThread starts and sits waiting for a connection.
     * Then ConnectThread starts and attempts to make a connection with the other devices AcceptThread
     **/

     public void startClient(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "startClient: Started.");

         //initprogress dialog
         ProgressDialog = ProgressDialog.show(mContext,"Connecting Bluetooth"
                  ,"Please Wait...", true );

         mConnectThread = new ConnectThread(device, uuid);
         mConnectThread.start();
     }

    /** The ConnectedThread is responsible for maintaining th BTConnection, sending the data, and
     * receiving incoming data through input/output streams respectively.
     **/
    private class ConnectedThread extends Thread{

         private final BluetoothSocket mmSocket;
         private final InputStream mmInstream;
         private final OutputStream mmOutputStream;

         public ConnectedThread(BluetoothSocket socket) {
             Log.d(TAG, "ConnectedThread: Starting.");

             mmSocket = socket;
             InputStream tmpIn = null;
             OutputStream tmpOut = null;

             //dismiss the progressdialog when connection is established
             try{
                 mProgressDialog.dismiss();
             } catch(NullPointerException e){


             try {
                 tmpIn = mmSocket.getInputStream();
                 tmpOut = mmSocket.getOutputStream();
             } catch (IOException e) {
                 e.printStackTrace();
             }

             mmInstream = tmpIn;
             mmOutputStream = tmpOut;
         }

        public void run(){
             byte[] buffer = new byte[1024]; //buffer store for the stream

             int bytes; //bytes returned from read()

             // Keep listening to the InputStream until an exception occurs
             while (true) {
                 //Read from the InputStream
                 try {
                     bytes = mmInstream.read(buffer);
                     String incomingMessage = new String(buffer, 0, bytes);
                     Log.d(TAG, "InputStream: " + incomingMessage);
                 } catch (IOException e) {
                     Log.e(TAG, "write: Error reading Input Stream. " + e.getMessage());
                     break;
                 }
             }
         }

         // Call this from the main activity to send data to the remote device
        public void write(byte[] bytes) {
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: Writing to outputstream: " + text);
            try {
                mmOutputStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "write: Error writing to output stream. " + e.getMessage());
            }
        }

         //Call this from the main activity to shutdown the connection
         public void cancel(){
             try {
                 mmSocket.close();
             } catch (IOException e) { }
         }
    }

    private void connected(BluetoothSocket mmSocket, BluetoothDevice mmDevice) {
        Log.d(TAG, "connected: Starting.");

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */

    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;

        // Synchronize a copy of the ConnectedThread
        Log.d(TAG, "write: Write Called.");
        r = mConnectedThread;

        // Perform the write unsynchronized ]
        r.write(out);
    }
}