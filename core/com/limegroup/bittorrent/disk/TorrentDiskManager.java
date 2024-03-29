package com.limegroup.bittorrent.disk;

import java.io.IOException;
import java.util.Set;

import org.limewire.collection.BitField;
import org.limewire.collection.NECallable;

import com.limegroup.bittorrent.BTInterval;
import com.limegroup.bittorrent.BTPiece;
import com.limegroup.bittorrent.PieceReadListener;
import com.limegroup.gnutella.downloader.serial.BTDiskManagerMemento;

/**
 * A facility that manages access to the collection of files 
 * belonging to a torrent and keeps track of which ranges are
 * verified, missing, requested, verified.
 */
public interface TorrentDiskManager {

	/**
	 * Opens this TorrentDiskManager.  MUST be called before anything
	 * else.
	 */
	public void open(final DiskManagerListener torrent) throws IOException;

	/**
	 * closes all file descriptors used by this.
	 */
	public void close();

	/**
	 * determines whether the files for this torrent are open.
	 */
	public boolean isOpen();

	/**
	 * Requests that a write be performed for a block that will be
	 * generated by the provided callable.
	 */
	public void writeBlock(NECallable<BTPiece> factory);

	/**
	 * Requests that a piece be read from disk
	 * @param in the <tt>BTInterval</tt> representing the piece
	 * @param listener the <tt>PieceReadListener</tt> to notify once the read
	 * completes
	 * @throws IOException if something goes wrong
	 */
	public void requestPieceRead(BTInterval in, PieceReadListener listener);
	
	/**
	 * @return true if the provided block number is verified.
	 */
	public boolean hasBlock(int block);

	/**
	 * @return true if this is currently verifying any data found on disk
	 */
	public boolean isVerifying();

	/**
	 * @return true if the all the blocks have been written and verified
	 */
	public boolean isComplete();

	/**
	 * returns a random available range that has preferrably not yet been
	 * requested
	 * 
	 * @param bs the <tt>BitField</tt> of available ranges to choose from
	 * @param exclude the set of ranges that the connection is already about to
	 * request
	 * @return a BTInterval that should be requested next.
	 */
	public BTInterval leaseRandom(BitField bs, Set<BTInterval> exclude);

	/**
	 * Removes an interval from the internal list of already requested intervals.
	 * 
	 * Note that during endgame several connections may be requesting the same interval
	 * and as one of them fails that interval will no longer be considered requested.
	 * That's ok as it will only result in that interval requested again.
	 */
	public void releaseInterval(BTInterval in);

	/**
	 * Creates a bitfield
	 * 
	 * @return returns an array of byte where the i'th byte is 1 if we have
	 *         written and verified the i'th piece of the torrent and 0
	 *         otherwise
	 *         
	 */
	public byte[] createBitField();

	/**
	 * @return number of bytes written and verified
	 */
	public long getVerifiedBlockSize();

	/**
	 * @return number of bytes written
	 */
	public long getBlockSize();

	/**
	 * @return number of bytes corrupted
	 */
	public long getNumCorruptedBytes();

	/**
	 * @return true if the remote host has any pieces we miss
	 */
	public boolean containsAnyWeMiss(BitField other);
	
	/**
	 * @return the number of pieces that this has verified
	 * and are not set in the provided <tt>BitField</tt>
	 */
	public int getNumMissing(BitField other);
	
	/**
	 * @return the amount of data pending write
	 */
	public int getAmountPending();
	
	/**
	 * @return a memento describing this.
	 */
	public BTDiskManagerMemento toMemento();
    
    /**
     * @return the last offset inside the torrent filesystem that has been 
     * succesfully verified.
     */
    public long getLastVerifiedOffset();

}