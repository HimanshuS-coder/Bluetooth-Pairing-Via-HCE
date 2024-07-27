package com.example.establishbluetoothviahce.Connection;

import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.provider.Settings;

public class NFC_Utils {

    public static boolean isNfcEnabled(NfcAdapter nfcAdapter, Context context) {
        nfcAdapter = NfcAdapter.getDefaultAdapter(context);
        return nfcAdapter != null && nfcAdapter.isEnabled();
    }

    public static void promptEnableNFC(Context context) {
        Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
        context.startActivity(intent);
    }


}
