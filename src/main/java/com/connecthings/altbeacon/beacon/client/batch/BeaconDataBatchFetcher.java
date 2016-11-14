package com.connecthings.altbeacon.beacon.client.batch;

import com.connecthings.altbeacon.beacon.Beacon;
import com.connecthings.altbeacon.beacon.utils.FixSizeCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Created by Connecthings on 27/09/16.
 */
public class BeaconDataBatchFetcher<BeaconContent extends BeaconIdentifiers> implements BeaconDataBatchNotifier<BeaconContent>{

    private static final String TAG = "BeaconDataBatchFetcher";

    private BeaconDataBatchProvider mBeaconDataBatchProvider;

    private FixSizeCache<String, BeaconContentFetchInfo<BeaconContent>> beaconContentInfoCache;
    private int mMaxBeaconCacheTime;
    private Collection<Beacon> beaconsToFetch;
    private final Object beaconsLock = new Object();
    private BatchCallProviderLimiter batchErrorLimiter;

    public BeaconDataBatchFetcher(BeaconDataBatchProvider beaconDataBatchProvider, int cacheSize, int maxBeaconCacheTime){
        this.mBeaconDataBatchProvider = beaconDataBatchProvider;
        this.batchErrorLimiter = new BatchCallProviderLimiter();
        beaconContentInfoCache = new FixSizeCache<>(cacheSize);
        this.mMaxBeaconCacheTime = maxBeaconCacheTime;
        beaconsToFetch = new HashSet<>(10);
    }


    public BeaconContentFetchInfo updateContentOrAddToFetch(Beacon beacon){
        BeaconContentFetchInfo<BeaconContent> beaconContentFetchInfo = null;
        if(!beacon.isExtraBeaconData() && ((!mBeaconDataBatchProvider.fetchEphemeralIds() && !beacon.hasEphemeralIdentifiers()) || (mBeaconDataBatchProvider.fetchEphemeralIds()))) {
            beaconContentFetchInfo = findFetchInfoOrAddToFetch(beacon);
            if(beaconContentFetchInfo != null) {
                BeaconContent content = beaconContentFetchInfo.getContent();
                if (content != null) {
                    if (content instanceof UpdateBeacon) {
                        ((UpdateBeacon) content).updateBeacon(beacon);
                    }
                    if (beacon.hasEphemeralIdentifiers()) {
                        beacon.setStaticIdentifiers(content.getStaticIdentifiers());
                    }
                }
            }
        }
        return beaconContentFetchInfo;
    }

    public BeaconBatchFetchInfo<BeaconContent> updateContentOrAddToFetch(Collection<Beacon> beacons){
        BeaconContentFetchStatus globalStatus = null;
        List<BeaconContent> contents = new ArrayList<>(beacons.size());
        List<Beacon> beaconsWithNoContent = new ArrayList<>();
        int countError = 0;
        int countInProgress = 0;
        BeaconContentFetchStatus errorStatus = null;
        for(Beacon beacon : beacons) {
            BeaconContentFetchInfo<BeaconContent> beaconContentFetchInfo = updateContentOrAddToFetch(beacon);
            if(beaconContentFetchInfo != null) {
                BeaconContent content = beaconContentFetchInfo.getContent();
                if(content != null) {
                    contents.add(content);
                }

                if(beaconContentFetchInfo.getStatus() == BeaconContentFetchStatus.IN_PROGRESS){
                    countInProgress++;
                }else if(beaconContentFetchInfo.getStatus() == BeaconContentFetchStatus.BACKEND_ERROR
                        || beaconContentFetchInfo.getStatus() == BeaconContentFetchStatus.NETWORK_ERROR
                        || beaconContentFetchInfo.getStatus() == BeaconContentFetchStatus.DB_ERROR){
                    countError++;
                    errorStatus = beaconContentFetchInfo.getStatus();
                }else if(beaconContentFetchInfo.getStatus() == BeaconContentFetchStatus.NO_CONTENT){
                    beaconsWithNoContent.add(beacon);
                }
            }
        }
        if(countError == 0 && countInProgress == 0){
            globalStatus = BeaconContentFetchStatus.SUCCESS;
        }else if(countError != 0){
            globalStatus = errorStatus;
        }else{
            globalStatus = BeaconContentFetchStatus.IN_PROGRESS;
        }
        return new BeaconBatchFetchInfo<BeaconContent>(contents, beaconsWithNoContent, globalStatus);
    }

    public void fetch(){
        synchronized (beaconsLock) {
            if (mBeaconDataBatchProvider != null && batchErrorLimiter.isTimeToCallBatchProvider()
                        && beaconsToFetch.size() != 0) {
                List<Beacon> beacons = new ArrayList<>(beaconsToFetch);
                for(Beacon beacon : beacons) {
                    updateFetchInfo(beacon, BeaconContentFetchStatus.IN_PROGRESS);
                }
                this.mBeaconDataBatchProvider.fetch(beacons, this);
            }
            beaconsToFetch.clear();
        }
    }

    @Override
    public void onBatchUpdate(Collection<BeaconContent> beaconContents, Collection<Beacon> unresolvedBeacons) {
        BeaconContentFetchInfo<BeaconContent> fetchInfo;
        for(BeaconContent beaconContent : beaconContents){
            updateFetchInfo(beaconContent, BeaconContentFetchStatus.SUCCESS);
        }
        for(Beacon beacon : unresolvedBeacons){
            updateFetchInfo(beacon, BeaconContentFetchStatus.NO_CONTENT);
        }
    }

    @Override
    public void onBatchError(Collection<Beacon> beacons, DataBatchProviderException providerException) {
        BeaconContentFetchInfo<BeaconContent> fetchInfo;
        for(Beacon beacon : beacons){
            updateFetchInfo(beacon, providerException.getStatus());
        }
        //Limit the call to the Provider only if it's backend error or on SQL error
        //because NETWORK ERROR can be detected before any WS call
        if(providerException.getStatus() != BeaconContentFetchStatus.NETWORK_ERROR) {
            batchErrorLimiter.addError();
        }
    }

    private BeaconContentFetchInfo findFetchInfoOrAddToFetch(Beacon beacon){
        BeaconContentFetchInfo<BeaconContent> fetchInfo = getFetchInfo(beacon);
        synchronized (beaconsLock) {
            if (fetchInfo == null
                    || (fetchInfo.isTimeToUpdate()
                        || fetchInfo.getStatus() == BeaconContentFetchStatus.BACKEND_ERROR
                            || fetchInfo.getStatus() == BeaconContentFetchStatus.DB_ERROR
                                || fetchInfo.getStatus() == BeaconContentFetchStatus.NETWORK_ERROR)) {
                beaconsToFetch.add(beacon);
                if(fetchInfo == null){
                    fetchInfo = createFetchInfo(beacon);
                }
            }
        }
        return fetchInfo;
    }

    BeaconContentFetchInfo<BeaconContent> getFetchInfo(BeaconIdentifiers beaconIdentifiers){
        BeaconContentFetchInfo<BeaconContent> fetchInfo = beaconContentInfoCache.get(beaconIdentifiers.getStaticIdentifiers().toString());
        if(fetchInfo == null && beaconIdentifiers.hasEphemeralIdentifiers()){
            fetchInfo = beaconContentInfoCache.get(beaconIdentifiers.getEphemeralIdentifiers().toString());
        }
        return fetchInfo;
    }

    private BeaconContentFetchInfo<BeaconContent> createFetchInfo(BeaconIdentifiers beaconIdentifiers){
        BeaconContentFetchInfo<BeaconContent> fetchInfo = new BeaconContentFetchInfo<BeaconContent>(null, mMaxBeaconCacheTime, BeaconContentFetchStatus.IN_PROGRESS);
        if(beaconIdentifiers.hasStaticIdentifiers()) {
            beaconContentInfoCache.put(beaconIdentifiers.getStaticIdentifiers().toString(), fetchInfo);
        }
        if(beaconIdentifiers.hasEphemeralIdentifiers()){
            beaconContentInfoCache.put(beaconIdentifiers.getEphemeralIdentifiers().toString(), fetchInfo);
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
            fetchInfo.updateStatus(BeaconContentFetchStatus.SUCCESS);
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
