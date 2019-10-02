package com.limegroup.gnutella.gui;

import javax.swing.SwingUtilities;

import org.limewire.service.ErrorService;
import org.limewire.util.OSUtils;

import com.apple.eawt.Application;
import com.limegroup.gnutella.browser.ExternalControl;

/**
 * JNI based GetURL AppleEvent handler for Mac OS X
 */
public final class GURLHandler {
    
    private static GURLHandler instance;
    private volatile boolean registered = false;    
    private volatile boolean enabled = false;
    private volatile String url;
    private volatile ExternalControl externalControl;
    
    public static Application APP;
    
    static {
        try {
            if (OSUtils.isMacOSX105() || OSUtils.isMacOSX106()) {
                System.loadLibrary("GURLLeopard");
                System.out.println("GURLLeopard loadded.");
            }
            else {
                System.loadLibrary("GURLTiger loaded.");
            }
        }
        catch (UnsatisfiedLinkError err) {
            ErrorService.error(err);
        }
    }
    
    public static synchronized GURLHandler getInstance() {
        if(instance == null)
            instance = new GURLHandler();
        return instance;
    }
        
    /** Called by the native code */
    @SuppressWarnings("unused")
    private void callback(final String url) {
		if ( enabled && externalControl.isInitialized() ) {
			Runnable runner = new Runnable() {
				public void run() {
                    try {
                        externalControl.handleMagnetRequest(url);
                    } catch(Throwable t) {
                        ErrorService.error(t);
                    }
				} 
			};
			SwingUtilities.invokeLater(runner);
		} else {
            this.url = url;
		}
    }
    
    /**
     * 
     */
    public void enable(ExternalControl externalControl) {
        this.externalControl = externalControl;
        externalControl.enqueueControlRequest(url);
        this.url = null;
        this.enabled = true;
    }
    
    /** Registers the GetURL AppleEvent handler. */
    public void register() {
		if (!registered) {
            if (InstallEventHandler() == 0) {
            	//System.out.println("GURLHandler - AppleEvent handler registered");
                registered = true;
            }
        }
    }
    
    /** We're nice guys and remove the GetURL AppleEvent handler although
    this never happens */
    @Override
    protected void finalize() throws Throwable {
        if (registered) {
            RemoveEventHandler();
        }
    }
    
    private synchronized final native int InstallEventHandler();
    private synchronized final native int RemoveEventHandler();
}