package com.connecthings.altbeacon.beacon.client.batch;

/**
 * Permits to manage the content cache options
 *
 * Created by Connecthings on 08/12/16.
 */
public interface BeaconDataCacheManagement {

    /**
     * How long does the {@link BeaconDataBatchFetcher} wait since the last content update before updating again the content
     * @return the time after which the content must be updated
     */
    public long getCacheTime();

    /**
     * Can the content be used after the cache time has expired ?
     * @return true if the content can be used when the cache time has expired
     */
    public boolean isContentAvailableWhenCacheTimeExpired();


}
