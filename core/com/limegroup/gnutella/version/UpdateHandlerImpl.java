package com.limegroup.gnutella.version;


import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.DefaultedHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.limewire.io.Connectable;
import org.limewire.io.IOUtils;
import org.limewire.io.IpPort;
import org.limewire.util.Clock;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.StringUtils;
import org.limewire.util.Version;
import org.limewire.util.VersionFormatException;
import org.limewire.util.VersionUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.NetworkUpdateSanityChecker;
import com.limegroup.gnutella.NetworkUpdateSanityChecker.RequestType;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.downloader.InNetworkDownloader;
import com.limegroup.gnutella.downloader.ManagedDownloader;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HttpClientListener;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.library.SharingUtils;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.UpdateSettings;
import com.limegroup.gnutella.util.FrostWireUtils;

/**
 * Manager for version updates.
 *
 * Handles queueing new data for parsing and keeping track of which current
 * version is stored in memory & on disk.
 */
@Singleton
public class UpdateHandlerImpl implements UpdateHandler {
    
    private static final Log LOG = LogFactory.getLog(UpdateHandlerImpl.class);
    
    private static final long THREE_DAYS = 3 * 24 * 60 * 60 * 1000;
    
    /** If we haven't had new updates in this long, schedule http failover */
    private static final long ONE_MONTH = 10L * THREE_DAYS;
    
    /**
     * The filename on disk where data is stored.
     */
    private static final String FILENAME = "version.xml";
    
    private static final int IGNORE_ID = Integer.MAX_VALUE;
    
    private static enum UpdateType {
        FROM_NETWORK, FROM_DISK, FROM_HTTP;
    }
    
    /**
     * init the random generator on class load time
     */
    private static final Random RANDOM = new Random();
    
    /**
     * means to override the current time for tests
     */
    private final Clock clock;
    
    /**
     * The most recent update info for this machine.
     */
    private volatile UpdateInformation _updateInfo;
    
    /**
     * A collection of DownloadInformation's that we need to retrieve an update for.
     */
    private volatile List<DownloadInformation> _updatesToDownload;
    
    /**
     * The most recent id of the update info.
     */
    private volatile int _lastId;
    
    /**
     * The bytes to send on the wire.
     *
     * TODO: Don't store in memory.
     */
    private volatile byte[] _lastBytes;
    
    /**
     * The timestamp of the latest update.
     */
    private long _lastTimestamp;
    
    /**
     * The next time we can make an attempt to download a pushed file.
     */
    private long _nextDownloadTime;
    
    private boolean _killingObsoleteNecessary;
    
    /** If an HTTP failover update is in progress */
    private final HttpRequestControl httpRequestControl = new HttpRequestControl();
    
    private final ScheduledExecutorService backgroundExecutor;
    private final Provider<ActivityCallback> activityCallback;
    private final ConnectionServices connectionServices;
    private final Provider<HttpExecutor> httpExecutor;
    private final Provider<HttpParams> defaultParams;
    private final Provider<NetworkUpdateSanityChecker> networkUpdateSanityChecker;
    private final CapabilitiesVMFactory capabilitiesVMFactory;
    private final Provider<ConnectionManager> connectionManager;
    private final Provider<DownloadManager> downloadManager;
    private final Provider<FileManager> fileManager;
    private final ApplicationServices applicationServices;
    private final UpdateCollectionFactory updateCollectionFactory;
    private final UpdateMessageVerifier updateMessageVerifier;
    private final RemoteFileDescFactory remoteFileDescFactory;
    
    private volatile String timeoutUpdateLocation = "http://update0.limewire.com/update.def";
    private volatile List<String> maxedUpdateList = Arrays.asList("http://update1.limewire.com/update.def",
            "http://update2.limewire.com/update.def",
            "http://update3.limewire.com/update.def",
            "http://update4.limewire.com/update.def",
            "http://update5.limewire.com/update.def",
            "http://update6.limewire.com/update.def",
            "http://update7.limewire.com/update.def",
            "http://update8.limewire.com/update.def",
            "http://update9.limewire.com/update.def",
            "http://update10.limewire.com/update.def");
    private volatile int minMaxHttpRequestDelay = 1000 * 60;
    private volatile int maxMaxHttpRequestDelay = 1000 * 60 * 30;
    private volatile int silentPeriodForMaxHttpRequest = 1000 * 60 * 5;
    
    @Inject
    UpdateHandlerImpl(@Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            Provider<ActivityCallback> activityCallback,
            ConnectionServices connectionServices,
            Provider<HttpExecutor> httpExecutor,
            @Named("defaults") Provider<HttpParams> defaultParams,
            Provider<NetworkUpdateSanityChecker> networkUpdateSanityChecker,
            CapabilitiesVMFactory capabilitiesVMFactory,
            Provider<ConnectionManager> connectionManager,
            Provider<DownloadManager> downloadManager,
            Provider<FileManager> fileManager,
            ApplicationServices applicationServices,
            UpdateCollectionFactory updateCollectionFactory,
            Clock clock,
            UpdateMessageVerifier updateMessageVerifier, 
            RemoteFileDescFactory remoteFileDescFactory) {
        this.backgroundExecutor = backgroundExecutor;
        this.activityCallback = activityCallback;
        this.connectionServices = connectionServices;
        this.httpExecutor = httpExecutor;
        this.defaultParams = defaultParams;
        this.networkUpdateSanityChecker = networkUpdateSanityChecker;
        this.capabilitiesVMFactory = capabilitiesVMFactory;
        this.connectionManager = connectionManager;
        this.downloadManager = downloadManager;
        this.fileManager = fileManager;
        this.applicationServices = applicationServices;
        this.updateCollectionFactory = updateCollectionFactory;
        this.clock = clock;
        this.updateMessageVerifier = updateMessageVerifier;
        this.remoteFileDescFactory = remoteFileDescFactory;
    }
        
    String getTimeoutUrl() {
        return timeoutUpdateLocation;
    }
    
    void setTimeoutUrl(String url) {
        this.timeoutUpdateLocation = url;
    }
    
    List<String> getMaxUrls() {
        return maxedUpdateList;
    }
    
    void setMaxUrls(List<String> urls) {
        this.maxedUpdateList = urls;
    }
    
    int getMinHttpRequestUpdateDelayForMaxFailover() {
        return minMaxHttpRequestDelay;
    }
    
    int getMaxHttpRequestUpdateDelayForMaxFailover() {
        return maxMaxHttpRequestDelay;
    }
    
    void setMinHttpRequestUpdateDelayForMaxFailover(int min) {
        minMaxHttpRequestDelay = min;
    }
    
    void setMaxHttpRequestUpdateDelayForMaxFailover(int max) {
        maxMaxHttpRequestDelay = max;
    }
    
    int getSilentPeriodForMaxHttpRequest() {
        return silentPeriodForMaxHttpRequest;
    }
    
    void setSilentPeriodForMaxHttpRequest(int silentPeriodForMaxHttpRequest) {
        this.silentPeriodForMaxHttpRequest = silentPeriodForMaxHttpRequest;
    }

    /**
     * Initializes data as read from disk.
     */
    public void initialize() {
//        LOG.trace("Initializing UpdateHandler");
//        backgroundExecutor.execute(new Runnable() {
//            public void run() {
//                handleDataInternal(FileUtils.readFileFully(getStoredFile()), UpdateType.FROM_DISK, null);
//            }
//        });
//        
//        // Try to update ourselves (re-use hosts for downloading, etc..)
//        // at a specified interval.
//        backgroundExecutor.schedule(new Poller(), UpdateSettings.UPDATE_RETRY_DELAY.getValue(), TimeUnit.MILLISECONDS);
    }
    
    /**
     * Sparks off an attempt to download any pending updates.
     */
    public void tryToDownloadUpdates() {
    	if (true) {
    		return;
    	}
    	
        backgroundExecutor.execute(new Runnable() {
            public void run() {
                UpdateInformation updateInfo = _updateInfo;
                
                if (updateInfo != null && 
                		updateInfo.getUpdateURN() != null &&
                		isMyUpdateDownloaded(updateInfo))
                    activityCallback.get().updateAvailable(updateInfo);
                
                downloadUpdates(_updatesToDownload, null);
            }
        });
    }
    
    /**
     * Notification that a ReplyHandler has received a VM containing an update.
     */
    public void handleUpdateAvailable(final ReplyHandler rh, final int version) {
        if(version == _lastId) {
            backgroundExecutor.execute(new Runnable() {
                public void run() {
                    addSourceIfIdMatches(rh, version);
                }
            });
        } else if(LOG.isDebugEnabled())
            LOG.debug("Another version from rh: " + rh + ", them: " + version + ", me: " + _lastId);
    }
    
    /**
     * Notification that a new message has arrived.
     *
     * (The actual processing is passed of to be run in a different thread.
     *  All notifications are processed in the same thread, sequentially.)
     */
    public void handleNewData(final byte[] data, final ReplyHandler handler) {
        if(data != null) {
            backgroundExecutor.execute(new Runnable() {
                public void run() {
                    LOG.trace("Parsing new data...");
                    handleDataInternal(data, UpdateType.FROM_NETWORK, handler);
                }
            });
        }
    }
    
    /**
     * Retrieves the latest id available.
     */
    public int getLatestId() {
        return _lastId;
    }
    
    
    /**
     * Gets the bytes to send on the wire.
     */
    public byte[] getLatestBytes() {
        return _lastBytes;
    }
    
    /**
     * Handles processing a newly arrived message.
     *
     * (Processes the data immediately.)
     */
    private void handleDataInternal(byte[] data, UpdateType updateType, ReplyHandler handler) {
        if (data == null) {
            if (updateType == UpdateType.FROM_NETWORK && handler != null)
                networkUpdateSanityChecker.get()
                        .handleInvalidResponse(handler, RequestType.VERSION);
            LOG.warn("No data to handle.");
            return;
        }

        String xml = updateMessageVerifier.getVerifiedData(data);
        if (xml == null) {
            if (updateType == UpdateType.FROM_NETWORK && handler != null)
                networkUpdateSanityChecker.get()
                        .handleInvalidResponse(handler, RequestType.VERSION);
            LOG.warn("Couldn't verify signature on data.");
            return;
        }
        
        if (updateType == UpdateType.FROM_NETWORK && handler != null)
            networkUpdateSanityChecker.get().handleValidResponse(handler, RequestType.VERSION);

        UpdateCollection uc = updateCollectionFactory.createUpdateCollection(xml);
        if (LOG.isDebugEnabled())
            LOG.debug("Got a collection with id: " + uc.getId() + ", from " + updateType + ".  Current id is: " + _lastId);

        switch (updateType) {
        case FROM_NETWORK:
            // the common case:
            // a) if max && no max already, do failover.
            // b) if not max && <= last, check stale.
            // c) if not max && > last, update
            if (uc.getId() == IGNORE_ID) {
                if (_lastId != IGNORE_ID)
                    doHttpMaxFailover(uc);
            } else if (uc.getId() <= _lastId) {
                checkForStaleUpdateAndMaybeDoHttpFailover(uc);
                addSourceIfIdMatches(handler, uc.getId());
            } else {// is greater
                storeAndUpdate(data, uc, updateType);
            }
            break;
        case FROM_DISK:
            // on first load:
            // a) always check for stale
            // b) update if we didn't get an update before this ran.
            checkForStaleUpdateAndMaybeDoHttpFailover(uc);
            if (uc.getId() > _lastId)
                storeAndUpdate(data, uc, updateType);
            break;
        case FROM_HTTP:
            // on HTTP response:
            // a) update if >= stored.
            // (note this is >=, different than >, which is from
            // network)
            if (uc.getId() >= _lastId)
                storeAndUpdate(data, uc, updateType);
            break;
        }
    }
    
    /**
     * Stores the given data to disk & posts an update to neighboring
     * connections. Starts the download of any updates
     */
    private void storeAndUpdate(byte[] data, UpdateCollection uc, UpdateType updateType) {
        if(LOG.isTraceEnabled())
            LOG.trace("Retrieved new data from: " + updateType + ", storing & updating.");
        if(uc.getId() == IGNORE_ID && updateType == UpdateType.FROM_NETWORK)
            throw new IllegalStateException("shouldn't be here!");
        
        // If an http max request is pending, don't even bother with this stuff.
        // We want to get it straight from the source...
        if (updateType == UpdateType.FROM_NETWORK && httpRequestControl.isRequestPending()
                && httpRequestControl.getRequestReason() == HttpRequestControl.RequestReason.MAX)
            return;
        
        _lastId = uc.getId();
        
        _lastTimestamp = uc.getTimestamp();
        UpdateSettings.LAST_UPDATE_TIMESTAMP.setValue(_lastTimestamp);
        
        long delay = UpdateSettings.UPDATE_DOWNLOAD_DELAY.getValue();
        long random = Math.abs(RANDOM.nextLong() % delay);
        _nextDownloadTime = _lastTimestamp + random;
        
        _lastBytes = data;
        
        if(updateType != UpdateType.FROM_DISK) {
            // cancel any http and pretend we just updated.
            if(httpRequestControl.getRequestReason() == HttpRequestControl.RequestReason.TIMEOUT)
                httpRequestControl.cancelRequest();
            UpdateSettings.LAST_HTTP_FAILOVER.setValue(clock.now());
            
            FileUtils.verySafeSave(CommonUtils.getUserSettingsDir(), FILENAME, data);
            capabilitiesVMFactory.updateCapabilities();
            connectionManager.get().sendUpdatedCapabilities();
        }

        Version limeV;
        try {
            limeV = new Version(FrostWireUtils.getFrostWireVersion());
        } catch(VersionFormatException vfe) {
            LOG.warn("Invalid LimeWire version", vfe);
            return;
        }

        Version javaV = null;        
        try {
            javaV = new Version(VersionUtils.getJavaVersion());
        } catch(VersionFormatException vfe) {
            LOG.warn("Invalid java version", vfe);
        }
        
        // don't allow someone to set the style to be above major.
        int style = Math.min(UpdateInformation.STYLE_MAJOR,
                             UpdateSettings.UPDATE_STYLE.getValue());
        
        UpdateData updateInfo = uc.getUpdateDataFor(limeV, 
                    ApplicationSettings.getLanguage(),
                    FrostWireUtils.isPro(),
                    style,
                    javaV);

        List<DownloadInformation> updatesToDownload = uc.getUpdatesWithDownloadInformation();
        _killingObsoleteNecessary = true;
        
        // if we have an update for our machine, prepare the command line
        // and move our update to the front of the list of updates
        if (updateInfo != null && updateInfo.getUpdateURN() != null) {
            prepareUpdateCommand(updateInfo);
            updatesToDownload = new LinkedList<DownloadInformation>(updatesToDownload);
            //updatesToDownload.add(0,updateInfo);
        }

        _updateInfo = updateInfo;
        _updatesToDownload = updatesToDownload;
        
        downloadUpdates(updatesToDownload, null);
        
        if(updateInfo == null) {
            LOG.warn("No relevant update info to notify about.");
            return;
        } else if (updateInfo.getUpdateURN() == null || isHopeless(updateInfo)) {
            if (LOG.isDebugEnabled())
                LOG.debug("we have an update, but it doesn't need a download.  " +
                    "or all our updates are hopeles. Scheduling URL notification...");
            
            updateInfo.setUpdateCommand(null);
            
            backgroundExecutor.schedule(new NotificationFailover(_lastId),
                    delay(clock.now(), uc.getTimestamp()),
                    TimeUnit.MILLISECONDS);
        } else if (isMyUpdateDownloaded(updateInfo)) {
            LOG.debug("there is an update for me, but I happen to have it on disk");
            activityCallback.get().updateAvailable(updateInfo);
        } else
            LOG.debug("we have an update, it needs a download.  Rely on callbacks");
    }

    /**
     * begins an http failover.
     */
    private void checkForStaleUpdateAndMaybeDoHttpFailover(UpdateCollection uc) {
        LOG.debug("checking for timeout http failover");
        long monthAgo = clock.now() - ONE_MONTH;
        if (UpdateSettings.LAST_UPDATE_TIMESTAMP.getValue() < monthAgo && // more than a month ago
                UpdateSettings.LAST_HTTP_FAILOVER.getValue() < monthAgo &&  // and last failover too
                !httpRequestControl.requestQueued(HttpRequestControl.RequestReason.TIMEOUT)) { // and we're not already doing a failover
            
            long when = (connectionServices.isConnected() ? 1 : 5 ) * 60 * 1000;
            if (LOG.isDebugEnabled())
                LOG.debug("scheduling http failover in "+when);
            
            backgroundExecutor.schedule(new Runnable() {
                public void run() {
                    try {
                        launchHTTPUpdate(timeoutUpdateLocation);
                    } catch (URISyntaxException e) {
                        httpRequestControl.requestFinished();
                        httpRequestControl.cancelRequest();
                        LOG.warn(e.toString(), e);
                    }
                }
            }, when, TimeUnit.MILLISECONDS);
        }
    }
    
    private void doHttpMaxFailover(UpdateCollection updateCollection) {
        long maxTimeAgo = clock.now() - silentPeriodForMaxHttpRequest; 
        if(!httpRequestControl.requestQueued(HttpRequestControl.RequestReason.MAX) &&
                UpdateSettings.LAST_HTTP_FAILOVER.getValue() < maxTimeAgo) {
            LOG.debug("Scheduling http max failover...");
            backgroundExecutor.schedule(new Runnable() {
                public void run() {
                    String url = maxedUpdateList.get(RANDOM.nextInt(maxedUpdateList.size()));
                    try {
                        launchHTTPUpdate(url);
                    } catch (URISyntaxException e) {
                        httpRequestControl.requestFinished();
                        httpRequestControl.cancelRequest();
                        LOG.warn(e.toString(), e);
                    }
                }
            }, RANDOM.nextInt(maxMaxHttpRequestDelay) + minMaxHttpRequestDelay, TimeUnit.MILLISECONDS);
        } else {
            LOG.debug("Ignoring http max failover.");
        }
    }

    /**
     * Launches an http update to the failover url.
     */
    private void launchHTTPUpdate(String url) throws URISyntaxException {
        if (!httpRequestControl.isRequestPending())
            return;
        LOG.debug("about to issue http request method");
        HttpGet get = new HttpGet(FrostWireUtils.addLWInfoToUrl(url, applicationServices.getMyGUID()));
        get.addHeader("User-Agent", FrostWireUtils.getHttpServer());
        get.addHeader(HTTPHeaderName.CONNECTION.httpStringValue(),"close");
        httpRequestControl.requestActive();
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, 10000);
        HttpConnectionParams.setSoTimeout(params, 10000);
        params = new DefaultedHttpParams(params, defaultParams.get());
        httpExecutor.get().execute(get, params, new RequestHandler());
    }
    
    /**
     * replaces tokens in the update command with info about the specific system
     * i.e. <PATH> -> C:\Documents And Settings.... 
     */
    private static void prepareUpdateCommand(UpdateData info) {
        if (info == null || info.getUpdateCommand() == null)
            return;
        
        File path = SharingUtils.PREFERENCE_SHARE.getAbsoluteFile();
        String name = info.getUpdateFileName();
        
        try {
            path = FileUtils.getCanonicalFile(path);
        }catch (IOException bad) {}

        String command = info.getUpdateCommand();
        command = StringUtils.replace(command,"$",path.getPath()+File.separator);
        command = StringUtils.replace(command,"%",name);
        info.setUpdateCommand(command);
    }

    /**
     * @return if the given update is considered hopeless
     */
    private static boolean isHopeless(DownloadInformation info) {
        return UpdateSettings.FAILED_UPDATES.contains(
                info.getUpdateURN().httpStringValue());
    }
    
    /**
     * Notification that a given ReplyHandler may have an update we can use.
     */
    private void addSourceIfIdMatches(ReplyHandler rh, int version) {
        if(version == _lastId)
            downloadUpdates(_updatesToDownload, rh);
        else if (LOG.isDebugEnabled())
            LOG.debug("Another version? Me: " + version + ", here: " + _lastId);
    }
    
    /**
     * Tries to download updates.
     * @return whether we had any non-hopeless updates.
     */
    private void downloadUpdates(List<? extends DownloadInformation> toDownload, ReplyHandler source) {
        if (toDownload == null)
            toDownload = Collections.emptyList();
        
        killObsoleteUpdates(toDownload);
        
        for(DownloadInformation next : toDownload) {
            if (isHopeless(next))
                continue; 
            
            if(downloadManager.get().isSavedDownloadsLoaded() && fileManager.get().isLoadFinished()) {
                
                FileDesc shared = fileManager.get().getFileDescForUrn(next.getUpdateURN());
                //TODO: remove the cast
                ManagedDownloader md = (ManagedDownloader)downloadManager.get().getDownloaderForURN(next.getUpdateURN());
                if(LOG.isDebugEnabled())
                    LOG.debug("Looking for: " + next + ", got: " + shared);
                
                if(shared != null && shared.getClass() == FileDesc.class) {
                    // if it's already shared, stop any existing download.
                    if(md != null)
                        md.stop();
                    continue;
                }
                
                // If we don't have an existing download ...
                // and there's no existing InNetwork downloads & 
                // no existing Store downloads & 
                // we're allowed to start a new one.
                if(md == null && !downloadManager.get().hasInNetworkDownload() && canStartDownload()) {
                    LOG.debug("Starting a new InNetwork Download");
                    try {
                        md = (ManagedDownloader)downloadManager.get().download(next, clock.now());
                    } catch(SaveLocationException sle) {
                        LOG.error("Unable to construct download", sle);
                    }
                }
                
                if(md != null) {
                    if(source != null) 
                        md.addDownload(rfd(source, next), false);
                    else
                        addCurrentDownloadSources(md, next);
                }
            }
        }
    }
    
    /**
     * kills all in-network downloaders whose URNs are not listed in the list of updates.
     * Deletes any files in the folder that are not listed in the update message.
     */
    private void killObsoleteUpdates(List<? extends DownloadInformation> toDownload) {
    	if (!downloadManager.get().isSavedDownloadsLoaded() || !fileManager.get().isLoadFinished())
    		return;
    	
        if (_killingObsoleteNecessary) {
            _killingObsoleteNecessary = false;
            downloadManager.get().killDownloadersNotListed(toDownload);
            
            Set<URN> urns = new HashSet<URN>(toDownload.size());
            for(DownloadInformation data : toDownload)
                urns.add(data.getUpdateURN());
            
            List<FileDesc> shared = fileManager.get().getSharedFilesInDirectory(SharingUtils.PREFERENCE_SHARE);
            for (FileDesc fd : shared) {
                if (fd.getSHA1Urn() != null && !urns.contains(fd.getSHA1Urn())) {
                    fileManager.get().removeFileIfShared(fd.getFile());
                    fd.getFile().delete();
                }
            }
        }
    }
    
    /**
     * Adds all current connections that have the right update ID as a source for this download.
     */
    private void addCurrentDownloadSources(ManagedDownloader md, DownloadInformation info) {
        for(RoutedConnection mc : connectionManager.get().getConnections()) {
            if(mc.getConnectionCapabilities().getRemoteHostUpdateVersion() == _lastId) {
                LOG.debug("Adding source: " + mc);
                md.addDownload(rfd(mc, info), false);
            } else
                LOG.debug("Not adding source because bad id: " + mc.getConnectionCapabilities().getRemoteHostUpdateVersion() + ", us: " + _lastId);
        }
    }
    
    /**
     * Constructs an RFD out of the given information & connection.
     */
    private RemoteFileDesc rfd(ReplyHandler rh, DownloadInformation info) {
        Set<URN> urns = new UrnSet(info.getUpdateURN());
        return remoteFileDescFactory.createRemoteFileDesc(rh.getAddress(), rh.getPort(), Integer.MAX_VALUE,
                info.getUpdateFileName(), (int)info.getSize(), rh.getClientGUID(), 0, false, 2, false, null, urns, false,
                false, "LIME", IpPort.EMPTY_SET, 0, 0, rh instanceof Connectable ? 
                      ((Connectable)rh).isTLSCapable() : false);  // tls capability
    }
    
    /**
     * Determines if we're far enough past the timestamp to start a new
     * in network download.
     */
    private boolean canStartDownload() {
        long now = clock.now();
        
        if (LOG.isDebugEnabled())
            LOG.debug("now is "+now+ " next time is "+_nextDownloadTime);
        
        return now > _nextDownloadTime;
    }
    
    /**
     * Determines if we should notify about there being new information.
     */
    private void notifyAboutInfo(int id) {
        if (id != _lastId)
            return;
        
        UpdateInformation update = _updateInfo;
        assert(update != null);
        
        activityCallback.get().updateAvailable(update);
    }
    
    /**
     * @return calculates a random delay after the timestamp, unless the timestamp
     * is more than 3 days in the future.
     */
    private static long delay(long now, long timestamp) {
        if (timestamp - now > THREE_DAYS)
            return 0;
        
        long delay = UpdateSettings.UPDATE_DELAY.getValue();
        long random = Math.abs(new Random().nextLong() % delay);
        long then = timestamp + random;
        
        if(LOG.isInfoEnabled()) {
            LOG.info("Delaying Update." +
                     "\nNow    : " + now + 
                     "\nStamp  : " + timestamp +
                     "\nDelay  : " + delay + 
                     "\nRandom : " + random + 
                     "\nThen   : " + then +
                     "\nDiff   : " + (then-now));
        }

        return Math.max(0,then - now);
    }
    
    /**
     * Notifies this that an update with the given URN has finished downloading.
     * 
     * If this was our update, we notify the gui.  Its ok if the user restarts
     * as the rest of the updates will be downloaded the next session.
     */
    public void inNetworkDownloadFinished(final URN urn, final boolean good) {
        
        Runnable r = new Runnable() {
            public void run() {
                
                // add it to the list of failed urns
                if (!good)
                    UpdateSettings.FAILED_UPDATES.add(urn.httpStringValue());
                
                UpdateData updateInfo = (UpdateData) _updateInfo;
                if (updateInfo != null && 
                        updateInfo.getUpdateURN() != null &&
                        updateInfo.getUpdateURN().equals(urn)) {
                    if (!good) {
                        // register a notification to the user later on.
                        updateInfo.setUpdateCommand(null);
                        long delay = delay(clock.now(),_lastTimestamp);
                        backgroundExecutor.schedule(new NotificationFailover(_lastId),delay,TimeUnit.MILLISECONDS);
                    } else {
                        activityCallback.get().updateAvailable(updateInfo);
                        connectionManager.get().sendUpdatedCapabilities();
                    }
                }
            }
        };
        
        backgroundExecutor.execute(r);
    }
    
    /**
     * @return whether we killed any hopeless update downloads
     */
    private void killHopelessUpdates(List<? extends DownloadInformation> updates) {
        if (updates == null)
            return;
        
        if (!downloadManager.get().hasInNetworkDownload())
            return;
        
        long now = clock.now();
        for(DownloadInformation info : updates) {
            Downloader downloader = downloadManager.get().getDownloaderForURN(info.getUpdateURN());
            if (downloader != null && downloader instanceof InNetworkDownloader) {
                InNetworkDownloader iDownloader = (InNetworkDownloader)downloader;
                if (isHopeless(iDownloader, now))  
                    iDownloader.stop();
            }
        }
    }
    
    /**
     * @param now what time is it now
     * @return whether the in-network downloader is considered hopeless
     */
    private boolean isHopeless(InNetworkDownloader downloader, long now) {
        if (now - downloader.getStartTime() < 
                UpdateSettings.UPDATE_GIVEUP_FACTOR.getValue() * 
                UpdateSettings.UPDATE_DOWNLOAD_DELAY.getValue())
            return false;
        
        if (downloader.getDownloadAttempts() < UpdateSettings.UPDATE_MIN_ATTEMPTS.getValue())
            return false;
        
        return true;
    }

    /**
     * @return true if the update for our specific machine is downloaded or
     * there was nothing to download
     */
    private boolean isMyUpdateDownloaded(UpdateInformation myInfo) {
        if (!fileManager.get().isLoadFinished())
            return false;
        
        URN myUrn = myInfo.getUpdateURN();
        if (myUrn == null)
            return true;
        
        FileDesc desc = fileManager.get().getFileDescForUrn(myUrn);
        
        if (desc == null)
            return false;
        return desc.getClass() == FileDesc.class;
    }
    
    /**
     * Simple accessor for the stored file.
     */
    private File getStoredFile() {
        return new File(CommonUtils.getUserSettingsDir(), FILENAME);
    }
    
    /**
     * a functor that repeatedly tries to download updates at a variable
     * interval. 
     */
    private class Poller implements Runnable {
        public void run() {
            downloadUpdates(_updatesToDownload, null);
            killHopelessUpdates(_updatesToDownload);
        }
    }
    
    private class NotificationFailover implements Runnable {
        private final int id;
        private boolean shown;
        
        NotificationFailover(int id) {
            this.id = id;
        }
        
        public void run() {
            if (shown)
                return;
            
            shown = true;
            notifyAboutInfo(id);
        }
    }

    private class RequestHandler implements HttpClientListener {

        public boolean requestComplete(HttpUriRequest request, HttpResponse response) {
            LOG.debug("http request method succeeded");
            
            // remember we made an attempt even if it didn't succeed
            UpdateSettings.LAST_HTTP_FAILOVER.setValue(clock.now());
            final byte[] inflated;
            try {
                if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                    throw new IOException("bad code "+response.getStatusLine().getStatusCode());
    
                byte [] resp = null;
                if(response.getEntity() != null) {
                    resp = IOUtils.readFully(response.getEntity().getContent());
                }
                if (resp == null || resp.length == 0)
                    throw new IOException("bad body");
    
                // inflate the response and process.
                inflated = updateMessageVerifier.inflateNetworkData(resp);
            } catch (IOException failed) {
                httpRequestControl.requestFinished();
                LOG.warn("couldn't fetch data ",failed);
                return false;
            } finally {
                httpExecutor.get().releaseResources(response);
            }
            
            // Handle the data in the background thread.
            backgroundExecutor.execute(new Runnable() {
                public void run() {
                    httpRequestControl.requestFinished();
                    
                    LOG.trace("Parsing new data...");
                    handleDataInternal(inflated, UpdateType.FROM_HTTP, null);
                }
            });
            
            return false; // no more requests
        }
        
        public boolean requestFailed(HttpUriRequest request, HttpResponse response, IOException exc) {
            LOG.warn("http failover failed",exc);
            httpRequestControl.requestFinished();
            UpdateSettings.LAST_HTTP_FAILOVER.setValue(clock.now());
            
            httpExecutor.get().releaseResources(response);
            // nothing we can do.
            return false;
        }
    }
    
    /**
     * A simple control to let the flow of HTTP requests happen differently
     * depending on why it was requested.
     */
    private static class HttpRequestControl {
        private static enum RequestReason { TIMEOUT, MAX };
        
        private final AtomicBoolean requestQueued = new AtomicBoolean(false);
        private final AtomicBoolean requestActive = new AtomicBoolean(false);
        private volatile RequestReason requestReason;
        
        /** Returns true if a request is queued or active. */
        boolean isRequestPending() {
            return requestActive.get() || requestQueued.get();
        }
        
        /** Sets a queued request and returns true if a request is pending or active. */
        boolean requestQueued(RequestReason reason) {
            boolean prior = requestQueued.getAndSet(true);
            if(!prior || reason == RequestReason.MAX) // upgrade reason
                requestReason = reason;
            return prior || requestActive.get();
        }
        
        /** Sets a request to be active. */
        void requestActive() {
            requestActive.set(true);
            requestQueued.set(false);
        }
        
        /** Returns the reason the last request was queueud. */
        RequestReason getRequestReason() {
            return requestReason;
        }
        
        void cancelRequest() {
            requestQueued.set(false);
        }
        
        void requestFinished() {
            requestActive.set(false);
        }
    }
    
}
