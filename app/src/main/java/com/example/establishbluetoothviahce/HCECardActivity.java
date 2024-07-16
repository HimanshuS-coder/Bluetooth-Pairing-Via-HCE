package com.example.establishbluetoothviahce;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.establishbluetoothviahce.Connection.BluetoothPermission;
import com.example.establishbluetoothviahce.Connection.NFC_Utils;
import com.example.establishbluetoothviahce.Connection.StoragePermission;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class HCECardActivity extends AppCompatActivity {

    private static final String TAG = "HCECardActivity";
    private static final String APP_NAME = "HCEBluetooth";
    private static final UUID MY_UUID = UUID.fromString("e8e10f95-1a70-4b27-9ccf-02010264e9c8");

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothPermission bluetoothPermission;
    private StoragePermission storagePermission;
    private AcceptThread acceptThread;


    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_hcecard);

        // Manage NFC
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        if (!NFC_Utils.isNfcEnabled(adapter,this)) {
            NFC_Utils.promptEnableNFC(this);
        }

        // Manage Bluetooth permissions
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothPermission = new BluetoothPermission(bluetoothAdapter,this);
        bluetoothPermission.enableBluetooth();
        Log.d("Bluetooth address of Card ", bluetoothAdapter.getName());
        //checkBluetoothPermissions();

        // Manage Storage Permissions
        storagePermission = new StoragePermission(getApplicationContext(), this);
        storagePermission.isStoragePermissionGranted();

        // Make this device discoverable (using Activity Result API)
        ActivityResultLauncher<Intent> discoverableIntentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_CANCELED) {
                        Toast.makeText(this, "Bluetooth discoverability not enabled", Toast.LENGTH_SHORT).show();
                    } else {
                        @SuppressLint("MissingPermission") boolean isDiscovering = bluetoothAdapter.startDiscovery();
                        Log.d("Bluetooth Discovery", "Discovery started: " + isDiscovering);
                    }
                }
        );

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300); // 5 minutes
        discoverableIntentLauncher.launch(discoverableIntent);  // Launch using the launcher


        acceptThread = new AcceptThread();
        acceptThread.start();
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

//    private void checkBluetoothPermissions() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            if (checkSelfPermission(android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
//                    checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
//                    checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
//                    checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//
//                requestPermissions(new String[]{
//                        android.Manifest.permission.BLUETOOTH,
//                        android.Manifest.permission.BLUETOOTH_SCAN,
//                        android.Manifest.permission.BLUETOOTH_ADMIN,
//                        android.Manifest.permission.BLUETOOTH_CONNECT
//                }, REQUEST_PERMISSIONS);
//            }else{
//                Log.d("inside check bluetooth permission","ANDROID S +");
//            }
//        } else{
//            if (checkSelfPermission(android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
//                    checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
//
//                requestPermissions(new String[]{
//                        android.Manifest.permission.BLUETOOTH,
//                        Manifest.permission.BLUETOOTH_ADMIN
//                }, REQUEST_PERMISSIONS);
//            }else{
//                Log.d("inside check bluetooth permission","ANDROID S -");
//            }
//        }
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == REQUEST_PERMISSIONS) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                Log.d("Permission Granted","Permission Granted");
//            } else {
//                Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket serverSocket;

        @SuppressLint("MissingPermission")
        public AcceptThread() {
            BluetoothServerSocket tmp = null;
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
                Log.d("inside Accept Thread","trying to receive something");
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID);
                Log.d("inside Accept Thread","received something in tmp");
            } catch (Exception e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
            serverSocket = tmp;
            Log.d("inside RUN IF","serverSocket initialised");
        }

        public void run() {
            Log.d("inside RUN","Just Started");
            BluetoothSocket socket = null;
            while (true) {
                try {
                    Log.d("inside RUN","Socket yet to accept");
                    socket = serverSocket.accept();
                    Log.d("inside RUN","Socket Accepted");
                } catch (Exception e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    Log.d("inside RUN IF","received something in socket");
                    manageConnectedSocketHCE_Card(socket);
                    try {
                        Log.d("inside RUN IF","going to close the socket");
                        //serverSocket.close();
                        Log.d("inside RUN IF","socket closed");
                    } catch (Exception e) {
                        Log.e(TAG, "Could not close the connect socket", e);
                    }
                    break;
                }
            }
        }

        private void manageConnectedSocketHCE_Card(BluetoothSocket socket) {
            Log.d("inside ManageConnectedSocket","Just Started");

            AsyncTask.execute(()->{
                InputStream inputStream = null;
                FileOutputStream fileOutputStream = null;
                OutputStream outputStream = null;
                try {

                    Log.d("inside ManageConnectedSocket","Try block");

                    inputStream = socket.getInputStream();
                    outputStream = socket.getOutputStream();

                    Log.d("inside ManageConnectedSocket InputStream","Received something in Input Stream");

                    // Get the public Downloads directory
                    File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

                    // Create a unique file name to avoid conflicts
                    String fileName = "received_document_" + System.currentTimeMillis() + ".pdf";
                    File file = new File(downloadsDir, fileName);

//                    File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "received_document.pdf");
                    fileOutputStream = new FileOutputStream(file);

                    Log.d("inside ManageConnectedSocket","Saving the file");

                    byte[] buffer = new byte[1024*64];
                    int bytesRead;

                    Log.d("Manage Connected SOcket","going to read inputstream");

//                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                    while (true) {
                        bytesRead = inputStream.read(buffer);
                        fileOutputStream.write(buffer, 0, bytesRead);
                        if (bytesRead == -1 || new String(buffer, 0, bytesRead).contains("EOF")) {
                            break;
                        }
                        Log.d("Manage Connected Socket", "Reading bytes: " + bytesRead);

                    }
                    Log.d("inside ManageConnectedSocket", "Out of While loop");

                    fileOutputStream.flush();

                    outputStream.write("ready".getBytes());

                    Log.d("inside ManageConnectedSocket", "Sent ready signal");

                    runOnUiThread(() -> Toast.makeText(HCECardActivity.this, "File received", Toast.LENGTH_SHORT).show());
                } catch (Exception e) {
                    Log.e(TAG, "Error occurred when managing the connected socket", e);
                } finally {
                    try {
                        Log.d("Finally Block","Started Closing things");
                        if (inputStream != null) inputStream.close();
                        if (fileOutputStream != null) fileOutputStream.close();
                        if (outputStream != null) outputStream.close();
                        if (socket != null) socket.close();
                        Log.d("Finally Block", "Closed all streams and socket");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

        }
    }
}