package com.connecthings.altbeacon.beacon.client.batch;

import android.os.SystemClock;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertTrue;


/**
 * Created by Connechtings on 06/10/16.
 */
@Config(sdk = 18)
@RunWith(RobolectricTestRunner.class)
public class BatchCallProviderLimiterTest {

    @Test
    public void testLimitCall(){
        BatchCallProviderLimiter batchCallProviderLimiter = new BatchCallProviderLimiter();
        assertTrue("It's not time to call provider, that strange", batchCallProviderLimiter.isTimeToCallBatchProvider());
        batchCallProviderLimiter.addError();
        assertTrue("It's time to call provider, that strange", !batchCallProviderLimiter.isTimeToCallBatchProvider());
        SystemClock.setCurrentTimeMillis(batchCallProviderLimiter.getCurrentBatchErrorLimiter().getTimeWaitingBeforeNewCall() + 500);
        assertTrue("It's not time to call provider, that strange", batchCallProviderLimiter.isTimeToCallBatchProvider());
    }

    @Test
    public void testBatchErrorLimiterProgression(){
        BatchCallProviderLimiter batchCallProviderLimiter = new BatchCallProviderLimiter();
        int position = 0;
        int nextBatchErrorPosition = 0;
        while(batchCallProviderLimiter.getCountError()<15){
            batchCallProviderLimiter.addError();
            nextBatchErrorPosition = position == batchCallProviderLimiter.getBatchErrorLimiters().size() -1 ? position : position +1;
            if(batchCallProviderLimiter.getCountError() < batchCallProviderLimiter.getBatchErrorLimiter(nextBatchErrorPosition).getMinErrorNumbers()){
                assertTrue("batchErrorLimiter have been updated and it's the time - nb error: " + batchCallProviderLimiter.getCountError(), batchCallProviderLimiter.getCurrentBatchErrorLimiter() == batchCallProviderLimiter.getBatchErrorLimiter(position));
            }else if(position != batchCallProviderLimiter.getBatchErrorLimiters().size() -1){
                assertTrue("batchErrorLimiter has not been updated while it's the time - nb error: " + batchCallProviderLimiter.getCountError(), batchCallProviderLimiter.getCurrentBatchErrorLimiter() == batchCallProviderLimiter.getBatchErrorLimiter(nextBatchErrorPosition));
                position = nextBatchErrorPosition;
            }
        }
    }

}
