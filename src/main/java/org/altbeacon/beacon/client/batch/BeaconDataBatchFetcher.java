package org.altbeacon.beacon.client.batch;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.utils.FixSizeCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * Created by Connecthings on 27/09/16.
 */
public class BeaconDataBatchFetcher<BeaconContent extends BeaconIdentifiers> implements BeaconDataBatchNotifier<BeaconContent>{

    private BeaconDataBatchProvider mBeaconDataBatchProvider;

    private FixSizeCache<String, BeaconContentFetchInfo<BeaconContent>> beaconContentInfoCache;
    private int mMaxBeaconCacheTime;
    private Collection<Beacon> beaconsToFetch;
    private final Object beaconsLock = new Object();
    private BeaconBatchFetchInfo<BeaconContent> lastBatchFetchInfo;
    private BatchCallProviderLimiter batchErrorLimiter;

    public BeaconDataBatchFetcher(BeaconDataBatchProvider beaconDataBatchProvider, int cacheSize, int maxBeaconCacheTime){
        this.mBeaconDataBatchProvider = beaconDataBatchProvider;
        this.batchErrorLimiter = new BatchCallProviderLimiter();
        beaconContentInfoCache = new FixSizeCache<>(cacheSize);
        this.mMaxBeaconCacheTime = maxBeaconCacheTime;
        beaconsToFetch = new HashSet<>(10);
    }

    public void updateContentOrAddToFetch(Beacon beacon){
        if(!beacon.isExtraBeaconData() && ((!mBeaconDataBatchProvider.fetchEphemeralIds() && !beacon.hasEphemeralIdentifiers()) || (mBeaconDataBatchProvider.fetchEphemeralIds()))) {
            beacon.updateBeaconFetchInfo(findFetchInfoOrAddToFetch(beacon));
        }
    }

    public void updateContentOrAddToFetch(Collection<Beacon> beacons){
        for(Beacon beacon : beacons) {
            updateContentOrAddToFetch(beacon);
        }
    }

    public void fetch(){
        synchronized (beaconsLock) {
            if (mBeaconDataBatchProvider != null && batchErrorLimiter.isTimeToCallBatchProvider()) {
                if(beaconsToFetch.size() !=0) {
                    this.mBeaconDataBatchProvider.fetch(new ArrayList<Beacon>(beaconsToFetch), this);
                }
            }
            beaconsToFetch.clear();
        }
    }

    @Override
    public void onBatchUpdate(Collection<BeaconContent> beaconContents, Collection<Beacon<BeaconContent>> unresolvedBeacons) {
        BeaconContentFetchInfo<BeaconContent> fetchInfo;
        for(BeaconContent beaconContent : beaconContents){
            updateFetchInfo(beaconContent, BeaconContentFetchStatus.SUCCESS);
        }
        for(Beacon<BeaconContent> beacon : unresolvedBeacons){
            updateFetchInfo(beacon, BeaconContentFetchStatus.NO_CONTENT);
        }
    }

    @Override
    public void onBatchError(Collection<Beacon<BeaconContent>> beacons, DataBatchProviderException providerException) {
        BeaconContentFetchInfo<BeaconContent> fetchInfo;
        for(Beacon beacon : beacons){
            updateFetchInfo(beacon, providerException.getStatus());
        }
        batchErrorLimiter.addError();
    }

    private BeaconContentFetchInfo findFetchInfoOrAddToFetch(Beacon beacon){
        BeaconContentFetchInfo<BeaconContent> fetchInfo = getFetchInfo(beacon);
        synchronized (beaconsLock) {
            if (fetchInfo == null || (fetchInfo.isTimeToUpdate() && fetchInfo.getStatus() != BeaconContentFetchStatus.IN_PROGRESS)) {
                beaconsToFetch.add(beacon);
                fetchInfo = updateFetchInfo(beacon, BeaconContentFetchStatus.IN_PROGRESS);
            }
        }
        return fetchInfo;
    }

    private BeaconContentFetchInfo<BeaconContent> getFetchInfo(BeaconIdentifiers beaconIdentifiers){
        BeaconContentFetchInfo<BeaconContent> fetchInfo = beaconContentInfoCache.get(beaconIdentifiers.getStaticIdentifiers().toString());
        if(fetchInfo == null && beaconIdentifiers.hasEphemeralIdentifiers()){
            fetchInfo = beaconContentInfoCache.get(beaconIdentifiers.getEphemeralIdentifiers().toString());
        }
        return fetchInfo;
    }

    private BeaconContentFetchInfo<BeaconContent> updateFetchInfo(BeaconIdentifiers beaconIdentifiers, BeaconContentFetchStatus status){
        BeaconContentFetchInfo<BeaconContent> fetchInfo = getFetchInfo(beaconIdentifiers);
        BeaconContent content = beaconIdentifiers instanceof Beacon? null: (BeaconContent) beaconIdentifiers;

        if (fetchInfo == null) {
            fetchInfo = new BeaconContentFetchInfo<BeaconContent>(content, mMaxBeaconCacheTime, status);
        } else if(content == null){
            fetchInfo.updateStatus(status);
        } else {
            fetchInfo.updateBeaconContent(content);
        }
        if(beaconIdentifiers.hasStaticIdentifiers()) {
            beaconContentInfoCache.put(beaconIdentifiers.getStaticIdentifiers().toString(), fetchInfo);
        }
        if(beaconIdentifiers.hasEphemeralIdentifiers()){
            beaconContentInfoCache.put(beaconIdentifiers.getEphemeralIdentifiers().toString(), fetchInfo);
        }
        return fetchInfo;
    }

    protected FixSizeCache<String, BeaconContentFetchInfo<BeaconContent>>  getContentInfoCache(){
        return beaconContentInfoCache;
    }

    protected Collection<Beacon> getBeaconsToFetch() {
        return beaconsToFetch;
    }

    public void clearCache(){
        synchronized (beaconsLock) {
            beaconContentInfoCache.clear();
        }
    }

}
