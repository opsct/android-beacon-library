package org.altbeacon.beacon.service.scanner.optimizer;

/**
 * Singleton to keep the current CycleScanStrategy if implementing the ScreenStateListener.
 * Necessary to permit to a CycleScanStrategy implementing the ScreenStateListener
 * to be notified about the ScreenState from the ScreenStateBroadcastReceiver
 *
 * Created by Connecthings
 */
public class CycleScanStrategyInstance implements ScreenStateListener{

    private static CycleScanStrategyInstance INSTANCE;

    private ScreenStateListener mScreenStateListener;

    private final Object mScreenStateListenerLock = new Object();

    public static CycleScanStrategyInstance getInstance(){
        if(INSTANCE == null){
            INSTANCE  = new CycleScanStrategyInstance();
        }
        return INSTANCE;
    }

    public void updateCycleScanStrategy(CycleScanStrategy cycleScanStrategy){
        synchronized (mScreenStateListenerLock) {
            if(cycleScanStrategy instanceof ScreenStateListener) {
                this.mScreenStateListener = (ScreenStateListener) cycleScanStrategy;
            }else{
                this.mScreenStateListener = null;
            }
        }
    }

    @Override
    public void onScreenOn() {
        synchronized (mScreenStateListenerLock) {
            if(mScreenStateListener != null){
                mScreenStateListener.onScreenOn();
            }
        }
    }

    @Override
    public void onScreenOff() {
        synchronized (mScreenStateListenerLock) {
            if(mScreenStateListener != null){
                mScreenStateListener.onScreenOff();
            }
        }
    }
}
