package org.altbeacon.beacon.service.scanner.optimizer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Permit to detect the screen state and to notify a CycleScanStrategy that implements the ScreenStateListener
 *
 * Created by Connecthings
 */
public class ScreenStateBroadcastReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        ScreenStateInstance cycleScanStrategyInstance = ScreenStateInstance.getInstance();
        if (Intent.ACTION_SCREEN_ON.equals(action)) {
            cycleScanStrategyInstance.onScreenOn();
        }else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
            cycleScanStrategyInstance.onScreenOff();
        }

    }
}
