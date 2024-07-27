package com.example.establishbluetoothviahce.Connection;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

public class BluetoothPermission {

    public static final int REQUEST_ENABLE_BT = 1;
    public static final int REQUEST_PERMISSIONS = 2;
    private Activity activity;
    private BluetoothAdapter bluetoothAdapter;

    public BluetoothPermission(BluetoothAdapter bluetoothAdapter, Activity activity) {
        this.activity = activity;
        this.bluetoothAdapter = bluetoothAdapter;
    }

    public void enableBluetooth() {

        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Toast.makeText(activity, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show();
            activity.finish();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 and above
            if (activity.checkSelfPermission(android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                    activity.checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                    activity.checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    activity.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(activity,new String[]{
                        android.Manifest.permission.BLUETOOTH,
                        android.Manifest.permission.BLUETOOTH_SCAN,
                        android.Manifest.permission.BLUETOOTH_ADMIN,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.BLUETOOTH_CONNECT
                }, REQUEST_PERMISSIONS);
            } else {
                Log.d("inside check bluetooth permission", "ANDROID S +");
                requestBluetoothEnable();
            }
        } else {
            // Android 6 to 11
            if (activity.checkSelfPermission(android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                    activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    activity.checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(activity,new String[]{
                        android.Manifest.permission.BLUETOOTH,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.BLUETOOTH_ADMIN
                }, REQUEST_PERMISSIONS);
            } else {
                Log.d("inside check bluetooth permission", "ANDROID S -");
                requestBluetoothEnable();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void requestBluetoothEnable() {
        // Will call onActivityResult method
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    // This method will run during any permission request
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Permission Granted","Permission Granted");
                requestBluetoothEnable();
            } else {
                Toast.makeText(activity, "Bluetooth permissions are required", Toast.LENGTH_SHORT).show();
                enableBluetooth();
            }
        }
    }

    // This method will run when user either grant or reject the intent
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is enabled
                Toast.makeText(activity, "Bluetooth has been enabled", Toast.LENGTH_SHORT).show();
            } else {
                // User did not enable Bluetooth or an error occurred
                Toast.makeText(activity, "Bluetooth enabling canceled, Retrying", Toast.LENGTH_SHORT).show();
                enableBluetooth();
            }
        }
    }
}
