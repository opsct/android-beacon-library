package org.altbeacon.beacon.service.scanner.optimizer;

import android.os.SystemClock;

import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.service.scanner.ScanPeriods;

/**
 *
 * Created by Connecthings
 */
public abstract class CycleScanStrategy {

    private ScanPeriods mActiveScanPeriod;
    private ScanPeriods mBackgroundScanPeriod;
    private boolean mBackgroundFlag;


}
