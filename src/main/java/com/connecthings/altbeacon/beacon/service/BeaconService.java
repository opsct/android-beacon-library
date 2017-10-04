/**
 * Radius Networks, Inc.
 * http://www.radiusnetworks.com
 *
 * @author David G. Young
 * <p/>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.connecthings.altbeacon.beacon.service;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.MainThread;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;

import com.connecthings.altbeacon.beacon.Beacon;
import com.connecthings.altbeacon.beacon.BeaconManager;
import com.connecthings.altbeacon.beacon.BeaconParser;
import com.connecthings.altbeacon.beacon.BuildConfig;
import com.connecthings.altbeacon.beacon.Region;
import com.connecthings.altbeacon.beacon.distance.DistanceCalculator;
import com.connecthings.altbeacon.beacon.distance.ModelSpecificDistanceCalculator;
import com.connecthings.altbeacon.beacon.logging.LogManager;
import com.connecthings.altbeacon.beacon.service.scanner.CycledLeScanCallback;
import com.connecthings.altbeacon.beacon.service.scanner.CycledLeScanner;
import com.connecthings.altbeacon.beacon.service.scanner.NonBeaconLeScanCallback;
import com.connecthings.altbeacon.beacon.startup.StartupBroadcastReceiver;
import com.connecthings.altbeacon.beacon.utils.ProcessUtils;
import com.connecthings.altbeacon.bluetooth.BluetoothCrashResolver;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static android.app.PendingIntent.FLAG_ONE_SHOT;
import static android.app.PendingIntent.getBroadcast;

/**
 * @author dyoung
 */

public class BeaconService extends Service {
    public static final String TAG = "BeaconService";
    private final Handler handler = new Handler();
    private BluetoothCrashResolver bluetoothCrashResolver;
    private ScanHelper mScanHelper;
    /*
     * The scan period is how long we wait between restarting the BLE advertisement scans
     * Each time we restart we only see the unique advertisements once (e.g. unique beacons)
     * So if we want updates, we have to restart.  For updates at 1Hz, ideally we
     * would restart scanning that often to get the same update rate.  The trouble is that when you
     * restart scanning, it is not instantaneous, and you lose any beacon packets that were in the
     * air during the restart.  So the more frequently you restart, the more packets you lose.  The
     * frequency is therefore a tradeoff.  Testing with 14 beacons, transmitting once per second,
     * here are the counts I got for various values of the SCAN_PERIOD:
     *
     * Scan period     Avg beacons      % missed
     *    1s               6                 57
     *    2s               10                29
     *    3s               12                14
     *    5s               14                0
     *
     * Also, because beacons transmit once per second, the scan period should not be an even multiple
     * of seconds, because then it may always miss a beacon that is synchronized with when it is stopping
     * scanning.
     *
     */

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class BeaconBinder extends Binder {
        public BeaconService getService() {
            LogManager.i(TAG, "getService of BeaconBinder called");
            // Return this instance of LocalService so clients can call public methods
            return BeaconService.this;
        }
    }

    /**
     * Command to the service to display a message
     */
    public static final int MSG_START_RANGING = 2;
    public static final int MSG_STOP_RANGING = 3;
    public static final int MSG_START_MONITORING = 4;
    public static final int MSG_STOP_MONITORING = 5;
    public static final int MSG_SET_SCAN_PERIODS = 6;
    public static final int MSG_SYNC_SETTINGS = 7;

    static class IncomingHandler extends Handler {
        private final WeakReference<BeaconService> mService;

        IncomingHandler(BeaconService service) {
            /*
             * Explicitly state this uses the main thread. Without this we defer to where the
             * service instance is initialized/created; which is usually the main thread anyways.
             * But by being explicit we document our code design expectations for where things run.
             */
            super(Looper.getMainLooper());
            mService = new WeakReference<BeaconService>(service);
        }

        @MainThread
        @Override
        public void handleMessage(Message msg) {
            BeaconService service = mService.get();
            if (service != null) {
                StartRMData startRMData = StartRMData.fromBundle(msg.getData());
                if (startRMData != null) {
                    switch (msg.what) {
                        case MSG_START_RANGING:
                            LogManager.i(TAG, "start ranging received");
                            service.startRangingBeaconsInRegion(startRMData.getRegionData(), new com.connecthings.altbeacon.beacon.service.Callback(startRMData.getCallbackPackageName()));
                            service.setScanPeriods(startRMData.getScanPeriod(), startRMData.getBetweenScanPeriod(), startRMData.getBackgroundFlag());
                            break;
                        case MSG_STOP_RANGING:
                            LogManager.i(TAG, "stop ranging received");
                            service.stopRangingBeaconsInRegion(startRMData.getRegionData());
                            service.setScanPeriods(startRMData.getScanPeriod(), startRMData.getBetweenScanPeriod(), startRMData.getBackgroundFlag());
                            break;
                        case MSG_START_MONITORING:
                            LogManager.i(TAG, "start monitoring received");
                            service.startMonitoringBeaconsInRegion(startRMData.getRegionData(), new com.connecthings.altbeacon.beacon.service.Callback(startRMData.getCallbackPackageName()));
                            service.setScanPeriods(startRMData.getScanPeriod(), startRMData.getBetweenScanPeriod(), startRMData.getBackgroundFlag());
                            break;
                        case MSG_STOP_MONITORING:
                            LogManager.i(TAG, "stop monitoring received");
                            service.stopMonitoringBeaconsInRegion(startRMData.getRegionData());
                            service.setScanPeriods(startRMData.getScanPeriod(), startRMData.getBetweenScanPeriod(), startRMData.getBackgroundFlag());
                            break;
                        case MSG_SET_SCAN_PERIODS:
                            LogManager.i(TAG, "set scan intervals received");
                            service.setScanPeriods(startRMData.getScanPeriod(), startRMData.getBetweenScanPeriod(), startRMData.getBackgroundFlag());
                            break;
                        default:
                            super.handleMessage(msg);
                    }
                }
                else if (msg.what == MSG_SYNC_SETTINGS) {
                    LogManager.i(TAG, "Received settings update from other process");
                    SettingsData settingsData = SettingsData.fromBundle(msg.getData());
                    if (settingsData != null) {
                        settingsData.apply(service);
                    }
                    else {
                        LogManager.w(TAG, "Settings data missing");
                    }
                }
                else {
                    LogManager.i(TAG, "Received unknown message from other process : "+msg.what);
                }

            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler(this));

    @MainThread
    @Override
    public void onCreate() {
        bluetoothCrashResolver = new BluetoothCrashResolver(this);
        bluetoothCrashResolver.start();

        mScanHelper = new ScanHelper(this);
        if (mScanHelper.getCycledScanner() == null) {
            mScanHelper.createCycledLeScanner(false, bluetoothCrashResolver);
        }
        mScanHelper.setMonitoringStatus(MonitoringStatus.getInstanceForApplication(this));
        mScanHelper.setRangedRegionState(new HashMap<Region, RangeState>());
        mScanHelper.setBeaconParsers(new HashSet<BeaconParser>());
        mScanHelper.setExtraDataBeaconTracker(new ExtraDataBeaconTracker());

        BeaconManager beaconManager = BeaconManager.getInstanceForApplication(getApplicationContext());
        beaconManager.setScannerInSameProcess(true);
        if (beaconManager.isMainProcess()) {
            LogManager.i(TAG, "beaconService version %s is starting up on the main process", BuildConfig.VERSION_NAME);
        }
        else {
            LogManager.i(TAG, "beaconService version %s is starting up on a separate process", BuildConfig.VERSION_NAME);
            ProcessUtils processUtils = new ProcessUtils(this);
            LogManager.i(TAG, "beaconService PID is "+processUtils.getPid()+" with process name "+processUtils.getProcessName());
        }

        try {
            PackageItemInfo info = this.getPackageManager().getServiceInfo(new ComponentName(this, BeaconService.class), PackageManager.GET_META_DATA);
            if (info != null && info.metaData != null && info.metaData.get("longScanForcingEnabled") != null &&
                    info.metaData.get("longScanForcingEnabled").toString().equals("true")) {
                LogManager.i(TAG, "longScanForcingEnabled to keep scans going on Android N for > 30 minutes");
                mScanHelper.getCycledScanner().setLongScanForcingEnabled(true);
            }
        } catch (PackageManager.NameNotFoundException e) {}

        mScanHelper.reloadParsers();

        DistanceCalculator defaultDistanceCalculator =  new ModelSpecificDistanceCalculator(this, BeaconManager.getDistanceModelUpdateUrl());
        Beacon.setDistanceCalculator(defaultDistanceCalculator);

        // Look for simulated scan data
        try {
            Class klass = Class.forName("com.connecthings.altbeacon.beacon.SimulatedScanData");
            java.lang.reflect.Field f = klass.getField("beacons");
            mScanHelper.setSimulatedScanData((List<Beacon>) f.get(null));
        } catch (ClassNotFoundException e) {
            LogManager.d(TAG, "No com.connecthings.altbeacon.beacon.SimulatedScanData class exists.");
        } catch (Exception e) {
            LogManager.e(e, TAG, "Cannot get simulated Scan data.  Make sure your com.connecthings.altbeacon.beacon.SimulatedScanData class defines a field with the signature 'public static List<Beacon> beacons'");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogManager.i(TAG,
                intent == null ?
                        "starting with null intent"
                        :
                        "starting with intent " + intent.toString()
        );
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        LogManager.i(TAG, "binding");
        return mMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        LogManager.i(TAG, "unbinding");
        return false;
    }

    @MainThread
    @Override
    public void onDestroy() {
        LogManager.e(TAG, "onDestroy()");
        if (android.os.Build.VERSION.SDK_INT < 18) {
            LogManager.w(TAG, "Not supported prior to API 18.");
            return;
        }
        bluetoothCrashResolver.stop();
        LogManager.i(TAG, "onDestroy called.  stopping scanning");
        handler.removeCallbacksAndMessages(null);
        mScanHelper.getCycledScanner().stop();
        mScanHelper.getCycledScanner().destroy();
        mScanHelper.getMonitoringStatus().stopStatusPreservation();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        LogManager.d(TAG, "task removed");
        if (Build.VERSION.RELEASE.contains("4.4.1") ||
                Build.VERSION.RELEASE.contains("4.4.2") ||
                Build.VERSION.RELEASE.contains("4.4.3")) {
            AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, getRestartIntent());
            LogManager.d(TAG, "Setting a wakeup alarm to go off due to Android 4.4.2 service restarting bug.");
        }
    }

    private PendingIntent getRestartIntent() {
        Intent restartIntent = new Intent(getApplicationContext(), StartupBroadcastReceiver.class);
        return getBroadcast(getApplicationContext(), 1, restartIntent, FLAG_ONE_SHOT);
    }

    /**
     * methods for clients
     */
    @MainThread
    public void startRangingBeaconsInRegion(Region region, Callback callback) {
        synchronized (mScanHelper.getRangedRegionState()) {
            if (mScanHelper.getRangedRegionState().containsKey(region)) {
                LogManager.i(TAG, "Already ranging that region -- will replace existing region.");
                mScanHelper.getRangedRegionState().remove(region); // need to remove it, otherwise the old object will be retained because they are .equal //FIXME That is not true
            }
            mScanHelper.getRangedRegionState().put(region, new RangeState(callback));
            LogManager.d(TAG, "Currently ranging %s regions.", mScanHelper.getRangedRegionState().size());
        }
        mScanHelper.getCycledScanner().start();
    }

    @MainThread
    public void stopRangingBeaconsInRegion(Region region) {
        int rangedRegionCount;
        synchronized (mScanHelper.getRangedRegionState()) {
            mScanHelper.getRangedRegionState().remove(region);
            rangedRegionCount = mScanHelper.getRangedRegionState().size();
            LogManager.d(TAG, "Currently ranging %s regions.", mScanHelper.getRangedRegionState().size());
        }

        if (rangedRegionCount == 0 && mScanHelper.getMonitoringStatus().regionsCount() == 0) {
            mScanHelper.getCycledScanner().stop();
        }
    }

    @MainThread
    public void startMonitoringBeaconsInRegion(Region region, Callback callback) {
        LogManager.d(TAG, "startMonitoring called");
        mScanHelper.getMonitoringStatus().addRegion(region, callback);
        LogManager.d(TAG, "Currently monitoring %s regions.", mScanHelper.getMonitoringStatus().regionsCount());
        mScanHelper.getCycledScanner().start();
    }

    @MainThread
    public void stopMonitoringBeaconsInRegion(Region region) {
        LogManager.d(TAG, "stopMonitoring called");
        mScanHelper.getMonitoringStatus().removeRegion(region);
        LogManager.d(TAG, "Currently monitoring %s regions.", mScanHelper.getMonitoringStatus().regionsCount());
        if (mScanHelper.getMonitoringStatus().regionsCount() == 0 && mScanHelper.getRangedRegionState().size() == 0) {
            mScanHelper.getCycledScanner().stop();
        }
    }

    @MainThread
    public void setScanPeriods(long scanPeriod, long betweenScanPeriod, boolean backgroundFlag) {
        mScanHelper.getCycledScanner().setScanPeriods(scanPeriod, betweenScanPeriod, backgroundFlag);
    }

    public void reloadParsers() {
        mScanHelper.reloadParsers();
    }

    @RestrictTo(Scope.TESTS)
    protected CycledLeScanCallback getCycledLeScanCallback() {
        return mScanHelper.getCycledLeScanCallback();
    }
}
