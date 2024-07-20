package com.example.establishbluetoothviahce.Connection;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.establishbluetoothviahce.HCECardActivity;

public class StoragePermission {

    Context context;
    Activity activity;
    public static final int REQUEST_CODE_STORAGE_PERMISSION = 100;

    public StoragePermission(Context context, Activity activity){
        this.context = context;
        this.activity = activity;
    }

    // Check if storage permissions are granted
    public void isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 and above: Check if the app can manage all files
            boolean isGranted = Environment.isExternalStorageManager();
            if (!isGranted) requestStoragePermission();
        } else {
            // Android 10 and below: Check traditional permissions
            int readPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
            int writePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            boolean isGranted = readPermission == PackageManager.PERMISSION_GRANTED && writePermission == PackageManager.PERMISSION_GRANTED;
            if (!isGranted) requestStoragePermission();
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 and above: Request "manage all files" permission
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
            intent.setData(uri);
            activity.startActivityForResult(intent, REQUEST_CODE_STORAGE_PERMISSION);

//        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            // Android 10: Open settings to let the user grant "manage all files" permission
//            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
//            activity.startActivityForResult(intent, REQUEST_CODE_STORAGE_PERMISSION);

        } else {
            // Android 9 and below: Request traditional permissions
            ActivityCompat.requestPermissions(activity,new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                    }, REQUEST_CODE_STORAGE_PERMISSION);
        }
    }

    // Result of user in Android 11 -
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Permission Granted","Permission Granted");
                Toast.makeText(activity, "Storage Permission Granted on Android 11 -", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(activity, "Storage permissions are required on Android 11 -", Toast.LENGTH_SHORT).show();
                isStoragePermissionGranted();
            }
        }
    }

    // Result of user choice in Android 11 +
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(activity, "Storage Permission Granted on Android 11 +", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(activity, "Storage Permission Required, Retrying on Android 11 +", Toast.LENGTH_SHORT).show();
                isStoragePermissionGranted();
            }
        }
    }
}
