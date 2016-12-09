package com.connecthings.altbeacon.beacon.client.batch;

import com.connecthings.altbeacon.beacon.Beacon;
import com.connecthings.altbeacon.beacon.utils.FixSizeCache;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Return the
 *
 * Created by Connecthings on 27/09/16.
 */
public class BeaconDataBatchFetcher<BeaconContent extends BeaconIdentifiers> implements BeaconDataBatchNotifier<BeaconContent>{

    private static final String TAG = "BeaconDataBatchFetcher";

    private BeaconDataBatchProvider mBeaconDataBatchProvider;

    private FixSizeCache<String, BeaconContentFetchInfo<BeaconContent>> beaconContentInfoCache;
    private int mMaxBeaconCacheTime;
    private boolean mIsContentAvailableWhenCacheTimeOver;
    private long mFetchDelay;
    private Collection<Beacon> beaconsToFetch;
    private final Object beaconsLock = new Object();
    private BatchCallProviderLimiter batchErrorLimiter;
    private Handler mFetchHandler;

    public BeaconDataBatchFetcher(BeaconDataBatchProvider beaconDataBatchProvider, int cacheSize, int maxBeaconCacheTime, boolean isContentAvailableWhenCacheTimeOver,long fetchDelay){
        this.mBeaconDataBatchProvider = beaconDataBatchProvider;
        this.batchErrorLimiter = new BatchCallProviderLimiter();
        beaconContentInfoCache = new FixSizeCache<>(cacheSize);
        this.mMaxBeaconCacheTime = maxBeaconCacheTime;
        this.mIsContentAvailableWhenCacheTimeOver = isContentAvailableWhenCacheTimeOver;
        this.mFetchDelay = fetchDelay;

        beaconsToFetch = new HashSet<>(10);
        mFetchHandler = new FetchHandler(this);
    }

    /**
     * Search the {@link BeaconContentFetchInfo} associated to the beacon
     * If no {@link BeaconContentFetchInfo} is found or the {@link BeaconContentFetchInfo} has reached is cached time limit, the beacon is added to the list of beacons
     * If the Beacon is using an ephemeral beacon id, and a {@link BeaconContentFetchInfo} is found with a BeaconContent, the beacon is updated with the
     * static identifier from the BeaconContent. This permits to the library to work a usual with beacon regions on static identifier.
     * @param beacon
     * @return the BeaconContentFetchInfo associated to the beacon
     */
    public @Nullable BeaconContentFetchInfo updateContentOrAddToFetch(Beacon beacon){
        if(mBeaconDataBatchProvider == null){
            return null;
        }
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

    /**
     * Search the BeaconContents associated to a list of Beacons.
     * If no BeaconContent has been found, the beacon is added to the list of beacons in which a content has to be fetched
     * @param beacons
     * @return a summarize information, with the list of BeaconContent, the list of beacon with no content attached, and the current global fetch status
     */
    public @Nullable BeaconBatchFetchInfo<BeaconContent> updateContentOrAddToFetch(Collection<Beacon> beacons){
        if(mBeaconDataBatchProvider == null){
            return null;
        }
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

    /**
     * The contents associated to beacon is fetched at a regular interval.
     * This method permits to launch the planning process.
     */
    public void planFetch(){
        if(mBeaconDataBatchProvider != null) {
            mFetchHandler.sendEmptyMessageDelayed(0, mFetchDelay);
        }
    }

    public void stopFetch(){
        mFetchHandler.removeCallbacksAndMessages(null);
    }

    protected void fetch(){
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
            planFetch();
        }
    }

    @Override
    public void onBatchUpdate(Collection<BeaconContent> beaconContents, Collection<Beacon> unresolvedBeacons) {
        synchronized (beaconsLock){
            batchErrorLimiter.reset();
        }
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
            synchronized (beaconsLock){
                batchErrorLimiter.addError();
            }
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
                if(fetchInfo == null || (fetchInfo.isTimeToUpdate() && !fetchInfo.isContentAvailableWhenCacheTimeExpired())){
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
        BeaconContentFetchInfo<BeaconContent> fetchInfo = new BeaconContentFetchInfo<BeaconContent>(mMaxBeaconCacheTime, mIsContentAvailableWhenCacheTimeOver, BeaconContentFetchStatus.IN_PROGRESS);
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
            fetchInfo = new BeaconContentFetchInfo<BeaconContent>(content, mMaxBeaconCacheTime, mIsContentAvailableWhenCacheTimeOver, status);
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


    private class FetchHandler extends Handler{
        WeakReference<BeaconDataBatchFetcher<BeaconContent>> weakReference;

        public FetchHandler(BeaconDataBatchFetcher<BeaconContent> beaconDataBatchFetcher){
            weakReference = new WeakReference<BeaconDataBatchFetcher<BeaconContent>>(beaconDataBatchFetcher);
        }

        @Override
        public void handleMessage(Message msg) {
            BeaconDataBatchFetcher<BeaconContent> fetcher = weakReference.get();
            if(fetcher == null){
                return;
            }
            fetcher.fetch();
        }
    }

}
