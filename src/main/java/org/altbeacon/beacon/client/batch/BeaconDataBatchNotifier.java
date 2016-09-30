package org.altbeacon.beacon.client.batch;

import org.altbeacon.beacon.Beacon;

import java.util.List;

/**
 * Created by Connecthings on 27/09/16.
 */

public interface BeaconDataBatchNotifier<BeaconContent extends BeaconIdentifiers> {

    public void onBatchUpdate(List<BeaconContent> beaconContents);

    public void onBatchError(List<Beacon> beacons, DataBatchProviderException providerException);

}
