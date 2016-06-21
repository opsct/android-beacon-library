package org.altbeacon.beacon.service.scanner.optimizer;

/**
 * Singleton to keep the current CycleScanStrategy if implementing the ScreenStateListener.
 * Necessary to permit to a CycleScanStrategy implementing the ScreenStateListener
 * to be notified about the ScreenState from the ScreenStateBroadcastReceiver
 *
 * Created by Connecthings
 */
public class ScreenStateInstance implements ScreenStateListener{

    private static ScreenStateInstance INSTANCE;

    private ScreenStateListener mScreenStateListener;

    private final Object mScreenStateListenerLock = new Object();

    public static ScreenStateInstance getInstance(){
        if(INSTANCE == null){
            INSTANCE  = new ScreenStateInstance();
        }
        return INSTANCE;
    }

    public void update(Object object){
        synchronized (mScreenStateListenerLock) {
            if(object instanceof ScreenStateListener) {
                this.mScreenStateListener = (ScreenStateListener) object;
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
