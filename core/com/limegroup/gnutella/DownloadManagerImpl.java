package com.limegroup.gnutella;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.DualIterator;
import org.limewire.collection.MultiIterable;
import org.limewire.i18n.I18nMarker;
import org.limewire.io.InvalidDataException;
import org.limewire.io.IpPort;
import org.limewire.service.MessageService;
import org.limewire.util.FileUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.bittorrent.BTMetaInfo;
import com.limegroup.bittorrent.TorrentFileSystem;
import com.limegroup.bittorrent.TorrentManager;
import com.limegroup.bittorrent.settings.BittorrentSettings;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.downloader.CantResumeException;
import com.limegroup.gnutella.downloader.CoreDownloader;
import com.limegroup.gnutella.downloader.CoreDownloaderFactory;
import com.limegroup.gnutella.downloader.DownloaderType;
import com.limegroup.gnutella.downloader.InNetworkDownloader;
import com.limegroup.gnutella.downloader.IncompleteFileManager;
import com.limegroup.gnutella.downloader.MagnetDownloader;
import com.limegroup.gnutella.downloader.ManagedDownloader;
import com.limegroup.gnutella.downloader.PushDownloadManager;
import com.limegroup.gnutella.downloader.PushedSocketHandlerRegistry;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.downloader.ResumeDownloader;
import com.limegroup.gnutella.downloader.serial.BTMetaInfoMemento;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;
import com.limegroup.gnutella.downloader.serial.DownloadSerializer;
import com.limegroup.gnutella.library.SharingUtils;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.settings.DownloadSettings;
import com.limegroup.gnutella.settings.UpdateSettings;
import com.limegroup.gnutella.version.DownloadInformation;

@Singleton
public class DownloadManagerImpl implements DownloadManager {
    
    private static final Log LOG = LogFactory.getLog(DownloadManagerImpl.class);
    
    /** The time in milliseconds between checkpointing downloads.dat.  The more
     * often this is written, the less the lost data during a crash, but the
     * greater the chance that downloads.dat itself is corrupt.  */
    private int SNAPSHOT_CHECKPOINT_TIME=30*1000; //30 seconds


    /** The list of all ManagedDownloader's attempting to download.
     *  INVARIANT: active.size()<=slots() && active contains no duplicates 
     *  LOCKING: obtain this' monitor */
    private final List <CoreDownloader> active=new LinkedList<CoreDownloader>();
    /** The list of all queued ManagedDownloader. 
     *  INVARIANT: waiting contains no duplicates 
     *  LOCKING: obtain this' monitor */
    
    private final List <CoreDownloader> waiting=new LinkedList<CoreDownloader>();
    
    private final MultiIterable<CoreDownloader> activeAndWaiting = 
        new MultiIterable<CoreDownloader>(active,waiting); 
    
    /**
     * Whether or not the GUI has been init'd.
     */
    private volatile boolean downloadsReadFromDisk = false;
    
    /** The number if IN-NETWORK active downloaders.  We don't count these when
     * determing how many downloaders are active.
     */
    private int innetworkCount = 0;

    /**
     * The number of times we've been bandwidth measures
     */
    private int numMeasures = 0;
    
    /**
     * The average bandwidth over all downloads.
     * This is only counted while downloads are active.
     */
    private float averageBandwidth = 0;
    
    /** The last measured bandwidth, as counted from measureBandwidth. */
    private volatile float lastMeasuredBandwidth;
    
    /**
     * The runnable that pumps inactive downloads to the correct state.
     */
    private Runnable _waitingPump;
    
    private final NetworkManager networkManager;
    private final DownloadCallback innetworkCallback;
    private final Provider<DownloadCallback> downloadCallback;
    private final Provider<MessageRouter> messageRouter;
    private final ScheduledExecutorService backgroundExecutor;
    private final Provider<TorrentManager> torrentManager;
    private final Provider<PushDownloadManager> pushDownloadManager;
    private final CoreDownloaderFactory coreDownloaderFactory;
    private final DownloadSerializer downloadSerializer;
    private final IncompleteFileManager incompleteFileManager;
    private final RemoteFileDescFactory remoteFileDescFactory;
    
    @Inject
    public DownloadManagerImpl(NetworkManager networkManager,
            @Named("inNetwork") DownloadCallback innetworkCallback,
            Provider<DownloadCallback> downloadCallback,
            Provider<MessageRouter> messageRouter,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            Provider<TorrentManager> torrentManager,
            Provider<PushDownloadManager> pushDownloadManager,
            CoreDownloaderFactory coreDownloaderFactory,
            DownloadSerializer downloaderSerializer,
            IncompleteFileManager incompleteFileManager,
            RemoteFileDescFactory remoteFileDescFactory) {
        this.networkManager = networkManager;
        this.innetworkCallback = innetworkCallback;
        this.downloadCallback = downloadCallback;
        this.messageRouter = messageRouter;
        this.backgroundExecutor = backgroundExecutor;
        this.torrentManager = torrentManager;
        this.pushDownloadManager = pushDownloadManager;
        this.coreDownloaderFactory = coreDownloaderFactory;
        this.downloadSerializer = downloaderSerializer;
        this.incompleteFileManager = incompleteFileManager;
        this.remoteFileDescFactory = remoteFileDescFactory;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#register(com.limegroup.gnutella.downloader.PushedSocketHandlerRegistry)
     */
    // DO NOT REMOVE!  Guice calls this because of the @Inject
    @Inject
    public void register(PushedSocketHandlerRegistry registry) {
        registry.register(this);
    }

    //////////////////////// Creation and Saving /////////////////////////

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#initialize()
     */
    public void initialize() {
        scheduleWaitingPump();
    }
    
    /**
     * Adds a new downloader to this manager.
     * @param downloader
     */
    public void addNewDownloader(CoreDownloader downloader) {
        synchronized(this) {
            waiting.add(downloader);
            downloader.initialize();
            callback(downloader).addDownload(downloader);
        }
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#postGuiInit()
     */
    public void loadSavedDownloadsAndScheduleWriting() {
        loadSavedDownloads();
        scheduleSnapshots();
    }
    
    public void loadSavedDownloads() {
        boolean failedAll = true;
        boolean failedSome = false;
        
        List<DownloadMemento> mementos;
        try {
            mementos = downloadSerializer.readFromDisk();
            if(mementos.isEmpty())
                failedAll = false;
        } catch(IOException ioex) {
            mementos = Collections.emptyList();
        }
        for(DownloadMemento memento : mementos) {
            CoreDownloader coreDownloader = prepareMemento(memento);
            if(coreDownloader != null) {
                failedAll = false;
                addNewDownloader(coreDownloader);
            } else {
                failedSome = true;
            }
        }
        
        downloadsReadFromDisk = true;
        
        if(failedAll)
            MessageService.showError(I18nMarker.marktr("Sorry, FrostWire couldn't read your old downloads.  You can restart them by going to your Library, viewing your 'Incomplete Files', and clicking to 'Resume' your downloads."));
        else if(failedSome)
            MessageService.showError(I18nMarker.marktr("Sorry, FrostWire couldn't read some of your old downloads.  You can restart them by going to your Library, viewing your 'Incomplete Files', and clicking to 'Resume' your downloads."));
    }
    
    public CoreDownloader prepareMemento(DownloadMemento memento) {
        try {
            return coreDownloaderFactory.createFromMemento(memento);
        } catch(InvalidDataException ide) {
            LOG.warn("Unable to read download from memento: " + memento, ide);
            return null;
        }
    }
    
    public void scheduleSnapshots() {
        Runnable checkpointer=new Runnable() {
            public void run() {
                if (downloadsInProgress() > 0) { //optimization
                    writeSnapshot();
                }
            }
        };
        backgroundExecutor.scheduleWithFixedDelay(checkpointer, 
                               SNAPSHOT_CHECKPOINT_TIME, 
                               SNAPSHOT_CHECKPOINT_TIME, TimeUnit.MILLISECONDS);   
    }      
    
    public void writeSnapshot() {
        List<DownloadMemento> mementos;
        synchronized(this) {
            mementos = new ArrayList<DownloadMemento>(active.size() + waiting.size());
            for(CoreDownloader downloader : activeAndWaiting) {
                mementos.add(downloader.toMemento());
            }
        }
        
        downloadSerializer.writeToDisk(mementos);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#isGUIInitd()
     */
    public boolean isSavedDownloadsLoaded() {
        return downloadsReadFromDisk;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#hasInNetworkDownload()
     */
    public synchronized boolean hasInNetworkDownload() {
        if(innetworkCount > 0)
            return true;
        for(Iterator<CoreDownloader> i = waiting.iterator(); i.hasNext(); ) {
            if(i.next().getDownloadType() == DownloaderType.INNETWORK)
                return true;
        }
        return false;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#killDownloadersNotListed(java.util.Collection)
     */
    public synchronized void killDownloadersNotListed(Collection<? extends DownloadInformation> updates) {
        if (updates == null)
            return;
        
        Set<String> urns = new HashSet<String>(updates.size());
        for(DownloadInformation ui : updates)
            urns.add(ui.getUpdateURN().httpStringValue());
        
        for (Iterator<CoreDownloader> iter = new DualIterator<CoreDownloader>(waiting.iterator(),active.iterator());
        iter.hasNext();) {
            CoreDownloader d = iter.next();
            if (d.getDownloadType() == DownloaderType.INNETWORK  && 
                    !urns.contains(d.getSha1Urn().httpStringValue())) 
                d.stop();
        }
        
        Set<String> hopeless = UpdateSettings.FAILED_UPDATES.getValue();
        hopeless.retainAll(urns);
        UpdateSettings.FAILED_UPDATES.setValue(hopeless);
    }
    
    PushDownloadManager getPushManager() {
        return pushDownloadManager.get();
    }

    /**
     * Delegates the incoming socket out to BrowseHostHandler & then attempts to assign it
     * to any ManagedDownloader.
     * 
     * Closes the socket if neither BrowseHostHandler nor any ManagedDownloaders wanted it.
     * 
     * @param file
     * @param index
     * @param clientGUID
     * @param socket
     */
    private synchronized boolean handleIncomingPush(String file, int index, byte [] clientGUID, Socket socket) {
         boolean handled = false;
         for (CoreDownloader md : activeAndWaiting) {
            if (! (md instanceof ManagedDownloader))
                continue; // pushes apply to gnutella downloads only
            ManagedDownloader mmd = (ManagedDownloader)md;
            if (mmd.acceptDownload(file, socket, index, clientGUID))
                handled = true;
         }                 
         return handled;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#acceptPushedSocket(java.lang.String, int, byte[], java.net.Socket)
     */
    public boolean acceptPushedSocket(String file, int index,
            byte[] clientGUID, Socket socket) {
        return handleIncomingPush(file, index, clientGUID, socket);
    }
    
    
    public void scheduleWaitingPump() {
        if(_waitingPump != null)
            return;
            
        _waitingPump = new Runnable() {
            public void run() {
                pumpDownloads();
            }
        };
        backgroundExecutor.scheduleWithFixedDelay(_waitingPump,
                               1000,
                               1000, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Pumps through each waiting download, either removing it because it was
     * stopped, or adding it because there's an active slot and it requires
     * attention.
     */
    protected synchronized void pumpDownloads() {
        int index = 1;
        for(Iterator<CoreDownloader> i = waiting.iterator(); i.hasNext(); ) {
            CoreDownloader md = i.next();
            
            if (md.getDownloadType() == DownloaderType.BTDOWNLOADER && !allowNewTorrents())
            	continue;
            
            if(md.isAlive()) {
                continue;
            } else if(md.shouldBeRemoved()) {
                i.remove();
                cleanupCompletedDownload(md, false);
            }
            else if(hasFreeSlot() && (md.shouldBeRestarted())) {
                i.remove();
                if(md.getDownloadType() == DownloaderType.INNETWORK)
                    innetworkCount++;
                active.add(md);
                md.startDownload();
            } else {
                if(md.isQueuable())
                    md.setInactivePriority(index++);
                md.handleInactivity();
            }
        }
    }
    
    public boolean allowNewTorrents() {
    	int activeCount = 0;
    	for (CoreDownloader downloader : active) {
    		if (downloader.getDownloadType() == DownloaderType.BTDOWNLOADER) {
   				activeCount++;
    		}
    	}
    	
    	return activeCount < BittorrentSettings.TORRENT_MAX_ACTIVE_DOWNLOADS.getValue();
    	
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#isIncomplete(com.limegroup.gnutella.URN)
     */
    public boolean isIncomplete(URN urn) {
        return incompleteFileManager.getFileForUrn(urn) != null;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#isActivelyDownloading(com.limegroup.gnutella.URN)
     */
    public boolean isActivelyDownloading(URN urn) {
        Downloader md = getDownloaderForURN(urn);
        
        if(md == null)
            return false;
            
        switch(md.getState()) {
        case QUEUED:
        case BUSY:
        case ABORTED:
        case GAVE_UP:
        case DISK_PROBLEM:
        case CORRUPT_FILE:
        case REMOTE_QUEUED:
        case WAITING_FOR_USER:
            return false;
        default:
            return true;
        }
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#getIncompleteFileManager()
     */
    public IncompleteFileManager getIncompleteFileManager() {
        return incompleteFileManager;
    }    
 
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#downloadsInProgress()
     */
    public synchronized int downloadsInProgress() {
        return active.size() + waiting.size();
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#getNumIndividualDownloaders()
     */
    public synchronized int getNumIndividualDownloaders() {
        int ret = 0;
        for (Iterator<CoreDownloader> iter=active.iterator(); iter.hasNext(); ) {  //active
            Object next = iter.next();
            if (! (next instanceof ManagedDownloader))
                continue; // TODO: count torrents separately
            ManagedDownloader md=(ManagedDownloader)next;
            ret += md.getNumDownloaders();
       }
       return ret;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#getNumActiveDownloads()
     */
    public synchronized int getNumActiveDownloads() {
        return active.size() - innetworkCount;
    }
   
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#getNum<Downloads()
     */
    public synchronized int getNumWaitingDownloads() {
        return waiting.size();
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#getDownloaderForURN(com.limegroup.gnutella.URN)
     */
    public synchronized Downloader getDownloaderForURN(URN sha1) {
        for (CoreDownloader md : activeAndWaiting) {
            if (md.getSha1Urn() != null && sha1.equals(md.getSha1Urn()))
                return md;
        }
        return null;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#getDownloaderForURNString(java.lang.String)
     */
    public synchronized Downloader getDownloaderForURNString(String urn) {
        for (CoreDownloader md : activeAndWaiting) {
            if (md.getSha1Urn() != null && urn.equals(md.getSha1Urn().toString()))
                return md;
        }
        return null;
    }    
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#getDownloaderForIncompleteFile(java.io.File)
     */
    public synchronized Downloader getDownloaderForIncompleteFile(File file) {
        for (CoreDownloader dl : activeAndWaiting) {
            if (dl.conflictsWithIncompleteFile(file)) {
                return dl;
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#isGuidForQueryDownloading(com.limegroup.gnutella.GUID)
     */
    public synchronized boolean isGuidForQueryDownloading(GUID guid) {
        for (CoreDownloader md : activeAndWaiting) {
            GUID dGUID = md.getQueryGUID();
            if ((dGUID != null) && (dGUID.equals(guid)))
                return true;
        }
        return false;
    }
    
    void clearAllDownloads() {
        List<Downloader> buf;
        synchronized(this) {
            buf = new ArrayList<Downloader>(active.size() + waiting.size());
            buf.addAll(active);
            buf.addAll(waiting);
            active.clear();
            waiting.clear();
        }
        for(Downloader md : buf ) 
            md.stop();
    }
           
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#download(com.limegroup.gnutella.RemoteFileDesc[], java.util.List, com.limegroup.gnutella.GUID, boolean, java.io.File, java.lang.String)
     */
    public synchronized Downloader download(RemoteFileDesc[] files,
                                            List<? extends RemoteFileDesc> alts, GUID queryGUID, 
                                            boolean overwrite, File saveDir,
                                            String fileName) 
        throws SaveLocationException {

        String fName = getFileName(files, fileName);
        if (conflicts(files, new File(saveDir,fName))) {
            throw new SaveLocationException
            (SaveLocationException.FILE_ALREADY_DOWNLOADING,
                    new File(fName != null ? fName : ""));
        }

        //Purge entries from incompleteFileManager that have no corresponding
        //file on disk.  This protects against stupid users who delete their
        //temporary files while FrostWire is running, either through the command
        //prompt or the library.  Note that you could optimize this by just
        //purging files corresponding to the current download, but it's not
        //worth it.
        incompleteFileManager.purge();

        //Start download asynchronously.  This automatically moves downloader to
        //active if it can.
        ManagedDownloader downloader =
            coreDownloaderFactory.createManagedDownloader(files, 
                queryGUID, saveDir, fileName, overwrite);

        initializeDownload(downloader);
        
        //Now that the download is started, add the sources w/o caching
        downloader.addDownload(alts,false);
        
        return downloader;
    }   
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#download(com.limegroup.gnutella.browser.MagnetOptions, boolean, java.io.File, java.lang.String)
     */
    public synchronized Downloader download(MagnetOptions magnet,
            boolean overwrite,
            File saveDir,
            String fileName)
    throws IllegalArgumentException, SaveLocationException {
        
        if (!magnet.isDownloadable()) 
            throw new IllegalArgumentException("magnet not downloadable");
        
        //remove entry from IFM if the incomplete file was deleted.
        incompleteFileManager.purge();
        
        if (fileName == null) {
            fileName = magnet.getFileNameForSaving();
        }
        if (conflicts(magnet.getSHA1Urn(), 0, new File(saveDir,fileName))) {
            throw new SaveLocationException
            (SaveLocationException.FILE_ALREADY_DOWNLOADING, new File(fileName));
        }

        //Note: If the filename exists, it would be nice to check that we are
        //not already downloading the file by calling conflicts with the
        //filename...the problem is we cannot do this effectively without the
        //size of the file (atleast, not without being risky in assuming that
        //two files with the same name are the same file). So for now we will
        //just leave it and download the same file twice.

        //Instantiate downloader, validating incompleteFile first.
        MagnetDownloader downloader = 
            coreDownloaderFactory.createMagnetDownloader( magnet,
                overwrite, saveDir, fileName);
        initializeDownload(downloader);
        return downloader;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#download(java.io.File)
     */ 
    public synchronized Downloader download(File incompleteFile)
            throws CantResumeException, SaveLocationException { 
     
        if (conflictsWithIncompleteFile(incompleteFile)) {
            throw new SaveLocationException
            (SaveLocationException.FILE_ALREADY_DOWNLOADING, incompleteFile);
        }
        
        if (IncompleteFileManager.isTorrentFolder(incompleteFile)) 
            return resumeTorrentDownload(incompleteFile);

        //Check if file exists.  TODO3: ideally we'd pass ALL conflicting files
        //to the GUI, so they know what they're overwriting.
        //if (! overwrite) {
        //    try {
        //        File downloadDir=SettingsManager.instance().getSaveDirectory();
        //        File completeFile=new File(
        //            downloadDir, 
        //            incompleteFileManager.getCompletedName(incompleteFile));
        //        if (completeFile.exists())
        //            throw new FileExistsException(filename);
        //    } catch (IllegalArgumentException e) {
        //        throw new CantResumeException(incompleteFile.getName());
        //    }
        //}

        //Purge entries from incompleteFileManager that have no corresponding
        //file on disk.  This protects against stupid users who delete their
        //temporary files while FrostWire is running, either through the command
        //prompt or the library.  Note that you could optimize this by just
        //purging files corresponding to the current download, but it's not
        //worth it.
        incompleteFileManager.purge();

        //Instantiate downloader, validating incompleteFile first.
        ResumeDownloader downloader=null;
        try {
            incompleteFile = FileUtils.getCanonicalFile(incompleteFile);
            String name=IncompleteFileManager.getCompletedName(incompleteFile);
            long size= IncompleteFileManager.getCompletedSize(incompleteFile);
            downloader = coreDownloaderFactory.createResumeDownloader(
                                              incompleteFile,
                                              name,
                                              size);
        } catch (IllegalArgumentException e) {
            throw new CantResumeException(incompleteFile.getName());
        } catch (IOException ioe) {
            throw new CantResumeException(incompleteFile.getName());
        }
        
        initializeDownload(downloader);
        return downloader;
    }
    
    private Downloader resumeTorrentDownload(File torrentFolder) 
    throws CantResumeException, SaveLocationException {
        File infohash = null; 
        for (File f : torrentFolder.listFiles()){
            if (f.getName().startsWith(".dat")) {
                infohash = f;
                break;
            }
        }
        
        String name = IncompleteFileManager.getCompletedName(torrentFolder);
        if(infohash == null)
            throw new CantResumeException(name);
        
        BTMetaInfoMemento memento = null;
        try {
            Object infoObj = FileUtils.readObject(infohash.getAbsolutePath());
            memento = (BTMetaInfoMemento)infoObj;
        } catch (Throwable bad) {
            throw new CantResumeException(name);
        }
        
        BTMetaInfo info;
        try {
            info = new BTMetaInfo(memento);
        } catch(InvalidDataException ide) {
            throw new CantResumeException(name);
        }
        
        Downloader ret = downloadTorrent(info, false);
        if (ret.isResumable())
            ret.resume();
        return ret;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#download(com.limegroup.gnutella.version.DownloadInformation, long)
     */
    public synchronized Downloader download(DownloadInformation info, long now) 
    throws SaveLocationException {
        File dir = SharingUtils.PREFERENCE_SHARE;
        dir.mkdirs();
        File f = new File(dir, info.getUpdateFileName());
        if(conflicts(info.getUpdateURN(), (int)info.getSize(), f))
            throw new SaveLocationException(SaveLocationException.FILE_ALREADY_DOWNLOADING, f);
        
        incompleteFileManager.purge();
        ManagedDownloader d = coreDownloaderFactory.createInNetworkDownloader(
                info, dir, now);
        initializeDownload(d);
        return d;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#downloadTorrent(com.limegroup.bittorrent.BTMetaInfo, boolean)
     */
    public synchronized Downloader downloadTorrent(BTMetaInfo info, boolean overwrite) 
    throws SaveLocationException {
        TorrentFileSystem system = info.getFileSystem();
        checkActiveAndWaiting(info.getURN(), system);
        if (!overwrite)
            checkTargetLocation(system, overwrite);
        else
            torrentManager.get().killTorrentForFile(system.getCompleteFile());
        CoreDownloader ret = coreDownloaderFactory.createBTDownloader(info);
        ret.setOverwrite(overwrite);
        initializeDownload(ret);
        return ret;
    }
    
    private void checkTargetLocation(TorrentFileSystem info, boolean overwrite) 
    throws SaveLocationException{
        for (File f : info.getFilesAndFolders()) {
            if (f.exists())
                throw new SaveLocationException
                (SaveLocationException.FILE_ALREADY_EXISTS, f);
        }
    }
    
    private void checkActiveAndWaiting(URN urn, TorrentFileSystem system) 
    throws SaveLocationException {
        for (CoreDownloader current : activeAndWaiting) {
            if (urn.equals(current.getSha1Urn())) {
                // this is the place to add new trackers eventually.
                throw new SaveLocationException
                (SaveLocationException.FILE_ALREADY_DOWNLOADING, system.getCompleteFile());
            }
            for (File f : system.getFilesAndFolders()) {
                if (current.conflictsSaveFile(f)) {
                    throw new SaveLocationException
                    (SaveLocationException.FILE_IS_ALREADY_DOWNLOADED_TO, f);
                }
            }
        }
    }
    
    /**
     * Performs common tasks for initializing the download.
     * 1) Initializes the downloader.
     * 2) Adds the download to the waiting list.
     * 3) Notifies the callback about the new downloader.
     * 4) Writes the new snapshot out to disk.
     */
    private void initializeDownload(CoreDownloader md) {
        md.initialize();
        waiting.add(md);
        callback(md).addDownload(md);
        backgroundExecutor.execute(new Runnable() {
            public void run() {
                writeSnapshot(); // Save state for crash recovery.
            }
        });
    }
    
    /**
     * Returns the callback that should be used for the given md.
     */
    private DownloadCallback callback(Downloader md) {
        return (md instanceof InNetworkDownloader) ? innetworkCallback : downloadCallback.get();
    }
        
    /**
     * Returns true if there already exists a download for the same file.
     * <p>
     * Same file means: same urn, or as fallback same filename + same filesize
     * @param rfds
     * @return
     */
    private boolean conflicts(RemoteFileDesc[] rfds, File... fileName) {
        URN urn = null;
        for (int i = 0; i < rfds.length && urn == null; i++) {
            urn = rfds[0].getSHA1Urn();
        }
        
        return conflicts(urn, rfds[0].getSize(), fileName);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#conflicts(com.limegroup.gnutella.URN, long, java.io.File)
     */
    public boolean conflicts(URN urn, long fileSize, File... fileName) {
        
        if (urn == null && fileSize == 0) {
            return false;
        }
        
        synchronized (this) {
            for (CoreDownloader md : activeAndWaiting) {
                if (md.conflicts(urn, fileSize, fileName)) 
                    return true;
            }
            return false;
        }
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#isSaveLocationTaken(java.io.File)
     */
    public synchronized boolean isSaveLocationTaken(File candidateFile) {
        for (CoreDownloader md : activeAndWaiting) {
            if (md.conflictsSaveFile(candidateFile)) 
                return true;
        }
        return false;
    }

    private synchronized boolean conflictsWithIncompleteFile(File incompleteFile) {
        for (CoreDownloader md : activeAndWaiting) {
            if (md.conflictsWithIncompleteFile(incompleteFile))
                return true;
        }
        return false;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#handleQueryReply(com.limegroup.gnutella.messages.QueryReply)
     */
    public void handleQueryReply(QueryReply qr) {
        // first check if the qr is of 'sufficient quality', if not just
        // short-circuit.
        if (qr.calculateQualityOfService(
                !networkManager.acceptedIncomingConnection(), networkManager) < 1)
            return;

        List<Response> responses;
        HostData data;
        try {
            responses = qr.getResultsAsList();
            data = qr.getHostData();
        } catch(BadPacketException bpe) {
            return; // bad packet, do nothing.
        }
        
        addDownloadWithResponses(responses, data);
    }

    /**
     * Iterates through all responses seeing if they can be matched
     * up to any existing downloaders, adding them as possible
     * sources if they do.
     */
    private void addDownloadWithResponses(List<? extends Response> responses, HostData data) {
        if(responses == null)
            throw new NullPointerException("null responses");
        if(data == null)
            throw new NullPointerException("null hostdata");

        // need to synch because active and waiting are not thread safe
        List<CoreDownloader> downloaders = new ArrayList<CoreDownloader>(active.size() + waiting.size());
        synchronized (this) { 
            // add to all downloaders, even if they are waiting....
            downloaders.addAll(active);
            downloaders.addAll(waiting);
        }
        
        // short-circuit.
        if(downloaders.isEmpty())
            return;

        //For each response i, offer it to each downloader j.  Give a response
        // to at most one downloader.
        // TODO: it's possible that downloader x could accept response[i] but
        //that would cause a conflict with downloader y.  Check for this.
        for(Response r : responses) {
            // Don't bother with making XML from the EQHD.
            RemoteFileDesc rfd = r.toRemoteFileDesc(data, remoteFileDescFactory);
            for(Downloader current : downloaders) {
                if ( !(current instanceof ManagedDownloader))
                    continue; // can't add sources to torrents yet
                ManagedDownloader currD = (ManagedDownloader) current;
                // If we were able to add this specific rfd,
                // add any alternates that this response might have
                // also.
                if (currD.addDownload(rfd, true)) {
                    for(IpPort ipp : r.getLocations()) {
                        // don't cache alts.
                        currD.addDownload(remoteFileDescFactory.createRemoteFileDesc(rfd, ipp), false);
                    }
                    break;
                }
            }
        }
    }

    // //////////// Callback Methods for ManagedDownloaders ///////////////////

    /** @requires this monitor' held by caller */
    private boolean hasFreeSlot() {
        return active.size() - innetworkCount
            < DownloadSettings.MAX_SIM_DOWNLOAD.getValue();
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#remove(com.limegroup.gnutella.downloader.CoreDownloader, boolean)
     */
    public synchronized void remove(CoreDownloader downloader, 
                                    boolean completed) {
        boolean isRemoved = active.remove(downloader);
        if(downloader.getDownloadType() == DownloaderType.INNETWORK)
            innetworkCount--;
        
        waiting.remove(downloader);
        if(completed)
            cleanupCompletedDownload(downloader, true);
        else
            waiting.add(downloader);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#bumpPriority(com.limegroup.gnutella.Downloader, boolean, int)
     */
    public synchronized void bumpPriority(Downloader downl,
                                          boolean up, int amt) {
        CoreDownloader downloader = (CoreDownloader)downl;
        int idx = waiting.indexOf(downloader);
        if(idx == -1)
            return;

        if(up && idx != 0) {
            waiting.remove(idx);
            if (amt > idx)
                amt = idx;
            if (amt != 0)
                waiting.add(idx - amt, downloader);
            else
                waiting.add(0, downloader);     //move to top of list
        } else if(!up && idx != waiting.size() - 1) {
            waiting.remove(idx);
            if (amt != 0) {
                amt += idx;
                if (amt > waiting.size())
                    amt = waiting.size();
                waiting.add(amt, downloader);
            } else {
                waiting.add(downloader);    //move to bottom of list
            }
        }
    }

    /**
     * Cleans up the given Downloader after completion.
     *
     * If ser is true, also writes a snapshot to the disk.
     */
    private void cleanupCompletedDownload(CoreDownloader dl, boolean ser) {
        dl.finish();
        if (dl.getQueryGUID() != null)
            messageRouter.get().downloadFinished(dl.getQueryGUID());
        callback(dl).removeDownload(dl);
        
        //Save this' state to disk for crash recovery.
        if(ser)
            writeSnapshot();

        // Enable auto shutdown
        if(active.isEmpty() && waiting.isEmpty())
            callback(dl).downloadsComplete();
    }           
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#sendQuery(com.limegroup.gnutella.downloader.ManagedDownloader, com.limegroup.gnutella.messages.QueryRequest)
     */
    public void sendQuery(QueryRequest query) {
        messageRouter.get().sendDynamicQuery(query);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#measureBandwidth()
     */
    public void measureBandwidth() {
        List<CoreDownloader> activeCopy;
        synchronized(this) {
            activeCopy = new ArrayList<CoreDownloader>(active);
        }
        
        float currentTotal = 0f;
        boolean c = false;
        for (BandwidthTracker bt : activeCopy) {
            if (bt instanceof InNetworkDownloader)
                continue;
            
            c = true;
            bt.measureBandwidth();
            currentTotal += bt.getAverageBandwidth();
        }
        
        if ( c ) {
            synchronized(this) {
                averageBandwidth = ( (averageBandwidth * numMeasures) + currentTotal ) 
                    / ++numMeasures;
            }
        }
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#getMeasuredBandwidth()
     */
    public float getMeasuredBandwidth() {
        List<CoreDownloader> activeCopy;
        synchronized(this) {
            activeCopy = new ArrayList<CoreDownloader>(active);
        }
        
        float sum=0;
        for (BandwidthTracker bt : activeCopy) {
            if (bt instanceof InNetworkDownloader)
                continue;
            
            float curr = 0;
            try{
                curr = bt.getMeasuredBandwidth();
            } catch(InsufficientDataException ide) {
                curr = 0;//insufficient data? assume 0
            }
            sum+=curr;
        }
                
        lastMeasuredBandwidth = sum;
        return sum;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#getAverageBandwidth()
     */
    public synchronized float getAverageBandwidth() {
        return averageBandwidth;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#getLastMeasuredBandwidth()
     */
    public float getLastMeasuredBandwidth() {
        return lastMeasuredBandwidth;
    }
    
    private String getFileName(RemoteFileDesc[] rfds, String fileName) {
        for (int i = 0; i < rfds.length && fileName == null; i++) {
            fileName = rfds[i].getFileName();
        }
        return fileName;
    }
    
    /*
     * @see com.limegroup.gnutella.DownloadMI#getAllDownloaders()
     */
    public final Iterable<CoreDownloader> getAllDownloaders() {
        return activeAndWaiting;
    }
    
    public final CoreDownloaderFactory getCoreDownloaderFactory() {
        return coreDownloaderFactory;
    }

}
