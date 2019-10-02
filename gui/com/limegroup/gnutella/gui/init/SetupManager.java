package com.limegroup.gnutella.gui.init;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;

import org.limewire.i18n.I18nMarker;
import org.limewire.setting.FileSetting;
import org.limewire.setting.SettingsGroupManager;
import org.limewire.util.CommonUtils;
import org.limewire.util.OSUtils;

import com.limegroup.gnutella.gui.ButtonRow;
import com.limegroup.gnutella.gui.FirewallUtils;
import com.limegroup.gnutella.gui.FramedDialog;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.Line;
import com.limegroup.gnutella.gui.SplashWindow;
import com.limegroup.gnutella.gui.shell.LimeAssociations;
import com.limegroup.gnutella.gui.util.BackgroundExecutorService;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.InstallSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.util.FrostWireUtils;

/**
 * This class manages the setup wizard.  It constructs all of the primary
 * classes and acts as the mediator between the various objects in the
 * setup windows.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

public class SetupManager {	

	/**
	 * the dialog window that holds all other gui elements for the setup.
	 */
	private FramedDialog dialogFrame;

	/** 
	 * the holder for the setup windows 
	 */
	private SetupWindowHolder _setupWindowHolder;

	/**
	 * holder for the current setup window.
	 */
	private SetupWindow _currentWindow;

	public static final int ACTION_PREVIOUS = 1;
	
	public static final int ACTION_NEXT = 2;
	
	public static final int ACTION_FINISH = 4;
	
	public static final int ACTION_CANCEL = 8;
	
	private PreviousAction previousAction = new PreviousAction();
	
	private NextAction nextAction = new NextAction();
	
	private FinishAction finishAction = new FinishAction();
	
	private CancelAction cancelAction = new CancelAction();
	
	private LanguageAwareAction[] actions = new LanguageAwareAction[] {
			previousAction, nextAction, finishAction, cancelAction
	};

    private List<SetupWindow> windows;
	    
    /**
     * Determines if the 'a firewall warning may be displayed' window should be shown.
     */
    public boolean shouldShowFirewallWindow() {
        if(InstallSettings.FIREWALL_WARNING.getValue())
            return false;

        // Only show the firewall warning if this is windows, and if
        // we're not capable of automatically changing the firewall.
        return OSUtils.isWindows() && !FirewallUtils.isStatuschangeCapable();
    }
    
    private boolean shouldShowAssociationsWindow() {
    	if (InstallSettings.ASSOCIATION_OPTION.getValue() == LimeAssociations.CURRENT_ASSOCIATIONS)
            return false;
    	
    	// display a window if silent grab failed. 
    	return !GUIMediator.getAssociationManager().checkAndGrab(false);
    }
    
    private static enum SaveStatus { NO, NEEDS, MIGRATE };
    private SaveStatus shouldShowSaveDirectoryWindow() {
        // If it's not setup, definitely show it!
        if(!InstallSettings.SAVE_DIRECTORY.getValue())
            return SaveStatus.NEEDS;
        
        // Otherwise, if it has been setup, it might need
        // additional tweaking because defaults have changed,
        // and we want to move the save directory to somewhere
        // else.
        FileSetting saveSetting = SharingSettings.DIRECTORY_FOR_SAVING_FILES;
        if(saveSetting.isDefault()) {
            // If the directory is default, it could be because older versions
            // of LW didn't write out their save directory (if it was default).
            // Check to see if the new one doesn't exist, but the old one does.
            File oldDefaultDir = new File(CommonUtils.getUserHomeDir(), "Shared");
            if(!saveSetting.getValue().exists() && oldDefaultDir.exists())
                return SaveStatus.MIGRATE;
        }
        
        
        if (!InstallSettings.LAST_FROSTWIRE_VERSION_WIZARD_INVOKED.getValue().equals(FrostWireUtils.getFrostWireVersion())) {
            return SaveStatus.NEEDS;
        }
        
        return SaveStatus.NO;
    }

    /**
     * Constructs the appropriate setup windows if needed.
     */
    public void createIfNeeded() {
        _setupWindowHolder = new SetupWindowHolder();
        
        windows = new LinkedList<SetupWindow>();

        SaveStatus saveDirectoryStatus = shouldShowSaveDirectoryWindow();
        if(saveDirectoryStatus != SaveStatus.NO)
            windows.add(new SaveWindow(this, saveDirectoryStatus == SaveStatus.MIGRATE));
            
        if( !InstallSettings.SPEED.getValue() ||
            !InstallSettings.START_STARTUP.getValue() && GUIUtils.shouldShowStartOnStartupWindow()) //FTA removed in FrostWire  ||             !InstallSettings.FILTER_OPTION.getValue() 
            windows.add(new MiscWindow(this));
        
        if( shouldShowFirewallWindow() ) {
            windows.add(new FirewallWindow(this));
        }
        
        if (shouldShowAssociationsWindow()) {
        	windows.add(new AssociationsWindow(this));
        }

        if( !InstallSettings.EXTENSION_OPTION.getValue())
            windows.add(new FileTypeWindow(this));        

        //THIS HAS TO GO LAST
        IntentWindow intentWindow = new IntentWindow(this);
        if(!intentWindow.isConfirmedWillNot())
        	windows.add(intentWindow);        
        
        // Nothing to install?.. Begone.
        if( windows.size() == 0 )
            return;
            
        // If the INSTALLED value is set, that means that a previous
        // installer has already been run.
        boolean partial = ApplicationSettings.INSTALLED.getValue();
        
        // We need to ask the user's language very very first,
        // so make sure that if the LanguageWindow is the first item,
        // that the WelcomeWindow is inserted second.
        // It's a little more tricky than that, though, because
        // it could be possible that the LanguageWindow was the only
        // item to be installed -- if that's the case, don't even
        // insert the WelcomeWindow & FinishWindow at all.
        if(partial && !(windows.size() == 1 && windows.get(0) instanceof IntentWindow))
            windows.add(0, new WelcomeWindow(this, partial));
        
        // Iterate through each displayed window and set them up correctly.
        SetupWindow prior = null;
        for(SetupWindow current : windows) {
            _setupWindowHolder.add(current);
            
            if (prior == null)
            	current.setPrevious(current);
            else
            	current.setPrevious(prior);
                
            if(prior != null)
                prior.setNext(current);
            
            prior = current;
        }
        assert prior != null;
        prior.setNext(prior);        
		
		// Actually display the setup dialog.
		createDialog(windows.get(0));
    }
    
    /*
	 * Creates the main <tt>JDialog</tt> instance and
	 * creates all of the setup window classes, buttons, etc.
	 */
	private void createDialog(SetupWindow firstWindow) {
		
        dialogFrame = new FramedDialog();
        dialogFrame.setTitle("FrostWire Setup");
        dialogFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                cancelSetup();
            }
        });

        JDialog dialog = dialogFrame.getDialog();
        dialog.setModal(true);
        dialog.setTitle(I18n.tr("FrostWire Setup Wizard"));
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancelSetup();
            }
        });

        // set the layout of the content pane
        Container container = dialog.getContentPane();
        GUIUtils.addHideAction((JComponent) container);
        BoxLayout containerLayout = new BoxLayout(container, BoxLayout.Y_AXIS);
        container.setLayout(containerLayout);

        // create the main panel
        JPanel setupPanel = new JPanel();
        setupPanel.setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 0));
        BoxLayout layout = new BoxLayout(setupPanel, BoxLayout.Y_AXIS);
        setupPanel.setLayout(layout);

        Dimension d = new Dimension(SetupWindow.SETUP_WIDTH, SetupWindow.SETUP_HEIGHT);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        dialog.setLocation((screenSize.width - d.width) / 2, (screenSize.height - d.height) / 2);

        // create the setup buttons panel
        setupPanel.add(_setupWindowHolder);
        setupPanel.add(Box.createVerticalStrut(17));

        JPanel bottomRow = new JPanel();
        bottomRow.setLayout(new BoxLayout(bottomRow, BoxLayout.X_AXIS));
        ButtonRow buttons = new ButtonRow(actions, ButtonRow.X_AXIS, ButtonRow.LEFT_GLUE);
        LanguagePanel languagePanel = new LanguagePanel(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateLanguage();
            }
        });
        bottomRow.add(languagePanel);
        bottomRow.add(Box.createHorizontalGlue());
        bottomRow.add(buttons);
        bottomRow.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        setupPanel.add(new Line());
        setupPanel.add(bottomRow);

        show(firstWindow);

        // add the panel and make it visible
        container.add(setupPanel);

        ((JComponent) container).setPreferredSize(new Dimension(SetupWindow.SETUP_WIDTH,
                SetupWindow.SETUP_HEIGHT));
        dialog.pack();

        SplashWindow.instance().setVisible(false);
        dialogFrame.showDialog();
        SplashWindow.instance().setVisible(true);
	}
   
	/**
     * Enables the bitmask of specified actions, the other actions are
     * explicitly disabled.
     * <p>
     * To enable finish and previous you would call
     * {@link #enableActions(int) enableActions(SetupManager.ACTION_FINISH|SetupManager.ACTION_PREVIOUS)}.
     * 
     * @param actions
     */
	public void enableActions(int actions) {
		previousAction.setEnabled((actions & ACTION_PREVIOUS) != 0);
		nextAction.setEnabled((actions & ACTION_NEXT) != 0);
		finishAction.setEnabled((actions & ACTION_FINISH) != 0);
		cancelAction.setEnabled((actions & ACTION_CANCEL) != 0);
	}
	
	public int getEnabledActions() {
		int actions = 0;
		if (previousAction.isEnabled()) {
			actions |= ACTION_PREVIOUS;
		}
		if (nextAction.isEnabled()) {
			actions |= ACTION_NEXT;
		}
		if (finishAction.isEnabled()) {
			actions |= ACTION_FINISH;
		}
		if (cancelAction.isEnabled()) {
			actions |= ACTION_CANCEL;
		}
		return actions;
	}
	
	public Component getOwnerComponent() {
	    return this.dialogFrame;
	}
	
	/**
	 * Displays the next window in the setup sequence.
	 */
	public void next() {
		SetupWindow newWindow = _currentWindow.getNext();
		try {			
			_currentWindow.applySettings(true);
			show(newWindow);
		} catch(ApplySettingsException ase) {
			// there was a problem applying the settings from
			// the current window, so display the error message 
			// to the user.
            if (ase.getMessage() != null && ase.getMessage().length() > 0)
                GUIMediator.showError(ase.getMessage());			
		}
	}

	/**
	 * Displays the previous window in the setup sequence.
	 */
	public void previous() {
		SetupWindow newWindow = _currentWindow.getPrevious();
        try {           
            _currentWindow.applySettings(false);
            show(newWindow);
        } catch(ApplySettingsException ase) {
            // ignore errors when going backwards
        }
	}

	
	/**
	 * Cancels the setup.
	 */
	public void cancelSetup() {
		dialogFrame.getDialog().dispose();
		System.exit(0);
	}

	/**
	 * Completes the setup.
	 */
	public void finishSetup() {
	    
	    if (_currentWindow != null) {
	        try {
                _currentWindow.applySettings(true);
            } catch (ApplySettingsException e) {
                // there was a problem applying the settings from
                // the current window, so display the error message 
                // to the user.
                if (e.getMessage() != null && e.getMessage().length() > 0) {
                    GUIMediator.showError(e.getMessage());
                }
                return;
            }
	    }
	    
		dialogFrame.getDialog().dispose();
		
		ApplicationSettings.INSTALLED.setValue(true);

        InstallSettings.SAVE_DIRECTORY.setValue(true);
        InstallSettings.SPEED.setValue(true);
        InstallSettings.SCAN_FILES.setValue(true);
        InstallSettings.LANGUAGE_CHOICE.setValue(true);
        InstallSettings.FILTER_OPTION.setValue(true);
        InstallSettings.EXTENSION_OPTION.setValue(true);

        if (GUIUtils.shouldShowStartOnStartupWindow())
            InstallSettings.START_STARTUP.setValue(true);
        if (OSUtils.isWindows())
            InstallSettings.FIREWALL_WARNING.setValue(true);
        InstallSettings.ASSOCIATION_OPTION.setValue(LimeAssociations.CURRENT_ASSOCIATIONS);
        
        InstallSettings.LAST_FROSTWIRE_VERSION_WIZARD_INVOKED.setValue(FrostWireUtils.getFrostWireVersion());
		
        Future<Void> future = BackgroundExecutorService.submit(new Callable<Void>() {
     		public Void call() {
                SettingsGroupManager.instance().save();
                return null;
            }
        });
        
        if(_currentWindow instanceof IntentWindow) {
        	IntentWindow intent = (IntentWindow)_currentWindow;
        	if(!intent.isConfirmedWillNot()) {
        		GUIMediator.showWarning("FrostWire is not distributed to people who intend to use it for the purposes of copyright infringement.\n\nThank you for your interest; however, you cannot continue to use FrostWire at this time.");
        		try {
        			future.get();
        		} catch(Exception ignored) {}
        		System.exit(1);
        	}
        	intent.applySettings(true);
        }

        dialogFrame.getDialog().dispose();

        return;       
	}
	
	/**
	 * Instructs the buttons to redo their text.
	 */
	public void updateLanguage() {
        for (int i = 0; i < actions.length; i++) {
			actions[i].updateLanguage();
		}
        try {
            _currentWindow.applySettings(false);
        } catch(ApplySettingsException ignored) {}
        _currentWindow.handleWindowOpeningEvent();
	}

	/**
	 * Show the specified window
	 */
	private void show(SetupWindow window) {
        window.handleWindowOpeningEvent();	    
		_setupWindowHolder.show(window.getKey());
		_currentWindow = window;
	}
	
	void add(SetupWindow window) {
		_setupWindowHolder.add(window, window.getKey());
	}

	private abstract class LanguageAwareAction extends AbstractAction {
		
		private final String nameKey;
		
		public LanguageAwareAction(String nameKey) {
			super(I18n.tr(nameKey));
			this.nameKey = nameKey;
		}
		
		public void updateLanguage() {
			putValue(Action.NAME, I18n.tr(nameKey));
		}
	}
	
	private class CancelAction extends LanguageAwareAction {

		public CancelAction() {
			super(I18nMarker.marktr("Cancel"));
		}

		public void actionPerformed(ActionEvent e) {
			cancelSetup();
		}
	}
	
	private class NextAction extends LanguageAwareAction {
		
		public NextAction() {
			super(I18nMarker.marktr("Next >>"));
		}

		public void actionPerformed(ActionEvent e) {
			next();
		}
	}
	
	private class PreviousAction extends LanguageAwareAction {

		public PreviousAction() {
			super(I18nMarker.marktr("<< Back"));
		}
		
		public void actionPerformed(ActionEvent e) {
			previous();
		}
	}
	
	private class FinishAction extends LanguageAwareAction {
		
		public FinishAction() {
			super(I18nMarker.marktr("Finish"));
		}

		public void actionPerformed(ActionEvent e) {
			finishSetup();
		}
		
	}
}