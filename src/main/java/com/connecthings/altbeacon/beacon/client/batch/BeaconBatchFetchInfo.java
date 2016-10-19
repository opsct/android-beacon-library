package com.connecthings.altbeacon.beacon.client.batch;

import android.os.Parcel;
import android.os.Parcelable;

import com.connecthings.altbeacon.beacon.Beacon;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Connecthings on 03/10/16.
 */
public class BeaconBatchFetchInfo<BeaconContent extends BeaconIdentifiers> {

    private BeaconContentFetchStatus fetchStatus;

    private List<BeaconContent> contents;

    public BeaconBatchFetchInfo(BeaconContentFetchStatus fetchStatus, List<BeaconContent> contents){
        this.fetchStatus = fetchStatus;
        this.contents = contents;
    }

    public List<BeaconContent> getContents() {
        return contents;
    }

    public BeaconContentFetchStatus getFetchStatus() {
        return fetchStatus;
    }
}
