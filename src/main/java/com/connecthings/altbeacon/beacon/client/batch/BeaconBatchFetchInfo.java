package com.connecthings.altbeacon.beacon.client.batch;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import com.connecthings.altbeacon.beacon.Beacon;
import com.connecthings.altbeacon.beacon.logging.LogManager;

import java.util.List;

/**
 * Created by Connecthings on 03/10/16.
 */
public class BeaconBatchFetchInfo<BeaconContent extends BeaconIdentifiers> implements Parcelable{

    private static final String TAG = "BeaconBatchFetchInfo";

    private BeaconContentFetchStatus fetchStatus;

    private List<BeaconContent> contents;

    private List<Beacon> beaconWithNoContents;

    public BeaconBatchFetchInfo(List<BeaconContent> contents, List<Beacon> beaconWithNoContents, BeaconContentFetchStatus fetchStatus){
        this.contents = contents;
        this.beaconWithNoContents = beaconWithNoContents;
        this.fetchStatus = fetchStatus;
    }

    private  BeaconBatchFetchInfo(Parcel in){
        readFromParcel(in);
    }

    public List<BeaconContent> getContents() {
        return contents;
    }

    public List<Beacon> getBeaconWithNoContents() {
        return beaconWithNoContents;
    }

    public BeaconContentFetchStatus getFetchStatus() {
        return fetchStatus;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if(contents.size()==0){
            dest.writeString("");
        }else{
            dest.writeString(contents.get(0).getClass().getCanonicalName());
            dest.writeList(contents);
        }
        dest.writeList(beaconWithNoContents);
        dest.writeString(fetchStatus.toString());
    }

    private void readFromParcel(Parcel in){
        String className = in.readString();
        if(!TextUtils.isEmpty(className)){
            try {
                contents = in.readArrayList(Class.forName(className).getClassLoader());
            } catch (ClassNotFoundException e) {
                LogManager.e(TAG, "impossible to find class to unparcel the content");
            }
        }
        beaconWithNoContents = in.readArrayList(Beacon.class.getClassLoader());
        fetchStatus = BeaconContentFetchStatus.valueOf(in.readString());
    }

    public static final Parcelable.Creator<BeaconBatchFetchInfo> CREATOR
            = new Parcelable.Creator<BeaconBatchFetchInfo>() {
        public BeaconBatchFetchInfo createFromParcel(Parcel in) {
            return new BeaconBatchFetchInfo(in);
        }

        public BeaconBatchFetchInfo[] newArray(int size) {
            return new BeaconBatchFetchInfo[size];
        }
    };
}
