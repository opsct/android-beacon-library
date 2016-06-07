package com.connecthings.altbeacon.beacon.startup;

import android.content.Context;

import com.connecthings.altbeacon.beacon.MonitorNotifier;

public interface BootstrapNotifier extends MonitorNotifier {
    public Context getApplicationContext();
}
