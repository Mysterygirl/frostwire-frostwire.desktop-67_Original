package com.limegroup.gnutella;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.settings.DownloadSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.statistics.TcpBandwidthStatistics;


/**
 * This class's primary functions is to run the timer that continually
 * checks the amount of bandwidth passed through upstream and downstream 
 * HTTP file transfers.  It records the maximum of the sum of these streams
 * to determine the node's bandwidth.
 * 
 * It then updates the UltrapeerCapable and DHTCapable status of this node
 * 
 */
// TODO starts DHTManager, should also stop it
@Singleton
class NodeAssignerImpl implements NodeAssigner {
    
    private static final Log LOG = LogFactory.getLog(NodeAssignerImpl.class);
    
    /**
     * Constant value for whether or not the operating system qualifies
     * this node for Ultrapeer status.
     */
    private static final boolean ULTRAPEER_OS = OSUtils.isHighLoadOS();
    
    /**
     * Constant for the number of milliseconds between the timer's calls
     * to its <tt>Runnable</tt>s.
     */
    static final int TIMER_DELAY = 1000;
    
    /**
     * Constant for the number of seconds between the timer's calls
     * to its <tt>Runnable</tt>s.
     */
    private static final int TIMER_DELAY_IN_SECONDS = TIMER_DELAY/1000;

    /**
     * Variable for the current uptime of this node.
     */
    private long _currentUptime = 0;

    /**
     * Variable for the maximum number of bytes per second transferred 
     * downstream over the history of the application.
     */
    private int _maxUpstreamBytesPerSec =
        UploadSettings.MAX_UPLOAD_BYTES_PER_SEC.getValue();

    /**
     * Variable for the maximum number of bytes per second transferred 
     * upstream over the history of the application.
     */
    private int _maxDownstreamBytesPerSec = 
        DownloadSettings.MAX_DOWNLOAD_BYTES_PER_SEC.getValue();
    
    /**
     * Variable for whether or not this node has such good values that it is too
     * good to pass up for becoming an Ultrapeer.
     */
    private volatile boolean _isTooGoodUltrapeerToPassUp = false;

    /**
     * Variable for the last time we attempted to become an Ultrapeer.
     */
    private volatile long _lastUltrapeerAttempt = 0L;

    /**
     * Number of times we've tried to become an Ultrapeer.
     */
    private int _ultrapeerTries = 0;
    
    /**
     * Wether or not this node is "Hardcore" capable
     */
    private boolean _isHardcoreCapable;
    
    /**
     * The node assigner's timer task
     */
    private ScheduledFuture<?>  timer;
    

    private final Provider<BandwidthTracker> uploadTracker;
    private final Provider<BandwidthTracker> downloadTracker;
    private final Provider<ConnectionManager> connectionManager;
    private final NetworkManager networkManager;
    private final SearchServices searchServices;
    private final Provider<DHTManager> dhtManager;
    private final ScheduledExecutorService backgroundExecutor;
    private final Executor unlimitedExecutor;
    private final ConnectionServices connectionServices;
    private final TcpBandwidthStatistics tcpBandwidthStatistics;
    private final NetworkInstanceUtils networkInstanceUtils;
    

    /** 
     * Creates a new <tt>NodeAssigner</tt>. 
     *
     * @param uploadTracker the <tt>BandwidthTracker</tt> instance for 
     *                      tracking bandwidth used for uploads
     * @param downloadTracker the <tt>BandwidthTracker</tt> instance for
     *                        tracking bandwidth used for downloads
     * @param connectionManager Reference to the ConnectionManager for this node
     */
    @Inject
    public NodeAssignerImpl(@Named("uploadTracker") Provider<BandwidthTracker>uploadTracker, 
                        @Named("downloadTracker") Provider<BandwidthTracker> downloadTracker,
                        Provider<ConnectionManager> connectionManager,
                        NetworkManager networkManager,
                        SearchServices searchServices,
                        Provider<DHTManager> dhtManager,
                        @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
                        @Named("unlimitedExecutor") Executor unlimitedExecutor,
                        ConnectionServices connectionServices,
                        TcpBandwidthStatistics tcpBandwidthStatistics,
                        NetworkInstanceUtils networkInstanceUtils) {
        this.uploadTracker = uploadTracker;
        this.downloadTracker = downloadTracker;  
        this.connectionManager = connectionManager;
        this.networkManager = networkManager;
        this.searchServices = searchServices;
        this.dhtManager = dhtManager;
        this.backgroundExecutor = backgroundExecutor;
        this.connectionServices = connectionServices;
        this.unlimitedExecutor = unlimitedExecutor;
        this.tcpBandwidthStatistics = tcpBandwidthStatistics;
        this.networkInstanceUtils = networkInstanceUtils;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.NodeAssigner#start()
     */
    public void start() {
        Runnable task=new Runnable() {
            public void run() {
                collectBandwidthData();
                //check if became Hardcore capable
                setHardcoreCapable();
                //check if became ultrapeer capable
                assignUltrapeerNode();
                //check if became DHT capable
                assignDHTMode();
            }
        };            
        timer = backgroundExecutor.scheduleWithFixedDelay(task, 0, TIMER_DELAY, TimeUnit.MILLISECONDS);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.NodeAssigner#stop()
     */
    public void stop() {
        if(timer != null) {
            timer.cancel(true);
        }
    }
    
    /**
     * Collects data on the bandwidth that has been used for file uploads
     * and downloads.
     */
    private void collectBandwidthData() {
        _currentUptime += TIMER_DELAY_IN_SECONDS;
        uploadTracker.get().measureBandwidth();
        downloadTracker.get().measureBandwidth();
        connectionManager.get().measureBandwidth();
        float bandwidth = 0;
        try {
            bandwidth = uploadTracker.get().getMeasuredBandwidth();
        }catch(InsufficientDataException ide) {
            bandwidth = 0;
        }
        int newUpstreamBytesPerSec = 
            (int)bandwidth
           +(int)connectionManager.get().getMeasuredUpstreamBandwidth();
        bandwidth = 0;
        try {
            bandwidth = downloadTracker.get().getMeasuredBandwidth();
        } catch (InsufficientDataException ide) {
            bandwidth = 0;
        }
        int newDownstreamBytesPerSec = 
            (int)bandwidth
           +(int)connectionManager.get().getMeasuredDownstreamBandwidth();
        if(newUpstreamBytesPerSec > _maxUpstreamBytesPerSec) {
            _maxUpstreamBytesPerSec = newUpstreamBytesPerSec;
            UploadSettings.MAX_UPLOAD_BYTES_PER_SEC.setValue(_maxUpstreamBytesPerSec);
        }
        if(newDownstreamBytesPerSec > _maxDownstreamBytesPerSec) {
            _maxDownstreamBytesPerSec = newDownstreamBytesPerSec;
            DownloadSettings.MAX_DOWNLOAD_BYTES_PER_SEC.setValue(_maxDownstreamBytesPerSec);
        }
    }
    
    /**
     * Determinates whether or not a node is capable of handling a special
     * function such as beeing an ultrapeer or connecting to the dht
     */
    private void setHardcoreCapable() {
        _isHardcoreCapable = 
        //Is upstream OR downstream high enough?
        ((_maxUpstreamBytesPerSec >= 
                UltrapeerSettings.MIN_UPSTREAM_REQUIRED.getValue() ||
         _maxDownstreamBytesPerSec >= 
                UltrapeerSettings.MIN_DOWNSTREAM_REQUIRED.getValue()) &&
        //AND I'm not a modem (in case estimate wrong)
        (ConnectionSettings.CONNECTION_SPEED.getValue() > SpeedConstants.MODEM_SPEED_INT) &&
        //AND am I not firewalled?
        ConnectionSettings.EVER_ACCEPTED_INCOMING.getValue() &&
        //AND am I a capable OS?
        ULTRAPEER_OS &&
        //AND I do not have a private address
        !networkInstanceUtils.isPrivate());
        
        if(LOG.isDebugEnabled()) {
            LOG.debug("Hardcore capable: "+_isHardcoreCapable);
        }
    }
    
    /**
     * Sets EVER_ULTRAPEER_CAPABLE to true if this has the necessary
     * requirements for becoming a ultrapeer if needed, based on 
     * the node's bandwidth, operating system, firewalled status, 
     * uptime, etc.  Does not modify the property if the capabilities
     * are not met.  If the user has disabled ultrapeer support, 
     * sets EVER_ULTRAPEER_CAPABLE to false.
     * 
     * @return true if we are or will try to become an ultrapeer, false otherwise
     */
    private void assignUltrapeerNode() {
        if (UltrapeerSettings.DISABLE_ULTRAPEER_MODE.getValue()) {
            LOG.debug("Ultrapeer mode disabled");
            UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(false);
            return;
        }
        
        // If we're already an Ultrapeer then don't bother
        if (connectionServices.isSupernode()) {
            LOG.debug("Already an ultrapeer, exiting");
            return;
        }
        
        boolean avgUptimePasses = ApplicationSettings.AVERAGE_UPTIME.getValue() >= UltrapeerSettings.MIN_AVG_UPTIME.getValue();
        boolean curUptimePasses = _currentUptime >= UltrapeerSettings.MIN_INITIAL_UPTIME.getValue();
        boolean uptimePasses = avgUptimePasses | curUptimePasses;
        
        boolean isUltrapeerCapable = _isHardcoreCapable && uptimePasses && networkManager.isGUESSCapable();
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("Node is ultrapeer capable: " + isUltrapeerCapable + "(hc: "
                    + _isHardcoreCapable + ", up: " + uptimePasses + ", gc: "
                    + networkManager.isGUESSCapable());
        }

        long curTime = System.currentTimeMillis();

        // check if this node has such good values that we simply can't pass
        // it up as an Ultrapeer -- it will just get forced to be one
        _isTooGoodUltrapeerToPassUp = isUltrapeerCapable &&
            networkManager.acceptedIncomingConnection() &&
            (curTime - searchServices.getLastQueryTime() > 5*60*1000) &&
            (tcpBandwidthStatistics.getAverageHttpUpstream() < 1);

        if(LOG.isDebugEnabled()) {
            LOG.debug("Node is "+(_isTooGoodUltrapeerToPassUp?"":"NOT")+" to good to pass up");
        }
        
        // record new ultrapeer capable value.
        if(isUltrapeerCapable)
            UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);

        if(_isTooGoodUltrapeerToPassUp && 
                shouldTryToBecomeAnUltrapeer(curTime) && 
                switchFromActiveDHTNodeToUltrapeer()) {
            
            if(LOG.isDebugEnabled()) {
                LOG.debug("Node WILL try to become an ultrapeer");
            }
            
            _ultrapeerTries++;
            // try to become an Ultrapeer -- how persistent we are depends on
            // how many times we've tried, and so how long we've been
            // running for
            final int demotes = 4 * _ultrapeerTries;
            Runnable ultrapeerRunner = new Runnable() {
                public void run() {
                    connectionManager.get().tryToBecomeAnUltrapeer(demotes);
                }
            };
                
            unlimitedExecutor.execute(ultrapeerRunner);
            return;
        } 
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("Node will not try to become an ultrapeer");
        }
    }
    
    /**
     * Checks whether or not we should try again to become an Ultrapeer.
     * 
     * @param curTime the current time in milliseconds
     * @return <tt>true</tt> if we should try again to become an Ultrapeer,
     *  otherwise <tt>false</tt>
     */
    private boolean shouldTryToBecomeAnUltrapeer(long curTime) {
        if(curTime - _lastUltrapeerAttempt < UltrapeerSettings.UP_RETRY_TIME.getValue()) {
            return false;
        }
        _lastUltrapeerAttempt = curTime;
        return true;
    }
    
    /**
     * If we are allready actively part of the DHT, switch to ultrapeer with a given 
     * (possibly biased) probability.
     * 
     * @return true if we switched, false otherwise
     */
    private boolean switchFromActiveDHTNodeToUltrapeer() {
        
        // If I'm not a DHT Node running in ACTIVE mode then
        // try to become an Ultrapeer
        if (dhtManager.get().getDHTMode() != DHTMode.ACTIVE) {
            return true;
        }
        
        // If I'm in ACTIVE mode and Ultrapeers are excluded
        // from running in ACTIVE mode then switch with a
        // certain probability...
        if (DHTSettings.EXCLUDE_ULTRAPEERS.getValue() && acceptUltrapeer()) {            
            if (LOG.isDebugEnabled())
                LOG.debug("Randomly switching from DHT node to ultrapeer!");
            return true;
        }
        
        // Don't switch from ACTIVE mode to Ultrapeer+PASSIVE
        return false;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.NodeAssigner#isTooGoodUltrapeerToPassUp()
     */
    public boolean isTooGoodUltrapeerToPassUp() {
        return _isTooGoodUltrapeerToPassUp;
    }
    
    /**
     * This method assigns a DHT mode based on the local Node's
     * Ultrapeer, uptime and firewall status.
     */
    private DHTMode assignDHTMode() {
        
        // Remember the old mode as we're only going to switch
        // if the new mode is different from the old mode!
        final DHTMode current = dhtManager.get().getDHTMode();
        assert (current != null) : "Current DHTMode is null, fix your DHTManager-Stub!";
        
        // Initial mode is to turn off the DHT
        DHTMode mode = DHTMode.INACTIVE;
        
        // Check if the DHT was disabled by somebody. If so shut it
        // down and return
        if (!dhtManager.get().isEnabled()) {
            if (current != mode) {
                switchDHTMode(current, mode);
            }
            return mode;
        }
        
        // If we're an Ultrapeer, connect to the DHT in passive mode or if 
        // we were connected as active node before, switch to passive mode
        // It is important to not assume that ultrapeers have the required uptime.  
        boolean isUltrapeer = connectionServices.isActiveSuperNode();
        if (isUltrapeer && isPassiveDHTCapable()) {
            mode = DHTMode.PASSIVE;
        } 
        
        // If we're not an ultrapeer or if ultrapeers are allowed to become
        // active DHT nodes then figure out if the local node matches the
        // requirements to become an active DHT node
        if (!isUltrapeer || !DHTSettings.EXCLUDE_ULTRAPEERS.getValue()) {
            
            // Make sure that the node has had the time to try to connect as an ultrapeer
            assert ((DHTSettings.MIN_ACTIVE_DHT_INITIAL_UPTIME.getValue()/1000L) 
                    > UltrapeerSettings.MIN_CONNECT_TIME.getValue()) : "Wrong minimum initial uptime";
                    
            final long averageTime = Math.max(connectionManager.get().getCurrentAverageUptime(),
                    ApplicationSettings.AVERAGE_CONNECTION_TIME.getValue());
            
            // This is the minimum requirement to connect to the DHT in passive leaf mode
            boolean passiveCapable = isPassiveLeafDHTCapable();
        
            // In order to be able to connect to the DHT in active mode you
            // must not be firewalled (must be able to receive unsolicited UDP)
            boolean activeCapable = isActiveDHTCapable();
            
            if (LOG.isDebugEnabled()) {
                if (passiveCapable && !activeCapable) {
                    LOG.debug("Node is passive DHT capable\n average time: " 
                            + averageTime + "\n currentUptime: " + _currentUptime);
                } else if (activeCapable) {
                    LOG.debug("Node is active DHT capable\n average time: " 
                            + averageTime + "\n currentUptime: " + _currentUptime);
                } else {
                    LOG.debug("Node is NOT DHT capable\n average time: " 
                            + averageTime + "\n currentUptime: " + _currentUptime);
                }
            }
            
            // Only leafs can become passive leaf nodes!
            if (passiveCapable && !isUltrapeer) {
                mode = DHTMode.PASSIVE_LEAF;
            }
            
            // Switch to active mode if possible!
            if (activeCapable) {
                mode = DHTMode.ACTIVE;
            }
        }
        
        if (mode == DHTMode.PASSIVE 
                && !DHTSettings.ENABLE_PASSIVE_DHT_MODE.getValue()) {
            mode = DHTMode.INACTIVE;
        } else if (mode == DHTMode.PASSIVE_LEAF
                && !DHTSettings.ENABLE_PASSIVE_LEAF_DHT_MODE.getValue()) {
            mode = DHTMode.INACTIVE;
        }
        
        if (DHTSettings.FORCE_DHT_CONNECT.getValue()) {
            mode = DHTMode.ACTIVE;
        }
        
        // We switch the mode if:
        // The current mode is different from the new mode AND we're either
        //   an Ultrapeer or get selected with a certain likehood.
        // OR the current mode is _not_ INACTIVE and the new mode is INACTIVE
        //   as we always want to allow to shut down the DHT! This can happen
        //   if an Ultrapeer is demoted to a Leaf and does not fulfil the
        //   requirements to be an ACTIVE or PASSIVE_LEAF node or if certain
        //   modes are forcibly disabled.
        if (((mode != current) && (isUltrapeer || acceptDHTNode()))
                || (current != DHTMode.INACTIVE && mode == DHTMode.INACTIVE)) {
            switchDHTMode(current, mode);
        }
        
        return mode;
    }
    
    /**
     * Switches the mode of the DHT
     */
    private void switchDHTMode(final DHTMode from, final DHTMode to) {
        Runnable init = new Runnable() {
            public void run() {
                if (to != DHTMode.INACTIVE) {
                    dhtManager.get().start(to);
                } else {
                    dhtManager.get().stop();
                }
                
                DHTSettings.DHT_MODE.setValue(to.toString());
            }
        };
        
        unlimitedExecutor.execute(init);
    }
    
    
    /**
     * @return whether the node is PASSIVE capable.
     */
    private boolean isPassiveDHTCapable() {
        long averageTime = getAverageTime();
        return ULTRAPEER_OS
        && (averageTime >= DHTSettings.MIN_PASSIVE_DHT_AVERAGE_UPTIME.getValue()
                && _currentUptime >= (DHTSettings.MIN_PASSIVE_DHT_INITIAL_UPTIME.getValue()/1000L))
                && networkManager.canReceiveSolicited();
    }
    
    /**
     * Returns whether ot not a Node is PASSIVE_LEAF capable
     */
    private boolean isPassiveLeafDHTCapable() {
        long averageTime = getAverageTime();
        
        return ULTRAPEER_OS
                && (averageTime >= DHTSettings.MIN_PASSIVE_LEAF_DHT_AVERAGE_UPTIME.getValue()
                && _currentUptime >= (DHTSettings.MIN_PASSIVE_LEAF_DHT_INITIAL_UPTIME.getValue()/1000L))
                && networkManager.canReceiveSolicited();
    }
    
    /**
     * Returns whether ot not a Node is ACTIVE capable
     */
    private boolean isActiveDHTCapable() {
        long averageTime = getAverageTime();
        
        return _isHardcoreCapable
                && (averageTime >= DHTSettings.MIN_ACTIVE_DHT_AVERAGE_UPTIME.getValue()
                && _currentUptime >= (DHTSettings.MIN_ACTIVE_DHT_INITIAL_UPTIME.getValue()/1000L))
                && networkManager.isGUESSCapable();
    }
    
    private long getAverageTime() {
        return Math.max(connectionManager.get().getCurrentAverageUptime(),
                ApplicationSettings.AVERAGE_CONNECTION_TIME.getValue());
    }
    
    /**
     * Returns true based on a certain probability. 
     * 
     * See DHTSetting.DHT_ACCEPT_PROBABILITY for more info!
     */
    private boolean acceptDHTNode() {
        return (Math.random() < DHTSettings.DHT_ACCEPT_PROBABILITY.getValue());
    }
    
    /**
     * Returns true based on a certain probability.
     * 
     * See DHTSetting.DHT_TO_ULTRAPEER_PROBABILITY for more info!
     */
    private boolean acceptUltrapeer() {
        return (Math.random() < DHTSettings.SWITCH_TO_ULTRAPEER_PROBABILITY.getValue());
    }
}
