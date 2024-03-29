package com.limegroup.gnutella.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.SettingsFactory;
import org.limewire.setting.StringSetting;

import com.limegroup.gnutella.util.FrostWireUtils;

/**
 * Handles installation preferences.
 */
public final class InstallSettings extends LimeWireSettings {

    private static final InstallSettings INSTANCE =
        new InstallSettings();
    private static final SettingsFactory FACTORY =
        INSTANCE.getFactory();

    public static InstallSettings instance() {
        return INSTANCE;
    }

    private InstallSettings() {
        super("installation.props", "FrostWire installs file");
    }
    
    /**
     * Whether or not the 'Choose your Save directory' question has
     * been asked.
     */
    public static final BooleanSetting SAVE_DIRECTORY =
        FACTORY.createBooleanSetting("SAVE_DIRECTORY", false);
    
    /**
     * Whether or not the 'Choose your speed' question has been asked.
     */
    public static final BooleanSetting SPEED =
        FACTORY.createBooleanSetting("SPEED", false);
    
    /**
     * Whether or not the 'Scan for files' question has been asked.
     */
    public static final BooleanSetting SCAN_FILES =
        FACTORY.createBooleanSetting("SCAN_FILES", false);
        
    /**
     * Whether or not the 'Start on startup' question has been asked.
     */
    public static final BooleanSetting START_STARTUP =
        FACTORY.createBooleanSetting("START_STARTUP", false);
        
    /**
     * Whether or not the 'Choose your language' question has been asked.
     */
    public static final BooleanSetting LANGUAGE_CHOICE =
        FACTORY.createBooleanSetting("LANGUAGE_CHOICE", false);
        
    /**
     * Whether or not the firewall warning question has been asked.
     */
    public static final BooleanSetting FIREWALL_WARNING =
        FACTORY.createBooleanSetting("FIREWALL_WARNING", false);
    
    /** Whether or not the filter question has been asked. */
    public static final BooleanSetting FILTER_OPTION =
        FACTORY.createBooleanSetting("FILTER_OPTION", false);
    
    /** Whether the association option has been asked */
    public static final IntSetting ASSOCIATION_OPTION =
    	FACTORY.createIntSetting("ASSOCIATION_OPTION", 1);
    
    /** Whether the association option has been asked */
    public static final BooleanSetting EXTENSION_OPTION =
        FACTORY.createBooleanSetting("EXTENSION_OPTION", false);
    
    public static final StringSetting LAST_FROSTWIRE_VERSION_WIZARD_INVOKED =
        FACTORY.createStringSetting("LAST_FROSTWIRE_VERSION_WIZARD_INVOKED", "");
}