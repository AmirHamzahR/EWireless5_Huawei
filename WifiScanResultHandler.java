package com.example.datacollectionapp_afp;


import android.net.wifi.ScanResult;

import java.util.List;

public interface WifiScanResultHandler {
    void onWifiScanResultReceived(List<ScanResult> scanResults);
}
