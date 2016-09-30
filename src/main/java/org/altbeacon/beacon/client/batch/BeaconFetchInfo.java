package org.altbeacon.beacon.client.batch;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.text.TextUtils;

import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.logging.Logger;

/**
 * Created by Connecthings on 27/09/16.
 */
public class BeaconFetchInfo<BeaconContent extends BeaconContentIdentifier> implements Parcelable{

    private static final String TAG = "BeaconFetchInfo";

    private BeaconContentFetchStatus status;

    private BeaconContent content;

    private long nextUpdateTime;

    private long maxCacheTime;

    public BeaconFetchInfo(BeaconContent content, long maxCacheTime, BeaconContentFetchStatus status) {
        this.content = content;
        this.maxCacheTime = maxCacheTime;
        this.status = status;
        nextUpdateTime = SystemClock.elapsedRealtime() + maxCacheTime;
    }

    private BeaconFetchInfo(Parcel from){
        readFromParcel(from);
    }

    public BeaconFetchInfo(long maxCacheTime, BeaconContentFetchStatus status){
        this(null, maxCacheTime, status);
    }

    public boolean isTimeToUpdate(){
        return SystemClock.elapsedRealtime() > nextUpdateTime;
    }

    public void updateStatus(BeaconContentFetchStatus status){
        this.status = status;
    }

    public void updateBeaconContent(BeaconContent beaconContent){
        content = beaconContent;
        status = BeaconContentFetchStatus.SUCCESS;
        nextUpdateTime = SystemClock.elapsedRealtime() + maxCacheTime;
    }


    public BeaconContent getContent() {
        return content;
    }

    public long getMaxCacheTime() {
        return maxCacheTime;
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
        dest.writeLong(maxCacheTime);
        dest.writeLong(nextUpdateTime);
        if(content == null){
            dest.writeString("");
        }else{
            dest.writeString(content.getClass().getCanonicalName());
            dest.writeParcelable(content, 0);
        }
    }

    public void readFromParcel(Parcel from){
        status = BeaconContentFetchStatus.valueOf(from.readString());
        maxCacheTime = from.readLong();
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

    public static final Creator<BeaconFetchInfo> CREATOR
            = new Creator<BeaconFetchInfo>() {
        public BeaconFetchInfo createFromParcel(Parcel in) {
            return new BeaconFetchInfo(in);
        }

        public BeaconFetchInfo[] newArray(int size) {
            return new BeaconFetchInfo[size];
        }
    };
}
