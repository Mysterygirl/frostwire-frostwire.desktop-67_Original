package com.limegroup.gnutella.gui;

import java.io.File;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import org.limewire.io.IpPort;

import com.google.inject.Singleton;
import com.limegroup.bittorrent.BTDownloader;
import com.limegroup.bittorrent.BTDownloaderImpl;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Downloader.DownloadStatus;
import com.limegroup.gnutella.FileManagerEvent;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.MediaType;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.chat.InstantMessenger;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.gui.chat.ChatUIManager;
import com.limegroup.gnutella.gui.download.DownloaderUtils;
import com.limegroup.gnutella.gui.logging.LogEvent;
import com.limegroup.gnutella.gui.search.SearchInformation;
import com.limegroup.gnutella.gui.search.SearchMediator;
import com.limegroup.gnutella.gui.sharing.ShareManager;
import com.limegroup.gnutella.gui.util.BackgroundExecutorService;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.settings.DaapSettings;
import com.limegroup.gnutella.settings.QuestionsHandler;
import com.limegroup.gnutella.settings.iTunesSettings;
import com.limegroup.gnutella.uploader.UploadType;
import com.limegroup.gnutella.util.QueryUtils;
import com.limegroup.gnutella.version.UpdateInformation;

/**
 * This class is the gateway from the backend to the frontend.  It
 * delegates all callbacks to the appropriate frontend classes, and it
 * also handles putting calls onto the Swing thread as necessary.
 * 
 * It implements the <tt>ActivityCallback</tt> callback interface, designed
 * to make it easy to swap UIs.
 */
@Singleton
public final class VisualConnectionCallback implements ActivityCallback {

	///////////////////////////////////////////////////////////////////////////
	//  Connection-related callbacks
	///////////////////////////////////////////////////////////////////////////
	
    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
        Runnable doWorkRunnable;
        RoutedConnection c = evt.getConnection();
        if(c != null){
            if(evt.isConnectionInitializingEvent()) {
                //Handle a new connection.
                doWorkRunnable = new ConnectionInitializing(c);
            } else if (evt.isConnectionInitializedEvent()) {
                //Change the status of a connection 
                //when it's been fully initialized
                doWorkRunnable = new ConnectionInitialized(c);
            } else if (evt.isConnectionClosedEvent()) {
                //Handle a removed connection
                doWorkRunnable = new ConnectionClosed(c);
            } else { //don't care about other events for now
                return;
            }
            SwingUtilities.invokeLater(doWorkRunnable);
        }
    }
    /**
     *  Handle a new connection.
     */
    private class ConnectionInitializing implements Runnable {
        private RoutedConnection  c;
        public ConnectionInitializing(RoutedConnection c) {
            this.c      = c;
        }
        public void run() { 
            mf().getConnectionMediator().add(c); 
        }
    }

    /**
     *  Change the status of a connection when it's been fully initialized
     */
    private class ConnectionInitialized implements Runnable {
        private RoutedConnection  c;
        public ConnectionInitialized(RoutedConnection c) {
            this.c = c;
        }
        public void run() { 
            mf().getConnectionMediator().update(c);
		}
    }

    /**
     *  Handle a removed connection.
     */
    private class ConnectionClosed implements Runnable {
        private RoutedConnection  c;
        public ConnectionClosed(RoutedConnection c) {
            this.c = c;
        }
        public void run() { 
            mf().getConnectionMediator().remove(c);
		}
    }
    
    

	
	///////////////////////////////////////////////////////////////////////////
	//  Query-related callbacks
	///////////////////////////////////////////////////////////////////////////

	/**
	 * Handle to the class that handles query strings.
	 */
    private final HandleQueryString HANDLE_QUERY_STRING = new HandleQueryString();
    
    /**
     *  Add a query string to the monitor screen
     */
    public void handleQueryString(String query) {
        HANDLE_QUERY_STRING.addQueryString(query);
    }
	
    /**
     *  Add a query reply to a query screen
     */

    public void handleQueryResult(final RemoteFileDesc rfd,
                                  final HostData data,
                                  final Set<? extends IpPort> locs) {
		SwingUtilities.invokeLater(new Runnable() {
			@SuppressWarnings("unchecked")
			public void run() {
			    SearchMediator.handleQueryResult(rfd, data, (Set<IpPort>) locs);
			}
		});
    }

    /**
     * @return true if the guid is still viewable to the user, else false.
     */
    public boolean isQueryAlive(GUID guid) {
        return SearchMediator.queryIsAlive(guid);
    }

    /**
     *  Add a query string to the monitor screen
     */
    private class HandleQueryString implements Runnable {
        private Vector<String>  list;
        private boolean active;

        public HandleQueryString() {
            list   = new Vector<String>();
            active = false;
        }

        public void addQueryString(String query) {
            list.add(query);
            if (active == false) {
                active = true;
                SwingUtilities.invokeLater(this);
            }
        }

        public void run() {
            String query;
            while (list.size() > 0) {
                query = list.elementAt(0);
                list.remove(0);
			    mf().getMonitorView().handleQueryString(query);
            }
            active = false;
        }
    }


	///////////////////////////////////////////////////////////////////////////
	//  Files-related callbacks
	///////////////////////////////////////////////////////////////////////////
	
    /**
     * File manager finished loading.
     */
    public void fileManagerLoaded() {
        if (DaapSettings.DAAP_ENABLED.getValue()) {
            Runnable r = new Runnable() {
                public void run() {
                    DaapManager.instance().fileManagerLoaded();
                }
            };

            BackgroundExecutorService.schedule(r);
        }
    }
    
	/** 
	 * This method notifies the frontend that the data for the 
	 * specified shared <tt>File</tt> instance has been 
	 * updated.
	 *
	 * @param file the <tt>File</tt> instance for the shared file whose
	 *  data has been updated
	 */
    public void handleSharedFileUpdate(final File file) {
        /**
         * NOTE: Pass this off directly to the library
         * so it can discard the update if the directory
         * of the file isn't selected.
         * This reduces the amount of Runnables created
         * by a very large amount.
         */
         mf().getLibraryMediator().updateSharedFile(file);
    }
        
	/**
	 * Handles events created by the FileManager. Passes these events on to DAAP
	 * or the Library.
	 */
    public void handleFileEvent(final FileManagerEvent evt) {
        BackgroundExecutorService.schedule(new Runnable() {
            public void run() {
                if (DaapSettings.DAAP_ENABLED.getValue()
                        && DaapManager.instance().isEnabled()) {
                    DaapManager.instance().handleFileManagerEvent(evt);
                }
            }
        });

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                mf().getLibraryMediator().handleFileManagerEvent(evt);
            }
        });
    }
    
    public void fileManagerLoading() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                mf().getLibraryMediator().clearLibrary();
            }
        });
        
        if (DaapSettings.DAAP_ENABLED.getValue()) {
            Runnable r = new Runnable() {
                public void run() {
                    DaapManager.instance().fileManagerLoading();
                }
            };

            BackgroundExecutorService.schedule(r);
        }
    }
    

	///////////////////////////////////////////////////////////////////////////
	//  Download-related callbacks
	///////////////////////////////////////////////////////////////////////////
	
    public void addDownload(Downloader mgr) {
        Runnable doWorkRunnable = new AddDownload(mgr);
        
        if (mgr instanceof BTDownloaderImpl) {
        	if (((BTDownloader) mgr).isCompleted()) {
        		//don't add it visually
        		return;
        	}
        }
        
        SwingUtilities.invokeLater(doWorkRunnable);
    }

    public void removeDownload(Downloader mgr) {
        Runnable doWorkRunnable = new RemoveDownload(mgr);
        SwingUtilities.invokeLater(doWorkRunnable);
        
        if (iTunesSettings.ITUNES_SUPPORT_ENABLED.getValue() 
                && mgr.getState() == DownloadStatus.COMPLETE) {
            
            iTunesMediator.instance().addSong(mgr.getSaveFile());
        }
    }
    
    public void downloadsComplete() {
        Finalizer.setDownloadsComplete();
    }

	/**
	 *  Show active downloads
	 */
	public void showDownloads() {
	    SwingUtilities.invokeLater(new Runnable() {
	        public void run() {
		        GUIMediator.instance().setWindow(GUIMediator.Tabs.SEARCH);	
            }
        });
	}	
    
    private class AddDownload implements Runnable {
        private Downloader mgr;
        public AddDownload(Downloader mgr) {
            this.mgr = mgr;
        }
        public void run() {
            mf().getDownloadMediator().add(mgr);
		}
    }

    private class RemoveDownload implements Runnable {
        private Downloader mgr;
        public RemoveDownload(Downloader mgr) {
            this.mgr = mgr;
        }
        public void run() {
            mf().getDownloadMediator().remove(mgr);
            mf().getLibraryMediator().quickRefresh();
            SearchMediator.updateResults();
            mf().getLoggingMediator().add(new LogEvent(mgr));
	    }
            
    }

	
	///////////////////////////////////////////////////////////////////////////
	//  Upload-related callbacks
	///////////////////////////////////////////////////////////////////////////
	
    public void addUpload(Uploader uploader) {
        UploadType type = uploader.getUploadType();
        if(!type.isInternal()) {
            SwingUtilities.invokeLater(new AddUpload(uploader));
        } else if(type == UploadType.BROWSE_HOST)
            SwingUtilities.invokeLater(new BrowseHosted(uploader));
    }

    public void removeUpload(Uploader uploader) {
        UploadType type = uploader.getUploadType();
        if(type != null && !type.isInternal()) {
            SwingUtilities.invokeLater(new RemoveUpload(uploader));
        }
    }
    
    public void uploadsComplete() {
        Finalizer.setUploadsComplete();
    }
    
    private class BrowseHosted implements Runnable {
        private Uploader up;
        public BrowseHosted(Uploader up) {
            this.up = up;
        }
        public void run() {
            mf().getLoggingMediator().add(new LogEvent(up));
        }
    }

    private class AddUpload implements Runnable {
        private Uploader up;
        public AddUpload(Uploader up) {
            this.up = up;
        }
        public void run() {
            mf().getUploadMediator().add(up);
            mf().getLoggingMediator().add(new LogEvent(up));
		}
    }

    private class RemoveUpload implements Runnable {
        private Uploader mgr;
        public RemoveUpload(Uploader mgr) {
            this.mgr = mgr;
        }
        public void run() {
            mf().getUploadMediator().remove(mgr);
	    }
    }
	

	///////////////////////////////////////////////////////////////////////////
	//  Chat-related callbacks
	///////////////////////////////////////////////////////////////////////////
	
	/** 
	 * Adds a new chat session, encapsulated in the specified 
	 * <tt>Chatter</tt> instance.
	 *
	 * @param chatter the <tt>Chatter</tt> instance that provides all
	 *  data access regarding the chat session
	 */
	public void acceptChat(final InstantMessenger chatter) {
	    SwingUtilities.invokeLater(new Runnable() {
	        public void run() {
	            ChatUIManager.instance().acceptChat(chatter);
			}
	    });
	}
	
	/**
	 * Receives a new chat message for a specific <tt>Chatter</tt>
	 * instance.
	 * 
	 * @param chatter the <tt>Chatter</tt> instance that is receiving
	 *  a new message
	 */
	public void receiveMessage(final InstantMessenger chatter, final String message) {
	    SwingUtilities.invokeLater(new Runnable() {
	        public void run() {
	            ChatUIManager.instance().receiveMessage(chatter, message);
			}
	    });
	}

	/** 
	 * Specifies that the given chat host is no longer available, thereby
	 * ending the chat session.
	 *
	 * @param chatter the <tt>Chatter</tt> instance for the chat session
	 *  that is terminating 
	 */
	public void chatUnavailable(final InstantMessenger chatter) {
	    SwingUtilities.invokeLater(new Runnable() {
	        public void run() {
	            ChatUIManager.instance().chatUnavailable(chatter);
			}
	    });
	}

	/** 
	 * Display an error message for the specified chat session.
	 *
	 * @param chatter the <tt>Chatter</tt> instance to show an error for
	 * @param str the error to display
	 */
	public void chatErrorMessage(final InstantMessenger chatter, final String str) {
	    SwingUtilities.invokeLater(new Runnable() {
	        public void run() {
		        ChatUIManager.instance().chatErrorMessage(chatter, str);
			}
	    });
	}

	
	///////////////////////////////////////////////////////////////////////////
	//  Other stuff
	///////////////////////////////////////////////////////////////////////////
	
    /**
     * Notification that the address has changed.
     */
    public void handleAddressStateChanged() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // don't touch GUI code if it isn't constructed.
                // this is necessary here only because addressStateChanged
                // is triggered by Acceptor, which is init'd prior to the
                // GUI actually existing.
                if (GUIMediator.isConstructed())
                    SearchMediator.addressChanged();
            }
        });
    }
    
    public void handleNoInternetConnection() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (GUIMediator.isConstructed())
                    GUIMediator.disconnected();
            }
        });
    }


    /**
     * Pops up a dialog that the user is attempting to share a sensitive
     * directory, and allows the user to either share or not share
     * the folder.  Returns true if the sensitive directory should be shared. 
     */
    public boolean warnAboutSharingSensitiveDirectory(final File dir) {
        final AtomicBoolean bool = new AtomicBoolean(false);
        GUIMediator.safeInvokeAndWait(new Runnable() {
            public void run() {
                bool.set(ShareManager.warnAboutSensitiveDirectory(dir));
            }
        });
        
        return bool.get();
    }
    
    public void setAnnotateEnabled(final boolean enabled) {
    	    SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                mf().getLibraryMediator().setAnnotateEnabled(enabled);
            }
        });
    }

	/**
     * Notification that a new update is available.
     */
    public void updateAvailable(UpdateInformation update) {
        GUIMediator.instance().showUpdateNotification(update);
    }
    
    /**
     * Display an error message for a ResultPanel (if it still exists)
     * @param guid The GUID of the ResultPanel.
     */
    public void browseHostFailed(final GUID guid) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
                SearchMediator.browseHostFailed(guid);
			}
		});
    }

    /**
     * Shows the user a message informing her that a file being downloaded 
     * is corrupt.
     * <p>
     * This method MUST call dloader.discardCorruptDownload(boolean b) 
     * otherwise there will be threads piling up waiting for a notification
     */
    public void promptAboutCorruptDownload(Downloader downloader) {    
        final Downloader dloader = downloader;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                DialogOption resp=GUIMediator.showYesNoMessage(
                              I18n.tr("FrostWire has detected a corruption in the file {0}. Do you want to continue the download?",dloader.getSaveFile().getName()),
                              QuestionsHandler.CORRUPT_DOWNLOAD, DialogOption.NO
                              );
                    
                // discard if they didn't want to save.                              
                dloader.discardCorruptDownload(
                    resp == DialogOption.NO);
            }
        });
    }
    
    public boolean promptAboutStopping() {
        DialogOption resp = GUIMediator.showYesNoMessage(I18n.tr("If you stop this upload, the torrent download will stop. Are you sure you want to do this?"), DialogOption.NO);
    	return resp == DialogOption.YES;
    }
    
    public boolean promptAboutSeeding() {
        DialogOption resp = GUIMediator.showYesNoMessage(I18n.tr("This upload is a torrent and it hasn\'t seeded enough. You should let it upload some more. Are you sure you want to stop it?"), DialogOption.NO);
    	return resp == DialogOption.YES;
    }

	/**
	 *  Tell the GUI to deiconify.
	 */  
	public void restoreApplication() {
	    SwingUtilities.invokeLater(new Runnable() {
	        public void run() {
		        GUIMediator.restoreView();
            }
        });
		    
	}
	
	/**
	 * Notification of a component loading.
	 */
	public void componentLoading(final String component) {
	    SwingUtilities.invokeLater(new Runnable() {
	        public void run() {
                GUIMediator.setSplashScreenString(
                    I18n.tr(component));
            }
        });
    }       
	
	/**
	 * Indicates that the firewalled state of this has changed. 
	 */
	public void acceptedIncomingChanged(final boolean status) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				GUIMediator.instance().getStatusLine().updateFirewallLabel(status);
			}
		});
	}

	public String getHostValue(String key) {
        return I18n.tr(key);
    }
    
    /**
     * Returns the MainFrame.
     */
    private MainFrame mf() {
        return GUIMediator.instance().getMainFrame();
    }

	/**
	 * Returns true since we want to kick off the magnet downloads ourselves.
	 */
	public boolean handleMagnets(final MagnetOptions[] magnets) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				boolean oneSearchStarted = false;
				for (int i = 0; i < magnets.length; i++) {
					// spawn search for keyword only magnet
					if (magnets[i].isKeywordTopicOnly() && !oneSearchStarted) {
						String query = QueryUtils.createQueryString
							(magnets[i].getKeywordTopic());
						SearchInformation info = 
							SearchInformation.createKeywordSearch
							(query, null, MediaType.getAnyTypeMediaType());
						if (SearchMediator.validateInfo(info) 
							== SearchMediator.QUERY_VALID) {
							oneSearchStarted = true;
							SearchMediator.triggerSearch(info);
						}
					}
					else {
						DownloaderUtils.createDownloader(magnets[i]);
					}
				}
				if (magnets.length > 0) {
					GUIMediator.instance().setWindow(GUIMediator.Tabs.SEARCH);
				}
			}
		});
		return true;
	}
	
	public void handleTorrent(final File torrentFile) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				GUIMediator.instance().openTorrent(torrentFile);
			}
		});
	}
   
    public void installationCorrupted() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                GUIMediator.showWarning(I18n.tr("<html><b>Your FrostWire may have been corrupted by a virus or trojan!</b><br><br>Please visit <a href=\"http://www.frostwire.com/download\">www.frostwire.com</a> and download the newest official version of FrostWire.</html>"));
            }
        });
    }

	@Override
	public void handleTorrentMagnet(final String request) {
		GUIMediator.instance().openTorrentMagnet(request);
	}
}