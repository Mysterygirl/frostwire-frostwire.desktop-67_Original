package com.limegroup.bittorrent;

import java.io.File;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.uploader.UploadType;
import com.limegroup.gnutella.util.EventDispatcher;

/**
 * A facade for the GUI to treat a single BitTorrent download as a single upload.
 * 
 * @see BTDownloader.createUploader() to create one of these quickly.
 */
public class BTUploader implements Uploader, TorrentEventListener {
	
	private final ManagedTorrent _torrent;
	
	private final BTDownloaderImpl _btDownloader;
	
	private final BTMetaInfo _info;
	
	private long startTime, stopTime;
	
	private final EventDispatcher<TorrentEvent, TorrentEventListener> dispatcher;

    private final ActivityCallback activityCallback;

	BTUploader(ManagedTorrent torrent, 
			BTMetaInfo info,
			EventDispatcher<TorrentEvent, 
			TorrentEventListener> dispatcher, 
			ActivityCallback activityCallback,
			BTDownloaderImpl btDownloader) {
		_torrent = torrent;
		_info = info;
		_btDownloader = btDownloader;
		this.dispatcher = dispatcher;
        this.activityCallback = activityCallback;
		dispatcher.addEventListener(this);
	}
	
	public ManagedTorrent getManagedTorrent() {
		return _torrent;
	}

	public void stop() {
		TorrentEvent stopping = new TorrentEvent(this,
				TorrentEvent.Type.STOP_REQUESTED,
				_torrent);
		dispatcher.dispatchEvent(stopping);
	}

	public String getFileName() {
		return _info.getName();
	}

	public long getFileSize() {
		return _info.getFileSystem().getTotalSize();
	}

	public FileDesc getFileDesc() {
		return _info.getFileDesc();
	}

	public int getIndex() {
		// negative will make sure it never conflicts with regular uploads
		return 0 - Math.abs(_info.getURN().hashCode());
	}
 
	public long amountUploaded() {
		return _torrent.getTotalUploaded();
	}

	public long getTotalAmountUploaded() {
		return _torrent.getTotalUploaded();
	}

	public String getHost() {
		return BITTORRENT_UPLOAD;
	}

	public UploadStatus getState() {
		if (!_torrent.isActive()) {
			if (_torrent.isComplete() && _torrent.getRatio() > 1)
				return UploadStatus.COMPLETE;
			//return UploadStatus.WAITING_REQUESTS;
		}
		
		if (_torrent.isUploading())
			return UploadStatus.UPLOADING;
		
		if (_torrent.isSuspended())
			return UploadStatus.SUSPENDED;
		
		// neither uploading, nor suspended..
		return UploadStatus.WAITING_REQUESTS;
	}

	public UploadStatus getLastTransferState() {
		return UploadStatus.UPLOADING;
	}

	public boolean isChatEnabled() {
		return false;
	}

	public boolean isBrowseHostEnabled() {
		return false;
	}

	public int getGnutellaPort() {
		return 0;
	}

	public String getUserAgent() {
		return BITTORRENT_UPLOAD;
	}

	public int getQueuePosition() {
		return 0;
	}

	public boolean isInactive() {
		switch(_torrent.getState()) {
		case PAUSED:
		case STOPPED:
			return true;
		}
		return false;
	}

	public void measureBandwidth() {
		_torrent.measureBandwidth();
	}

	public float getMeasuredBandwidth() throws InsufficientDataException {
		if (!_torrent.isActive())
			return 0.f;
		return _torrent.getMeasuredBandwidth(false);
	}

	public void handleTorrentEvent(TorrentEvent evt) {
		if (evt.getTorrent() != _torrent)
			return;
		
		switch(evt.getType()) {
		case STARTED : torrentStarted(); break;
		case STOP_APPROVED:
		    activityCallback.removeUpload(this);
		    //_torrent.stop();
		    break;
		case STOPPED : 
			torrentStopped();
			dispatcher.removeEventListener(this);
			break;
            
        // the below don't need any special handling...
        case COMPLETE:
        case STARTING:
        case STOP_REQUESTED:
		}
	}
	
	public float getAverageBandwidth() {
		long now = stopTime > 0 ? stopTime : System.currentTimeMillis();
		long runTime = (now - startTime);
		return runTime > 0 ? getTotalAmountUploaded() / runTime : 0;
	}
	
	private void torrentStarted() {
		startTime = System.currentTimeMillis();
		stopTime = 0;
		activityCallback.addUpload(this);
	}
	
	private void torrentStopped() {
		//this was commented maybe for a good reason
		//activityCallback.removeUpload(this);
		stopTime = System.currentTimeMillis();
	}
	
	public String getCustomIconDescriptor() {
		if (_info.getFileSystem().getFiles().size() == 1)
			return null;
		return BITTORRENT_UPLOAD;
	}

    public UploadType getUploadType() {
        return UploadType.SHARED_FILE;
    }

    public boolean isTLSCapable() {
        return false; // TODO: SSLUtils.isTLSEnabled(mySocket)
    }

    public String getAddress() {
        return "torrent upload";
    }

    public InetAddress getInetAddress() {
        return null;
    }

    public int getPort() {
        return -1;
    }
    
    public InetSocketAddress getInetSocketAddress() {
        return null;
    }

    public int getNumUploadConnections() {
	return _torrent.getNumConnections();
    }

    public String getAddressDescription() {
	return null;
    }
   
    public File getFile() {
	return _torrent.getTorrentDataFile();
    }

    public URN getUrn() {
	return _torrent.getURN();
    }

    public float getSeedRatio() {
	return _torrent.getRatio();
    }
    
    public BTDownloaderImpl getBTDownloader() {
    	return _btDownloader;
    }
}
