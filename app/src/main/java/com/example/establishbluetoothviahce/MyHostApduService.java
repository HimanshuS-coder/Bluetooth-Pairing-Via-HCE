package com.example.establishbluetoothviahce;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class MyHostApduService extends HostApduService {

    private static final String TAG = "MyHostApduService";

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle bundle) {

        if (Arrays.equals(commandApdu,Utils.SELECT_APDU)){
            return Utils.SELECT_OK_SW;
        }else if(commandApdu[0] == (byte) 0x70 && commandApdu[1] == (byte) 0x02){
            byte[] deviceNameInByteArray = Arrays.copyOfRange(commandApdu,5,commandApdu.length);
            String deviceName = new String(deviceNameInByteArray, StandardCharsets.UTF_8);
            Log.d("Received Device Name",deviceName);

            // Create and send a broadcast
            Intent intent = new Intent("com.example.establishbluetoothviahce");
            intent.putExtra("remote_device_name", deviceName);
            sendBroadcast(intent);

//            HceCardActiviy.remoteDeviceName = deviceName;

            return new byte[0];
        }
        return new byte[0];
    }

    @Override
    public void onDeactivated(int i) {
        Log.i(TAG, "Deactivated: HCE connection lost" );
    }
}
