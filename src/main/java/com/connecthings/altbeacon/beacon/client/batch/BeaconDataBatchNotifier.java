package com.connecthings.altbeacon.beacon.client.batch;

import com.connecthings.altbeacon.beacon.Beacon;

import java.util.Collection;
import java.util.List;

/**
 * Created by Connecthings on 27/09/16.
 */

public interface BeaconDataBatchNotifier<BeaconContent extends BeaconIdentifiers> {

    public void onBatchUpdate(Collection<BeaconContent> beaconContents, Collection<Beacon> unresolvedBeacons);

    public void onBatchError(Collection<Beacon> beacons, DataBatchProviderException providerException);

}
