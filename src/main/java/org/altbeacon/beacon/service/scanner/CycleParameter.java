package org.altbeacon.beacon.service.scanner;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Permit to transmit the CycleParameter from the BeaconManager to the BeaconService
 * Created by Connecthings.
 */
public class CycleParameter implements Parcelable{

    private long scanPeriod;
    private long betweenScanPeriod;
    private boolean backgroundFlag;


    public CycleParameter(long scanPeriod, long betweenScanPeriod, boolean backgroundFlag) {
        this.scanPeriod = scanPeriod;
        this.betweenScanPeriod = betweenScanPeriod;
        this.backgroundFlag = backgroundFlag;
    }

    public long getBetweenScanPeriod() {
        return betweenScanPeriod;
    }

    public long getScanPeriod() {
        return scanPeriod;
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
        dest.writeLong(scanPeriod);
        dest.writeLong(betweenScanPeriod);
        dest.writeInt(backgroundFlag ? 1 : 0);
    }

    public static final Parcelable.Creator<CycleParameter> CREATOR
            = new Parcelable.Creator<CycleParameter>() {
        public CycleParameter createFromParcel(Parcel in) {
            return new CycleParameter(in);
        }

        public CycleParameter[] newArray(int size) {
            return new CycleParameter[size];
        }
    };

    private CycleParameter(Parcel in) {
        scanPeriod = in.readLong();
        betweenScanPeriod = in.readLong();
        backgroundFlag = in.readInt() == 1;
    }
}
