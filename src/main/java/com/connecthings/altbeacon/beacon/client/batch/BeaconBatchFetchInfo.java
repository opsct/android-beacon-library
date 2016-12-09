package com.connecthings.altbeacon.beacon.client.batch;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import com.connecthings.altbeacon.beacon.Beacon;
import com.connecthings.altbeacon.beacon.logging.LogManager;

import java.util.ArrayList;
import java.util.List;

/**
 * To sumarize the fetch information
 *
 * Created by Connecthings on 03/10/16.
 */
public class BeaconBatchFetchInfo<BeaconContent extends BeaconIdentifiers> implements Parcelable{

    private static final String TAG = "BeaconBatchFetchInfo";

    /**
     * A global status about the fetch
     * An error status indicates that at least one content can't be downloaded because of an error
     * The IN_PROGRESS status indicates that at least one content has be retrieved
     * The SUCCESS status indicates that all the content has been retrieved
     *
     * An error (BACKEND_ERROR, NETWORK_ERROR, DB_ERROR) status is prioritory on the IN_PROGRESS status and the SUCCESS status
     * The IN_PROGRESS status is prioritary on the SUCESS status
     */
    private BeaconContentFetchStatus fetchStatus;

    /**
     * The contents already retrieved
     */
    private List<BeaconContent> contents;

    /**
     * Beacons with no content attached.
     */
    private List<Beacon> beaconWithNoContents;

    public BeaconBatchFetchInfo(List<BeaconContent> contents, List<Beacon> beaconWithNoContents, BeaconContentFetchStatus fetchStatus){
        this.contents = contents;
        this.beaconWithNoContents = beaconWithNoContents;
        this.fetchStatus = fetchStatus;
    }

    private  BeaconBatchFetchInfo(Parcel in){
        readFromParcel(in);
    }

    public @NonNull List<BeaconContent> getContents() {
        return contents;
    }

    public @NonNull List<Beacon> getBeaconWithNoContents() {
        return beaconWithNoContents;
    }

    public @NonNull BeaconContentFetchStatus getFetchStatus() {
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
        if(TextUtils.isEmpty(className)) {
            contents = new ArrayList<>();
        }else{
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
