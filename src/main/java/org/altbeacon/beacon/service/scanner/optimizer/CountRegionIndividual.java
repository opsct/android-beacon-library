package org.altbeacon.beacon.service.scanner.optimizer;

import org.altbeacon.beacon.Region;

/**
 * Created by olivierstevens on 22/06/16.
 */
public class CountRegionIndividual extends CountRegion{
    private int count;
    private long lastIn;
    private long lastOut;
    private int nbIdentifierToCheck;

    public CountRegionIndividual(int nbIdentifierToCheck){
        this.nbIdentifierToCheck = nbIdentifierToCheck;
    }

    protected boolean checkRegion(Region region){
        int positionOfNonNUllId = nbIdentifierToCheck-1;
        for(int i = 2;i>positionOfNonNUllId;i--){
            if(region.getIdentifier(i)!=null){
                return false;
            }
        }
        for(int i = positionOfNonNUllId;i>=0;i--){
            if(region.getIdentifier(i)==null){
                return false;
            }
        }
        return true;
    }

}
