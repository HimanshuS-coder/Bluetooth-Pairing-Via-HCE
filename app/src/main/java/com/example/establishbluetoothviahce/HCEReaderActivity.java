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
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.establishbluetoothviahce.Connection.BluetoothPermission;
import com.example.establishbluetoothviahce.Connection.NFC_Utils;
import com.example.establishbluetoothviahce.Connection.StoragePermission;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;

public class HCEReaderActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    private static final String TAG = "HCEReaderActivity";
    private static final UUID MY_UUID = UUID.fromString("e8e10f95-1a70-4b27-9ccf-02010264e9c8");
    private NfcAdapter nfcAdapter;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothPermission bluetoothPermission;
    private StoragePermission storagePermission;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;
    String bluetoothAddress;
    String bluetoothDeviceName = "Galaxy Tab Active2";

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_hcereader);


        // Manage NFC
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        if (!NFC_Utils.isNfcEnabled(adapter,this)) {
            NFC_Utils.promptEnableNFC(this);
        }
        adapter.enableReaderMode(
                this,
                this,
                NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                null
        );

        // Manage Bluetooth Permissions
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothPermission = new BluetoothPermission(bluetoothAdapter, this);
        bluetoothPermission.enableBluetooth();

        // Manage Storage Permissions
        storagePermission = new StoragePermission(getApplicationContext(),this);
        storagePermission.isStoragePermissionGranted();

        // Start device discovery
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        Log.d("Start Intent for Bluetooth","Bluetooth discovery about to start");
        registerReceiver(receiver, filter);
        Log.d("Start Intent for Bluetooth","Register receiver");

        // Make this device discoverable (using Activity Result API)
        ActivityResultLauncher<Intent> discoverableIntentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_CANCELED) {
                        Toast.makeText(this, "Bluetooth discoverability not enabled", Toast.LENGTH_SHORT).show();
                    } else {
                        boolean isDiscovering = bluetoothAdapter.startDiscovery();
                        Log.d("Bluetooth Discovery", "Discovery started: " + isDiscovering);
                    }
                }
        );

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 600); // 10 minutes
        discoverableIntentLauncher.launch(discoverableIntent);  // Launch using the launcher



    }

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        public void onReceive(Context context, Intent intent) {
            Log.d("inside Broadcast","started onReceive method");
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.d("inside Broadcast","inside if");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d("inside Broadcast","receive device");
                if (device != null && device.getName().equals(bluetoothDeviceName)) {
                    // bluetoothAdapter.cancelDiscovery();
                    Log.d("inside Broadcast","going to pairDevice");
                    pairDevice(device);
                }
//                @SuppressLint("MissingPermission") String deviceName = device.getName();
//                String deviceAddress = device.getAddress();
//                Log.d(TAG, "Device found: " + deviceName + " [" + deviceAddress + "]");
//                // Pair with the device
//                Log.d("inside Broadcast","going to pairDevice");
//                pairDevice(device);
            }
        }
    };

    private void pairDevice(BluetoothDevice device) {
        try {
            Log.d("inside Pair Device","just started");
//            Method method = device.getClass().getMethod("createBond", (Class[]) null);
//            method.invoke(device, (Object[]) null);
            Log.d("inside Broadcast","started onReceive method");
            connectToBluetoothDevice(device);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        adapter.enableReaderMode(
                this,
                this,
                NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                null
        );
        registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("data finished", "done");
        NfcAdapter.getDefaultAdapter(this).disableReaderMode(this);
    }


    @Override
    public void onTagDiscovered(Tag tag) {

        IsoDep isoDep = IsoDep.get(tag);

        if (isoDep != null) {
            try {
                isoDep.connect();
                byte[] responseApdu = isoDep.transceive(Utils.SELECT_APDU);

                if (Arrays.equals(responseApdu,Utils.SELECT_OK_SW)){
                    responseApdu = isoDep.transceive(Utils.BLUETOOTH_REQUEST);

                }
                bluetoothAddress = new String(responseApdu);
                Log.d("Address received",bluetoothAddress);
//                checkBluetoothPermissions();
                //connectToBluetoothDevice(bluetoothAddress);



            } catch (Exception e) {
                Log.e(TAG, "Error communicating with HCE device", e);
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        bluetoothPermission.onActivityResult(requestCode, resultCode, data);
        storagePermission.onActivityResult(requestCode,resultCode,data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        bluetoothPermission.onRequestPermissionsResult(requestCode, permissions, grantResults);
        storagePermission.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    private void connectToBluetoothDevice(BluetoothDevice device) {
//        Log.d("inside connect To Bluetooth",address);
//        device = bluetoothAdapter.getRemoteDevice(address);
//        Log.d("inside connect To Bluetooth","going to connect thread");
//        new ConnectThread(device).start();
        if (device != null) {
            ConnectThread connectThread = new ConnectThread(device);
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
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                    if (checkSelfPermission(android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
//                            checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
//                            checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
//                            checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                        checkBluetoothPermissions();
//                    }
//                } else{
//                    if (checkSelfPermission(android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
//                            checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
//                        checkBluetoothPermissions();
//                    }
//                }
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
            if (ActivityCompat.checkSelfPermission(HCEReaderActivity.this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Log.d("INSIDE IF BLOCK TO CHECK PERMISSIONS","Will call check bluetooth method");
                // checkBluetoothPermissions();
            }
            Log.d("inside run","going to cancel discovery");
            bluetoothAdapter.cancelDiscovery();
            Log.d("inside run","canceled discovery");
            try {
                Log.d("inside run","going to connect socket");
                socket.connect();
                Log.d("inside run","socket connected");
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

            AsyncTask.execute(()->{
                OutputStream outputStream = null;
                FileInputStream fileInputStream = null;
                InputStream inputStream = null;
                try {
                    Log.d("Manage Connected SOcket","inside try block");

                    outputStream = socket.getOutputStream();
                    inputStream = socket.getInputStream();

                    Log.d("Manage Connected SOcket","got socket outputstream");

                    File file = new File("/storage/emulated/0/Download/test.pdf");

                    Log.d("Manage Connected SOcket","select file");

                    if (!file.exists()) {
                        Log.e(TAG, "File does not exist: " + file.getAbsolutePath());
                        return;
                    }

                    Log.d("Manage Connected SOcket","file selected"+file.getAbsolutePath());

                    fileInputStream = new FileInputStream(file);

                    Log.d("Manage Connected SOcket","file input stream");

                    byte[] buffer = new byte[1024*64];
                    int bytesRead;

                    Log.d("Manage Connected SOcket","going to write bytes");

                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        Log.d("Manage Connected Socket", "Writing bytes: " + bytesRead);
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    Log.d("Inside manage connected socket","out of while loop in HCE Reader");

                    outputStream.write("EOF".getBytes());
                    outputStream.flush();

                    // Wait for "ready" signal
                    byte[] readyBuffer = new byte[5];
                    Log.d("inside ManageConnectedSocket", "going to wait for ready signal");
                    int readyBytes = inputStream.read(readyBuffer); // Wait for "ready" from HCE Card
                    Log.d("inside ManageConnectedSocket", "ready signal received");
                    String readySignal = new String(readyBuffer, 0, readyBytes);
                    if ("ready".equals(readySignal)) {
                        Log.d("ManageConnectedSocket HCE Reader", "Received 'ready' signal");
                        runOnUiThread(() -> Toast.makeText(HCEReaderActivity.this, "File sent and acknowledged", Toast.LENGTH_SHORT).show());
                    } else {
                        Log.d("ManageConnectedSocket HCE Reader", "Didn't receive 'ready' signal: " + readySignal); // If no "ready" is received
                    }



                    Log.d("inside manageConnectedSocket","loaded the pdf to output Stream");


                } catch (Exception e) {
                    Log.e(TAG, "Error occurred when managing the connected socket", e);
                } finally {
                    try {
                        Log.d("ManageConnectedSocket HCE Reader", "Closing streams and socket");
                        if (fileInputStream != null) fileInputStream.close();
                        if (outputStream != null) outputStream.close();
                        //if (socket != null) socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }
}