package org.altbeacon.beacon.client.batch;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.utils.FixSizeCache;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Connecthings on 27/09/16.
 */
public class BeaconDataBatchFetcher<BeaconContent extends BeaconContentIdentifier> implements BeaconDataBatchNotifier<BeaconContent>{

    private BeaconDataBatchProvider mBeaconDataBatchProvider;

    private FixSizeCache<String, BeaconFetchInfo<BeaconContent>> beaconContentInfoCache;
    private int mMaxBeaconCacheTime;
    private List<Beacon> beacons;
    private final Object beaconsLock = new Object();

    public BeaconDataBatchFetcher(BeaconDataBatchProvider beaconDataBatchProvider, int cacheSize, int maxBeaconCacheTime){
        this.mBeaconDataBatchProvider = beaconDataBatchProvider;
        beaconContentInfoCache = new FixSizeCache<>(cacheSize);
        this.mMaxBeaconCacheTime = maxBeaconCacheTime;
        beacons = new ArrayList<>(10);
    }

    public void addBeacon(Beacon beacon){
        synchronized (beaconsLock) {
            beacons.add(beacon);
        }
    }

    public void fetch(){
        synchronized (beaconsLock) {
            if (mBeaconDataBatchProvider != null) {
                List<Beacon> beaconsToFetch = new ArrayList<>(beacons.size());
                BeaconFetchInfo<BeaconContent> fetchInfo = null;
                String id;
                for (Beacon beacon : beacons) {
                    if (!beacon.isExtraBeaconData()) {
                        fetchInfo = null;
                        id = null;
                        if (beacon.getIdentifiers().size() != 0) {
                            id = beacon.getIdentifiers().toString();
                            fetchInfo = beaconContentInfoCache.get(id);

                        }
                        if (fetchInfo == null && beacon.getEphemeralIdentifiers().size() != 0) {
                            if (id == null) {
                                id = beacon.getEphemeralIdentifiers().toString();
                            }
                            fetchInfo = beaconContentInfoCache.get(id == null ? beacon.getEphemeralIdentifiers().toString() : id);
                        }
                        if (fetchInfo == null || (fetchInfo.isTimeToUpdate() && fetchInfo.getStatus() != BeaconContentFetchStatus.IN_PROGRESS)) {
                            beaconsToFetch.add(beacon);
                            if (fetchInfo == null) {
                                fetchInfo = new BeaconFetchInfo<>(null, mMaxBeaconCacheTime, BeaconContentFetchStatus.IN_PROGRESS);
                            } else {
                                fetchInfo.updateStatus(BeaconContentFetchStatus.IN_PROGRESS);
                            }
                        }
                        if (fetchInfo != null) {
                            beacon.updateBeaconFetchInfo(fetchInfo);

                        }
                    }
                }
                this.mBeaconDataBatchProvider.fetch(beaconsToFetch, this);
            }
            beacons.clear();
        }
    }

    @Override
    public void onBatchUpdate(List<BeaconContent> beaconContents) {
        BeaconFetchInfo<BeaconContent> fetchInfo;
        for(BeaconContent beaconContent : beaconContents){
            fetchInfo = beaconContentInfoCache.get(beaconContent.getStaticIdentifier());
            if(fetchInfo == null && beaconContent instanceof BeaconEphemeralIdentifier){
                fetchInfo = beaconContentInfoCache.get(((BeaconEphemeralIdentifier) beaconContent).getEphemeralIdentifier());
            }
            if(fetchInfo == null){
                fetchInfo = new BeaconFetchInfo<BeaconContent>(beaconContent, mMaxBeaconCacheTime, BeaconContentFetchStatus.SUCCESS);
            }else{
                fetchInfo.updateBeaconContent(beaconContent);
            }
            beaconContentInfoCache.put(beaconContent.getStaticIdentifier().toString(), fetchInfo);
            if(beaconContent instanceof  BeaconEphemeralIdentifier){
                beaconContentInfoCache.put(((BeaconEphemeralIdentifier) beaconContent).getEphemeralIdentifier().toString(), fetchInfo);
            }
        }
    }

    @Override
    public void onBatchError(List<Beacon> beacons, DataBatchProviderException providerException) {
        BeaconFetchInfo<BeaconContent> fetchInfo;
        for(Beacon beacon : beacons){
            fetchInfo = beaconContentInfoCache.get(beacon.getIdentifiers());
            if(fetchInfo == null){
                fetchInfo = beaconContentInfoCache.get(beacon.getEphemeralIdentifiers());
            }
            if(fetchInfo != null) {
                fetchInfo.updateStatus(providerException.getStatus());
            }
        }
    }
}
