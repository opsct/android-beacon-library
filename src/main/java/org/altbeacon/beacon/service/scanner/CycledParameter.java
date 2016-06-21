package org.altbeacon.beacon.service.scanner;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Permit to transmit the CycleParameter from the BeaconManager to the BeaconService
 * Created by Connecthings.
 */
public class CycledParameter implements Parcelable{

    private ScanPeriods scanPeriods;
    private boolean backgroundFlag;


    public CycledParameter(long scanPeriod, long betweenScanPeriod, boolean backgroundFlag) {
        scanPeriods = new ScanPeriods(scanPeriod, betweenScanPeriod);
        this.backgroundFlag = backgroundFlag;
    }

    public ScanPeriods getScanPeriods() {
        return scanPeriods;
    }

    public boolean getBackgroundFlag(){
        return backgroundFlag;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(scanPeriods, flags);
        dest.writeInt(backgroundFlag ? 1 : 0);
    }

    public static final Parcelable.Creator<CycledParameter> CREATOR
            = new Parcelable.Creator<CycledParameter>() {
        public CycledParameter createFromParcel(Parcel in) {
            return new CycledParameter(in);
        }

        public CycledParameter[] newArray(int size) {
            return new CycledParameter[size];
        }
    };

    private CycledParameter(Parcel in) {
        scanPeriods = in.readParcelable(ScanPeriods.class.getClassLoader());
        backgroundFlag = in.readInt() == 1;
    }
}
