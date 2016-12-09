package com.connecthings.altbeacon.beacon.client.batch;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.text.TextUtils;

import com.connecthings.altbeacon.beacon.logging.LogManager;

/**
 * Object that manages the BeaconContent and the current {@link BeaconContentFetchStatus}
 *
 * Permits to the {@link BeaconDataBatchFetcher} to return to good information about the fetching status and the contents attached to the beacon
 *
 * This object is kept in cache by the {@link BeaconDataBatchFetcher}.
 *
 * Created by Connecthings on 27/09/16.
 */
public class BeaconContentFetchInfo<BeaconContent extends BeaconIdentifiers> implements Parcelable, BeaconDataCacheManagement{

    private static final String TAG = "BeaconFetchInfo";

    private BeaconContentFetchStatus status;

    private BeaconContent content;

    private long nextUpdateTime;

    private long cacheTime;

    private boolean isContentAvailableWhenCacheTimeOver;

    public BeaconContentFetchInfo(BeaconContent content, long cacheTime, boolean isContentAvailableWhenCacheTimeOver, BeaconContentFetchStatus status) {
        this.content = content;
        this.status = status;
        if(content instanceof BeaconDataCacheManagement){
            BeaconDataCacheManagement cacheManagement = (BeaconDataCacheManagement) content;
            this.cacheTime = cacheManagement.getCacheTime();
            this.isContentAvailableWhenCacheTimeOver = cacheManagement.isContentAvailableWhenCacheTimeExpired();
        }else {
            this.cacheTime = cacheTime;
            this.isContentAvailableWhenCacheTimeOver = isContentAvailableWhenCacheTimeOver;
        }
        nextUpdateTime = SystemClock.elapsedRealtime() + cacheTime;
    }

    private BeaconContentFetchInfo(Parcel from){
        readFromParcel(from);
    }

    public BeaconContentFetchInfo(long maxCacheTime, boolean isContentAvailableWhenCacheTimeOver, BeaconContentFetchStatus status){
        this(null, maxCacheTime, isContentAvailableWhenCacheTimeOver, status);
    }

    public boolean isTimeToUpdate(){
        return SystemClock.elapsedRealtime() > nextUpdateTime;
    }

    public void updateStatus(BeaconContentFetchStatus status){
        this.status = status;
        nextUpdateTime = SystemClock.elapsedRealtime() + cacheTime;
    }

    public void updateBeaconContent(BeaconContent beaconContent){
        content = beaconContent;
        nextUpdateTime = SystemClock.elapsedRealtime() + cacheTime;
    }


    public BeaconContent getContent() {
        return content;
    }


    @Override
    public long getCacheTime() {
        return cacheTime;
    }

    @Override
    public boolean isContentAvailableWhenCacheTimeExpired() {
        return isContentAvailableWhenCacheTimeOver;
    }

    public long getNextUpdateTime() {
        return nextUpdateTime;
    }

    public BeaconContentFetchStatus getStatus() {
        return status;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(status.toString());
        dest.writeLong(cacheTime);
        dest.writeLong(nextUpdateTime);
        if(content == null){
            dest.writeString("");
        }else{
            dest.writeString(content.getClass().getCanonicalName());
            dest.writeParcelable((Parcelable)content, 0);
        }
    }

    public void readFromParcel(Parcel from){
        status = BeaconContentFetchStatus.valueOf(from.readString());
        cacheTime = from.readLong();
        nextUpdateTime = from.readLong();
        String className = from.readString();
        if(!TextUtils.isEmpty(className)){
            try {
                content = from.readParcelable(Class.forName(className).getClassLoader());
            } catch (ClassNotFoundException e) {
                LogManager.e(TAG, "can't read content parcelable", e);
            }
        }
    }

    public static final Creator<BeaconContentFetchInfo> CREATOR
            = new Creator<BeaconContentFetchInfo>() {
        public BeaconContentFetchInfo createFromParcel(Parcel in) {
            return new BeaconContentFetchInfo(in);
        }

        public BeaconContentFetchInfo[] newArray(int size) {
            return new BeaconContentFetchInfo[size];
        }
    };
}
