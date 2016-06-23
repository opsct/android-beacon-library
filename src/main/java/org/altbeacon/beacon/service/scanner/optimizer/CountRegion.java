package org.altbeacon.beacon.service.scanner.optimizer;

import android.os.SystemClock;

import org.altbeacon.beacon.Region;

/**
 * Created by Connecthings on 22/06/16.
 */
public abstract class CountRegion {
    private int count;
    private long previousIn;
    private long previousOut;
    private long lastIn;
    private long lastOut;
    private long last;
    private long previous;

    public boolean isIn(){
        return count != 0;
    }

    public boolean isLastUpdateForLessThen(long delay){
        long time = SystemClock.elapsedRealtime() - delay;
        return lastIn >= time || lastOut >= time;
    }

    public boolean isIntervalLessThan(long delay){
        return (last - previous) < delay;
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

    public long getLast() {
        return last;
    }

    public long getPrevious() {
        return previous;
    }

    public long getPreviousIn() {
        return previousIn;
    }

    public long getPreviousOut() {
        return previousOut;
    }

    protected abstract boolean checkRegion(Region region);

    public boolean add(Region region){
        if(checkRegion(region)){
            count ++;
            previousIn = lastIn;
            lastIn = SystemClock.elapsedRealtime();
            previous = last;
            last = lastIn;
            return true;
        }
        return false;
    }

    public boolean remove(Region region){
        if(checkRegion(region)) {
            count --;
            previousOut = lastOut;
            lastOut = SystemClock.elapsedRealtime();
            previous = last;
            last = lastOut;
            return true;
        }
        return false;
    }

}
