package org.altbeacon.beacon.service.scanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.startup.StartupBroadcastReceiver;
import org.altbeacon.bluetooth.BluetoothCrashResolver;

import java.util.Date;

@TargetApi(18)
public class CycledLeScanner implements Parcelable{
    private static final String TAG = "CycledLeScanner";
    private BluetoothAdapter mBluetoothAdapter;

    private long mLastScanCycleStartTime = 0l;
    private long mLastScanCycleEndTime = 0l;
    private long mNextScanCycleStartTime = 0l;
    private long mScanCycleStopTime = 0l;

    private boolean mScanning;
    private boolean mScanningPaused;
    private boolean mScanCyclerStarted = false;
    private boolean mScanningEnabled = false;
    private Context mContext;
    private ScanPeriods mActiveScanPeriods;
    private ScanPeriods mPassiveScanPeriods;
    private ScanPeriods mCurrentScanPeriods;

    private final Handler mHandler = new Handler();

    private BluetoothCrashResolver mBluetoothCrashResolver;
    private CycledLeScanCallback mCycledLeScanCallback;

    private boolean mBackgroundFlag = false;
    private boolean mActiveMode = false;
    private boolean mRestartNeeded = false;
    private LeScanner mLeScanner;
    private Runnable mNextCycleRunnable;

    public CycledLeScanner(ScanPeriods activeScanPeriods, ScanPeriods passiveScanPeriods) {
        this.mActiveScanPeriods = activeScanPeriods;
        this.mPassiveScanPeriods = passiveScanPeriods;
        mBackgroundFlag = false;
        mActiveMode = true;
        mCurrentScanPeriods = mActiveScanPeriods;
    }

    public void initScanning(Context context, CycledLeScanCallback cycledLeScanCallback){
        mContext = context;
        mCycledLeScanCallback = cycledLeScanCallback;
        mBluetoothCrashResolver = new BluetoothCrashResolver(context);
        mLeScanner = createLeScanner(context, cycledLeScanCallback, mBluetoothCrashResolver);
        mBluetoothCrashResolver.start();
    }

    private LeScanner createLeScanner(Context context, CycledLeScanCallback cycledLeScanCallback, BluetoothCrashResolver crashResolver){
        boolean useAndroidLScanner;
        if (android.os.Build.VERSION.SDK_INT < 18) {
            LogManager.w(TAG, "Not supported prior to API 18.");
            return null;
        }

        if (android.os.Build.VERSION.SDK_INT < 21) {
            LogManager.i(TAG, "This is not Android 5.0.  We are using old scanning APIs");
            useAndroidLScanner = false;
        } else {
            if (BeaconManager.isAndroidLScanningDisabled()) {
                LogManager.i(TAG, "This Android 5.0, but L scanning is disabled. We are using old scanning APIs");
                useAndroidLScanner = false;
            } else {
                LogManager.i(TAG, "This Android 5.0.  We are using new scanning APIs");
                useAndroidLScanner = true;
            }
        }

        if (useAndroidLScanner) {
            return new LeScannerForLollipop(context, cycledLeScanCallback, crashResolver);
        } else {
            return new LeScannerForJellyBeanMr2(context, cycledLeScanCallback, crashResolver);
        }
    }

    public LeScanner getLeScanner(){
        return mLeScanner;
    }

    protected boolean getBackgroundFlag(){
        return mBackgroundFlag;
    }

    protected boolean getActiveMode(){
        return mActiveMode;
    }

    /**
     * Tells the cycler the scan rate and whether it is in operating in background mode.
     * Background mode flag  is used only with the Android 5.0 scanning implementations to switch
     * between LOW_POWER_MODE vs. LOW_LATENCY_MODE
     */
    public void updateCycledParameter(CycledParameter cycledParameter) {
        mBackgroundFlag = cycledParameter.getBackgroundFlag();
        LogManager.d(TAG, "Background mode must have changed - Background Mode : %s.", mBackgroundFlag);
        if (mBackgroundFlag) {
            LogManager.d(TAG, "We are in the background.  Setting wakeup alarm");
            setWakeUpAlarm();
            mLeScanner.onBackground();
        } else {
            LogManager.d(TAG, "We are not in the background.  Cancelling wakeup alarm");
            cancelWakeUpAlarm();
            mLeScanner.onForeground();
        }
        updateMode(cycledParameter.getScanPeriods(), !cycledParameter.getBackgroundFlag());
    }

    public void updateMode(ScanPeriods scanPeriods, boolean activeMode){
        if(activeMode){
            mActiveScanPeriods = scanPeriods;
        }else{
            mPassiveScanPeriods = scanPeriods;
        }
        updateMode(activeMode);
    }

    public void updateMode(boolean activeMode){
        if (mActiveMode != activeMode) {
            mRestartNeeded = true;
        }
        mActiveMode = activeMode;
        mCurrentScanPeriods = activeMode? mActiveScanPeriods:mPassiveScanPeriods;
        LogManager.d(TAG, "Set scan periods called with %s, %s Active mode must have changed.",
                mCurrentScanPeriods.getScanPeriod(), mActiveMode);

        long now = SystemClock.elapsedRealtime();
        if (mNextScanCycleStartTime > now) {
            //if we switch to the active mode we start to scan directly
            if(activeMode){
                mNextScanCycleStartTime = now;
                cancelNextCycledRunnable();
                scanLeDevice(true);
            }else {
                // We are waiting to start scanning.  We may need to adjust the next start time
                // only do an adjustment if we need to make it happen sooner.  Otherwise, it will
                // take effect on the next cycle.
                long proposedNextScanStartTime = (mLastScanCycleEndTime + mCurrentScanPeriods.getBetweenScanPeriod());
                if (proposedNextScanStartTime < mNextScanCycleStartTime) {
                    mNextScanCycleStartTime = proposedNextScanStartTime;
                    LogManager.i(TAG, "Adjusted nextScanStartTime to be %s",
                            new Date(mNextScanCycleStartTime - SystemClock.elapsedRealtime() + System.currentTimeMillis()));
                }
            }
        }
        if (mScanCycleStopTime > now) {
            // we are waiting to stop scanning.  We may need to adjust the stop time
            // only do an adjustment if we need to make it happen sooner.  Otherwise, it will
            // take effect on the next cycle.
            long proposedScanStopTime = (mLastScanCycleStartTime + mCurrentScanPeriods.getScanPeriod());
            if (proposedScanStopTime < mScanCycleStopTime) {
                mScanCycleStopTime = proposedScanStopTime;
                LogManager.i(TAG, "Adjusted scanStopTime to be %s", mScanCycleStopTime);
            }
        }
    }

    public void start() {
        LogManager.d(TAG, "start called");
        mScanningEnabled = true;
        if (!mScanCyclerStarted) {
            scanLeDevice(true);
        } else {
            LogManager.d(TAG, "scanning already started");
        }
    }

    @SuppressLint("NewApi")
    public void stop() {
        LogManager.d(TAG, "stop called");
        mScanningEnabled = false;
        if (mScanCyclerStarted) {
            scanLeDevice(false);
        }
        if (mBluetoothAdapter != null) {
            stopScan();
            mLastScanCycleEndTime = SystemClock.elapsedRealtime();
        }
    }

    public void onDestroy(){
        mBluetoothCrashResolver.stop();
        this.stop();
    }

    private void stopScan(){
        mLeScanner.stopScan();
    }

    protected long calculateNextDelayDefault(long referenceTime){
        return  referenceTime - SystemClock.elapsedRealtime();
    }

    protected  long calculateNextScanLeDeviceDelayBackground(){
        return calculateNextDelayDefault(mNextScanCycleStartTime);
    }

    protected void scheduleRunnable(Runnable runnable, long delay){
        mHandler.postDelayed(runnable, delay);
    }

    protected  void cancelRunnable(Runnable runnable){
        mHandler.removeCallbacks(runnable);
    }

    protected void cancelNextCycledRunnable(){
        cancelRunnable(mNextCycleRunnable);
    }

    private boolean deferScanIfNeeded(){
        long millisecondsUntilStart =  mBackgroundFlag?calculateNextScanLeDeviceDelayBackground():calculateNextDelayDefault(mNextScanCycleStartTime);
        if (millisecondsUntilStart > 0) {
            LogManager.d(TAG, "Waiting to start next Bluetooth scan for another %s milliseconds",
                    millisecondsUntilStart);
            // Don't actually wait until the next scan time -- only wait up to 1 second.  This
            // allows us to start scanning sooner if a consumer enters the foreground and expects
            // results more quickly.
            if (mLeScanner.onDeferScanIfNeeded(true)) {
                setWakeUpAlarm();
            }
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    scanLeDevice(true);
                }
            };
            scheduleRunnable(runnable, millisecondsUntilStart < 1000?millisecondsUntilStart:1000);
            mNextCycleRunnable = runnable;
            return true;
        }else{
            mLeScanner.onDeferScanIfNeeded(false);
        }
        return false;
    }

    protected void startScan(){
        mLeScanner.startScan();
    }

    @SuppressLint("NewApi")
    protected void scanLeDevice(final Boolean enable) {
        mScanCyclerStarted = true;
        if (getBluetoothAdapter() == null) {
            LogManager.e(TAG, "No Bluetooth adapter.  beaconService cannot scan.");
        }
        if (enable) {
            if (deferScanIfNeeded()) {
                return;
            }
            LogManager.d(TAG, "starting a new scan cycle");
            if (!mScanning || mScanningPaused || mRestartNeeded) {
                mScanning = true;
                mScanningPaused = false;
                try {
                    if (getBluetoothAdapter() != null) {
                        if (getBluetoothAdapter().isEnabled()) {
                            if (mBluetoothCrashResolver != null && mBluetoothCrashResolver.isRecoveryInProgress()) {
                                LogManager.w(TAG, "Skipping scan because crash recovery is in progress.");
                            } else {
                                if (mScanningEnabled) {
                                    if (mRestartNeeded) {
                                        mRestartNeeded = false;
                                        LogManager.d(TAG, "restarting a bluetooth le scan");
                                    } else {
                                        LogManager.d(TAG, "starting a new bluetooth le scan");
                                    }
                                    try {
                                        if (android.os.Build.VERSION.SDK_INT < 23 || checkLocationPermission()) {
                                            startScan();
                                        }
                                    } catch (Exception e) {
                                        LogManager.e(e, TAG, "Internal Android exception scanning for beacons");
                                    }
                                } else {
                                    LogManager.d(TAG, "Scanning unnecessary - no monitoring or ranging active.");
                                }
                            }
                            mLastScanCycleStartTime = SystemClock.elapsedRealtime();
                        } else {
                            LogManager.d(TAG, "Bluetooth is disabled.  Cannot scan for beacons.");
                        }
                    }
                } catch (Exception e) {
                    LogManager.e(e, TAG, "Exception starting Bluetooth scan.  Perhaps Bluetooth is disabled or unavailable?");
                }
            } else {
                LogManager.d(TAG, "We are already scanning");
            }
            mScanCycleStopTime = (SystemClock.elapsedRealtime() + mCurrentScanPeriods.getScanPeriod());
            scheduleScanCycleStop();

            LogManager.d(TAG, "Scan started");
        } else {
            LogManager.d(TAG, "disabling scan");
            mScanning = false;
            mScanCyclerStarted = false;
            stopScan();
            mLastScanCycleEndTime = SystemClock.elapsedRealtime();
        }
    }

    protected long calculateNextStopCyleDelayBackground(){
       return calculateNextDelayDefault(mScanCycleStopTime);
    }

    private void scheduleScanCycleStop() {
        // Stops scanning after a pre-defined scan period.
        long millisecondsUntilStart = mBackgroundFlag?calculateNextStopCyleDelayBackground():calculateNextDelayDefault(mScanCycleStopTime);
        if (millisecondsUntilStart > 0) {
            LogManager.d(TAG, "Waiting to stop scan cycle for another %s milliseconds",
                    millisecondsUntilStart);
            if (mBackgroundFlag) {
                setWakeUpAlarm();
            }
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    scheduleScanCycleStop();
                }
            };
            scheduleRunnable(runnable, millisecondsUntilStart < 1000?millisecondsUntilStart:1000);
            mNextCycleRunnable = runnable;
        } else {
            finishScanCycle();
        }
    }

    private void finishScan(){
        mLeScanner.finishScan();
        mScanningPaused = true;
    }

    private void finishScanCycle() {
        LogManager.d(TAG, "Done with scan cycle");
        mCycledLeScanCallback.onCycleEnd();
        if (mScanning) {
            if (getBluetoothAdapter() != null) {
                if (getBluetoothAdapter().isEnabled()) {
                    try {
                        LogManager.d(TAG, "stopping bluetooth le scan");

                        finishScan();

                    } catch (Exception e) {
                        LogManager.w(e, TAG, "Internal Android exception scanning for beacons");
                    }
                    mLastScanCycleEndTime = SystemClock.elapsedRealtime();
                } else {
                    LogManager.d(TAG, "Bluetooth is disabled.  Cannot scan for beacons.");
                }
            }
            mNextScanCycleStartTime = getNextScanStartTime();
            if (mScanningEnabled) {
                scanLeDevice(true);
            }
        }
        if (!mScanningEnabled) {
            LogManager.d(TAG, "Scanning disabled.  No ranging or monitoring regions are active.");
            mScanCyclerStarted = false;
            cancelWakeUpAlarm();
        }
    }

    protected BluetoothAdapter getBluetoothAdapter() {
        if (mBluetoothAdapter == null) {
            // Initializes Bluetooth adapter.
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) mContext.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
            if (mBluetoothAdapter == null) {
                LogManager.w(TAG, "Failed to construct a BluetoothAdapter");
            }
        }
        return mBluetoothAdapter;
    }


    private PendingIntent mWakeUpOperation = null;

    // In case we go into deep sleep, we will set up a wakeup alarm when in the background to kickoff
    // off the scan cycle again
    protected void setWakeUpAlarm() {
        // wake up time will be the maximum of 5 minutes, the scan period, the between scan period
        long milliseconds = 1000l * 60 * 5; /* five minutes */
        if (milliseconds < mCurrentScanPeriods.getBetweenScanPeriod()) {
            milliseconds = mCurrentScanPeriods.getBetweenScanPeriod();
        }
        if (milliseconds < mCurrentScanPeriods.getScanPeriod()) {
            milliseconds = mCurrentScanPeriods.getScanPeriod();
        }

        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + milliseconds, getWakeUpOperation());
        LogManager.d(TAG, "Set a wakeup alarm to go off in %s ms: %s", milliseconds, getWakeUpOperation());
    }

    protected PendingIntent getWakeUpOperation() {
        if (mWakeUpOperation == null) {
            Intent wakeupIntent = new Intent();
            //intent.setFlags(Intent.FLAG_UPDATE_CURRENT);
            wakeupIntent.setClassName(mContext, StartupBroadcastReceiver.class.getName());
            wakeupIntent.putExtra("wakeup", true);
            mWakeUpOperation = PendingIntent.getBroadcast(mContext, 0, wakeupIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        return mWakeUpOperation;
    }

    protected void cancelWakeUpAlarm() {
        LogManager.d(TAG, "cancel wakeup alarm: %s", mWakeUpOperation);
        // We actually don't cancel the wakup alarm... we just reschedule for a long time in the
        // future.  This is to get around a limit on 500 alarms you can start per app on Samsung
        // devices.
        long milliseconds = Long.MAX_VALUE; // 2.9 million years from now
        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, milliseconds, getWakeUpOperation());
        LogManager.d(TAG, "Set a wakeup alarm to go off in %s ms: %s", milliseconds - SystemClock.elapsedRealtime(), getWakeUpOperation());

    }

    private long getNextScanStartTime() {
        // Because many apps may use this library on the same device, we want to try to synchronize
        // scanning as much as possible in order to save battery.  Therefore, we will set the scan
        // intervals to be on a predictable interval using a modulus of the system time.  This may
        // cause scans to start a little earlier than otherwise, but it should be acceptable.
        // This way, if multiple apps on the device are using the default scan periods, then they
        // will all be doing scans at the same time, thereby saving battery when none are scanning.
        // This, of course, won't help at all if people set custom scan periods.  But since most
        // people accept the defaults, this will likely have a positive effect.
        if (mCurrentScanPeriods.getBetweenScanPeriod() == 0) {
            return SystemClock.elapsedRealtime();
        }
        long fullScanCycle = mCurrentScanPeriods.getScanPeriod() + mCurrentScanPeriods.getScanPeriod();
        long normalizedBetweenScanPeriod = mCurrentScanPeriods.getBetweenScanPeriod()-(SystemClock.elapsedRealtime() % fullScanCycle);
        LogManager.d(TAG, "Normalizing between scan period from %s to %s", mCurrentScanPeriods.getBetweenScanPeriod(),
                normalizedBetweenScanPeriod);

        return SystemClock.elapsedRealtime()+normalizedBetweenScanPeriod;
    }

    private boolean checkLocationPermission() {
        return checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION) || checkPermission(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private boolean checkPermission(final String permission) {
        return mContext.checkPermission(permission, android.os.Process.myPid(), android.os.Process.myUid()) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mActiveScanPeriods, flags);
        dest.writeParcelable(mPassiveScanPeriods, flags);
        dest.writeInt(mBackgroundFlag?1:0);
        dest.writeInt(mActiveMode?1:0);
    }

    protected CycledLeScanner(Parcel in){
        ClassLoader classLoader = ScanPeriods.class.getClassLoader();
        mActiveScanPeriods = in.readParcelable(classLoader);
        mPassiveScanPeriods = in.readParcelable(classLoader);
        mBackgroundFlag = in.readInt() == 1;
        mActiveMode = in.readInt() == 1;
        mCurrentScanPeriods = mActiveMode ? mActiveScanPeriods : mPassiveScanPeriods;
    }

    public static final Parcelable.Creator<CycledLeScanner> CREATOR
            = new Parcelable.Creator<CycledLeScanner>() {
        public CycledLeScanner createFromParcel(Parcel in) {
            return new CycledLeScanner(in);
        }

        public CycledLeScanner[] newArray(int size) {
            return new CycledLeScanner[size];
        }
    };
}
