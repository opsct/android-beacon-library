package org.altbeacon.beacon.client.batch;

import android.os.SystemClock;

import java.util.ArrayList;
import java.util.List;

/**
 * Permits to limit the call to the {@link BeaconDataBatchProvider} in case of error.
 *
 * This is particularly useful if the backend service is down
 *
 * Created by Connecthings on 03/10/16.
 */
public class BatchCallProviderLimiter {

    private int currentPosition = 0;
    private int countError;
    private long nextTimeToCallBatchProvider;

    private List<BatchErrorLimiter> batchErrorLimiters;

    public BatchCallProviderLimiter(){
        batchErrorLimiters = new ArrayList<>();
        batchErrorLimiters.add(new BatchErrorLimiter(0, 5));
        batchErrorLimiters.add(new BatchErrorLimiter(4, 10));
        batchErrorLimiters.add(new BatchErrorLimiter(6, 20));
        batchErrorLimiters.add(new BatchErrorLimiter(10, 60));
        batchErrorLimiters.add(new BatchErrorLimiter(12, 60 * 5));
    }

    List<BatchErrorLimiter> getBatchErrorLimiters() {
        return batchErrorLimiters;
    }

    BatchErrorLimiter getBatchErrorLimiter(int position) {
        return batchErrorLimiters.get(position);
    }

    BatchErrorLimiter getCurrentBatchErrorLimiter() {
        return batchErrorLimiters.get(currentPosition);
    }

    int getCountError(){
        return countError;
    }

    int getCurrentPosition(){
        return currentPosition;
    }

    public synchronized boolean isTimeToCallBatchProvider(){
        return nextTimeToCallBatchProvider < SystemClock.elapsedRealtime();
    }

    public synchronized void reset(){
        countError = 0;
        currentPosition = 0;
        nextTimeToCallBatchProvider = 0;
    }

    public synchronized void addError(){
        countError ++;
        int size = batchErrorLimiters.size();
        BatchErrorLimiter nextBatchErrorLimiter = null;
        BatchErrorLimiter currentBatchErrorLimiter = null;
        for(int i = currentPosition; i < size; i++){
            if( i == (size -1)){
                currentBatchErrorLimiter = batchErrorLimiters.get(i);
                currentPosition = i;
            }else{
                nextBatchErrorLimiter = batchErrorLimiters.get(i+1);
                if(countError < nextBatchErrorLimiter.minErrorNumbers){
                    currentPosition = i;
                    currentBatchErrorLimiter = batchErrorLimiters.get(i);
                    break;
                }
            }
        }
        nextTimeToCallBatchProvider = currentBatchErrorLimiter.timeWaitingBeforeNewCall + SystemClock.elapsedRealtime();
    }

    class BatchErrorLimiter {
        private int minErrorNumbers;
        private long timeWaitingBeforeNewCall;

        public BatchErrorLimiter(int maxError, long timeWaitingBeforeNewCall) {
            this.minErrorNumbers = maxError;
            this.timeWaitingBeforeNewCall = timeWaitingBeforeNewCall * 1000;
        }

        public int getMinErrorNumbers() {
            return minErrorNumbers;
        }

        public long getTimeWaitingBeforeNewCall() {
            return timeWaitingBeforeNewCall;
        }
    }

}
