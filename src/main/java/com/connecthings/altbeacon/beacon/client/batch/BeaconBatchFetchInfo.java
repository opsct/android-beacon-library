package com.connecthings.altbeacon.beacon.client.batch;

import android.os.Parcel;
import android.os.Parcelable;

import com.connecthings.altbeacon.beacon.Beacon;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Connecthings on 03/10/16.
 */
public class BeaconBatchFetchInfo<BeaconContent extends BeaconIdentifiers> implements Parcelable{

    private BeaconContentFetchStatus fetchStatus;

    private List<Beacon<BeaconContent>> unresolvedBeacons;

    public BeaconBatchFetchInfo(BeaconContentFetchStatus fetchStatus, List<Beacon<BeaconContent>> unresolvedBeacons){
        this.fetchStatus = fetchStatus;
        this.unresolvedBeacons = unresolvedBeacons==null?new ArrayList<Beacon<BeaconContent>>():unresolvedBeacons;
    }

    private BeaconBatchFetchInfo(Parcel from){
        readFromParcel(from);
    }

    public BeaconContentFetchStatus getFetchStatus() {
        return fetchStatus;
    }

    public List<Beacon<BeaconContent>> getUnresolvedBeacons() {
        return unresolvedBeacons;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(fetchStatus.toString());
        dest.writeList(unresolvedBeacons);
    }

    private void readFromParcel(Parcel from){
        fetchStatus = BeaconContentFetchStatus.valueOf(from.readString());
        unresolvedBeacons = new ArrayList<>(from.readArrayList(Beacon.class.getClassLoader()));
    }

    public static final Parcelable.Creator<BeaconBatchFetchInfo> CREATOR
            = new Parcelable.Creator<BeaconBatchFetchInfo>() {
        public BeaconBatchFetchInfo createFromParcel(Parcel in) {
            return new BeaconBatchFetchInfo(in);
        }

        public BeaconBatchFetchInfo[] newArray(int size) {
            return new BeaconBatchFetchInfo[size];
        }
    };

}
