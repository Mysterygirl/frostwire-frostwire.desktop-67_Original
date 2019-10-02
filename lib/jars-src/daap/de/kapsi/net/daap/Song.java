/* 
 * Digital Audio Access Protocol (DAAP)
 * Copyright (C) 2004, 2005 Roger Kapsi, info at kapsi dot de
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import de.kapsi.net.daap.chunks.BooleanChunk;
import de.kapsi.net.daap.chunks.Chunk;
import de.kapsi.net.daap.chunks.DateChunk;
import de.kapsi.net.daap.chunks.LongChunk;
import de.kapsi.net.daap.chunks.SByteChunk;
import de.kapsi.net.daap.chunks.SShortChunk;
import de.kapsi.net.daap.chunks.StringChunk;
import de.kapsi.net.daap.chunks.UByteChunk;
import de.kapsi.net.daap.chunks.UIntChunk;
import de.kapsi.net.daap.chunks.UShortChunk;
import de.kapsi.net.daap.chunks.impl.ContainerItemId;
import de.kapsi.net.daap.chunks.impl.HasVideo;
import de.kapsi.net.daap.chunks.impl.ITMSArtistId;
import de.kapsi.net.daap.chunks.impl.ITMSComposerId;
import de.kapsi.net.daap.chunks.impl.ITMSGenreId;
import de.kapsi.net.daap.chunks.impl.ITMSPlaylistId;
import de.kapsi.net.daap.chunks.impl.ITMSSongId;
import de.kapsi.net.daap.chunks.impl.ITMSStorefrontId;
import de.kapsi.net.daap.chunks.impl.ItemId;
import de.kapsi.net.daap.chunks.impl.ItemKind;
import de.kapsi.net.daap.chunks.impl.ItemName;
import de.kapsi.net.daap.chunks.impl.NormVolume;
import de.kapsi.net.daap.chunks.impl.PersistentId;
import de.kapsi.net.daap.chunks.impl.Podcast;
import de.kapsi.net.daap.chunks.impl.SongAlbum;
import de.kapsi.net.daap.chunks.impl.SongArtist;
import de.kapsi.net.daap.chunks.impl.SongBeatsPerMinute;
import de.kapsi.net.daap.chunks.impl.SongBitrate;
import de.kapsi.net.daap.chunks.impl.SongCategory;
import de.kapsi.net.daap.chunks.impl.SongCodecSubtype;
import de.kapsi.net.daap.chunks.impl.SongCodecType;
import de.kapsi.net.daap.chunks.impl.SongComment;
import de.kapsi.net.daap.chunks.impl.SongCompilation;
import de.kapsi.net.daap.chunks.impl.SongComposer;
import de.kapsi.net.daap.chunks.impl.SongContentDescription;
import de.kapsi.net.daap.chunks.impl.SongContentRating;
import de.kapsi.net.daap.chunks.impl.SongDataKind;
import de.kapsi.net.daap.chunks.impl.SongDataUrl;
import de.kapsi.net.daap.chunks.impl.SongDateAdded;
import de.kapsi.net.daap.chunks.impl.SongDateModified;
import de.kapsi.net.daap.chunks.impl.SongDescription;
import de.kapsi.net.daap.chunks.impl.SongDisabled;
import de.kapsi.net.daap.chunks.impl.SongDiscCount;
import de.kapsi.net.daap.chunks.impl.SongDiscNumber;
import de.kapsi.net.daap.chunks.impl.SongEqPreset;
import de.kapsi.net.daap.chunks.impl.SongFormat;
import de.kapsi.net.daap.chunks.impl.SongGenre;
import de.kapsi.net.daap.chunks.impl.SongGrouping;
import de.kapsi.net.daap.chunks.impl.SongKeywords;
import de.kapsi.net.daap.chunks.impl.SongLongDescription;
import de.kapsi.net.daap.chunks.impl.SongRelativeVolume;
import de.kapsi.net.daap.chunks.impl.SongSampleRate;
import de.kapsi.net.daap.chunks.impl.SongSize;
import de.kapsi.net.daap.chunks.impl.SongStartTime;
import de.kapsi.net.daap.chunks.impl.SongStopTime;
import de.kapsi.net.daap.chunks.impl.SongTime;
import de.kapsi.net.daap.chunks.impl.SongTrackCount;
import de.kapsi.net.daap.chunks.impl.SongTrackNumber;
import de.kapsi.net.daap.chunks.impl.SongUserRating;
import de.kapsi.net.daap.chunks.impl.SongYear;

/**
 * There isn't much to say: a Song is a Song.
 * <p>Note: although already mentioned in StringChunk I'd like to
 * point out that <code>null</code> is a valid value for DAAP. Use
 * it to reset Strings. See StringChunk for more information!</p>
 *
 * @author  Roger Kapsi
 */
public class Song {
    
    /** songId is an 32bit unsigned value! */
    private static long songId = 1;
    
    private static final SongFormat FORMAT = new SongFormat(SongFormat.MP3);
    private static final SongSampleRate SAMPLE_RATE = new SongSampleRate(SongSampleRate.KHZ_44100);
    
    private final Map<String, Chunk> chunks = new HashMap<String, Chunk>();
    
    private final ItemKind itemKind = new ItemKind(ItemKind.AUDIO);
    private final ItemId itemId = new ItemId();
    private final ItemName itemName = new ItemName();
    private final ContainerItemId containerItemId = new ContainerItemId();
    private final PersistentId persistentId = new PersistentId();
    
    private SongAlbum album;
    private SongArtist artist;
    private SongBeatsPerMinute bpm;
    private SongBitrate bitrate;
    private SongComment comment;
    private SongCompilation compilation;
    private SongComposer composer;
    private SongDataKind dataKind;
    private SongDataUrl dataUrl;
    private SongDateAdded dateAdded;
    private SongDateModified dateModified;
    private SongDescription description;
    private SongDisabled disabled;
    private SongDiscCount discCount;
    private SongDiscNumber discNumber;
    private SongEqPreset eqPreset;
    private SongFormat format;
    private SongGenre genre;
    private SongRelativeVolume relativeVolume;
    private SongSampleRate sampleRate;
    private SongSize size;
    private SongStartTime startTime;
    private SongStopTime stopTime;
    private SongTime time;
    private SongTrackCount trackCount;
    private SongTrackNumber trackNumber;
    private SongUserRating userRating;
    private SongYear year;
    private SongGrouping grouping;
    private NormVolume normVolume;
    private SongCodecType codecType;
    private SongCodecSubtype codecSubtype;
    
    private ITMSArtistId itmsArtistId;
    private ITMSComposerId itmsComposerId;
    private ITMSGenreId itmsGenreId;
    private ITMSPlaylistId itmsPlaylistId;
    private ITMSStorefrontId itmsStorefrontId;
    private ITMSSongId itmsSongId;
    
    // @since iTunes 5.0
    private Podcast podcast;
    private SongCategory category;
    private SongContentDescription contentDescription;
    private SongContentRating contentRating;
    private SongKeywords keywords;
    private SongLongDescription longDescription;
    
    // @since iTunes 6.0.2
    private HasVideo hasVideo;
    
    /** An arbitrary Object (most likely a File) */
    private Object attachment;
    
    /**
     * Creates a new Song
     */
    public Song() {
        
        synchronized(Song.class) {
            itemId.setValue(songId++);
        }
        
        persistentId.setValue(itemId.getValue());
        containerItemId.setValue(itemId.getValue());
        init();
    }
    
    /**
     * Creates a new Song with the provided name
     */
    public Song(String name) {
        this();
        itemName.setValue(name);
    }
    
    private void init() {
        addChunk(itemKind);
        addChunk(itemName);
        addChunk(itemId);
        addChunk(containerItemId);
        
        // Some clients do not init format (implicit mp3)
        // and use uninitialized garbage instead
        addChunk(FORMAT); 
        
        // VLC requires the sample rate
        addChunk(SAMPLE_RATE);
        
        /*addChunk(album);
        addChunk(artist);
        addChunk(bpm);
        addChunk(bitrate);
        addChunk(comment);
        addChunk(compilation);
        addChunk(composer);
        addChunk(dataKind);
        addChunk(dataUrl);
        addChunk(dateAdded);
        addChunk(dateModified);
        addChunk(description);
        addChunk(disabled);
        addChunk(discCount);
        addChunk(discNumber);
        addChunk(eqPreset);
        addChunk(format); // and overwrite if not null
        addChunk(genre);
        addChunk(relativeVolume);
        addChunk(sampleRate);
        addChunk(size);
        addChunk(startTime);
        addChunk(stopTime);
        addChunk(time);
        addChunk(trackCount);
        addChunk(trackNumber);
        addChunk(userRating);
        addChunk(year);
        addChunk(grouping);
        addChunk(persistentId);
        addChunk(normVolume);
        
        addChunk(codecType);
        addChunk(codecSubtype);
        
        addChunk(itmsArtistId);
        addChunk(itmsComposerId);
        addChunk(itmsGenreId);
        addChunk(itmsPlaylistId);
        addChunk(itmsStorefrontId);
        addChunk(itmsSongId);*/
    }
    
    /**
     * Returns the unique id of this song
     */
    protected long getItemId() {
        return itemId.getUnsignedValue();
    }
    
    /**
     * Returns the id of this Songs container.
     * Note: same as getId()
     */
    protected long getContainerId() {
        return containerItemId.getUnsignedValue();
    }
    
    /**
     * Returns the name of this Song
     */
    public String getName() {
        return getStringValue(itemName);
    }
    
    /**
     * Sets the name of this Song
     */
    public void setName(Transaction txn, String itemName) {
        setStringValue(txn, "itemName", itemName);
    }
    
    /**
     * Sets the album of this Song
     */
    public void setAlbum(Transaction txn, String album) {
        setStringValue(txn, "album", album);
    }
    
    /**
     * Returns the album of this Song
     */
    public String getAlbum() {
        return getStringValue(album);
    }
    
    /**
     * Sets the artist of this Song
     */
    public void setArtist(Transaction txn, String artist) {
        setStringValue(txn, "artist", artist);
    }
    
    /**
     * Returns the artist of this Song
     */
    public String getArtist() {
        return getStringValue(artist);
    }
    
    /**
     * Sets the beats per minute of this Song
     */
    public void setBeatsPerMinute(Transaction txn, int bpm) {
        setUShortValue(txn, "bpm", bpm);
    }
    
    /**
     * Returns the beats per minute of this Song
     */
    public int getBeatsPerMinute() {
        return getUShortValue(bpm);
    }
    
    /**
     * Sets the bitrate of this Song
     */
    public void setBitrate(Transaction txn, int bitrate) {
        setUShortValue(txn, "bitrate", bitrate);
    }
    
    /**
     * Returns the bitrate of this Song
     */
    public int getBitrate() {
        return getUShortValue(bitrate);
    }
    
    /**
     * Sets the comment of this Song
     */
    public void setComment(Transaction txn, String comment) {
        setStringValue(txn, "comment", comment);
    }
    
    /**
     * Returns the comment of this Song
     */
    public String getComment() {
        return getStringValue(comment);
    }
    
    /**
     * Sets if this Song is a compilation
     */
    public void setCompilation(Transaction txn, boolean compilation) {
        setBooleanValue(txn, "compilation", compilation);
    }
    
    /**
     * Returns <tt>true</tt> if this Song is a
     * compilation
     */
    public boolean isCompilation() {
        return getBooleanValue(compilation);
    }
    
    /**
     * Sets the composer of this Song
     **/
    public void setComposer(Transaction txn, String composer) {
        setStringValue(txn, "composer", composer);
    }
    
    /** 
     * Returns the composer of this Song
     */
    public String getComposer() {
        return getStringValue(composer);
    }
    
    /**
     * Sets whether this Song is a Radio or a DAAP
     * stream. See SongDataKind for more information.
     * Note: you must set the DataUrl with setDataUrl()
     * if dataKind is Radio!
     */
    public void setDataKind(Transaction txn, int dataKind) {
        setUByteValue(txn, "dataKind", dataKind);
    }
    
    /**
     * Returns the kind of this Song
     */
    public int getDataKind() {
        return getUByteValue(dataKind);
    }
    
    /**
     * Sets the URL of this Song
     */
    public void setDataUrl(Transaction txn, String dataUrl) {
        setStringValue(txn, "dataUrl", dataUrl);
    }
    
    /**
     * Returns the URL of this Song
     */
    public String getDataUrl() {
        return getStringValue(dataUrl);
    }
    
    /**
     * Sets the date when this Song was added to the
     * Library. Note: the date is in seconds since
     * 1970.
     * <code>(int)(System.currentTimeMillis()/1000)</code>
     */
    public void setDateAdded(Transaction txn, long dateAdded) {
        setDateValue(txn, "dateAdded", dateAdded);
    }
    
    /**
     * Returns the date when this Song was added to 
     * the Library
     */
    public long getDateAdded() {
        return getDateValue(dateAdded);
    }
    
    /**
     * Sets the date when this Song was modified.
     * Note: the date is in seconds since 1970.
     * <code>(int)(System.currentTimeMillis()/1000)</code>
     */
    public void setDateModified(Transaction txn, long dateModified) {
        setDateValue(txn, "dateModified", dateModified);
    }
    
    /**
     * Returns the date when this song was modified
     */
    public long getDateModified() {
        return getDateValue(dateModified);
    }
    
    /**
     * Sets the description of this Song.
     * Note: the description of a Song is its
     * file format. The description of a MP3
     * file is for example 'MPEG Audio file'. 
     * See SongDescription for more information.
     */
    public void setDescription(Transaction txn, String description) {
        setStringValue(txn, "description", description);
    }
    
    /**
     * Returns the description of this Song
     */
    public String getDescription() {
        return getStringValue(description);
    }
    
    /**
     * Sets if this Song is either disabled or enabled.
     * This is indicated in iTunes by the small checkbox
     * next to the Song name.
     */
    public void setDisabled(Transaction txn, boolean disabled) {
        setBooleanValue(txn, "disabled", disabled);
    }
    
    /**
     * Returns <tt>true</tt> if this Song is disabled
     */
    public boolean isDisabled() {
        return getBooleanValue(disabled);
    }
    
    /**
     * Sets the number of discs of this Song
     */
    public void setDiscCount(Transaction txn, int discCount) {
        setUShortValue(txn, "discCount", discCount);
    }
    
    /**
     * Returns the number of discs
     */
    public int getDiscCount() {
        return getUShortValue(discCount);
    }
    
    /**
     * Sets the disc number of this Song
     */
    public void setDiscNumber(Transaction txn, int discNumber) {
        setUShortValue(txn, "discNumber", discNumber);
    }
    
    /**
     * Returns the disc number of this Song
     */
    public int getDiscNumber() {
        return getUShortValue(discNumber);
    }
    
    /**
     * Sets the equalizer of this Song.
     * Note: See SongEqPreset for more information
     */
    public void setEqPreset(Transaction txn, String eqPreset) {
        setStringValue(txn, "eqPreset", eqPreset);
    }
    
    /**
     * Returns the equalizer of this Song
     */
    public String getEqPreset() {
        return getStringValue(eqPreset);
    }
    
    /**
     * Sets the format of this Song.
     * Note: See SongFormat for more information
     */
    public void setFormat(Transaction txn, String format) {
        setStringValue(txn, "format", format);
    }
    
    /**
     * Returns the format of this Song
     */
    public String getFormat() {
        return getStringValue(format);
    }
    
    /**
     * Sets the genre of this Song.
     * Note: See SongGenre for more information
     */
    public void setGenre(Transaction txn, String genre) {
        setStringValue(txn, "genre", genre);
    }
    
    /**
     * Returns the genre of this Song
     */
    public String getGenre() {
        return getStringValue(genre);
    }
    
    /**
     * Unknown purpose
     */
    public void setRelativeVolume(Transaction txn, int relativeVolume) {
        setSByteValue(txn, "relativeVolume", relativeVolume);
    }
    
    /**
     * Unknown purpose
     */
    public int getRelativeVolume() {
        return getSByteValue(relativeVolume);
    }
    
    /**
     * Sets the sample rate of this Song in kHz
     */
    public void setSampleRate(Transaction txn, long sampleRate) {
        setUIntValue(txn, "sampleRate", sampleRate);
    }
    
    /**
     * Returns the sample rate of this Song
     */
    public long getSampleRate() {
        return getUIntValue(sampleRate);
    }
    
    /**
     * Sets the file size of this Song
     */
    public void setSize(Transaction txn, long size) {
        setUIntValue(txn, "size", size);
    }
    
    /**
     * Returns the file size of this Song
     */
    public long getSize() {
        return getUIntValue(size);
    }
    
    /**
     * Sets the start time of this Song in 
     * <tt>milliseconds</tt>.
     */
    public void setStartTime(Transaction txn, long startTime) {
        setUIntValue(txn, "startTime", startTime);
    }
    
    /**
     * Returns the start time of this Song
     */
    public long getStartTime() {
        return getUIntValue(startTime);
    }
    
    /**
     * Sets the stop time of this Song in 
     * <tt>milliseconds</tt>.
     */
    public void setStopTime(Transaction txn, long stopTime) {
        setUIntValue(txn, "stopTime", stopTime);
    }
    
    /**
     * Returns the stop time of this Song
     */
    public long getStopTime() {
        return getUIntValue(stopTime);
    }
    
    /**
     * Sets the time (length) of this Song in
     * <tt>milliseconds</tt>.
     */
    public void setTime(Transaction txn, long time) {
        setUIntValue(txn, "time", time);
    }
    
    /**
     * Returns the time (length) of this Song
     */
    public long getTime() {
        return getUIntValue(time);
    }
    
    /**
     * Sets the track count of this Song
     */
    public void setTrackCount(Transaction txn, int trackCount) {
        setUShortValue(txn, "trackCount", trackCount);
    }
    
    /**
     * Returns the track count of this Song
     */
    public int getTrackCount() {
        return getUShortValue(trackCount);
    }
    
    /**
     * Sets the track number of this Song
     */
    public void setTrackNumber(Transaction txn, int trackNumber) {
        setUShortValue(txn, "trackNumber", trackNumber);
    }
    
    /**
     * Returns the track number of this Song
     */
    public int getTrackNumber() {
        return getUShortValue(trackNumber);
    }
    
    /**
     * Sets the user rating of this Song.
     * Note: See SongUserRating for more informations
     */
    public void setUserRating(Transaction txn, int userRating) {
        setUByteValue(txn, "userRating", userRating);
    }
    
    /**
     * Returns the user rating of this Song
     */
    public int getUserRating() {
        return getUByteValue(userRating);
    }
    
    /**
     * Sets the year of this Song
     */
    public void setYear(Transaction txn, int year) {
        setUShortValue(txn, "year", year);
    }
    
    /**
     * Returns the year of this Song
     */
    public int getYear() {
        return getUShortValue(year);
    }
    
    /**
     * Sets the grouping of this Song
     */
    public void setGrouping(Transaction txn, String grouping) {
        setStringValue(txn, "grouping", grouping);
    }
    
    /**
     * Returns the grouping of this Song
     */
    public String getGrouping() {
        return getStringValue(grouping);
    }
    
    /**
     * Sets the ITMS Artist Id
     */
    public void setITMSArtistId(Transaction txn, long itmsArtistId) {
        setUIntValue(txn, "itmsArtistId", itmsArtistId);
    }
    
    /**
     * Returns the ITMS Artist Id
     */
    public long getITMSArtistId() {
        return getUIntValue(itmsArtistId);
    }
    
    /**
     * Sets the ITMS Composer Id
     */
    public void setITMSComposerId(Transaction txn, long itmsComposerId) {
        setUIntValue(txn, "itmsComposerId", itmsComposerId);
    }
    
    /**
     * Returns the ITMS Composer Id
     */
    public long getITMSComposerId() {
        return getUIntValue(itmsComposerId);
    }
    
    /**
     * Sets the ITMS Genre Id
     */
    public void setITMSGenreId(Transaction txn, long itmsGenreId) {
        setUIntValue(txn, "itmsGenreId", itmsGenreId);
    }
    
    /**
     * Returns the ITMS Genre Id
     */
    public long getITMSGenreId() {
        return getUIntValue(itmsGenreId);
    }
    
    /**
     * Sets the ITMS Playlist (=Album) Id
     */
    public void setITMSPlaylistId(Transaction txn, long itmsPlaylistId) {
        setUIntValue(txn, "itmsPlaylistId", itmsPlaylistId);
    }
    
    /**
     * Returns the ITMS Playlist (=Album) Id
     */
    public long getITMSPlaylistId() {
        return getUIntValue(itmsPlaylistId);
    }
    
    /**
     * Sets the ITMS Storefront Id
     */
    public void setITMSStorefrontId(Transaction txn, long itmsStorefrontId) {
        setUIntValue(txn, "itmsStorefrontId", itmsStorefrontId);
    }
    
    /**
     * Returns the ITMS Storefront Id
     */
    public long getITMSStrorefrontId() {
        return getUIntValue(itmsStorefrontId);
    }
    
    /**
     * Sets the ITMS Song Id
     */
    public void setITMSSongId(Transaction txn, long itmsSongId) {
        setUIntValue(txn, "itmsSongId", itmsSongId);
    }
    
    /**
     * Returns the ITMS Song Id
     */
    public long getITMSSongId() {
        return getUIntValue(itmsSongId);
    }
    
    /**
     * Sets the codec type
     */
    public void setCodecType(Transaction txn, long codecType) {
        setUIntValue(txn, "codecType", codecType);
    }
    
    /**
     * Returns the codec type
     */
    public long getCodecType() {
        return getUIntValue(codecType);
    }
    
    /**
     * Sets the codec subtype
     */
    public void setCodecSubtype(Transaction txn, long codecSubtype) {
        setUIntValue(txn, "codecSubtype", codecSubtype);
    }
    
    /**
     * Returns the codec subtype
     */
    public long getCodecSubtype() {
        return getUIntValue(codecSubtype);
    }
    
    /**
     * Sets the norm volume
     */
    public void setNormVolume(Transaction txn, long normVolume) {
        setUIntValue(txn, "normVolume", normVolume);
    }
    
    /**
     * Returns the norm volume of this Song
     */
    public long getNormVolume() {
        return getUIntValue(normVolume);
    }
    
    /**
     * 
     */
    public void setPodcast(Transaction txn, boolean podcast) {
        setBooleanValue(txn, "podcast", podcast);
    }
    
    /**
     * 
     */
    public boolean isPodcast() {
        return getBooleanValue(podcast);
    }
    
    /**
     * 
     */
    public void setCategory(Transaction txn, String category) {
        setStringValue(txn, "category", category);
    }
    
    /**
     * 
     */
    public String getCategory() {
        return getStringValue(category);
    }
    
    /**
     * 
     */
    public void setContentDescription(Transaction txn, String contentDescription) {
        setStringValue(txn, "contentDescription", contentDescription);
    }
    
    /**
     * 
     */
    public String getContentDescription() {
        return getStringValue(contentDescription);
    }
    
    /**
     * 
     */
    public void setContentRating(Transaction txn, int contentRating) {
        setUByteValue(txn, "contentRating", contentRating);
    }
    
    /**
     * 
     */
    public int getContentRating() {
        return getUByteValue(contentRating);
    }
    
    /**
     * 
     */
    public void setKeywords(Transaction txn, String keywords) {
        setStringValue(txn, "keywords", keywords);
    }
    
    /**
     * 
     */
    public String getKeywords() {
        return getStringValue(keywords);
    }
    
    /**
     * 
     */
    public void setLongDescription(Transaction txn, String longDescription) {
        setStringValue(txn, "longDescription", longDescription);
    }
    
    /**
     * 
     */
    public String getLongDescription() {
        return getStringValue(longDescription);
    }
    
    /**
     * Sets wheather or not this Song contains Video data
     */
    public void setHasVideo(Transaction txn, boolean hasVideo) {
        setBooleanValue(txn, "hasVideo", hasVideo);
    }
    
    /**
     * Returns wheather or not this Song contains Video data
     */
    public boolean hasVideo() {
        return getBooleanValue(hasVideo);
    }
    
    /**
     * 
     */
    private void addChunk(Chunk chunk) {
        if (chunk != null) {
            chunks.put(chunk.getName(), chunk);
        }
    }
    
    protected Chunk getChunk(String name) {
        return chunks.get(name);
    }
    
    /**
     * Sets a new attachment and returns the old attachment. 
     * The attachment is an arbitrary Object but most likely
     * a File, path, URI etc.
     */
    public Object setAttachment(Object attachment) {
        Object old = this.attachment;
        this.attachment = attachment;
        return old;
    }
    
    /**
     * Returns the attachment.
     */
    public Object getAttachment() {
        return attachment;
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        
        buffer.append("Name: ").append(getName()).append("\n");
        buffer.append("ID: ").append(getItemId()).append("\n");
        buffer.append("Artist: ").append(getArtist()).append("\n");
        buffer.append("Album: ").append(getAlbum()).append("\n");
        buffer.append("Bitrate: ").append(getBitrate()).append("\n");
        buffer.append("Genre: ").append(getGenre()).append("\n");
        buffer.append("Comment: ").append(getComment()).append("\n");
        
        return buffer.append("\n").toString();
    }
    
    public int hashCode() {
        return (int)getItemId();
    }
    
    public boolean equals(Object o) {
        if (!(o instanceof Song)) {
            return false;
        }
        
        return ((Song)o).getItemId() == getItemId();
    }
    
    protected void setBooleanValue(Transaction txn, String fieldName, boolean value) {
        if (txn != null) {
            txn.addTxn(this, createNewTxn(fieldName, value));
        } else {
            setValue(fieldName, value);
        }
    }
    
    protected void setUByteValue(Transaction txn, String fieldName, int value) {
        UByteChunk.checkUByteRange(value);
        if (txn != null) {
            txn.addTxn(this, createNewTxn(fieldName, value));
        } else {
            setValue(fieldName, value);
        }
    }
    
    protected void setSByteValue(Transaction txn, String fieldName, int value) {
        SByteChunk.checkSByteRange(value);
        if (txn != null) {
            txn.addTxn(this, createNewTxn(fieldName, value));
        } else {
            setValue(fieldName, value);
        }
    }
    
    protected void setUShortValue(Transaction txn, String fieldName, int value) {
        UShortChunk.checkUShortRange(value);
        if (txn != null) {
            txn.addTxn(this, createNewTxn(fieldName, value));
        } else {
            setValue(fieldName, value);
        }
    }
    
    protected void setSShortValue(Transaction txn, String fieldName, int value) {
        SShortChunk.checkSShortRange(value);
        if (txn != null) {
            txn.addTxn(this, createNewTxn(fieldName, value));
        } else {
            setValue(fieldName, value);
        }
    }
    
    protected void setUIntValue(Transaction txn, String fieldName, long value) {
        UIntChunk.checkUIntRange(value);
        if (txn != null) {
            txn.addTxn(this, createNewTxn(fieldName, value));
        } else {
            setValue(fieldName, value);
        }
    }
    
    protected void setSIntValue(Transaction txn, String fieldName, int value) {
        //SIntChunk.checkSIntRange(value);
        if (txn != null) {
            txn.addTxn(this, createNewTxn(fieldName, value));
        } else {
            setValue(fieldName, value);
        }
    }
    
    protected void setULongValue(Transaction txn, String fieldName, long value) {
        //ULongChunk.checkULongRange(value);
        if (txn != null) {
            txn.addTxn(this, createNewTxn(fieldName, value));
        } else {
            setValue(fieldName, value);
        }
    }
    
    protected void setSLongValue(Transaction txn, String fieldName, long value) {
        //SLongChunk.checkSLongRange(value);
        if (txn != null) {
            txn.addTxn(this, createNewTxn(fieldName, value));
        } else {
            setValue(fieldName, value);
        }
    }
    
    protected void setStringValue(Transaction txn, String fieldName, String value) {
        if (txn != null) {
            txn.addTxn(this, createNewTxn(fieldName, value));
        } else {
            setValue(fieldName, value);
        }
    }
    
    protected void setDateValue(Transaction txn, String fieldName, long value) {
        DateChunk.checkDateRange(value);
        if (txn != null) {
            txn.addTxn(this, createNewTxn(fieldName, value));
        } else {
            setValue(fieldName, value);
        }
    }
    
    protected boolean getBooleanValue(BooleanChunk chunk) {
        return (chunk != null) ? chunk.getBooleanValue() : false;
    }
    
    protected int getSByteValue(SByteChunk chunk) {
        return (chunk != null) ? chunk.getValue() : 0;
    }
    
    protected int getUByteValue(UByteChunk chunk) {
        return (chunk != null) ? chunk.getValue() : 0;
    }
    
    protected int getUShortValue(UShortChunk chunk) {
        return (chunk != null) ? chunk.getValue() : 0;
    }

    protected long getUIntValue(UIntChunk chunk) {
        return (chunk != null) ? chunk.getUnsignedValue() : 0;
    }
    
    protected long getLongValue(LongChunk chunk) {
        return (chunk != null) ? chunk.getValue() : 0;
    }
    
    protected long getDateValue(DateChunk chunk) {
        return (chunk != null) ? chunk.getValue() : 0;
    }
    
    protected String getStringValue(StringChunk chunk) {
        return (chunk != null) ? chunk.getValue() : null;
    }
    
    protected Txn createNewTxn(final String name, boolean value) {
        return createNewTxn(name, boolean.class, new Boolean(value));
    }
    
    protected Txn createNewTxn(final String name, int value) {
        return createNewTxn(name, int.class, new Integer(value));
    }
    
    protected Txn createNewTxn(final String name, long value) {
        return createNewTxn(name, long.class, new Long(value));
    }

    protected Txn createNewTxn(final String name, String value) {
        return createNewTxn(name, String.class, value);
    }
    
    protected Txn createNewTxn(final String fieldName, final Class valueClass, final Object value) {
        Txn txn = new Txn() {
            public void commit(Transaction txn) {
                setValue(fieldName, valueClass, value);
            }
        };
        
        return txn;
    }
    
    protected void setValue(String fieldName, boolean value) {
        setValue(fieldName, boolean.class, new Boolean(value));
    }
    
    protected void setValue(String fieldName, int value) {
        setValue(fieldName, int.class, new Integer(value));
    }
    
    protected void setValue(String fieldName, long value) {
        setValue(fieldName, long.class, new Long(value));
    }

    protected void setValue(String fieldName, String value) {
        setValue(fieldName, String.class, value);
    }
    
    protected void setValue(String fieldName, Class valueClass, Object value) {
        try {

            Field field = Song.class.getDeclaredField(fieldName);
            field.setAccessible(true);

            Chunk chunk = (Chunk)field.get(this);
            if (chunk == null) {
                Constructor con = field.getType().getConstructor(new Class[] { valueClass });
                chunk = (Chunk)con.newInstance(new Object[]{value});
                field.set(this, chunk);
                addChunk(chunk);
            } else {
                Method method = field.getType().getMethod("setValue", new Class[]{ valueClass });
                method.invoke(chunk, new Object[]{value});
            }
            
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
    }
}
