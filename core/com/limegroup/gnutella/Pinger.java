package com.limegroup.gnutella;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.settings.PingPongSettings;

/**
 * This class continually sends broadcast pings on behalf of an Ultrapeer
 * to update the host caches of both itself and its leaves.  This class 
 * reduces overall ping and pong traffic because it allows us not to forward
 * pings received from other hosts.  Instead, we use pong caching to respond
 * to those pings with cached pongs, and send pings periodically in this 
 * class to obtain fresh host data.
 */
@Singleton
public final class Pinger implements Runnable {

    /**
     * Constant for the number of milliseconds to wait between ping 
     * broadcasts.  Public to make testing easier.
     */
    public static final int PING_INTERVAL = 3000;
    
    private final ScheduledExecutorService backgroundExecutor;
    private final ConnectionServices connectionServices;
    private final Provider<MessageRouter> messageRouter;

    private final PingRequestFactory pingRequestFactory;
        
    @Inject
    public Pinger(
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            ConnectionServices connectionServices,
            Provider<MessageRouter> messageRouter,
            PingRequestFactory pingRequestFactory) {
        this.backgroundExecutor = backgroundExecutor;
        this.connectionServices = connectionServices;
        this.messageRouter = messageRouter;
        this.pingRequestFactory = pingRequestFactory;
    }


    /**
     * Starts the thread that continually sends broadcast pings on behalf of
     * this node if it's an Ultrapeer.
     */
    public void start() {
        backgroundExecutor.scheduleWithFixedDelay(this, PING_INTERVAL, PING_INTERVAL, TimeUnit.MILLISECONDS);
    }


    /**
     * Broadcasts a ping to all connections.
     */
    public void run() {
        if (connectionServices.isSupernode() && PingPongSettings.PINGS_ACTIVE.getValue()) {
            messageRouter.get().broadcastPingRequest(pingRequestFactory.createPingRequest((byte) 3));
        }
    }
}




