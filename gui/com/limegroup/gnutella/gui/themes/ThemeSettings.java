package com.limegroup.gnutella.gui.themes;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.service.ErrorService;
import org.limewire.setting.FileSetting;
import org.limewire.setting.IntSetting;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.OSUtils;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.settings.LimeProps;
import com.limegroup.gnutella.util.Expand;
import com.limegroup.gnutella.util.FrostWireUtils;

/**
 * Class for handling all LimeWire settings that are stored to disk.  To
 * add a new setting, simply add a new public static member to the list
 * of settings.  Construct settings using the <tt>FACTORY</tt> instance
 * from the <tt>AbstractSettings</tt> superclass.  Each setting factory
 * constructor takes the name of the key and the default value, and all
 * settings are typed.  Choose the correct <tt>Setting</tt> factory constructor
 * for your setting type.  It is also important to choose a unique string key
 * for your setting name -- otherwise there will be conflicts, and a runtime
 * exception will be thrown.
 */
public final class ThemeSettings extends LimeProps {
    
    private static final Log LOG = LogFactory.getLog(ThemeSettings.class);
    
    private ThemeSettings() {}
            
    /**
     * The extension for theme packs to allow people to search for them --
     * stands for "LimeWire Theme Pack".
     */
    public static final String EXTENSION = "fwtp";
    
    public static final File THEME_DIR_FILE =
		new File(CommonUtils.getUserSettingsDir(), "themes");
    
    /**
     * The normal 'LimeWire' theme.
     */
    public static final String FROSTWIRE_THEME_NAME =
		"frostwirePro_theme."+EXTENSION;
    
    /**
     * The default name of the theme file name for OS X.
     */
    public static final String PINSTRIPES_OSX_THEME_NAME =
		"pinstripes_theme_osx."+EXTENSION;
		
    /**
     * The metal theme name.
     */
    public static final String BRUSHED_METAL_OSX_THEME_NAME =
        "brushed_metal_theme_osx."+EXTENSION;
    
    /**
     * The default name of the windows laf theme file name.
     */
    public static final String WINDOWS_LAF_THEME_NAME =
        "windows_theme."+EXTENSION;
        
    /**
     * The default name of the gtk laf theme file name.
     */
    public static final String GTK_LAF_THEME_NAME =
        "GTK_theme." + EXTENSION;
        
    /**
     * The default name of the theme file name for non-OS X pro users.
     */
    public static final String PRO_THEME_NAME =
        "frostwirePro_theme."+EXTENSION;
        
    /**
     * The name for the unknown theme file.
     */
    public static final String OTHER_THEME_NAME =
        "other_theme." + EXTENSION;
    
    /**
     * The full path to the LimeWire theme file.
     */
    public static final File FROSTWIRE_THEME_FILE =
		new File(THEME_DIR_FILE, FROSTWIRE_THEME_NAME);
    
    /**
     * The full path to the default theme file on OS X.
     */
    static final File PINSTRIPES_OSX_THEME_FILE =
		new File(THEME_DIR_FILE, PINSTRIPES_OSX_THEME_NAME);
		
    /**
     * The full path to the metal theme file on OS X.
     */
    static final File BRUSHED_METAL_OSX_THEME_FILE =
		new File(THEME_DIR_FILE, BRUSHED_METAL_OSX_THEME_NAME);		
		
    /** 
     * The full path to the windows theme file for the windows LAF
     */
    static final File WINDOWS_LAF_THEME_FILE =
        new File(THEME_DIR_FILE, WINDOWS_LAF_THEME_NAME);
        
    /**
     * The full path to the GTK theme file for the GTK LAF
     */
    static final File GTK_LAF_THEME_FILE =
        new File(THEME_DIR_FILE, GTK_LAF_THEME_NAME);
        
    /**
     * The full path to the pro only theme.
     */
    static final File PRO_THEME_FILE =
        new File(THEME_DIR_FILE, PRO_THEME_NAME);
        
    /**
     * The path for the 'other' theme name.
     */
    static final File OTHER_THEME_FILE =
        new File(THEME_DIR_FILE, OTHER_THEME_NAME);
    
    /**
     * Find the themes jar and delete any zip files on disk if
     * they're older than the ones in our jar.
     */
    static {
        File themesJar = getThemesJar();    
        //System.out.println("ThemeSettings - Themes jar: " + themesJar);
        if(themesJar == null || !themesJar.isFile())
            JAR_THEME_NAMES = Collections.emptyList();
        else
            JAR_THEME_NAMES = scanJarFileForThemes(themesJar);        
    }
    
    /**
     * Scans through the themes.jar file for all themes.
     * This will delete themes that are older than the timestamp
     * in the themes.jar and return a list of all potential themes.
     */ 
    private static List<String> scanJarFileForThemes(File jarFile) {
    	//System.out.println("ThemeSettings - *************scanning jar for themes..." + jarFile.lastModified());
        List<String> themeFiles = new ArrayList<String>();
        ZipFile zf = null;
        try {        	
            long jarMod = jarFile.lastModified();
            zf = new ZipFile(jarFile);
            Enumeration<? extends ZipEntry> entries = zf.entries();
            while(entries.hasMoreElements()) {
                ZipEntry ze = entries.nextElement();
                String name = ze.getName();
                if(!name.endsWith(".fwtp"))
                    continue;
                //System.out.println("ThemeSettings - Added from jar: " + name);
                themeFiles.add(name);
                File existingFile = new File(THEME_DIR_FILE, name);
                File existingDir = extractThemeDir(existingFile);                
                
                if(existingFile.isFile() || existingDir.isDirectory()) {
                    if(jarMod > existingFile.lastModified()
                            || jarMod > existingDir.lastModified())
                        deleteZipWithOldTimestamp(existingFile);
                }
            }
        } catch(IOException ioe) {
            ErrorService.error(ioe);
        } finally {
            if(zf != null) {
                try {
                    zf.close();
                } catch(IOException ignored) {}
            }
        }
        
        return Collections.unmodifiableList(themeFiles);
    }
    
    /** Returns the themes.jar file. */
    private static File getThemesJarFromFrostWireThemePath() {
    	//System.out.println("ThemeSettings - Frostwire theme resource name is: " + FROSTWIRE_THEME_NAME);
        URL themeURL = ThemeSettings.class.getClassLoader().getResource(FROSTWIRE_THEME_NAME);
        
        if(themeURL != null) {
            String url = themeURL.toExternalForm();
            if (url != null && url.startsWith("jar:file:")) {
                url = url.substring("jar:file:".length(), url.length());
                url = url.substring(0, url.length() - FROSTWIRE_THEME_NAME.length() - "!/".length());
                return new File(url);
            }
        }

        return null;
    }
    
    private static File getThemesJar() {
    	//the jar should be on the same folder as the frostwire executable
    	File themeJar = new File(System.getProperty("user.dir") + File.separatorChar + "themes.jar");
    	
    	if (themeJar != null &&
    		themeJar.exists() &&
    		themeJar.isFile())
    		return themeJar;
    	
    	//if we're not running from a binary, then we have to look for the jar elsewhere
    	return getThemesJarFromFrostWireThemePath();
    }

    /**
     * Utility method that deletes specified theme file and expanded folder 
     * from the themes folder.
     *
     * @param themeFile the theme zip file to delete
     */
    private static void deleteZipWithOldTimestamp(File themeFile) {
        File themeDir = extractThemeDir(themeFile);

        if(themeDir.exists()) {
            String[] children = themeDir.list();
            if (children != null) {
            	for (int i=0; i<children.length; i++)
            		new File(themeDir, children[i]).delete();
            }
            themeDir.delete();
        }

        themeFile.delete();
    }

    /**
     * Expands the specified theme zip file to the specified directory.
     *
     * @param themeFile the theme zip file to expand
     * @param themeDir the directory to expand to -- the themes directory
     *  plus the name of the theme
     * @param overwrite whether or not to force the overwriting of existing
     *  files in the destination folder when we expand the zip, regardless 
     *  of File/ZipEntry timestamp
     */
    static boolean expandTheme(File themeFile, File themeDir, 
                            boolean overwrite, boolean showError) {
        themeDir.mkdirs();
        try {
            FileUtils.setWriteable(themeDir);
            Expand.expandFile(themeFile, themeDir, overwrite);
        } catch(ZipException ze) {
            // invalid theme, tell the user.
            GUIMediator.showError(I18n.tr("The theme you are applying is invalid. FrostWire will revert to the default theme."));
            return false;
        } catch(IOException e) {
            // this should never really happen, so report it
            if (showError)
                ErrorService.error(e);
            return false;
        }
        return true;
    }
    
    /**
     * Convenience method for determining in the path of the themes directory
     * for a given theme file.  The directory is the path of the themes
     * directory plus the name of the theme.
     *
     * @param themeFile the <tt>File</tt> instance denoting the location 
     *  of the theme file on disk
     * @return a new <tt>File</tt> instance denoting the appropriate path
     *  for the directory for this specific theme
     */
    static File extractThemeDir(File themeFile) {
		String dirName = themeFile.getName();
		dirName = dirName.substring(0, dirName.length()-5);
		return new File(new File(CommonUtils.getUserSettingsDir(),"themes"), 
                        dirName);
        
    }
    
    /**
     * Determines whether or not the specified file is a theme file.
     */
    static boolean isThemeFile(File f) {
        return f.getName().toLowerCase().endsWith("." + EXTENSION);
    }
    
    /**
     * Determines whether or not the current theme file is the default theme
     * file.
     *
     * @return <tt>true</tt> if the current theme file is the default,
     *  otherwise <tt>false</tt>
     */
    public static boolean isDefaultTheme() {
        return THEME_FILE.getValue().equals(THEME_DEFAULT);
    }
    
    /**
     * Determines if the current theme is the GTK theme.
     */
    public static boolean isGTKTheme() {
        return THEME_FILE.getValue().equals(GTK_LAF_THEME_FILE);
    }
    
    /** 
     * Determines whether or not the current theme is the windows theme,
     * designed to be used for the windows laf.
     * @return <tt>true</tt> if the current theme is the windows theme,
     *  otherwise <tt>false</tt>
     */
    public static boolean isWindowsTheme() {
        return THEME_FILE.getValue().equals(WINDOWS_LAF_THEME_FILE);
    }
    
    /**
     * Determines if the theme is the brushed metal theme.
     */
    public static boolean isBrushedMetalTheme() {
        return THEME_FILE.getValue().equals(BRUSHED_METAL_OSX_THEME_FILE);
    }
    
    /**
     * Determines if the theme is the pinstripes theme.
     */
    public static boolean isPinstripesTheme() {
        return THEME_FILE.getValue().equals(PINSTRIPES_OSX_THEME_FILE);
    }
    
    /**
     * Determines if the current theme is the native OSX theme.
     */
    public static boolean isNativeOSXTheme() {
        return OSUtils.isMacOSX() &&
              (isPinstripesTheme() || isBrushedMetalTheme());
    }
    
    /**
     * Determines if the current theme is the native theme.
     */
    public static boolean isNativeTheme() {
        return isNativeOSXTheme() || isWindowsTheme() || isGTKTheme();
    }
    
    /**
     * Determines if the current theme is the 'other' theme.
     */
    public static boolean isOtherTheme() {
        return THEME_FILE.getValue().equals(OTHER_THEME_FILE);
    }
    
    /**
     * Determines whether or not the current theme is valid.
     */
    public static boolean isValid() {
        if(isOtherTheme()) {
            String name = getOtherLF();
            if(name != null) {
                try {
                    Class.forName(name);
                    return true;
                } catch(ClassNotFoundException nfe) {}
            }
            return false;
        }
        return true;
    }
    
    /**
     * Gets the L&F that should be used if this is the 'other' theme.
     */
    public static String getOtherLF() {
        BufferedReader in = null;
        try {
            in = new BufferedReader(
                    new InputStreamReader(
                        new FileInputStream(
                            new File(THEME_DIR_FILE, "other_theme/name.txt"))));
            String classname = in.readLine();
            if(classname != null)
                return classname.trim();
        } catch(IOException ignored) {
        	//System.out.println("ThemeSettings - GetOtherLF - Cannot set other look and feel:" + ignored.getMessage());
            LOG.warn("Ignoring IOX", ignored);
        } finally {
            if(in != null)
                try { in.close(); } catch(IOException ignored) {}
        }
        return null;
    }
    
    /**
     * Sets the other L&F classname.
     */
    public static void setOtherLF(String classname) {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(
                    new FileWriter(
                        new File(THEME_DIR_FILE, "other_theme/name.txt"), false));
            out.write(classname);
            out.flush();
        } catch(IOException ignored) {
            LOG.warn("Ignoring IOX", ignored);
        } finally {
            if(out != null)
                try { out.close(); } catch(IOException ignored) {}
        }
    }
        
    /**
     * Formats a theme name, removing the underscore characters,
     * capitalizing the first letter of each word, and removing
     * the 'fwtp'.
     */
    public static String formatName(String name) {
        // strip off the .fwtp
        name = name.substring(0, name.length()-5);
        StringBuilder formatted = new StringBuilder(name.length());
        StringTokenizer st = new StringTokenizer(name, "_");
        String next;
        for(; st.hasMoreTokens(); ) {
            next = st.nextToken();
            String lower = next.toLowerCase();
            if(lower.equals("osx"))
                next = "(OSX)";
            else if(lower.equals("limewire"))
                next = "LimeWire";
            else if(lower.equals("frostwirepro"))
                next = "FrostWire";
            formatted.append(" " + next.substring(0,1).toUpperCase(Locale.US));
            if(next.length() > 1)
                formatted.append(next.substring(1));
            
        }
        return formatted.toString().trim();
    }        
    
    /**
     * Setting for the default theme file to use for FrostWire display.
     */
    public static final File THEME_DEFAULT;
    public static final File THEME_DEFAULT_DIR;
    static {
        File theme, dir;
        if(OSUtils.isMacOSX()) {
            theme = PINSTRIPES_OSX_THEME_FILE;
            dir = new File(THEME_DIR_FILE, "pinstripes_theme_osx");
        } else if(FrostWireUtils.isPro()) {
            theme = PRO_THEME_FILE;
            dir = new File(THEME_DIR_FILE, "frostwirePro_theme");
        } else if(OSUtils.isNativeThemeWindows()) {
            theme = WINDOWS_LAF_THEME_FILE;
            dir = new File(THEME_DIR_FILE, "windows_theme");
        } else if(OSUtils.isLinux()) {
            theme = GTK_LAF_THEME_FILE;
            dir = new File(THEME_DIR_FILE, "GTK_theme");
        } else {
            theme = FROSTWIRE_THEME_FILE;
            dir = new File(THEME_DIR_FILE, "frostwirePro_theme");
        }
        THEME_DEFAULT = theme;
        THEME_DEFAULT_DIR = dir;
    }
	
	/**
	 * Setting for the file name of the theme file.
	 */
	public static final FileSetting THEME_FILE =
		FACTORY.createFileSetting("THEME_FILE", THEME_DEFAULT);
	
	/**
	 * Setting for the file name of the theme directory.
	 */
	public static final FileSetting THEME_DIR =
		FACTORY.createFileSetting("THEME_DIR", THEME_DEFAULT_DIR);

    /**
     * ArrayList containing the names of theme files in our jar as Strings.
     */
    public static final List<String> JAR_THEME_NAMES;
    
    /**
     * Setting for the value all fonts should be incremented by.
     * 
     * Can be 0 and negative.
     */
    public static final IntSetting FONT_SIZE_INCREMENT = 
        FACTORY.createIntSetting("FONT_SIZE_INCREMENT", 0);
    
    /**
	 * Gets the themes for the themes.jar file in the current working directory
	 */
	public static boolean recoverFromJar() {
		GUIMediator.showMessage("FTA: FrostWire will look for skin's jar file. Using system directory it would be: " + System.getProperty("user.dir")); // FTA: DEBUG MESSAGE TO TRY FROM BINARY
		//System.getProperty("user.dir")); or CommonUtils.getCurrentDirectory() will take same result 
		File themesJarFile = new File(CommonUtils.getCurrentDirectory() + System.getProperty("file.separator") + "themes.jar");
		//GUIMediator.showMessage("FTA: FrostWire will look for skin's jar file in: " + CommonUtils.getCurrentDirectory() + System.getProperty("file.separator") + "themes.jar\nUsing system directory it would be: " + System.getProperty("user.dir")); // FTA: DEBUG MESSAGE TO TRY FROM BINARY
		if (!themesJarFile.exists()) {
    		GUIMediator.showError(I18n.tr("The file containing default themes for FrostWire cannot be found.\nYou will need to download the files manually."));
    		return false;
		}
		
    	List<String> JAR_THEME_NAMES = scanJarFileForThemes(themesJarFile);
    	
    	File themeDir = THEME_DIR_FILE;
    	 try {
             FileUtils.setWriteable(themeDir);
             Expand.expandFile(themesJarFile, themeDir, false);
         } catch(ZipException ze) {
             // invalid theme, tell the user.
             GUIMediator.showError(I18n.tr("The theme you are applying is invalid. FrostWire will revert to the default theme."));             
             return false;
         } catch(IOException e) {
             return false;
         }
         
         
        return true;
		//THEME_DIR_FILE
      //System.out.println("ThemeSettings - Copying file from " + existingFile.getName() + " to " + existingFile);
        //CommonUtils.copyResourceFile(existingFile.getName(), existingFile, true);
	}
}

	