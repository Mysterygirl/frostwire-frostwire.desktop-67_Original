package com.limegroup.gnutella.util;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.limewire.inspection.InspectablePrimitive;
import org.limewire.setting.SettingsFactory;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.OSUtils;
import org.limewire.util.SystemUtils;
import org.limewire.util.SystemUtils.SpecialLocations;
import org.limewire.util.VersionUtils;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.settings.ApplicationSettings;


/**
 * This class handles common utility functions that many classes
 * may want to access.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class FrostWireUtils {

	/** 
	 * Constant for the current version of FrostWire.
	 */
    @InspectablePrimitive("limewire version")
	private static final String FROSTWIRE_VERSION = "4.21.8";
    
    /**
     * The current revision of the BitTorrent protocol implementation.
     * For an up-to-date mapping to LimeWire versions, check out
     * http://www.limewire.org/wiki/index.php?title=BitTorrentRevision
     */
    @InspectablePrimitive("bittorrent revision")
	public static final String BT_REVISION = "0002";

    /** True if this is a beta. */
    private static final boolean betaVersion = true;
    
    /** True if this is an alpha */
    private static final boolean alphaVersion = false;

    /**
     * The cached value of the major revision number.
     */
    private static final int _majorVersionNumber = 
        getMajorVersionNumberInternal(FROSTWIRE_VERSION);

    /**
     * The cached value of the minor revision number.
     */
    private static final int _minorVersionNumber = 
        getMinorVersionNumberInternal(FROSTWIRE_VERSION);
        
    /**
     * The cached value of the really minor version number.
     */
    private static final int _serviceVersionNumber =
        getServiceVersionNumberInternal(FROSTWIRE_VERSION);

    /**
     * The cached value of the GUESS major revision number.
     */
    private static final int _guessMajorVersionNumber = 0;

    /**
     * The cached value of the GUESS minor revision number.
     */
    private static final int _guessMinorVersionNumber = 1;

    /**
     * The cached value of the Ultrapeer major revision number.
     */
    private static final int _upMajorVersionNumber = 0;

    /**
     * The cached value of the Ultrapeer minor revision number.
     */
    private static final int _upMinorVersionNumber = 1;
    
    /**
     * The vendor code for QHD.  WARNING: to avoid character
     * encoding problems, this is hard-coded in QueryReply as well.  So if you
     * change this, you must change QueryReply.
     */
    public static final String QHD_VENDOR_NAME = "LIME";
     
	/**
	 * Cached constant for the HTTP Server: header value.
	 */
	private static final String HTTP_SERVER;

    public static final String LIMEWIRE_PREFS_DIR_NAME = ".frostwire4.20";
    
    public static final String FROSTWIRE_418_DIR_NAME = ".frostwire4.18";

    /**
     * Variable for whether or not this is a PRO version of LimeWire. 
     */
    @InspectablePrimitive("pro")
    private static boolean _isPro = true;

    /** Whether or not a temporary directory is in use. */
    private static boolean temporaryDirectoryInUse;

	/**
	 * Make sure the constructor can never be called.
	 */
	private FrostWireUtils() {}
    
	/**
	 * Initialize the settings statically. 
	 */
	static {
		if(!FROSTWIRE_VERSION.endsWith("Pro")) {
			HTTP_SERVER = "LimeWire/" + FROSTWIRE_VERSION;
		}
		else {
			HTTP_SERVER = ("LimeWire/"+FROSTWIRE_VERSION.
                           substring(0, FROSTWIRE_VERSION.length()-4)+" (Pro)");
            _isPro = true;
		}
	}
    
    /** Returns true if we're a beta. */
    public static boolean isBetaRelease() {
        return betaVersion;
    }
    
    public static boolean isAlphaRelease() {
        return alphaVersion;
    }
	
	/** Gets the major version of GUESS supported.
     */
    public static int getGUESSMajorVersionNumber() {    
        return _guessMajorVersionNumber;
    }
    
    /** Gets the minor version of GUESS supported.
     */
    public static int getGUESSMinorVersionNumber() {
        return _guessMinorVersionNumber;
    }

    /** Gets the major version of Ultrapeer Protocol supported.
     */
    public static int getUPMajorVersionNumber() {    
        return _upMajorVersionNumber;
    }
    
    /** Gets the minor version of Ultrapeer Protocol supported.
     */
    public static int getUPMinorVersionNumber() {
        return _upMinorVersionNumber;
    }

	/**
	 * Returns the current version number of LimeWire as
     * a string, e.g., "1.4".
	 */
	public static String getFrostWireVersion() {
        return FROSTWIRE_VERSION;
	}

    /** Gets the major version of LimeWire.
     */
    public static int getMajorVersionNumber() {    
        return _majorVersionNumber;
    }
    
    /** Gets the minor version of LimeWire.
     */
    public static int getMinorVersionNumber() {
        return _minorVersionNumber;
    }
    
    /** Gets the minor minor version of LimeWire.
     */
   public static int getServiceVersionNumber() {
        return _serviceVersionNumber;
   }
    

    static int getMajorVersionNumberInternal(String version) {
        if (!version.equals("@" + "version" + "@")) {
            try {
                int firstDot = version.indexOf(".");
                String majorStr = version.substring(0, firstDot);
                return new Integer(majorStr).intValue();
            }
            catch (NumberFormatException nfe) {
            }
        }
        // in case this is a mainline version or NFE was caught (strange)
        return 2;
    }

    /**
     * Accessor for whether or not this is LimeWire pro.
     *
     * @return <tt>true</tt> if it is pro, otherwise <tt>false</tt>
     */
    public static boolean isPro() {
        return _isPro;
    }
    
    /**
     * Accessor for whether or not this is a testing version
     * (@version@) of LimeWire.
     *
     * @return <tt>true</tt> if the version is @version@,
     *  otherwise <tt>false</tt>
     */
    public static boolean isTestingVersion() {
        return FROSTWIRE_VERSION.equals("@" + "version" + "@");
    }
    
    static int getMinorVersionNumberInternal(String version) {
        if (!version.equals("@" + "version" + "@")) {
            try {
                int firstDot = version.indexOf(".");
                String minusMajor = version.substring(firstDot+1);
                int secondDot = minusMajor.indexOf(".");
                String minorStr = minusMajor.substring(0, secondDot);
                return new Integer(minorStr).intValue();
            }
            catch (NumberFormatException nfe) {
            }
        }
        // in case this is a mainline version or NFE was caught (strange)
        return 7;
    }
    
    static int getServiceVersionNumberInternal(String version) {
        if (!version.equals("@" + "version" + "@")) {
            try {
                int firstDot = version.indexOf(".");
                int secondDot = version.indexOf(".", firstDot+1);
                
                int p = secondDot+1;
                int q = p;
                
                while(q < version.length() && 
                            Character.isDigit(version.charAt(q))) {
                    q++;
                }
                
                if (p != q) {
                    String service = version.substring(p, q);
                    return new Integer(service).intValue();
                }
            }
            catch (NumberFormatException nfe) {
            }
        }
        // in case this is a mainline version or NFE was caught (strange)
        return 0;
    }    

	/**
	 * Returns a version number appropriate for upload headers.
     * Same as '"LimeWire "+getLimeWireVersion'.
	 */
	public static String getVendor() {
		return "LimeWire " + FROSTWIRE_VERSION;
	}    

	/**
	 * Returns the string for the server that should be reported in the HTTP
	 * "Server: " tag.
	 * 
	 * @return the HTTP "Server: " header value
	 */
	public static String getHttpServer() {
		return HTTP_SERVER;
	}

    /** Returns a temporary directory that can be used for settings. */
    public static File getTemporarySettingsDirectory() throws IOException {
        File tempDir = FileUtils.createTempFile("frostwire4.18", "-temp").getAbsoluteFile();
        File tempDirParent = tempDir.getParentFile();
        tempDir.delete();
        if(!tempDir.exists()) {
            if(tempDir.mkdir()) {
                if(tempDir.exists() && tempDir.isDirectory()) {
                    return tempDir;
                }
            }
        }
        
        // If we couldn't convert a temporary file into a temporary directory...
        for(int i = 0; i < 1000; i++) {
            tempDir = new File(tempDirParent, "frostwire4.18-tempdir-" + i);
            if(!tempDir.exists()) {
                if(tempDir.mkdir()) {
                    if(tempDir.exists() && tempDir.isDirectory()) {
                        return tempDir;
                    }
                }
            }
        }
        
        throw new IOException("temporary directory failed.  parent [" + tempDirParent + "]");
    }
    
    /** Clears all potential temporary LW directories. */
    public static void clearTemporarySettingsDirectories() {
        File tempDir;
        try {
            tempDir = FileUtils.createTempFile("frostwire4.18", "-temp").getAbsoluteFile();
        } catch(IOException failure) {
            return; // can't do much from here.
        }
        
        File tempDirParent = tempDir.getParentFile();
        tempDir.delete();
        
        for(int i = 0; i < 1000; i++) {
            File dir = new File(tempDirParent, "frostwire4.18-tempdir-" + i);
            // If we can't delete it immediately, try deleting all contents first.
            if(!dir.delete())
                FileUtils.deleteRecursive(dir);
        }
        
    }
      
    /**
     * Returns the location where the user settings directory should be placed.
     */
    public static File getRequestedUserSettingsLocation() {
        // LOGIC:
        
        // On all platforms other than Windows or OSX,
        // this will return <user-home>/.frostwire<versionMajor.versionMinor>
        
        // On OSX, this will return <user-home>/Library/Preferences/LimeWire
        
        // On Windows, this first tries to find:
        // a) <user-home>/$LIMEWIRE_PREFS_DIR/.frostwire
        // b) <user-home>/$APPDATA/FrostWire
        // c) <user-home/.frostwire
        // If the $LIMEWIRE_PREFS_DIR variable doesn't exist, it falls back
        // to trying b).  If The $APPDATA variable can't be read or doesn't
        // exist, it falls back to a).
        // If using a) or b), and neither of those directories exist, but c)
        // does, then c) is used.  Once a) or b) exist, they are used indefinitely.
        // If neither a), b) nor c) exist, then the former is created in preference of
        // of a), then b).        
        File userDir = CommonUtils.getUserHomeDir();

        // Changing permissions without permission in Unix is rude
        if(!OSUtils.isPOSIX() && userDir != null && userDir.exists())
            FileUtils.setWriteable(userDir);
        
        File settingsDir = new File(userDir, LIMEWIRE_PREFS_DIR_NAME);

        if (OSUtils.isWindows()) {
        	
            String appdata = System.getProperty("LIMEWIRE_PREFS_DIR", SystemUtils.getSpecialPath(SpecialLocations.APPLICATION_DATA));

            if (appdata != null && appdata.length() > 0) {
                appdata = stripQuotes(appdata);
                File tempSettingsDir = new File(appdata, "FrostWire");
                if (tempSettingsDir.isDirectory() || !settingsDir.exists()) {
                    FileUtils.setWriteable(new File(appdata));
                    try {
                        CommonUtils.validateSettingsDirectory(tempSettingsDir);
                        return tempSettingsDir;
                    } catch (IOException e) { // Ignore errors and fall back on default
                    } catch (SecurityException e) {} // Ignore errors and fall back on default
                }
            }
        } else if(OSUtils.isMacOSX()) {
            settingsDir = new File(CommonUtils.getUserHomeDir(), "Library/Preferences/FrostWire");
        } 
      
        return settingsDir;
    }
    
    /** Strips out any quotes that we left on the data. */
    private static String stripQuotes(String incoming) {
        if (incoming == null || incoming.length() <= 2)
            return incoming;

        incoming = incoming.trim();
        if (incoming.startsWith("\""))
            incoming = incoming.substring(1);
        if (incoming.endsWith("\""))
            incoming = incoming.substring(0, incoming.length() - 1);
        return incoming;
    }
    
    /**
     * Updates a URL to contain common information about the LW installation.
     */
    public static String addLWInfoToUrl(String url, byte[] myClientGUID) {
        if(url.indexOf('?') == -1)
            url += "?";
        else
            url += "&";
        url += "guid=" + EncodingUtils.encode(new GUID(myClientGUID).toHexString())+ 
            "&pro="   + FrostWireUtils.isPro() + 
            "&lang=" + EncodingUtils.encode(ApplicationSettings.getLanguage()) +
            "&lv="   + EncodingUtils.encode(FrostWireUtils.getFrostWireVersion()) +
            "&jv="   + EncodingUtils.encode(VersionUtils.getJavaVersion()) +
            "&os="   + EncodingUtils.encode(OSUtils.getOS()) +
            "&osv="  + EncodingUtils.encode(OSUtils.getOSVersion());
               
        return url;
    }

    /** Returns whether or not a temporary directory is in use. */
    public static boolean isTemporaryDirectoryInUse() {
        return temporaryDirectoryInUse;
    }
    
    /** Returns whether or not failures were encountered in load/save settings on startup. */
    public static boolean hasSettingsLoadSaveFailures() {
        return SettingsFactory.hasLoadSaveFailure();
    }


    /** Sets whether or not a temporary directory is in use. */
    public static void setTemporaryDirectoryInUse(boolean inUse) {
        temporaryDirectoryInUse = inUse;
    }
    
    public static void resetSettingsLoadSaveFailures() {
        SettingsFactory.resetLoadSaveFailure();
    }
    
    /**
     * Returns the root folder from which all Saved/Shared/etc..
     * folders should be placed.
     */
    public static File getLimeWireRootFolder() {
        String root = null;
        
        if (OSUtils.isWindowsVista() || OSUtils.isWindows7()) {
        	root = SystemUtils.getSpecialPath(SpecialLocations.DOWNLOADS);
        } else if(OSUtils.isWindows()) {
            root = SystemUtils.getSpecialPath(SpecialLocations.DOCUMENTS);
        }
        
        if(root == null || "".equals(root))
            root = CommonUtils.getUserHomeDir().getPath();
        
        return new File(root, "FrostWire");
    }
    
    public static Tagged<String> getArg(Map<String, String> args, String name, String action) {
        String res = args.get(name);
        if (res == null || res.equals("")) {
            //String detail = "Invalid '" + name + "' while " + action;                                                                       
            return new Tagged<String>("missing.callback.parameter", false);
        }
        String result = res;
        try {
            result = URLDecoder.decode(res);
        } catch (IOException e) {
            // no the end of the world                                                                                                        
        }
        return new Tagged<String>(result, true);
    }
    
}



