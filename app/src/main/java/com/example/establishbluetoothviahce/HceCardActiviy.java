package com.example.establishbluetoothviahce;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.establishbluetoothviahce.Connection.BluetoothPermission;
import com.example.establishbluetoothviahce.Connection.StoragePermission;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

public class HceCardActiviy extends AppCompatActivity{

    private static final String TAG = "HceCardActivity";
    private static final UUID MY_UUID = UUID.fromString("e8e10f95-1a70-4b27-9ccf-02010264e9c8");
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothPermission bluetoothPermission;
    private StoragePermission storagePermission;
    BluetoothDeviceFoundReceiver receiver;
    private ActivityResultLauncher<Intent> discoverableIntentLauncher;
    private static final int REQUEST_CODE_PICK_PDF = 5;
    private ActivityResultLauncher<Intent> pickPdfLauncher;
    private String pdfPath;
    private BluetoothDevice foundDevice;
    private boolean isReadyToSend = false;
    private Button sendButton;
    private ProgressBar progressBar;
    private TextView progressText;
    private String targetDeviceName;
    private boolean isReceiverRegistered = false;
    ConnectThread connectThread;
    public static String remoteDeviceName = null;


    @SuppressLint("MissingPermission")

    private BroadcastReceiver dataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"STARTED BROADCAST RECEIVER");
            String receivedData = intent.getStringExtra("remote_device_name");
            pairDevice(receivedData);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_hcereader);

        // Fetching Chooser Button
        Button chooseDocument = findViewById(R.id.buttonView);
        TextView textView = findViewById(R.id.textView);
        progressText = findViewById(R.id.progressText);
        progressBar = findViewById(R.id.progressBar);

        // Fetching Send Button
        sendButton = findViewById(R.id.sendButton);

        // On Chooser button clicked
        chooseDocument.setOnClickListener(v -> {
            pickPdf();
        });

        // On Send Button clicked
        sendButton.setOnClickListener(view -> {
            if (isReadyToSend && pdfPath != null) {
                progressBar.setVisibility(View.VISIBLE);
                progressText.setText("Sending Document..");
                connectToBluetoothDevice(foundDevice); // Use the foundDevice stored in pairDevice
            } else {
                Toast.makeText(this, "Not ready to send yet or no file selected", Toast.LENGTH_SHORT).show();
            }
        });


//        // Manage NFC
//        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
//
//        adapter.enableReaderMode(
//                this,
//                this,
//                NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
//                null
//        );

        // Manage Bluetooth Permissions
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Intent to make this device discoverable (using Activity Result API)
        discoverableIntentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_CANCELED) {
                        Toast.makeText(this, "Bluetooth discoverability not enabled", Toast.LENGTH_SHORT).show();
                    } else {
                        @SuppressLint("MissingPermission")
                        boolean isDiscovering = bluetoothAdapter.startDiscovery();
                        Log.d("Bluetooth Discovery", "Discovery started: " + isDiscovering);
                    }
                }
        );

        // Intent to launch media picker
        pickPdfLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            Uri uri = data.getData();
                            if (uri != null) {
                                pdfPath = Utils.getPathFromUri(this,uri);
                                textView.setText("Doc Path: "+pdfPath);
                                Log.d("MainActivity", "Selected PDF Path: " + pdfPath);
                                Toast.makeText(this, "Selected PDF Path: " + pdfPath, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );

        // Register the receiver
        IntentFilter filter = new IntentFilter("com.example.establishbluetoothviahce");
        registerReceiver(dataReceiver, filter);

    }

    private void pickPdf() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        pickPdfLauncher.launch(intent);
    }



    private void pairDevice(String deviceName) {
        Log.d(TAG,"Inside pariDevice");
        targetDeviceName = deviceName;

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 600); // 10 minutes
        discoverableIntentLauncher.launch(discoverableIntent);  // Launch using the launcher

        runOnUiThread(()->{
            // Make progressBar visible with text
            progressBar.setVisibility(View.VISIBLE);
            progressText.setVisibility(View.VISIBLE);
            String search = "Searching for device: " + targetDeviceName;
            progressText.setText(search);
        });

        Log.d(TAG,"Starting Braodcast Receiver");
        receiver = new BluetoothDeviceFoundReceiver();
        receiver.setHceCardActiviy(this);
        receiver.setTargetDeviceName(targetDeviceName);

    }

    @SuppressLint("MissingPermission")
    public void onDeviceFound(BluetoothDevice device) {
        Log.d(TAG, "Device found: " + device.getName());
//        bluetoothAdapter.cancelDiscovery();
        unregisterReceiver(receiver);
        isReceiverRegistered = false;

        foundDevice = device; // Store the discovered device
        isReadyToSend = true;
        Log.d("inside Broadcast", "Device paired and ready to send.");
        runOnUiThread(() ->{
            // Alternate Code in order to send document when the send button is clicked
            progressText.setText("Device found: " + targetDeviceName);
            progressBar.setVisibility(View.INVISIBLE);
            Toast.makeText(HceCardActiviy.this,"Device is Ready to Send the Document",Toast.LENGTH_SHORT).show();
            sendButton.setEnabled(true);
        } ); // Enable the send button
    }


    @Override
    protected void onResume() {
        super.onResume();
//        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
//        adapter.enableReaderMode(
//                this,
//                this,
//                NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
//                null
//        );
        if (!isReceiverRegistered && receiver != null && targetDeviceName != null) {
            // These lines should be placed below BroadCast Receiver so that after discovering first device it again run these lines and after discover more devices
            // Start device discovery
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(receiver, filter);
            isReceiverRegistered = true; // Mark the receiver as registered
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("data finished", "done");
//        NfcAdapter.getDefaultAdapter(this).disableReaderMode(this);
    }


//    @Override
//    public void onTagDiscovered(Tag tag) {
//
//        IsoDep isoDep = IsoDep.get(tag);
//
//        if (isoDep != null) {
//            try {
//                isoDep.connect();
//                byte[] responseApdu = isoDep.transceive(Utils.SELECT_APDU);
//
//                if (Arrays.equals(responseApdu,Utils.SELECT_OK_SW)){
//                    responseApdu = isoDep.transceive(Utils.BLUETOOTH_REQUEST);
//                }
//                String deviceName = new String(responseApdu, StandardCharsets.UTF_8);
//                Log.d("Received Device Name",deviceName);
//                pairDevice(deviceName);
//
//
//            } catch (Exception e) {
//                Log.e(TAG, "Error communicating with HCE device", e);
//            }
//        }
//
//    }

    private void connectToBluetoothDevice(BluetoothDevice device) {
        if (device != null) {
            connectThread = new ConnectThread(device);
            connectThread.start();
        } else {
            Log.d(TAG, "No device selected for connection.");
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;

        @SuppressLint("MissingPermission")
        public ConnectThread(BluetoothDevice device) {
            Log.d("inside ConnectThread Constructor","just started");
            BluetoothSocket tmp = null;

            try {
                Log.d("Inside Connect Thread ","Got inside");
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (Exception e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            Log.d("Inside Connect Thread","Outside Try Block");
            socket = tmp;
            Log.d("Inside Connect Thread","Method Finished");
        }

        public void run() {
            if (ActivityCompat.checkSelfPermission(HceCardActiviy.this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Log.d("INSIDE IF BLOCK TO CHECK PERMISSIONS","Will call check bluetooth method");
                // checkBluetoothPermissions();
            }
            bluetoothAdapter.cancelDiscovery();
            try {
                socket.connect();
                manageConnectedSocketHCE_Reader(socket);
            } catch (Exception e) {
                Log.e(TAG, "Unable to connect; closing the socket", e);
                try {
                    socket.close();
                } catch (Exception ex) {
                    Log.e(TAG, "Could not close the client socket", ex);
                }
            }
        }

        private void manageConnectedSocketHCE_Reader(BluetoothSocket socket) {
            Log.d("Manage Connected SOcket","got inside");

//            AsyncTask.execute(()->{
                OutputStream outputStream = null;
                FileInputStream fileInputStream = null;
                InputStream inputStream = null;
                try {

                    outputStream = socket.getOutputStream();
                    inputStream = socket.getInputStream();

                    File file = new File(pdfPath);

                    if (!file.exists()) {
                        Log.e(TAG, "File does not exist: " + file.getAbsolutePath());
                        return;
                    }

                    Log.d("Manage Connected SOcket","file selected"+file.getAbsolutePath());

                    fileInputStream = new FileInputStream(file);

                    byte[] buffer = new byte[1024];
                    int bytesRead;

                    // Sending the total file size so as to track the received progress on the other side
                    DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
                    dataOutputStream.writeInt((int) file.length());
                    dataOutputStream.flush();
                    // **************************************

                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        Log.d("Manage Connected Socket", "Writing bytes: " + bytesRead);
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    Log.d("Inside manage connected socket","out of while loop in HCE Reader");

                    outputStream.write("EOF".getBytes());
                    outputStream.flush();

                    // Wait for "ready" signal
                    byte[] readyBuffer = new byte[5];
                    int readyBytes = inputStream.read(readyBuffer); // Wait for "ready" from HCE Card
                    String readySignal = new String(readyBuffer, 0, readyBytes);
                    if ("ready".equals(readySignal)) {
                        Log.d("ManageConnectedSocket HCE Reader", "Received 'ready' signal");
                        runOnUiThread(() ->{
                            progressBar.setVisibility(View.GONE);
                            progressText.setText("Document Sent :)");
                            Toast.makeText(HceCardActiviy.this, "File sent and acknowledged", Toast.LENGTH_SHORT).show();
                        } );
                    } else {
                        Log.d("ManageConnectedSocket HCE Reader", "Didn't receive 'ready' signal: " + readySignal); // If no "ready" is received
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error occurred when managing the connected socket", e);
                } finally {
                    try {
                        Log.d("ManageConnectedSocket HCE Reader", "Closing streams and socket");
                        if (fileInputStream != null) fileInputStream.close();
                        if (outputStream != null) outputStream.close();
                        if(inputStream!= null) inputStream.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
//            });

        }

        @SuppressLint("MissingPermission")
        public void cancel() {
            try {
                // Unpair Device using Reflection
                if(foundDevice != null) {
                    Method removeBondMethod = foundDevice.getClass().getMethod("removeBond");
                    boolean result = (boolean) removeBondMethod.invoke(foundDevice);
                    if (result) {
                        Log.d(TAG, "Successfully unpaired device");
                        runOnUiThread(() -> Toast.makeText(HceCardActiviy.this, "Unpaired", Toast.LENGTH_SHORT).show());
                    } else {
                        Log.e(TAG, "Failed to unpair device");
                    }
                }

                Log.d("inside RUN IF","socket Cancelled");
                if (socket != null) socket.close();
                Log.d("inside RUN IF","socket closed");
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e){
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(dataReceiver);
        if (isReceiverRegistered && receiver != null) { // Unregister only if registered
            unregisterReceiver(receiver);
        }
        if(connectThread!=null){
            connectThread.cancel();
        }
    }
}