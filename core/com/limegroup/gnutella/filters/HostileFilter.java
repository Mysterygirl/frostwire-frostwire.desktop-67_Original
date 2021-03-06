package com.limegroup.gnutella.filters;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IP;
import org.limewire.io.NetworkInstanceUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.settings.FilterSettings;

@Singleton
public class HostileFilter extends  AbstractIPFilter {

    private static final Log LOG = LogFactory.getLog(HostileFilter.class);
    
    
    private volatile IPList hostileHosts = new IPList();
    
    private final NetworkInstanceUtils networkInstanceUtils;
    
    @Inject
    public HostileFilter(NetworkInstanceUtils networkInstanceUtils) {
        this.networkInstanceUtils = networkInstanceUtils;
    }
    
    /**
     * Refresh the IPFilter's instance.
     */
    public void refreshHosts(IPFilterCallback callback) {
        refreshHosts();
        callback.ipFiltersLoaded();
    }
    
    public void refreshHosts() {
        LOG.info(this.hashCode()+ " refreshing hosts at hostile level");
        // Load hostile, making sure the list is valid
        IPList newHostile = new IPList();
        String [] allHosts = FilterSettings.HOSTILE_IPS.getValue();
        try {
            for (String ip : allHosts)
                newHostile.add(new IP(ip));
            
            //if (newHostile.isValidFilter(true, networkInstanceUtils)) //allow private ips
            if (newHostile.isValidFilter(false, networkInstanceUtils)) //dont allow private ips
                hostileHosts = newHostile;
        } catch (IllegalArgumentException badSimpp){}
    }
    
    public boolean hasBlacklistedHosts() {
        return !hostileHosts.isEmpty();
    }
    
    public int logMinDistanceTo(IP ip) {
        return hostileHosts.logMinDistanceTo(ip);
    }
    
    protected boolean allowImpl(IP ip) {
        return !hostileHosts.contains(ip);
    }

    public void forceRefreshHosts() {
        // TODO Auto-generated method stub
        refreshHosts(); //it makes sense on LocalIPFilter.x
    }
    
    @Override
    public boolean isBlocked(String addr) {
        return hostileHosts.contains(new IP(addr));
    }
}
