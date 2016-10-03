package com.connecthings.altbeacon.beacon.client.batch;

import com.connecthings.altbeacon.beacon.Beacon;

import java.util.List;

/**
 * Created by Connecthings on 27/09/16.
 */

public interface BeaconDataBatchNotifier<BeaconContent extends BeaconIdentifiers> {

    public void onBatchUpdate(List<BeaconContent> beaconContents, List<Beacon<BeaconContent>> unresolvedBeacons);

    public void onBatchError(List<Beacon<BeaconContent>> beacons, DataBatchProviderException providerException);

}
