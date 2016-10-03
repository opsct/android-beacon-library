package com.connecthings.altbeacon.beacon.client.batch;

import java.util.ArrayList;
import java.util.List;

/**
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

    public synchronized boolean isTimeToCallBatchProvider(){
        return nextTimeToCallBatchProvider < System.currentTimeMillis();
    }

    public synchronized void reset(){
        countError = 0;
        currentPosition = 0;
        nextTimeToCallBatchProvider = 0;
    }

    public synchronized void addError(){
        countError ++;
        BatchErrorLimiter currentBackendErrorColler= null;
        int size = batchErrorLimiters.size();
        for(int i = currentPosition; i < size; i++){
            currentBackendErrorColler = batchErrorLimiters.get(i);
            if(i==currentPosition && countError==currentBackendErrorColler.minErrorNumbers){
                break;
            }else if(countError < currentBackendErrorColler.minErrorNumbers){
                break;
            }
        }
        nextTimeToCallBatchProvider = currentBackendErrorColler.timeWaitingBeforeNewCall + System.currentTimeMillis();
    }

    private class BatchErrorLimiter {
        private int minErrorNumbers;
        private long timeWaitingBeforeNewCall;

        public BatchErrorLimiter(int maxError, long timeWaitingBeforeNewCall) {
            this.minErrorNumbers = maxError;
            this.timeWaitingBeforeNewCall = timeWaitingBeforeNewCall * 1000;
        }
    }

}
