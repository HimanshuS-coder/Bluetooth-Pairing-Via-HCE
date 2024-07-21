package com.example.establishbluetoothviahce;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.establishbluetoothviahce.Connection.BluetoothPermission;
import com.example.establishbluetoothviahce.Connection.NFC_Utils;
import com.example.establishbluetoothviahce.Connection.StoragePermission;

import org.w3c.dom.Text;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

public class HCECardActivity extends AppCompatActivity {

    private static final String TAG = "HCECardActivity";
    private static final String APP_NAME = "HCEBluetooth";
    private static final UUID MY_UUID = UUID.fromString("e8e10f95-1a70-4b27-9ccf-02010264e9c8");

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothPermission bluetoothPermission;
    private StoragePermission storagePermission;
    private AcceptThread acceptThread;
    private ActivityResultLauncher<Intent> discoverableIntentLauncher;
    TextView progressText;
    Boolean isReceived = false;
    Button openDoc;
    String pathToReceivedFile;


    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_hcecard);

        // Fetching Progress Text
        progressText = findViewById(R.id.progressText);

        // Fetching Open Document button
        openDoc = findViewById(R.id.openDoc);

        // Click listener to open the received document
        openDoc.setOnClickListener(view -> {
            if(isReceived && pathToReceivedFile!=null){
                File file = new File(pathToReceivedFile);

                Uri contentUri = FileProvider.getUriForFile(this,  "com.example.establishbluetoothviahce.provider", file);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(contentUri, "application/pdf");
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);  // Add this flag
                startActivity(intent);
            }else {
                Toast.makeText(this, "No path to the file received", Toast.LENGTH_SHORT).show();
            }
        });

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

        // Make this device discoverable (using Activity Result API)
        discoverableIntentLauncher = registerForActivityResult(
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


        acceptThread = new AcceptThread();
        acceptThread.start();
    }

    public void makeDiscoverable(){
        // This method will be called by StoragePermission class only when the storage permissions are already granted
        // Lines to make the bluetooth discoverable
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 600); // 10 minutes
        discoverableIntentLauncher.launch(discoverableIntent);  // Launch using the launcher
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BluetoothPermission.REQUEST_ENABLE_BT) { // Specific to Bluetooth
            Log.d("activity result","request bluetooth enable");
            bluetoothPermission.onActivityResult(requestCode, resultCode, data);
            Log.d("activity result","request bluetooth enable after");

            makeDiscoverable();

            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is enabled, Now ask for storage permissions
                Log.d("Bluetooth permission granted","going for storage permission");
                storagePermission.isStoragePermissionGranted();
            }

        } else if (requestCode == StoragePermission.REQUEST_CODE_STORAGE_PERMISSION) { // Specific to Storage
            storagePermission.onActivityResult(requestCode, resultCode, data);

            if (resultCode == Activity.RESULT_OK) {
                // Permission granted for storage , now make the device discoverable
                Log.d("Storage permission oncreate","Permission granted , making the device discoverable");
                // Lines to make the bluetooth discoverable
                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 600); // 10 minutes
                discoverableIntentLauncher.launch(discoverableIntent);  // Launch using the launcher
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == BluetoothPermission.REQUEST_PERMISSIONS) { // Handle Bluetooth permission result
            bluetoothPermission.onRequestPermissionsResult(requestCode, permissions, grantResults);

        } else if (requestCode == StoragePermission.REQUEST_CODE_STORAGE_PERMISSION) { // Handle storage permission result
            Log.d("Storage permission","Going for storage permission");
            storagePermission.onRequestPermissionsResult(requestCode, permissions, grantResults);

           // After permission granted for storage make the device in discoverable mode
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 600); // 10 minutes
                discoverableIntentLauncher.launch(discoverableIntent);  // Launch using the launcher
            }

        }
    }


    private class AcceptThread extends Thread {
        private final BluetoothServerSocket serverSocket;

        @SuppressLint("MissingPermission")
        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {

                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID);

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
//                    String fileName = "received_document_" + System.currentTimeMillis() + ".pdf";
                    String fileName = "received_document.pdf";
                    File file = new File(downloadsDir, fileName);

                    // Fetching the absolute path of the file received to use it later for opening it.
                    pathToReceivedFile = file.getAbsolutePath();
                    Log.d("File Path", pathToReceivedFile); // Log the path for debugging

//                    File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "received_document.pdf");
                    fileOutputStream = new FileOutputStream(file);

                    Log.d("inside ManageConnectedSocket","Saving the file");

                    byte[] buffer = new byte[1024*2];
                    int bytesRead;

                    // Tracking the Progress
                    DataInputStream dataInputStream = new DataInputStream(inputStream);
                    int fileSize = dataInputStream.readInt(); // Read file size as an integer
                    Log.d("Received File Size","Expected FIle size: "+fileSize);
                    long totalBytesRead = 0;

                    Log.d("Manage Connected SOcket","going to read inputstream");

//                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                    while (true) {
                        bytesRead = inputStream.read(buffer);
                        fileOutputStream.write(buffer, 0, bytesRead);
                        if (bytesRead == -1 || new String(buffer, 0, bytesRead).contains("EOF")) {
                            break;
                        }

                        // Tracking the progress
                        totalBytesRead += bytesRead;

                        final int progress = (int) ((totalBytesRead * 100)/fileSize);
                        runOnUiThread(()->{
                            progressText.setText("Receiving Doc: "+progress+"%");
                        });

                        Log.d("Manage Connected Socket", "Reading bytes: " + bytesRead);

                    }
                    Log.d("inside ManageConnectedSocket", "Out of While loop");

                    fileOutputStream.flush();

                    outputStream.write("ready".getBytes());

                    Log.d("inside ManageConnectedSocket", "Sent ready signal");

                    runOnUiThread(() -> {
                        isReceived = true;
                        openDoc.setEnabled(true);
                        progressText.setText("Document Received");
                        Toast.makeText(HCECardActivity.this, "File received in Downloads Folder", Toast.LENGTH_LONG).show();
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error occurred when managing the connected socket", e);
                } finally {
                    try {
                        Log.d("Finally Block","Started Closing things");
                        if (inputStream != null) inputStream.close();
                        if (fileOutputStream != null) fileOutputStream.close();
                        if (outputStream != null) outputStream.close();

                        // Unpair using reflection
                        if (socket != null && socket.getRemoteDevice() != null) {
                            try {
                                Method removeBondMethod = socket.getRemoteDevice().getClass().getMethod("removeBond");

                                boolean result = false;
                                Object returnValue = removeBondMethod.invoke(socket.getRemoteDevice());
                                if (returnValue instanceof Boolean) {
                                    result = (Boolean) returnValue;
                                }

                                if (result) {
                                    Log.d(TAG, "Successfully unpaired device from receiver");
                                    runOnUiThread(() -> {
                                        Toast.makeText(HCECardActivity.this, "Unpaired", Toast.LENGTH_SHORT).show();


                                    });
                                } else {
                                    Log.e(TAG, "Failed to unpair device from receiver");
                                }
                            } catch (NoSuchMethodException e) {
                                Log.e(TAG, "Method removeBond not found (receiver)", e);
                            } catch (Exception e) {
                                Log.e(TAG, "Error occurred while unpairing (receiver)", e);
                            }
                        } else {
                            Log.e(TAG, "Cannot unpair: socket or remote device is null (receiver)");
                        }

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