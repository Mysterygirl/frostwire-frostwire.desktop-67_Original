/* 
 * Digital Audio Access Protocol (DAAP)
 * Copyright (C) 2004 Roger Kapsi, info at kapsi dot de
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package de.kapsi.net.daap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.zip.GZIPOutputStream;

import org.apache.http.Header;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.impl.auth.DigestScheme;

import de.kapsi.net.daap.chunks.Chunk;
import de.kapsi.net.daap.chunks.UIntChunk;

/**
 * Misc methods and constants
 *
 * @author  Roger Kapsi
 */
public final class DaapUtil {
    
    //private static final Log LOG = LogFactory.getLog(DaapUtil.class);
    
    /** */
    private static final Random generator = new Random();
    
    /**
     * NULL value (Zero) is a forbidden value (in some cases) in 
     * DAAP and means that a value is not initialized (basically 
     * <code>null</code> for primitive types).
     */
    public static final int NULL = 0;
    
    /**
     * Global flag to turn gzip compression on and off
     */
    public static final boolean COMPRESS = true;
    
    /** ISO Latin 1 encoding */
    public static final String ISO_8859_1 = "ISO-8859-1";
    
    /** UTF-8 encoding */
    public static final String UTF_8 = "UTF-8";
    
    /** "\r\n" <b>DON'T TOUCH!</b> */
    static final byte[] CRLF = { (byte)'\r', (byte)'\n' };
    
    //private static final Log LOG = LogFactory.getLog(DaapUtil.class);
    
    private final static SimpleDateFormat formatter = 
        new SimpleDateFormat("EEE, d MMM yyyy hh:mm:ss z", Locale.US);
    
    /** DAAP 1.0.0 (iTunes 4.0) */
    public static final int DAAP_VERSION_1 = 0x00010000; // 1.0.0
    
    /** DAAP 2.0.0 (iTunes 4.1, 4.2) */
    public static final int DAAP_VERSION_2 = 0x00020000; // 2.0.0
    
    /** DAAP Version 3.0.0 (iTunes 4.5, 4.6) */
    public static final int DAAP_VERSION_3 = 0x00030000; // 3.0.0
    
    /** DAAP Version 3.0.2 (iTunes 5.0) */
    public static final int DAAP_VERSION_302 = 0x003002; // 3.0.2
    
    /** DMAP Version 2.0.1 */
    public static final int DMAP_VERSION_201 = 0x00020001; // 2.0.1
    
    /** DMAP Version 2.0.1 (iTunes 5.0) */
    public static final int DMAP_VERSION_202 = 0x00020002; // 2.0.2
    
    /** Music Sharing Version 2.0.1 */
    public static final int MUSIC_SHARING_VERSION_201 = 0x00020001; // 2.0.1
    
    /** 0, 1, ... F */
    private static final char[] HEX = { 
        '0', '1', '2', '3', '4', '5', 
        '6', '7', '8', '9', 'A', 'B', 
        'C', 'D', 'E', 'F' 
    };
    
    /** Default DAAP realm */
    static final String DAAP_REALM = "daap";
    
    /** 
     * List of sharable formats/extensions. The list
     * is likely not complete!
     * 
     * TODO: complete list
     */
    private static final String[] SUPPORTED_FORMATS = {
        ".mp3", ".m4a", ".m4p", ".wav", 
        ".aif", ".aiff", ".m1a"
    };
    
    private DaapUtil() {
    }
    
    /** 
     * Returns <code>true</code> if version is a supported protocol
     * version. At the moment only {@see #VERSION_3} and later are
     * supported.
     * 
     * @param version a protocol version
     * @return <code>true</code> if version is a supported
     */
    public static boolean isSupportedProtocolVersion(int version) {
        if (version >= DAAP_VERSION_3) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Converts a four character content code to an int and returns it.
     * 
     * @param contentCode a four character content code
     * @return content code
     */
    public static int toContentCodeNumber(String contentCode) {
        if (contentCode.length() != 4) {
            throw new IllegalArgumentException("content code must have 4 characters!");
        }
        
        return    ((contentCode.charAt(0) & 0xFF) << 24)
                | ((contentCode.charAt(1) & 0xFF) << 16)
                | ((contentCode.charAt(2) & 0xFF) <<  8)
                | ((contentCode.charAt(3) & 0xFF));
    }
    
    /**
     * Converts an four byte int to a string
     */
    public static String toContentCodeString(int contentCode) {
        char[] code = new char[4];
        code[0] = (char)((contentCode >> 24) & 0xFF);
        code[1] = (char)((contentCode >> 16) & 0xFF);
        code[2] = (char)((contentCode >>  8) & 0xFF);
        code[3] = (char)((contentCode      ) & 0xFF);
        return new String(code);
    }
    
    /**
     * Returns the current Date/Time in "iTunes time format"
     */
    public static final String now() {
        return formatter.format(new Date());
    }
    
    /**
     * Serializes the <code>chunk</code> and compresses it optionally.
     * The serialized data is returned as a byte-Array.
     */
    public static final byte[] serialize(Chunk chunk, boolean compress) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(255);
        DaapOutputStream out = null;
        
        if (DaapUtil.COMPRESS && compress) {
            GZIPOutputStream gzip = new GZIPOutputStream(buffer);
            out = new DaapOutputStream(gzip);
        } else {
            out = new DaapOutputStream(buffer);
        }
        
        out.writeChunk(chunk);
        out.close();
        
        return buffer.toByteArray();
    }
    
    /**
     * Splits a query String ("key1=value1&key2=value2...") and
     * stores the data in a Map
     * 
     * @param queryString a query String
     * @return the splitten query String as Map
     */
    public static final Map<String, String> parseQuery(String queryString) {
        
        Map<String, String> map = new HashMap<String, String>();
        
        if (queryString != null && queryString.length() != 0) {
            StringTokenizer tok = new StringTokenizer(queryString, "&");
            while(tok.hasMoreTokens()) {
                String token = tok.nextToken();
                
                int q = token.indexOf('=');
                if (q != -1 && q != token.length()) {
                    String key = token.substring(0, q);
                    String value = token.substring(++q);
                    map.put(key, value);
                }
            }
        }
        
        return map;
    }
    
    /**
     * Splits a meta String ("foo,bar,alice,bob") and stores the data
     * in an ArrayList
     * 
     * @param meta a meta String
     * @return the splitten meta String as ArrayList
     */
    public static final List<String> parseMeta(String meta) {
        StringTokenizer tok = new StringTokenizer(meta, ",");
        List<String> list = new ArrayList<String>(tok.countTokens());
        boolean flag = false;
        
        while(tok.hasMoreTokens()) {
            String token = tok.nextToken();
            
            // Must be te fist! See DAAP documentation 
            // for more info!
            if (!flag && token.equals("dmap.itemkind")) {
                list.add(0, token);
                flag = true;
            } else {
                list.add(token);
            }
        }
        return list;
    }
    
    /**
     * Converts major, minor to a DAAP version.
     * Version 2 is for example 0x00020000
     * 
     * @param major the major version (x)
     * @return x.0.0
     */
    public static int toVersion(int major) {
        return toVersion(major, 0, 0);
    }
    
    /**
     * Converts major, minor to a DAAP version.
     * Version 2.1 is for example 0x00020100
     * 
     * @param major the major version (x)
     * @param minor the minor version (y)
     * @return x.y.0
     */
    public static int toVersion(int major, int minor) {
        return toVersion(major, minor, 0);
    }
    
    /**
     * Converts major, minor and patch to a DAAP version.
     * Version 2.1.3 is for example 0x00020103
     * 
     * @param major the major version (x)
     * @param minor the minor version (y)
     * @param micro the patch version (z)
     * @return x.y.z
     */
    public static int toVersion(int major, int minor, int micro) {
       return (major & 0xFFFF) << 16
               | (minor & 0xFF) << 8
               | (micro & 0xFF);
    }
    
    /**
     * This method tries the determinate the protocol version
     * and returns it or {@see #NULL} if version could not be
     * estimated...
     */
    public static int getProtocolVersion(DaapRequest request) {
        
        if (request.isUnknownRequest())
            return DaapUtil.NULL;
        
        Header header = request.getHeader(DaapRequest.CLIENT_DAAP_VERSION);
        
        if (header == null && request.isSongRequest()) {
            header = request.getHeader(DaapRequest.USER_AGENT);
        }
        
        if (header == null)
            return DaapUtil.NULL;
        
        String name = header.getName();
        String value = header.getValue();

        // Unfortunately song requests do not have a Client-DAAP-Version
        // header. As a workaround we can estimate the protocol version
        // by User-Agent but that is weak an may break with non iTunes
        // hosts...
        if ( request.isSongRequest() && name.equals(DaapRequest.USER_AGENT)) {
            
            // Note: the protocol version of a Song request is estimated
            // by the server with the aid of the sessionId, i.e. this block
            // is actually never touched...
            if (value.startsWith("iTunes/5.0")) {
                return DaapUtil.DAAP_VERSION_302;
            } else if (value.startsWith("iTunes/4.9")
                    || value.startsWith("iTunes/4.8")
                    || value.startsWith("iTunes/4.7") 
                    || value.startsWith("iTunes/4.6")
                    || value.startsWith("iTunes/4.5")) {
                return DaapUtil.DAAP_VERSION_3;
            } else if (value.startsWith("iTunes/4.2") 
                    || value.startsWith("iTunes/4.1")) {
                return DaapUtil.DAAP_VERSION_2;
            } else if (value.startsWith("iTunes/4.0")) {
                return DaapUtil.DAAP_VERSION_1;
            } else {
                return DaapUtil.NULL;
            }
        } else {
            
            StringTokenizer tokenizer = new StringTokenizer(value, ".");
            int count = tokenizer.countTokens();
            
            if (count >= 2 && count <= 3) {
                try {

                    int major = DaapUtil.NULL;
                    int minor = DaapUtil.NULL;
                    int patch = DaapUtil.NULL;

                    major = Integer.parseInt(tokenizer.nextToken());
                    minor = Integer.parseInt(tokenizer.nextToken());

                    if (count == 3)
                        patch = Integer.parseInt(tokenizer.nextToken());

                    return DaapUtil.toVersion(major, minor, patch);

                } catch (NumberFormatException err) {
                }
            }
        }
        
        return DaapUtil.NULL;
    }
    
    /**
     * 
     */
    public static long parseUInt(String value) 
            throws NumberFormatException {
        try {
            return UIntChunk.checkUIntRange(Long.parseLong(value));
        } catch (IllegalArgumentException err) {
            throw new NumberFormatException("For input: " + value);
        }
    }
    
    /**
     * Generates a random int
     */
    public static int nextInt() {
        synchronized(generator) {
            return generator.nextInt();
        }
    }
    
    /**
     * Generates a random int
     */
    public static int nextInt(int max) {
        synchronized(generator) {
            return generator.nextInt(max);
        }
    }

    /**
     * String to byte Array
     */
    public static byte[] getBytes(String s, String charsetName) {
        try {
            return s.getBytes(charsetName);
        } catch (UnsupportedEncodingException e) {
            // should never happen
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Byte Array to String
     */
    public static String toString(byte[] b, String charsetName) {
        try {
            return new String(b, charsetName);
        } catch (UnsupportedEncodingException e) {
            // should never happen
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Returns b as hex String
     */
    public static String toHexString(byte[] b) {
        if (b.length % 2 != 0) {
            throw new IllegalArgumentException("Argument's length must be power of 2");
        }
        
        StringBuffer buffer = new StringBuffer(b.length * 2);
        for(int i = 0; i < b.length; i++) {
            char hi = HEX[((b[i] >> 4) & 0xF)];
            char lo = HEX[b[i] & 0xF];
            
            buffer.append(hi).append(lo);
        }
        return buffer.toString();
    }
    
    public static byte[] parseHexString(String s) {
        if (s.length() % 2 != 0) {
            throw new IllegalArgumentException("Argument's length() must be power of 2");
        }
        
        byte[] buffer = new byte[s.length() / 2];
        for(int i = 0, j = 0; i < buffer.length; i++) { 
            buffer[i] = (byte)((parseHexToInt(s.charAt(j++) & 0xFF) << 0x4)
                                | parseHexToInt(s.charAt(j++) & 0xFF));
        }
        return buffer;
    }
    
    private static int parseHexToInt(int hex) {
        switch(hex) {
            case '0': return 0;
            case '1': return 1;
            case '2': return 2;
            case '3': return 3;
            case '4': return 4;
            case '5': return 5;
            case '6': return 6;
            case '7': return 7;
            case '8': return 8;
            case '9': return 9;
            case 'A': return 10;
            case 'a': return 10;
            case 'B': return 11;
            case 'b': return 11;
            case 'C': return 12;
            case 'c': return 12;
            case 'D': return 13;
            case 'd': return 13;
            case 'E': return 14;
            case 'e': return 14;
            case 'F': return 15;
            case 'f': return 15;
            default: throw new NumberFormatException("'" + Character.toString((char)hex) + "'");
        }
    }
    
    /**
     * Creates a random nonce
     */
    public static String nonce() {
        return DigestScheme.createCnonce();
    }
    
    public static byte[] toMD5(String s) {
        try {
            return MessageDigest.getInstance("MD5").digest(getBytes(s, ISO_8859_1));
        } catch (NoSuchAlgorithmException err) {
            // should never happen
            throw new RuntimeException(err);
        }
    }
    
    public static String calculateHA1(String username, String password) {
        return calculateHA1(username, getBytes(password, ISO_8859_1));
    }
    
    public static String calculateHA1(String username, byte[] password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(getBytes(username, ISO_8859_1));
            md.update((byte)':');
            md.update(getBytes(DAAP_REALM, ISO_8859_1));
            md.update((byte)':');
            //md.update(getBytes(password, ISO_8859_1));
            md.update(password);
            return toHexString(md.digest());
        } catch (NoSuchAlgorithmException err) {
            // should never happen
            throw new RuntimeException(err);
        }
    }
    
    public static String calculateHA2(String uri) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");

            md.update(getBytes("GET", ISO_8859_1));
            md.update((byte)':');
            md.update(getBytes(uri, ISO_8859_1));
            return toHexString(md.digest());
        } catch (NoSuchAlgorithmException err) {
            // should never happen
            throw new RuntimeException(err);
        }
    }
    
    public static String digest(String ha1, String ha2, String nonce) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            
            md.update(getBytes(ha1, ISO_8859_1));
            md.update((byte)':');
            md.update(getBytes(nonce, ISO_8859_1));
            md.update((byte)':');
            md.update(getBytes(ha2, ISO_8859_1));
            return toHexString(md.digest());
        } catch (NoSuchAlgorithmException err) {
            // should never happen
            throw new RuntimeException(err);
        }
    }
    
    // see org.apache.commons.httpclient.auth.DigestScheme
    /*public static String digest(String username, byte[] password, String nonce, String uri) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            
            md.update(getBytes(username, ISO_8859_1));
            md.update((byte)':');
            md.update(getBytes(DAAP_REALM, ISO_8859_1));
            md.update((byte)':');
            //md.update(getBytes(password, ISO_8859_1));
            md.update(password);
            final String HA1 = toHexString(md.digest());
            md.reset();
            
            md.update(getBytes("GET", ISO_8859_1));
            md.update((byte)':');
            md.update(getBytes(uri, ISO_8859_1));
            final String HA2 = toHexString(md.digest());
            md.reset();
            
            md.update(getBytes(HA1, ISO_8859_1));
            md.update((byte)':');
            md.update(getBytes(nonce, ISO_8859_1));
            md.update((byte)':');
            md.update(getBytes(HA2, ISO_8859_1));
            return toHexString(md.digest());
        } catch (NoSuchAlgorithmException err) {
            // should never happen
            throw new RuntimeException(err);
        }
    }
    
    /**
     * Returns the extension of file or null if file
     * has no extension
     */
    public static String getExtension(File file) {
        return file.isFile() ? getExtension(file.getName()) : null;
    }
    
    /**
     * Returns the extension of fileName or null if file
     * has no extension
     */
    public static String getExtension(String fileName) {
        int p = fileName.lastIndexOf('.');
        if (p != -1 && ++p < fileName.length()) {
            return fileName.substring(p).toLowerCase(Locale.US);
        }
        return null;
    }
    
    /**
     * Returns true if file is a supported format
     */
    public static boolean isSupportedFormat(File file) {
        return file.isFile() && isSupportedFormat(file.getName());
    }
    
    /**
     * Returns true if fileName is a supported format 
     */
    public static boolean isSupportedFormat(String fileName) {
        fileName = fileName.toLowerCase(Locale.US);
        for(int i = 0; i < SUPPORTED_FORMATS.length; i++) {
            if (fileName.endsWith(SUPPORTED_FORMATS[i])) {
                return true;
            }
        }
        return false;
    }
}
