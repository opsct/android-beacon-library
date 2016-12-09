package com.connecthings.altbeacon.beacon.client.batch;

import com.connecthings.altbeacon.beacon.Beacon;
import android.support.annotation.NonNull;


import java.util.Collection;

/**
 * Callback to notify the SDK when beacon contents have been retrieved
 * Implemented by the {@link BeaconDataBatchFetcher} class
 *
 * Created by Connecthings on 27/09/16.
 */

public interface BeaconDataBatchNotifier<BeaconContent extends BeaconIdentifiers> {

    public void onBatchUpdate(@NonNull Collection<BeaconContent> beaconContents, @NonNull Collection<Beacon> unresolvedBeacons);

    public void onBatchError(@NonNull Collection<Beacon> beacons, @NonNull DataBatchProviderException providerException);

}
