package com.limegroup.gnutella.gui;

import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicHTML;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.i18n.I18nMarker;
import org.limewire.service.ErrorService;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.I18NConvert;
import org.limewire.util.OSUtils;
import org.limewire.util.Stopwatch;
import org.limewire.util.SystemUtils;

import com.frostwire.bittorrent.AzureusStarter;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.limegroup.gnutella.LimeCoreGlue;
import com.limegroup.gnutella.LimeCoreGlue.InstallFailedException;
import com.limegroup.gnutella.LimeWireCore;
import com.limegroup.gnutella.browser.ExternalControl;
import com.limegroup.gnutella.bugs.BugManager;
import com.limegroup.gnutella.gui.init.SetupManager;
import com.limegroup.gnutella.gui.notify.NotifyUserProxy;
import com.limegroup.gnutella.gui.themes.ThemeSettings;
import com.limegroup.gnutella.gui.util.BackgroundExecutorService;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DaapSettings;
import com.limegroup.gnutella.settings.StartupSettings;
import com.limegroup.gnutella.util.FrostWireUtils;

/** Initializes (creates, starts, & displays) the LimeWire Core & UI. */
public final class Initializer {
    private final Log LOG;
    
    /** Refuse to start after this date */
    private final long EXPIRATION_DATE = Long.MAX_VALUE;
    
    /** True if is running from a system startup. */
    private volatile boolean isStartup = false;
    
    /** The start memory -- only set if debugging. */
    private long startMemory;
    
    /** A stopwatch for debug logging. */
    private final Stopwatch stopwatch;
    
    Initializer() {
        LOG = LogFactory.getLog(Initializer.class);
        
        if(LOG.isTraceEnabled()) {
            startMemory = Runtime.getRuntime().totalMemory()
                        - Runtime.getRuntime().freeMemory();
            LOG.trace("START Initializer, using: " + startMemory + " memory");
        }
        
        stopwatch = new Stopwatch(LOG);
    }
    
    /**
     * Initializes all of the necessary application classes.
     * 
     * If this throws any exceptions, then LimeWire was not able to construct
     * properly and must be shut down.
     */
     void initialize(String args[], Frame awtSplash) throws Throwable {
        // ** THE VERY BEGINNING -- DO NOT ADD THINGS BEFORE THIS **
        //System.out.println("Initializer.initialize() preinit()");
        preinit();        
        
        // Various startup tasks...
        //System.out.println("Initializer.initialize() setup callbacks and listeners");
        setupCallbacksAndListeners();     
        validateStartup(args);        
        
        // Creates LimeWire itself.
        //System.out.println("Initializer.initialize() create Limewire");
        LimeWireGUI limewireGUI = createLimeWire(); 
        LimeWireCore limeWireCore = limewireGUI.getLimeWireCore();

        // Various tasks that can be done after core is glued & started.
        //System.out.println("Initializer.initialize() glue core");
        glueCore(limeWireCore);        
        validateEarlyCore(limeWireCore);
        
        // Validate any arguments or properties outside of the LW environment.
        //System.out.println("Initializer.initialize() run external checks");
        runExternalChecks(limeWireCore, args);

        // Starts some system monitoring for deadlocks.
        //System.out.println("Initializer.initialize() monitor deadlocks");
        DeadlockSupport.startDeadlockMonitoring();
        //stopwatch.resetAndLog("Start deadlock monitor");
        
        // Installs properties & resources.
        //System.out.println("Initializer.initialize() install properties");
        installProperties();        
        installResources();
        
        // Construct the SetupManager, which may or may not be shown.
        final SetupManager setupManager = new SetupManager();
        //stopwatch.resetAndLog("construct SetupManager");

        // Move from the AWT splash to the Swing splash & start early core.
        //System.out.println("Initializer.initialize() switch splashes");
        switchSplashes(awtSplash);
        startEarlyCore(setupManager, limeWireCore);
        
        // Initialize early UI components, display the setup manager (if necessary),
        // and ensure the save directory is valid.
        //System.out.println("Initializer.initialize() init early UI");
        initializeEarlyUI();
        startSetupManager(setupManager);
        validateSaveDirectory();
        
        // Load the UI, system tray & notification handlers,
        // and hide the splash screen & display the UI.
        //System.out.println("Initializer.initialize() load UI");
        loadUI();
        loadTrayAndNotifications();
        hideSplashAndShowUI();
        
        // Initialize late tasks, like Icon initialization & install listeners.
        loadLateTasksForUI();
        installListenersForUI(limeWireCore);
        
        // Start the core & run any queued control requests, and load DAAP.
        //System.out.println("Initializer.initialize() start core");
        startCore(limeWireCore);
        runQueuedRequests(limeWireCore);
        startDAAP();
        
        startAzureusCore();
        
        // Run any after-init tasks.
        //System.out.println("Initializer.initialize() post init");
        postinit();
    }
    
     
    /**
     * If this is the first time we run and there was an old FrostWire
     * Bring the .frostwire4.18 preferences so the user won't loose his library
     * or preferences. Only for Windows.
     */
     private void tryMigratingOldPreferences() {
    	 File userDir = CommonUtils.getUserHomeDir();
    	 File settingsDir = new File(userDir, FrostWireUtils.LIMEWIRE_PREFS_DIR_NAME);
    	 
    	 if (!OSUtils.isWindows() ||
    		 (settingsDir.exists() && settingsDir.list()!=null)) {
    		 //System.out.println("Initializer.tryMigratingOldPreferences(): Not needed.");
    		 return;
    	 }
    	 
    	 File possibleOldDir = new File(userDir, FrostWireUtils.FROSTWIRE_418_DIR_NAME);

    	 if ((!settingsDir.exists() || settingsDir.list()==null) && 
    		possibleOldDir.exists() && 
    		possibleOldDir.isDirectory() &&
    		possibleOldDir.list() != null) {
    		 //System.out.println("Initializer.tryMigratingOldPreferences(): Copying old settings...");
    		 FileUtils.copyDirectoryRecursively(possibleOldDir, settingsDir);
    		 
    		 //get rid of overlays, make sure you get new ones.
    		 FileUtils.deleteRecursive(new File(settingsDir,"overlays"));
    		 FileUtils.delete(new File(settingsDir,"overlays.dat"), false);
    	 }
     }
    
    /** Initializes the very early things. */
    /*
     * DO NOT CHANGE THIS WITHOUT KNOWING WHAT YOU'RE DOING.
     * PREINSTALL MUST BE DONE BEFORE ANYTHING ELSE IS REFERENCED.
     * (Because it sets the preference directory in CommonUtils.)
     */
    private void preinit() {        
        // Make sure the settings directory is set.
        try {
        	tryMigratingOldPreferences();
        	LimeCoreGlue.preinstall();
            //stopwatch.resetAndLog("Preinstall");
        } catch(InstallFailedException ife) {
            failPreferencesPermissions();
        }
    }
    
    /** Installs all callbacks & listeners. */
    private void setupCallbacksAndListeners() {        
        // Set the error handler so we can receive core errors.
        ErrorService.setErrorCallback(new ErrorHandler());
        //stopwatch.resetAndLog("ErrorHandler install");
        
        // Set the messaging handler so we can receive core messages
        org.limewire.service.MessageService.setCallback(new MessageHandler());
        //stopwatch.resetAndLog("MessageHandler install");
        
        // Set the default event error handler so we can receive uncaught
        // AWT errors.
        DefaultErrorCatcher.install();
        //stopwatch.resetAndLog("DefaultErrorCatcher install");
        
        if (OSUtils.isMacOSX()) {
            // Raise the number of allowed concurrent open files to 1024.
            SystemUtils.setOpenFileLimit(1024);
            stopwatch.resetAndLog("Open file limit raise");
            
            if(ThemeSettings.isBrushedMetalTheme())
                System.setProperty("apple.awt.brushMetalLook", "true");     

            MacEventHandler.instance();
            stopwatch.resetAndLog("MacEventHandler instance");
        }
    }
    
    /**
     * Ensures this should continue running, by checking
     * for expiration failures or startup settings. 
     */
    private void validateStartup(String[] args) {        
        // check if this version has expired.
        if (System.currentTimeMillis() > EXPIRATION_DATE) 
            failExpired();
        
        // If this is a request to launch a pmf then just do it and exit.
        if ( args.length >= 2 && "-pmf".equals(args[0]) ) {
            PackagedMediaFileLauncher.launchFile(args[1], false); 
            System.exit(0);
        }
        
        // Yield so any other events can be run to determine
        // startup status, but only if we're going to possibly
        // be starting...
        if(StartupSettings.RUN_ON_STARTUP.getValue()) {
            stopwatch.reset();
            Thread.yield();
            //stopwatch.resetAndLog("Thread yield");
        }
        
        if (args.length >= 1 && "-startup".equals(args[0]))
            isStartup = true;
        
        if (isStartup) {
            // if the user doesn't want to start on system startup, exit the
            // JVM immediately
            if(!StartupSettings.RUN_ON_STARTUP.getValue())
                System.exit(0);
        }
    }
    
    /** Wires together LimeWire. */
    private LimeWireGUI createLimeWire() {
        Injector injector = Guice.createInjector(new LimeWireModule());
        LimeWireGUI limeWireGUI = injector.getInstance(LimeWireGUI.class);
        return limeWireGUI;
    }
    
    /** Wires together remaining non-Guiced pieces. */
    private void glueCore(LimeWireCore limeWireCore) {
        limeWireCore.getLimeCoreGlue().install();
    }
    
    /** Tasks that can be done after core is created, before it's started. */
    private void validateEarlyCore(LimeWireCore limeWireCore) {        
        // See if our NIODispatcher clunked out.
        if(!limeWireCore.getNIODispatcher().isRunning()) {
            failInternetBlocked();
        }
    }
    
    /**
     * Initializes any code that is dependent on external controls.
     * Specifically, GURLHandler & MacEventHandler on OS X,
     * ensuring that multiple LimeWire's can't run at once,
     * and processing any arguments that were passed to LimeWire.
     */ 
    private void runExternalChecks(LimeWireCore limeWireCore, String[] args) {        
        ExternalControl externalControl = limeWireCore.getExternalControl();
        //stopwatch.resetAndLog("Get externalControl");
        if(OSUtils.isMacOSX()) {
            GURLHandler.getInstance().enable(externalControl);
            //stopwatch.resetAndLog("Enable GURL");
            MacEventHandler.instance().enable(externalControl, this);
            //stopwatch.resetAndLog("Enable macEventHandler");
        }
        
        // Test for preexisting LimeWire and pass it a magnet URL if one
        // has been passed in.
        if (args.length > 0 && !args[0].equals("-startup")) {
            String arg = externalControl.preprocessArgs(args);
            //JOptionPane.showMessageDialog(null, "Initializer.runExternalChecks() - arg => \n" + arg);
            //stopwatch.resetAndLog("Preprocess args");
            externalControl.checkForActiveLimeWire(arg);
            //stopwatch.resetAndLog("Check for active LW");
            externalControl.enqueueControlRequest(arg);
            //stopwatch.resetAndLog("Enqueue control req");
        } else if (!StartupSettings.ALLOW_MULTIPLE_INSTANCES.getValue()) {
            // if we don't want multiple instances, we need to check if
            // frostwire is already active.
            externalControl.checkForActiveLimeWire();
            //stopwatch.resetAndLog("Check for active FW");
        }
    }
    
    /** Installs any system properties. */
    private void installProperties() {        
        System.setProperty("http.agent", FrostWireUtils.getHttpServer());
        stopwatch.resetAndLog("set system properties");
        
        if (OSUtils.isMacOSX()) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            stopwatch.resetAndLog("set OSX properties");
        }
    }
    
    /** Sets up ResourceManager. */
    private void installResources() {
        GUIMediator.safeInvokeAndWait(new Runnable() {
            public void run() {
                //stopwatch.resetAndLog("wait for event queue");
                ResourceManager.instance();
                //stopwatch.resetAndLog("ResourceManager instance");
            }
        });
        //stopwatch.resetAndLog("come back from evt queue");
    }
    
    /** Starts any early core-related functionality. */
    private void startEarlyCore(SetupManager setupManager, LimeWireCore limeWireCore) {        
        // Add this running program to the Windows Firewall Exceptions list
        boolean inFirewallException = FirewallUtils.addToFirewall();
        //stopwatch.resetAndLog("add firewall exception");
        
        if(!inFirewallException && !setupManager.shouldShowFirewallWindow()) {
            limeWireCore.getLifecycleManager().loadBackgroundTasks();
            //stopwatch.resetAndLog("load background tasks");
        }
    }
    
    /** Switches from the AWT splash to the Swing splash. */
    private void switchSplashes(Frame awtSplash) {
        GUIMediator.safeInvokeAndWait(new Runnable() {
            public void run() {
                // Show the splash screen if we're not starting automatically on 
                // system startup
                if(!isStartup) {
                    SplashWindow.instance().begin();
                    //stopwatch.resetAndLog("begin splash window");
                }
            }
        });
        
        if(awtSplash != null) {
            awtSplash.dispose();
            //stopwatch.resetAndLog("dispose AWT splash");
        }
    }
    
    /** Initializes any early UI tasks, such as HTML loading & the Bug Manager. */
    private void initializeEarlyUI() {
        // Load up the HTML engine.
        GUIMediator.setSplashScreenString(I18n.tr("Loading HTML Engine..."));
        //stopwatch.resetAndLog("update splash for HTML engine");

        GUIMediator.safeInvokeAndWait(new Runnable() {
            public void run() {
                //stopwatch.resetAndLog("enter evt queue");

                JLabel label = new JLabel();
                // setting font and color to null to minimize generated css
                // script
                // which causes a parser exception under circumstances
                label.setFont(null);
                label.setForeground(null);
                BasicHTML.createHTMLView(label, "<html>.</html>");
                //stopwatch.resetAndLog("create HTML view");
            }
        });
        //stopwatch.resetAndLog("return from evt queue");

        // Initialize the bug manager
        BugManager.instance();
        //stopwatch.resetAndLog("BugManager instance");
    }
    
    /** Starts the SetupManager, if necessary. */
    private void startSetupManager(final SetupManager setupManager) {        
        // Run through the initialization sequence -- this must always be
        // called before GUIMediator constructs the LibraryTree!
        GUIMediator.safeInvokeAndWait(new Runnable() {
            public void run() {
                //stopwatch.resetAndLog("event evt queue");
                // Then create the setup manager if needed.
                setupManager.createIfNeeded();     
                //stopwatch.resetAndLog("create setupManager if needed");
            }
        });
        //stopwatch.resetAndLog("return from evt queue");
    }
    
    /** Ensures the save directory is valid. */
    private void validateSaveDirectory() {        
        // Make sure the save directory is valid.
        SaveDirectoryHandler.handleSaveDirectory();
        //stopwatch.resetAndLog("check save directory validity");
    }
    
    /** Loads the UI. */
    private void loadUI() {
        GUIMediator.setSplashScreenString(I18n.tr("Loading User Interface..."));
        stopwatch.resetAndLog("update splash for UI");

        // To prevent deadlocks, the GUI must be constructed in the Swing thread.
        // (Except on OS X, which is strange.)
        if (OSUtils.isMacOSX()) {
            GUIMediator.instance();
            stopwatch.resetAndLog("OSX GUIMediator instance");
        } else {
            GUIMediator.safeInvokeAndWait(new Runnable() {
                public void run() {
                    //stopwatch.resetAndLog("enter evt queue");
                    GUIMediator.instance();
                    //stopwatch.resetAndLog("GUImediator instance");
                }
            });
            //stopwatch.resetAndLog("return from evt queue");
        }
        
        GUIMediator.setSplashScreenString(I18n.tr("Loading Core Components..."));
        //stopwatch.resetAndLog("update splash for core");
    }
    
    /** Loads the system tray & other notifications. */
    private void loadTrayAndNotifications() {        
        // Create the user desktop notifier object.
        // This must be done before the GUI is made visible,
        // otherwise the user can close it and not see the
        // tray icon.
        GUIMediator.safeInvokeAndWait(new Runnable() {
                public void run() {
                    //stopwatch.resetAndLog("enter evt queue");
                    
                    NotifyUserProxy.instance();
                    //stopwatch.resetAndLog("NotifYUserProxy instance");
                    
                    if (!ApplicationSettings.DISPLAY_TRAY_ICON.getValue())
                        NotifyUserProxy.instance().hideTrayIcon();
                    
                    SettingsWarningManager.checkTemporaryDirectoryUsage();
                    SettingsWarningManager.checkSettingsLoadSaveFailure();
                    
                    //stopwatch.resetAndLog("end notify runner");
                }
        });
        //stopwatch.resetAndLog("return from evt queue");
    }
    
    /** Hides the splash screen and sets the UI for allowing viz. */
    private void hideSplashAndShowUI() {        
        // Hide the splash screen and recycle its memory.
        if(!isStartup) {
            SplashWindow.instance().dispose();
            stopwatch.resetAndLog("hide splash");
        }
        
        GUIMediator.allowVisibility();
        stopwatch.resetAndLog("allow viz");
        
        // Make the GUI visible.
        if(!isStartup) {
            GUIMediator.setAppVisible(true);
            stopwatch.resetAndLog("set app visible TRUE");
        } else {
            GUIMediator.startupHidden();
            stopwatch.resetAndLog("start hidden");
        }
    }
    
    /** Runs any late UI tasks, such as initializing Icons, I18n support. */
    private void loadLateTasksForUI() {        
        // Initialize IconManager.
        GUIMediator.setSplashScreenString(I18n.tr("Loading Icons..."));
        GUIMediator.safeInvokeAndWait(new Runnable() {
            public void run() {
                IconManager.instance();
            }
        });
        stopwatch.resetAndLog("IconManager instance");

        // Touch the I18N stuff to ensure it loads properly.
        GUIMediator.setSplashScreenString(I18n.tr("Loading Internationalization Support..."));
        I18NConvert.instance();
        stopwatch.resetAndLog("I18nConvert instance");
    }
    
    /** Sets up any listeners for the UI. */
    private void installListenersForUI(LimeWireCore limeWireCore) {        
        limeWireCore.getFileManager().addFileEventListener(new FileManagerWarningManager(NotifyUserProxy.instance()));
    }
    
    /** Starts the core. */
    private void startCore(LimeWireCore limeWireCore) {        
        // Start the backend threads.  Note that the GUI is not yet visible,
        // but it needs to be constructed at this point  
        limeWireCore.getLifecycleManager().start();
        stopwatch.resetAndLog("lifecycle manager start");
        
        if (!ConnectionSettings.DISABLE_UPNP.getValue()) {
            limeWireCore.getUPnPManager().start();
            stopwatch.resetAndLog("start UPnPManager");
        }
        
        // Instruct the gui to perform tasks that can only be performed
        // after the backend has been constructed.
        GUIMediator.instance().coreInitialized();        
        stopwatch.resetAndLog("core initialized");
    }
    
    /** Start Azureus Core. */
    private void startAzureusCore() {
    	BackgroundExecutorService.schedule(new Runnable() {
    		public void run() {
    			AzureusStarter.start();
    		}
    	});
    }
    
    /** Runs control requests that we queued early in initializing. */
    private void runQueuedRequests(LimeWireCore limeWireCore) {        
        // Activate a download for magnet URL locally if one exists
        limeWireCore.getExternalControl().runQueuedControlRequest();
        stopwatch.resetAndLog("run queued control req");
    }

    /** Starts DAAP. */
    private void startDAAP() {
        if (DaapSettings.DAAP_ENABLED.getValue()) {
            try {
                GUIMediator.setSplashScreenString(I18n.tr("Loading Digital Audio Access Protocol..."));
                DaapManager.instance().start();
                stopwatch.resetAndLog("daap start");
                DaapManager.instance().init();
                stopwatch.resetAndLog("daap init");
            } catch (IOException err) {
                GUIMediator.showError(I18n.tr("FrostWire was unable to start the Digital Audio Access Protocol Service (for sharing files in iTunes). This feature will be turned off. You can turn it back on in options, under iTunes -> Sharing."));
                DaapSettings.DAAP_ENABLED.setValue(false);
            }
        }
    }
    
    /** Runs post initialization tasks. */
    private void postinit() {
        
        // Tell the GUI that loading is all done.
        GUIMediator.instance().loadFinished();
        stopwatch.resetAndLog("load finished");
        
        // update the repaintInterval after the Splash is created,
        // so that the splash gets the smooth animation.
        if(OSUtils.isMacOSX())
            UIManager.put("ProgressBar.repaintInterval", new Integer(500));
        
        if(LOG.isTraceEnabled()) {
            long stopMemory = Runtime.getRuntime().totalMemory()
                            - Runtime.getRuntime().freeMemory();
            LOG.trace("STOP Initializer, using: " + stopMemory +
                      " memory, consumed: " + (stopMemory - startMemory));
        }
    }
    
    /**
     * Sets the startup property to be true.
     */
    void setStartup() {
        isStartup = true;
    }
    
    /** Fails because alpha expired. */
    private void failExpired() {
        fail(I18nMarker.marktr("This Alpha version has expired.  Press Ok to exit. "));
    }
    
    /** Fails because internet is blocked. */
    private void failInternetBlocked() {
        fail(I18nMarker
                .marktr("FrostWire was unable to initialize and start. This is usually due to a firewall program blocking FrostWire\'s access to the internet or loopback connections on the local machine. Please allow FrostWire access to the internet and restart FrostWire."));
    }
    
    /** Fails because preferences can't be set. */
    private void failPreferencesPermissions() {
        fail(I18nMarker
                .marktr("FrostWire could not create a temporary preferences folder.\n\nThis is generally caused by a lack of permissions.  Please make sure that FrostWire (and you) have access to create files/folders on your computer.  If the problem persists, please visit www.frostwire.com and click the 'Support' link.\n\nFrostWire will now exit.  Thank You."));
    }
    
    /** Shows a msg & fails. */
    private void fail(final String msgKey) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    JOptionPane.showMessageDialog(null, 
                            new MultiLineLabel(I18n.tr(msgKey), 400),
                            I18n.tr("Error"),
                            JOptionPane.ERROR_MESSAGE);
                }
            });
        } catch (InterruptedException ignored) {
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if(cause instanceof RuntimeException)
                throw (RuntimeException)cause;
            if(cause instanceof Error)
                throw (Error)cause;
            throw new RuntimeException(cause);
        }
        System.exit(1);
    }      
}
