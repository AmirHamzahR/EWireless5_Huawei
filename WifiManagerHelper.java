package com.example.datacollectionapp_afp;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.widget.TextView;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class WifiManagerHelper{
    private Context context;
    protected WifiManager wifiManager;
    protected WifiScanReceiver wifiScanReceiver;
    protected Handler handlerWifi;
    private TimerTask scanTaskWifi;
    private WifiScanResultHandler wifiScanResultHandler;
    protected Timer timerWifi;
    protected TextView wifiTextView;
    protected TextView wifiStatusView;
    protected TextView wifiInfoView;
    protected TextView wifiConnectionView;
    protected float measuredWifiFreq;
    protected double lastWifiTimestamp;
    protected String wifiFeaturesText;
    protected double timestamp;
    protected long initialTimeNs;
    private long timestampNs;
    protected StringBuilder wifiCsvData = new StringBuilder();

    public WifiManagerHelper(Context context, WifiScanResultHandler wifiScanResultHandler) {
        this.context = context;
        this.wifiScanResultHandler = wifiScanResultHandler;
        wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    public void registerWifiReceiver() {
        context.registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    public void unregisterWifiReceiver() {
        context.unregisterReceiver(wifiScanReceiver);
    }

    public void setWiFiScanHandler() {
        String wifiStr;
        if (wifiManager != null) {
            if (!wifiManager.isWifiEnabled()) { // if Wi-Fi is not enabled, turn it on
                wifiStatusView.setText("\n" + " WIFI: Switched OFF");
                wifiStatusView.setBackgroundColor(0xFFFF0000); // red color (later the timer will turn it to green)
                wifiStr = "\n" + " Not available ";
                if (wifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLING) { // if Wi-Fi is not already in the process of being enabled, turn it on
                    wifiManager.setWifiEnabled(true); // turn Wi-Fi on
                }
            } else {
                wifiStatusView.setText( "\n" + " WIFI: Switched ON");
                wifiStatusView.setBackgroundColor(0x00FF00); //lime green color
                wifiStr = "\n" + " WiFi MAC address: " + wifiManager.getConnectionInfo().getMacAddress();
            }
        } else {
            wifiStatusView.setText("\n" + " WIFI: Not available");
            wifiStatusView.setBackgroundColor(0xFFFF0000); // red color
            wifiStr = "\n" + "No Features";
        }
        wifiInfoView.setText(wifiStr);

        // Register a broadcast receiver that listens for WiFi scan results.
        wifiScanReceiver = new WifiScanReceiver(wifiScanResultHandler) {
            @Override
            public void onReceive(Context context, Intent intent) {
                long rawTimestampNs = System.nanoTime();
                if (rawTimestampNs >= initialTimeNs) {
                    timestampNs = rawTimestampNs - initialTimeNs;
                } else {
                    timestampNs = (rawTimestampNs - initialTimeNs) + Long.MAX_VALUE;
                }
                timestamp = ((double) (timestampNs)) * 1E-9;
                List<ScanResult> results = wifiManager.getScanResults();
                wifiScanResultHandler.onWifiScanResultReceived(results);
            }
        };

        scanTaskWifi = new TimerTask() {
            public void run() {
                handlerWifi.post(() -> {
                    if (wifiManager != null && wifiManager.isWifiEnabled()) {
                        wifiManager.startScan();
                    }
                });
            }
        };

        // Initialize the handler and timer
        handlerWifi = new Handler();
        timerWifi = new Timer();

        // Schedule the timer to run the scanTaskWifi every 10 seconds
        timerWifi.schedule(scanTaskWifi, 3000, 3000);
    }


    public WifiScanReceiver getWifiScanReceiver() {
        return wifiScanReceiver;
    }


}


