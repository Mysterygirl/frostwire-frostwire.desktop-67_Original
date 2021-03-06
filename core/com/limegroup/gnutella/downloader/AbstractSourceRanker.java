package com.limegroup.gnutella.downloader;

import java.util.Collection;

import com.limegroup.gnutella.RemoteFileDesc;

/**
 * A class that ranks sources for a download. 
 * 
 * It uses a factory pattern to provide the best ranker based on system
 * conditions.
 */
public abstract class AbstractSourceRanker implements SourceRanker {

    /**
     * The mesh handler to inform when altlocs fail
     */
    protected MeshHandler meshHandler;

    public boolean addToPool(Collection<? extends RemoteFileDesc> hosts) {
        boolean ret = false;
        for(RemoteFileDesc host : hosts) {
            if (addToPool(host))
                ret = true;
        }
        return ret;
    }
    
    public abstract boolean addToPool(RemoteFileDesc host);
	
    public abstract boolean hasMore();
    
    public abstract RemoteFileDesc getBest();
    
    /**
     * @return the collection of hosts that can be shared with other rankers
     */
    public abstract Collection<RemoteFileDesc> getShareableHosts();
    
    public abstract int getNumKnownHosts();
    
    public synchronized boolean hasNonBusy() {
        return getNumKnownHosts() > getNumBusyHosts();
    }

    public synchronized int getNumBusyHosts() {
        int ret = 0;
        long now = System.currentTimeMillis();
        for(RemoteFileDesc rfd : getPotentiallyBusyHosts()) {
            if (rfd.isBusy(now))
                ret++;
        }
        return ret;
    }
    
    public synchronized int calculateWaitTime() {
        if (!hasMore())
            return 0;
        
        // waitTime is in seconds
        int waitTime = Integer.MAX_VALUE;
        long now = System.currentTimeMillis();
        for(RemoteFileDesc rfd : getPotentiallyBusyHosts()) {
            if (!rfd.isBusy(now))
                continue;
            waitTime = Math.min(waitTime, rfd.getWaitTime(now));
        }
        
        // waitTime was in seconds
        return (waitTime*1000);
    }
    
    protected abstract Collection<RemoteFileDesc> getPotentiallyBusyHosts();
    
    public synchronized void stop() {
        clearState();
        meshHandler = null;
    }
    
    protected void clearState() {}
    
    public synchronized void setMeshHandler(MeshHandler handler) {
        meshHandler = handler;
    }
    
    public synchronized MeshHandler getMeshHandler() {
        return meshHandler;
    }
}
