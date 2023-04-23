package com.example.datacollectionapp_afp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import java.util.List;

public class WifiScanReceiver extends BroadcastReceiver {
    private WifiScanResultHandler wifiScanResultHandler;
    private List<ScanResult> scanResults;

    public WifiScanReceiver(WifiScanResultHandler wifiScanResultHandler) {
        this.wifiScanResultHandler = wifiScanResultHandler;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            scanResults = wifiManager.getScanResults();
            // Call onWifiScanResultReceived with the scan results
            wifiScanResultHandler.onWifiScanResultReceived(scanResults);
        }
    }

}
