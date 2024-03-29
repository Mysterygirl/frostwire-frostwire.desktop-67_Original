package com.limegroup.gnutella.settings;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.FileSetSetting;
import org.limewire.setting.FileSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.Setting;
import org.limewire.setting.StringArraySetting;
import org.limewire.setting.StringSetting;
import org.limewire.util.FileUtils;

import com.frostwire.CoreFrostWireUtils;
import com.limegroup.gnutella.MediaType;
import com.limegroup.gnutella.util.FrostWireUtils;


/**
 * Settings for sharing
 */
public class SharingSettings extends LimeProps {
      
    private SharingSettings() {}

	/**
	 * Stores the download directory file settings for each media type by its
	 * description key {@link MediaType#getDescriptionKey()}. The settings
	 * are loaded lazily during the first request.
	 */
	private static final Hashtable<String, FileSetting> downloadDirsByDescription =
        new Hashtable<String, FileSetting>();
	
    
    public static final File DEFAULT_SAVE_DIR =
        new File(FrostWireUtils.getLimeWireRootFolder(), "Saved");
    
    public static final File DEFAULT_SHARE_DIR = 
        new File(FrostWireUtils.getLimeWireRootFolder(), "Shared");
    
    public static final File DEFAULT_SHARED_TORRENTS_DIR = 
    	new File(FrostWireUtils.getLimeWireRootFolder(), "Torrents");
    
    /**
     * The default folder where Torrent Data will be saved. This folder CANNOT BE SHARED
     * to avoid sharing inconsistencies. 
     */
    public static final File DEFAULT_TORRENT_DATA_DIR =
    	new File(FrostWireUtils.getLimeWireRootFolder(), "Torrent Data");

    /**
     * The folder value where Torrent Data will be saved. This folder CANNOT BE SHARED
     * to avoid sharing inconsistencies. 
     */
    public static final FileSetting TORRENT_DATA_DIR_SETTING = 
    	FACTORY.createFileSetting("DEFAULT_TORRENT_DATA_DIR_SETTING", 
    			DEFAULT_TORRENT_DATA_DIR).setAlwaysSave(true);
    
    public static final BooleanSetting SEED_FINISHED_TORRENTS =
    	FACTORY.createBooleanSetting("SEED_FINISHED_TORRENTS", true);
    
    public static final File IMAGE_CACHE_DIR = 
        new File(CoreFrostWireUtils.getPreferencesFolder(), "image_cache");
    
    /**
     * Whether or not we're going to add an alternate for ourselves
     * to our shared files.  Primarily set to false for testing.
     */
    public static final BooleanSetting ADD_ALTERNATE_FOR_SELF =
        FACTORY.createBooleanSetting("ADD_ALTERNATE_FOR_SELF", true);

    /**
     * The directory for saving files.
     */
    public static final FileSetting DIRECTORY_FOR_SAVING_FILES = 
        FACTORY.createFileSetting("DIRECTORY_FOR_SAVING_FILES", 
            DEFAULT_SAVE_DIR).setAlwaysSave(true);
    
    /**
     * The directory where incomplete files are stored (downloads in progress).
     */
    public static final FileSetting INCOMPLETE_DIRECTORY =
        FACTORY.createFileSetting("INCOMPLETE_DIRECTORY", 
            (new File(DIRECTORY_FOR_SAVING_FILES.getValue().getParent(),
                "Incomplete")));
    
    /**
	 * A file with a snapshot of current downloading files.
	 */                
    public static final FileSetting OLD_DOWNLOAD_SNAPSHOT_FILE =
        FACTORY.createFileSetting("DOWNLOAD_SNAPSHOT_FILE", 
            (new File(INCOMPLETE_DIRECTORY.getValue(), "downloads.dat")));
            
    /**
	 * A file with a snapshot of current downloading files.
	 */                
    public static final FileSetting OLD_DOWNLOAD_SNAPSHOT_BACKUP_FILE =
        FACTORY.createFileSetting("DOWNLOAD_SNAPSHOT_BACKUP_FILE", 
            (new File(INCOMPLETE_DIRECTORY.getValue(), "downloads.bak")));            
    
    /** The minimum age in days for which incomplete files will be deleted.
     *  This values may be zero or negative; doing so will cause LimeWire to
     *  delete ALL incomplete files on startup. */   
    public static final IntSetting INCOMPLETE_PURGE_TIME =
        FACTORY.createIntSetting("INCOMPLETE_PURGE_TIME", 7);
    
    /**
     * The time, in days, after which .torrent meta data files are deleted.
     */
    public static final IntSetting TORRENT_METADATA_PURGE_TIME = 
        FACTORY.createIntSetting("TORRENT_METADATA_PURGE_TIME", 7);
    
    /**
     * Specifies whether or not completed downloads
     * should automatically be cleared from the download window.
     */    
    public static final BooleanSetting CLEAR_DOWNLOAD =
        FACTORY.createBooleanSetting("CLEAR_DOWNLOAD", false);
    
    /**
     * Initial directory for open desktop explorer.
     */
    public static final FileSetting DIRECTORY_FOR_OPEN_DESKTOP_EXPLORER = 
        FACTORY.createFileSetting("DIRECTORY_FOR_OPEN_DESKTOP_EXPLORER", DEFAULT_SAVE_DIR).setAlwaysSave(true);
        
    
    /**
     * Helper method left from SettingsManager.
     *
	 * Sets the directory for saving files.
	 *
     * <p><b>Modifies:</b> DIRECTORY_FOR_SAVING_FILES, INCOMPLETE_DIRECTORY, 
     *                     DOWNLOAD_SNAPSHOT_FILE</p>
     *
	 * @param   saveDir  A <tt>File</tt> instance denoting the
	 *                   abstract pathname of the directory for
	 *                   saving files.
	 *
	 * @throws  <tt>IOException</tt>
	 *          If the directory denoted by the directory pathname
	 *          String parameter did not exist prior to this method
	 *          call and could not be created, or if the canonical
	 *          path could not be retrieved from the file system.
	 *
	 * @throws  <tt>NullPointerException</tt>
	 *          If the "dir" parameter is null.
	 */
    public static final void setSaveDirectory(File saveDir) throws IOException {
		if(saveDir == null) throw new NullPointerException();
		if(!saveDir.isDirectory()) {
			if(!saveDir.mkdirs()) throw new IOException("could not create save dir");
		}
        
		String parentDir = saveDir.getParent();
		File incDir = new File(parentDir, "Incomplete");
		if(!incDir.isDirectory()) {
			if(!incDir.mkdirs()) throw new IOException("could not create incomplete dir");
		}
        
        FileUtils.setWriteable(saveDir);
        FileUtils.setWriteable(incDir);

		if(!saveDir.canRead() || !saveDir.canWrite() ||
		   !incDir.canRead()  || !incDir.canWrite()) {
			throw new IOException("could not write to selected directory");
		}
		
		// Canonicalize the files ... 
		try {
		    saveDir = FileUtils.getCanonicalFile(saveDir);
		} catch(IOException ignored) {}
		try {
		    incDir = FileUtils.getCanonicalFile(incDir);
		} catch(IOException ignored) {}
		File snapFile = new File(incDir, "downloads.dat");
		try {
		    snapFile = FileUtils.getCanonicalFile(snapFile);
		} catch(IOException ignored) {}
		File snapBackup = new File(incDir, "downloads.bak");
		try {
		    snapBackup = FileUtils.getCanonicalFile(snapBackup);
		} catch(IOException ignored) {}
		
        DIRECTORY_FOR_SAVING_FILES.setValue(saveDir);
        INCOMPLETE_DIRECTORY.setValue(incDir);
        OLD_DOWNLOAD_SNAPSHOT_FILE.setValue(snapFile);
        OLD_DOWNLOAD_SNAPSHOT_BACKUP_FILE.setValue(snapBackup);
    }
    
    /**
     * Retrieves the default save directory for a given file name.
     * Getting the save directory for null will result in the
     * default save directory.
     */
    public static final File getSaveDirectory(String fileName) {
    	if (fileName == null) {
    		return DIRECTORY_FOR_SAVING_FILES.getValue();
    	}
    	String extension = FileUtils.getFileExtension(fileName);
    	if (extension == null) {
    		return DIRECTORY_FOR_SAVING_FILES.getValue();
    	}
        MediaType type = MediaType.getMediaTypeForExtension(extension);
    	if (type == null)
    		return DIRECTORY_FOR_SAVING_FILES.getValue();	
    	FileSetting fs = getFileSettingForMediaType(type);
    	if (fs.isDefault()) {
    		return DIRECTORY_FOR_SAVING_FILES.getValue();
    	}
    	return fs.getValue();
    }

    public static final File getSaveDirectory() { 
    	return DIRECTORY_FOR_SAVING_FILES.getValue();
    }
    
    /**  
      * Gets all potential save directories.  
     */  
    public static final Set<File> getAllSaveDirectories() {  
        Set<File> set = new HashSet<File>(7);  
        set.add(getSaveDirectory());  
        synchronized(downloadDirsByDescription) {  
            for(FileSetting next : downloadDirsByDescription.values())  {
                set.add(next.getValue());
            }
        }  
        return set;  
    }
    
    public static final File getImageCacheDirectory() {
        if (!IMAGE_CACHE_DIR.exists()) {
            IMAGE_CACHE_DIR.mkdirs();
        }
        return IMAGE_CACHE_DIR;
    }
    
    /**
     * Default file extensions.
     */
    private static final String DEFAULT_EXTENSIONS_TO_SHARE =
		"asx;html;htm;xml;txt;pdf;ps;rtf;doc;tex;mp3;mp4;wav;wax;aif;aiff;"+
		"ra;ram;wm;wmv;mp2v;mlv;mpa;mpv2;mid;midi;rmi;aifc;snd;flac;fla;"+
		"mpg;mpeg;asf;qt;mov;avi;mpe;swf;dcr;gif;jpg;jpeg;jpe;png;tif;tiff;"+
		"exe;zip;gz;gzip;hqx;tar;tgz;z;rmj;lqt;rar;ace;sit;smi;img;ogg;rm;"+
		"bin;dmg;jve;nsv;med;mod;7z;iso;fwtp;lwtp;pmf;m4a;bz2;sea;pf;arc;arj;"+
		"bz;tbz;mime;taz;ua;toast;lit;rpm;deb;pkg;sxw;l6t;srt;sub;torrent;idx;mkv;"+
		"ogm;shn;dvi;rmvp;kar;cdg;ccd;cue;c;h;m;java;jar;pl;py;pyc;"+
		"pyo;pyz";
    
    /**
     * Default disabled extensions.
     */
    private static final String DEFAULT_EXTENSIONS_TO_DISABLE =
        "au;doc;pdf;xls;rtf;bak;csv;dat;docx;xlsx;xlam;xltx;xltm;xlsm;xlsb;dotm;docm;dotx;dot;qdf;qtx;qph;qel;qdb;qsd;qif;mbf;mny;wma";
        
    
    /**
     * The list of extensions shared by default
     */
    public static final String[] getDefaultExtensions() {
        return StringArraySetting.decode(DEFAULT_EXTENSIONS_TO_SHARE); 
    }
    
    /**
     * The list of extensions shared by default
     */
    public static final String getDefaultExtensionsAsString() {
        return DEFAULT_EXTENSIONS_TO_SHARE; 
    }
    
    /**
     * The list of extensions disabled by default in the file types sharing screen
     */
    public static final String[] getDefaultDisabledExtensions() {
        return StringArraySetting.decode(DEFAULT_EXTENSIONS_TO_DISABLE); 
    }
    
    /**
     * The list of extensions disabled by default in the file types sharing screen
     */
    public static final String getDefaultDisabledExtensionsAsString() {
        return DEFAULT_EXTENSIONS_TO_DISABLE; 
    }
    
    
    /**
	 * The shared directories. 
	 */
    public static final FileSetSetting DIRECTORIES_TO_SHARE =
        FACTORY.createFileSetSetting("DIRECTORIES_TO_SEARCH_FOR_FILES", new File[0]);

    /**
     * Whether or not to auto-share files when using 'Download As'.
     */
	public static final BooleanSetting SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES =
		FACTORY.createBooleanSetting("SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES", false);
    
    /**
     * Whether or not to auto-share .torrent files.
     */
    public static final BooleanSetting SHARE_TORRENT_META_FILES =
        FACTORY.createBooleanSetting("SHARE_TORRENT_META_FILES", true);
    
    /**
     * Whether or not to show .torrent directory in Library.
     */
    public static final BooleanSetting SHOW_TORRENT_META_FILES =
        FACTORY.createBooleanSetting("SHOW_TORRENT_META_FILES", true); 
    
    /**
	 * File extensions that are shared.
	 */
    public static final StringSetting EXTENSIONS_TO_SHARE =
        FACTORY.createStringSetting("EXTENSIONS_TO_SEARCH_FOR", DEFAULT_EXTENSIONS_TO_SHARE);
    
    // New Settings for extension management

    /**
     * Used to flag the first use of the new database type to migrate the 
     *  extensions database across into the new settings 
     */
    public static final BooleanSetting EXTENSIONS_MIGRATE = 
        FACTORY.createBooleanSetting("EXTENSIONS_MIGRATE", true);
    
    /**
     * List of Extra file extensions.
     */
    public static final StringSetting EXTENSIONS_LIST_CUSTOM =
         FACTORY.createStringSetting("EXTENSIONS_LIST_CUSTOM", "");
    
    /**
     * File extensions that are not shared.
     */
    public static final StringSetting EXTENSIONS_LIST_UNSHARED =
         FACTORY.createStringSetting("EXTENSIONS_LIST_UNSHARED", "");
    
    
    
    /**
     * If to not force disable sensitive extensions.
     */
    public static final BooleanSetting DISABLE_SENSITIVE =
        FACTORY.createBooleanSetting("DISABLE_SENSITIVE_EXTS", true);
    
    /**
     * Sets the probability (expressed as a percentage) that an incoming
     * freeloader will be accepted.   For example, if allowed==50, an incoming
     * connection has a 50-50 chance being accepted.  If allowed==100, all
     * incoming connections are accepted.
     */                                                        
    public static final IntSetting FREELOADER_ALLOWED =
        FACTORY.createIntSetting("FREELOADER_ALLOWED", 100);
    
    /**
     * Minimum the number of files a host must share to not be considered
     * a freeloader.  For example, if files==0, no host is considered a
     * freeloader.
     */
    public static final IntSetting FREELOADER_FILES =
        FACTORY.createIntSetting("FREELOADER_FILES", 1);
    
    /**
	 * The timeout value for persistent HTTP connections in milliseconds.
	 */
    public static final IntSetting PERSISTENT_HTTP_CONNECTION_TIMEOUT =
        FACTORY.createIntSetting("PERSISTENT_HTTP_CONNECTION_TIMEOUT", 15000);
    
    /**
     * Specifies whether or not completed uploads
     * should automatically be cleared from the upload window.
     */
    public static final BooleanSetting CLEAR_UPLOAD =
        FACTORY.createBooleanSetting("CLEAR_UPLOAD", true);
    
    /**
	 * Whether or not browsers should be allowed to perform uploads.
	 */
    public static final BooleanSetting ALLOW_BROWSER =
        FACTORY.createBooleanSetting("ALLOW_BROWSER", false);

    /**
     * Whether to throttle hashing of shared files.
     */
    public static final BooleanSetting FRIENDLY_HASHING =
        FACTORY.createBooleanSetting("FRIENDLY_HASHING", true);	
    
    /** 
     * Setting for the threshold of when to warn the user that a lot of 
     *  files are being shared
     */
    public static final IntSetting FILES_FOR_WARNING =
        FACTORY.createIntSetting("FILES_FOR_WARNING", 1000);

    /** 
     * Setting for the threshold of when to warn the user that a lot of 
     *  files are being shared
     */
    public static final IntSetting DEPTH_FOR_WARNING =
        FACTORY.createIntSetting("DEPTH_FOR_WARNING", 4);
    
	/**
	 * Returns the download directory file setting for a mediatype. The
	 * settings are created lazily when they are requested for the first time.
	 * The default download directory is a file called "invalidfile" the file
	 * setting should not be used when its {@link Setting#isDefault()} returns
	 * true.  Use {@link #DIRECTORY_FOR_SAVING_FILES} instead then.
	 * @param type the mediatype for which to look up the file setting
	 * @return the filesetting for the media type
	 */
	public static final FileSetting getFileSettingForMediaType(MediaType type) {
		FileSetting setting = downloadDirsByDescription.get(type.getMimeType());
		if (setting == null) {
			setting = FACTORY.createProxyFileSetting
			("DIRECTORY_FOR_SAVING_" + type.getMimeType() + "_FILES",
			 DIRECTORY_FOR_SAVING_FILES);
			downloadDirsByDescription.put(type.getMimeType(), setting);
		}
		return setting;
	}
	
	/**
	 * The Creative Commons explanation URL
	 */
	public static final StringSetting CREATIVE_COMMONS_INTRO_URL = 
		FACTORY.createRemoteStringSetting
		("CREATIVE_COMMONS_URL","http://creativecommons.org/about/licenses/how1","creativeCommonsURL");
	
	/**
	 * The Creative Commons verification explanation URL
	 */
	public static final StringSetting CREATIVE_COMMONS_VERIFICATION_URL = 
		FACTORY.createRemoteStringSetting
		("CREATIVE_COMMONS_VERIFICATION_URL","http://creativecommons.org/technology/embedding#2","creativeCommonsVerificationURL");
    
    /**
     * Setting for whether or not to allow partial files to be shared.
     */
    public static final BooleanSetting ALLOW_PARTIAL_SHARING =
        FACTORY.createBooleanSetting("ALLOW_PARTIAL_SHARING", false);
    
    /**
     * Remote switch to turn off partial results.
     */
    public static final BooleanSetting ALLOW_PARTIAL_RESPONSES = 
        FACTORY.createRemoteBooleanSetting("ALLOW_PARTIAL_RESPONSES", true, "SharingSettings.allowPartialResponses");
    
    /**
     * Maximum size in bytes for the encoding of available ranges per Response object
     */
    public static final IntSetting MAX_PARTIAL_ENCODING_SIZE =
        FACTORY.createRemoteIntSetting("MAX_PARTIAL_ENCODING_SIZE", 20, 
                "SharingSettings.maxPartialEncodingSize", 10, 40);
    
    /**
     * Whether to publish keywords from partial files in the qrp.
     */
    public static final BooleanSetting PUBLISH_PARTIAL_QRP = 
        FACTORY.createRemoteBooleanSetting("PUBLISH_PARTIAL_QRP", true, "SharingSettings.publishPartialQRP");
    
    /**
     * Whether to load keywords from incomplete files in the trie
     */
    public static final BooleanSetting LOAD_PARTIAL_KEYWORDS = 
        FACTORY.createRemoteBooleanSetting("LOAD_PARTIAL_KEYWORDS", true, "SharingSettings.loadPartialKeywords");
}
