package com.limegroup.gnutella.gui;

import com.limegroup.gnutella.gui.notify.Notification;
import com.limegroup.gnutella.gui.notify.NotifyUserProxy;
import com.limegroup.gnutella.util.FrostWireUtils;

/**
 * Provides methods to display notifications for common settings problems
 */
public class SettingsWarningManager {

    /**
     *  Warn about temporary directories 
     */
    public static void checkTemporaryDirectoryUsage() {
        
        if(FrostWireUtils.isTemporaryDirectoryInUse()) {
            NotifyUserProxy.instance().showMessage(
                    new Notification(I18n.tr(
                    "FrostWire was unable to create your settings folder and is using a temporary folder.  Your settings may be deleted when you close FrostWire. ")
                    ));
        }
        
    }
    
    
    /**
     * Warn about load/save problems
     */ 
    public static void checkSettingsLoadSaveFailure() { 

        String msg = null;
        
        if(FrostWireUtils.hasSettingsLoadSaveFailures()) {
            msg = I18n.tr("FrostWire has encountered problems in managing your settings.  Your settings changes may not be saved on shutdown.");
            FrostWireUtils.resetSettingsLoadSaveFailures();
        } else if (ResourceManager.hasLoadFailure()) {
            msg = I18n.tr("FrostWire has encountered problems in loading your settings.  FrostWire will attempt to use the default values; however, may behave unexpectedly.");
            ResourceManager.resetLoadFailure();
        }
        
        if (msg != null) {
            NotifyUserProxy.instance().showMessage(new Notification(msg));
        }
        
    }
    
}
