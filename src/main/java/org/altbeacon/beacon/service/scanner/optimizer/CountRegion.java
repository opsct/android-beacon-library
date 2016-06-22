package org.altbeacon.beacon.service.scanner.optimizer;

import android.os.SystemClock;

import org.altbeacon.beacon.Region;

/**
 * Created by Connecthings on 22/06/16.
 */
public abstract class CountRegion {
    private int count;
    private long lastIn;
    private long lastOut;

    public boolean isIn(){
        return count != 0;
    }

    public boolean isLastUpdateForLessThen(long delay){
        long time = SystemClock.elapsedRealtime() - delay;
        return lastIn >= time || lastOut >= time;
    }

    public boolean isInForLessThan(long delay){
        return count != 0 && lastIn + delay >= SystemClock.elapsedRealtime();
    }

    public boolean isOutForLessThan(long delay){
        return count == 0 && lastOut + delay >= SystemClock.elapsedRealtime();
    }

    public int getCount() {
        return count;
    }

    public long getLastIn() {
        return lastIn;
    }

    public long getLastOut() {
        return lastOut;
    }

    protected abstract boolean checkRegion(Region region);

    public boolean add(Region region){
        if(checkRegion(region)){
            count ++;
            lastIn = SystemClock.elapsedRealtime();
            return true;
        }
        return false;
    }

    public boolean remove(Region region){
        if(checkRegion(region)) {
            count --;
            lastOut = SystemClock.elapsedRealtime();
            return true;
        }
        return false;
    }

}
