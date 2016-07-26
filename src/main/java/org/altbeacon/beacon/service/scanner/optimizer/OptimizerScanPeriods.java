package org.altbeacon.beacon.service.scanner.optimizer;

import android.os.Parcel;
import android.os.Parcelable;

import org.altbeacon.beacon.service.scanner.ScanPeriods;

/**
 * Created by Connecthings on 22/06/16.
 */
public class OptimizerScanPeriods extends ScanPeriods{

    /**
     * Indicate for how long the <code>OptimizerScanPeriods</code> must be used.
     * The <code>CycledLeScannerScreenStateOptimizer</code> manages a list of <code>OptimizerScanPeriods</code>
     * and switch to the next <code>OptimizerScanPeriods</code> when the periodDuration is reached.
     */
    private long periodDuration;
    /**
     * If there is an entry or exit from a region in that delay when the application is in background, the <code>CycledLeScannerScreenStateOptimizer</code> will
     * choose the previous <code>OptimizerScanPeriods</code> (as consequence, the scanning detection will be better but will consumme more battery)
     */
    private long maxDelayToSwitchToPreviousPeriod;


    /**
     * Permits to construct an <code>OptimizerScanPeriods</code> that manages three periods
     * @param periodDuration Indicates for how long this <code>OptimizerScanPeriods</code> must be used by the <code>CycledLeScannerScreenStateOptimizer</code>.
     * @param maxDelayToSwitchToPreviousPeriod the delay in which an entry or exit region must be dected to switch to the previous <code>OptimizerScanPeriods</code>
     * @param scanPeriod
     * @param betweenScanPeriod
     */
    public OptimizerScanPeriods(long periodDuration, long maxDelayToSwitchToPreviousPeriod,long scanPeriod, long betweenScanPeriod) {
        super(scanPeriod, betweenScanPeriod);
        this.periodDuration = periodDuration;
        this.maxDelayToSwitchToPreviousPeriod = maxDelayToSwitchToPreviousPeriod;
    }

    public long getPeriodDuration() {
        return periodDuration;
    }

    public long getMaxDelayToSwitchToPreviousPeriod() {
        return maxDelayToSwitchToPreviousPeriod;
    }


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeLong(periodDuration);
        dest.writeLong(maxDelayToSwitchToPreviousPeriod);
    }

    protected OptimizerScanPeriods(Parcel in){
        super(in);
        periodDuration = in.readLong();
        maxDelayToSwitchToPreviousPeriod = in.readLong();
    }

    public static final Parcelable.Creator<OptimizerScanPeriods> CREATOR
            = new Parcelable.Creator<OptimizerScanPeriods>() {
        public OptimizerScanPeriods createFromParcel(Parcel in) {
            return new OptimizerScanPeriods(in);
        }

        public OptimizerScanPeriods[] newArray(int size) {
            return new OptimizerScanPeriods[size];
        }
    };


}
