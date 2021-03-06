package com.limegroup.bittorrent;

import java.io.File;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.net.ConnectionAcceptor;
import org.limewire.net.ConnectionDispatcher;
import org.limewire.nio.AbstractNBSocket;
import org.limewire.util.FileLocker;
import org.limewire.util.FileUtils;
import org.limewire.util.OSUtils;

import com.frostwire.bittorrent.AzureusStarter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.bittorrent.Torrent.TorrentState;
import com.limegroup.bittorrent.handshaking.IncomingConnectionHandler;
import com.limegroup.bittorrent.settings.BittorrentSettings;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.SpeedConstants;
import com.limegroup.gnutella.library.SharingUtils;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.util.EventDispatcher;

/**
 * Class which manages active torrents and dispatching of 
 * incoming BT connections.
 *   
 * Active torrents are torrents either in downloading or 
 * seeding state.
 * 
 * There number of active torrents cannot exceed certain limit.
 * 
 * After a torrent finishes its download, it stays in seeding state
 * indefinitely.  If the user wishes to start a new torrent download 
 * and the limit for active torrents is reached, the seeding torrent
 * with the best upload:download ratio gets terminated.
 * 
 * If active torrent limit is reached and none of the torrents are seeding,
 * the new torrent is queued.
 */
@Singleton
public class TorrentManager implements
	FileLocker, 
	ConnectionAcceptor, 
	TorrentEventListener, 
	EventDispatcher<TorrentEvent, TorrentEventListener> {
	

	private static final Log LOG = LogFactory.getLog(TorrentManager.class);

	/**
	 * The set of active torrents.
	 */
	private final Set <ManagedTorrent>_active = new HashSet<ManagedTorrent>();
	
	/**
	 * The set of active torrents that are seeding.
	 * INVARIANT: subset of _active.
	 */
	private final Set <ManagedTorrent>_seeding = new HashSet<ManagedTorrent>();
	
	/**
	 * Set of torrents that are about to get started.
	 */
	private final Set <ManagedTorrent> _starting = new HashSet<ManagedTorrent>();
	
	private final List <TorrentEventListener> listeners = 
		new CopyOnWriteArrayList<TorrentEventListener>();

    /** The File Manager */
    private final FileManager fileManager;
    
    /** Thread pool used to dispatch file manager events. These involve disk IO 
     *  and acquiring locks in the FileManager and should therefore not be executed
     *  on the NIODispatcher thread */
    private final ScheduledExecutorService threadPool;

    private final IncomingConnectionHandler incomingConnectionHandler;
    
    @Inject
    public TorrentManager(FileManager fileManager,
            @Named("backgroundExecutor") ScheduledExecutorService threadPool,
            IncomingConnectionHandler incomingConnectionHandler) {
        this.incomingConnectionHandler = incomingConnectionHandler;
        this.fileManager = fileManager;
        this.threadPool = threadPool;
        // we are a torrent event listener too.
        listeners.add(this);
    }
    
    /**
	 * Initializes this. Always call this method before starting any torrents.
	 */
	public void initialize(ConnectionDispatcher dispatcher) {
        if (LOG.isDebugEnabled())
			LOG.debug("initializing TorrentManager");
		
		// register ourselves as an acceptor.
        StringBuilder word = new StringBuilder();
		word.append((char)19);
		word.append("BitTorrent");
		dispatcher.addConnectionAcceptor(
				this,
				false,
				word.toString());
        
        FileUtils.addFileLocker(this);
	}
	
	public boolean isBlocking() {
	    return false;
	}
	
	/**
	 * @return number of allowed torrents for this speed.. this should
	 * probably be a setting
	 */
	private static int getMaxActiveTorrents() { 
		return BittorrentSettings.TORRENT_MAX_ACTIVE_DOWNLOADS.getValue();
	}
	
	/**
	 * @return the number of connections per torrent we'll try to maintain.
	 * This is somewhat arbitrary
	 */
	public static int getMaxTorrentConnections() {
		
		// windows 98 50 connection limit
		if (OSUtils.isWindows() && !OSUtils.isGoodWindows())
			return 30;
		
		if (ConnectionSettings.CONNECTION_SPEED.getValue() <= 
			SpeedConstants.MODEM_SPEED_INT)
			return 40;
		return 150;
	}
	
	public void addEventListener(TorrentEventListener listener) {
		if (!listeners.add(listener))
			throw new IllegalArgumentException("listener "+listener+" already registered");
	}
	
	public void removeEventListener(TorrentEventListener listener) {
		listeners.remove(listener);
	}
	
	public void dispatchEvent(TorrentEvent evt) {
		for(TorrentEventListener l : listeners) {
			if (l != evt.getSource())
				l.handleTorrentEvent(evt);
		}
	}
	
	public void handleTorrentEvent(TorrentEvent evt) {
		if (evt.getSource() == this)
			return;
		ManagedTorrent t = (ManagedTorrent)evt.getTorrent();
		switch(evt.getType()) {
		case STARTING: torrentStarting(t); break;
		case STARTED: torrentStarted(t); break;
        case DOWNLOADING: shareTorrent(t); break;
		case STOPPED: torrentStopped(t); break;
		case COMPLETE: torrentComplete(t); break;
        
        // the below aren't handled specially...
        case STOP_APPROVED: torrentStoppedApproved(t); break;
        case STOP_REQUESTED:
		}
	}
	
	private void torrentStoppedApproved(ManagedTorrent t) {
		if (_active != null && t != null) {
			_active.remove(t);
		}
	}

	/**
	 * @return active torrent for the given infoHash, null if no such.
	 */
	public synchronized ManagedTorrent getTorrentForHash(byte[] infoHash) {
		for (ManagedTorrent torrent : _active) {
			if (Arrays.equals(torrent.getInfoHash(), infoHash))
				return torrent;
		}
		return null;
	}
	
	public void acceptConnection(String word, Socket sock) {
		incomingConnectionHandler.handleIncoming(
				(AbstractNBSocket)sock, this);
	}

	private synchronized void torrentComplete(ManagedTorrent t) {
	    assert(_active.contains(t));
		_seeding.add(t);
	}
	
	private synchronized void torrentStarting(ManagedTorrent t) {
		_starting.add(t);
	}

	private synchronized void torrentStarted(ManagedTorrent t) {
	    //assert(_starting.contains(t));
		if (_starting.contains(t))
			_starting.remove(t);
		// if the user is adding a seeding torrent.. 
		// effectively restart it
		if (_seeding.remove(t))
			_active.remove(t);
			
		// when force-start is implemented, this needs to become a while
        // and we need to have a way to potentially stop a started torrent
        // from really starting. (or disable some of the force-started ones),
        // or etc....
		if (_active.size() >= getMaxActiveTorrents()) {
			ManagedTorrent best = null;
			for (ManagedTorrent torrent : _seeding) {
				if (best == null || torrent.getRatio() > best.getRatio())
					best = torrent;
			}
			if (best != null) 
				best.stop();
		}
		
		_active.add(t);
	}

	private synchronized void torrentStopped(ManagedTorrent t) {
		_active.remove(t);
		unshareTorrent(t, _seeding.remove(t));
	}
	
	public synchronized boolean allowNewTorrent() {
		return _starting.size() + 
		_active.size() - _seeding.size() < getMaxActiveTorrents();
	}
	
	public synchronized int getNumActiveTorrents() {
		return _active.size();
	}
	
	synchronized boolean hasNonSeeding() {
		return _active.size() > _seeding.size();
	}
        
    public boolean releaseLock(File file) {
        return killTorrentForFile(file);
    }
	
	public boolean killTorrentForFile(File f) {
		ManagedTorrent found = null;
		synchronized(this) {
			for (ManagedTorrent t: _active) {
				TorrentFileSystem system = t.getMetaInfo().getFileSystem();
				if (system.conflicts(f) ||
						system.conflictsIncomplete(f)) {
					found = t;
					break;
				}
			}
		}
		
		if (found != null) {
			found.stop();
			return true;
		} else 
			return false;
	}
    
    /**
     * Shares the torrent by adding it to the FileManager
     */
    private synchronized void shareTorrent(ManagedTorrent t) {
        if(!SharingSettings.SHARE_TORRENT_META_FILES.getValue() || t.getMetaInfo().isPrivate()) 
            return;
        
        final File f = getSharedTorrentMetaDataFile(t.getMetaInfo());
        
        Runnable r = new Runnable() {
            public void run() {
            	if (SharingUtils.isFilePhysicallyShareable(f))
            		fileManager.addFileForSession(f);
            }
        };
        threadPool.execute(r);
    }
    
    /**
     * Unshares the torrent from the FileManager.
     * If the tracker failed, this method deletes the corresponding .torrent
     * @return The FileDesc of the object removed from the FileManager. 
     * Can be null. 
     */
    private synchronized void unshareTorrent(final ManagedTorrent t, boolean delete) {
        final File f = getSharedTorrentMetaDataFile(t.getMetaInfo());
        
        final boolean fdelete = delete || t.getState().equals(TorrentState.TRACKER_FAILURE); 
        Runnable r = new Runnable() {
            public void run() {
                FileDesc fd = fileManager.stopSharingFile(f);          
                if(fd != null && fdelete){
                    FileUtils.delete(fd.getFile(), false);
                } else
                	f.setLastModified(System.currentTimeMillis());
            }
        };
        threadPool.execute(r);
    }
    
    
    /**
     * Returns the expected shared .torrent meta data file. 
     * Can be null.
     */
    public File getSharedTorrentMetaDataFile(BTMetaInfo info) {
        String fileName = info.getFileSystem().getName().concat(".torrent");
        
        File f = new File(SharingUtils.APPLICATION_SPECIAL_SHARE, fileName);
        //File f = new File(SharingSettings.DEFAULT_SHARED_TORRENTS_DIR, fileName);
        return f;
    }
}	
