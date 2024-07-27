package com.example.establishbluetoothviahce;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.establishbluetoothviahce.Connection.BluetoothPermission;
import com.example.establishbluetoothviahce.Connection.NFC_Utils;
import com.example.establishbluetoothviahce.Connection.StoragePermission;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothPermission bluetoothPermission;
    private StoragePermission storagePermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Button Signer = findViewById(R.id.CardButton);
        Button Signee = findViewById(R.id.ReaderButton);

        // Manage NFC
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        if (!NFC_Utils.isNfcEnabled(adapter,this)) {
            NFC_Utils.promptEnableNFC(this);
        }

        // Manage Bluetooth permissions
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothPermission = new BluetoothPermission(bluetoothAdapter,this);
        bluetoothPermission.enableBluetooth();

        storagePermission = new StoragePermission(getApplicationContext(),this);

        Signer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, HCECardActivity.class);
                startActivity(intent);
            }
        });

        Signee.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, HCEReaderActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BluetoothPermission.REQUEST_ENABLE_BT) { // Specific to Bluetooth
            Log.d("activity result","request bluetooth enable");
            bluetoothPermission.onActivityResult(requestCode, resultCode, data);
            Log.d("activity result","request bluetooth enable after");

            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is enabled, Now ask for storage permissions
                Log.d("Bluetooth permission granted","going for storage permission");
                storagePermission.isStoragePermissionGranted();
            }

        } else if (requestCode == StoragePermission.REQUEST_CODE_STORAGE_PERMISSION) { // Specific to Storage
            storagePermission.onActivityResult(requestCode, resultCode, data);
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

        }
    }
}