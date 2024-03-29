package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;

import org.limewire.concurrent.ThreadExecutor;
import org.limewire.io.NetworkUtils;
import org.limewire.service.ErrorService;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.Message.Network;

/**
 * This class handles Multicast messages.
 * Currently, this only listens for messages from the Multicast group.
 * Sending is done on the GUESS port, so that other nodes can reply
 * appropriately to the individual request, instead of multicasting
 * replies to the whole group.
 *
 * @see UDPService
 * @see MessageRouter
 */
@Singleton
public final class MulticastService implements Runnable {

	/** 
     * LOCKING: Grab the _recieveLock before receiving.  grab the _sendLock
     * before sending.  Moreover, only one thread should be wait()ing on one of
     * these locks at a time or results cannot be predicted.
	 * This is the socket that handles sending and receiving messages over 
	 * Multicast.
	 * (Currently only used for recieving)
	 */
	private volatile MulticastSocket _socket;
	
    /**
     * Used for synchronized RECEIVE access to the Multicast socket.
     * Should only be used by the Multicast thread.
     */
    private final Object _receiveLock = new Object();
    
    /**
     * The group we're joined to listen to.
     */
    private InetAddress _group = null;
    
    /**
     * The port of the group we're listening to.
     */
    private int _port = -1;

	/**
	 * Constant for the size of Multicast messages to accept -- dependent upon
	 * IP-layer fragmentation.
	 */
	private final int BUFFER_SIZE = 1024 * 32;
	
	/**
	 * Buffer used for reading messages.
	 */
	private final byte[] HEADER_BUF = new byte[23];

	/**
	 * The thread for listening of incoming messages.
	 */
	private final Thread MULTICAST_THREAD;
	
	private final Provider<UDPService> udpService;
	private final Provider<MessageDispatcher> messageDispatcher;

    private final MessageFactory messageFactory;

	@Inject
    MulticastService(Provider<UDPService> udpService,
            Provider<MessageDispatcher> messageDispatcher,
            MessageFactory messageFactory) {
        this.udpService = udpService;
        this.messageDispatcher = messageDispatcher;
        this.messageFactory = messageFactory;
        MULTICAST_THREAD = ThreadExecutor.newManagedThread(this, "MulticastService");
        MULTICAST_THREAD.setDaemon(true);
    }

    /**
     * Starts the Multicast service.
     */
	public void start() {
        MULTICAST_THREAD.start();
    }
	    


    /** 
     * Returns a new MulticastSocket that is bound to the given port.  This
     * value should be passed to setListeningSocket(MulticastSocket) to commit
     * to the new port.  If setListeningSocket is NOT called, you should close
     * the return socket.
     * @return a new MulticastSocket that is bound to the specified port.
     * @exception IOException Thrown if the MulticastSocket could not be
     * created.
     */
    MulticastSocket newListeningSocket(int port, InetAddress group) throws IOException {
        try {
            MulticastSocket sock = new MulticastSocket(port);
            sock.setTimeToLive(3);
            sock.joinGroup(group);
            _port = port;
            _group = group;            
            return sock;
        }
        catch (SocketException se) {
            throw new IOException("socket could not be set on port: "+port);
        }
        catch (SecurityException se) {
            throw new IOException("security exception on port: "+port);
        }
    }


	/** 
     * Changes the MulticastSocket used for sending/receiving.
     * This must be common among all instances of LimeWire on the subnet.
     * It is not synched with the typical gnutella port, because that can
     * change on a per-servent basis.
     * Only MulticastService should mutate this.
     * @param multicastSocket the new listening socket, which must be be the
     *  return value of newListeningSocket(int).  A value of null disables 
     *  Multicast sending and receiving.
	 */
	void setListeningSocket(MulticastSocket multicastSocket) {
        //a) Close old socket (if non-null) to alert lock holders...
        if (_socket != null) 
            _socket.close();
        //b) Replace with new sock.  Notify the udpThread.
        synchronized (_receiveLock) {
            // if the input is null, then the service will shut off ;) .
            // leave the group if we're shutting off the service.
            if (multicastSocket == null 
             && _socket != null
             && _group != null) {
                try {
                    _socket.leaveGroup(_group);
                } catch(IOException ignored) {
                    // ideally we would check if the socket is closed,
                    // which would prevent the exception from happening.
                    // but that's only available on 1.4 ... 
                }                        
            }
            _socket = multicastSocket;
            _receiveLock.notify();
        }
	}


	/**
	 * Busy loop that accepts incoming messages sent over the
	 * multicast socket and dispatches them to their appropriate handlers.
	 */
	public void run() {
        try {
            byte[] datagramBytes = new byte[BUFFER_SIZE];
            while (true) {
                // prepare to receive
                DatagramPacket datagram = new DatagramPacket(datagramBytes, 
                                                             BUFFER_SIZE);
                
                // when you first can, try to recieve a packet....
                // *----------------------------
                synchronized (_receiveLock) {
                    while (_socket == null) {
                        try {
                            _receiveLock.wait();
                        }
                        catch (InterruptedException ignored) {
                            continue;
                        }
                    }
                    try {
                        _socket.receive(datagram);
                    } 
                    catch(InterruptedIOException e) {
                        continue;
                    } 
                    catch(IOException e) {
                        continue;
                    } 
                }
                // ----------------------------*                
                // process packet....
                // *----------------------------
                if(!NetworkUtils.isValidAddress(datagram.getAddress()))
                    continue;
                if(!NetworkUtils.isValidPort(datagram.getPort()))
                    continue;
                
                byte[] data = datagram.getData();
                try {
                    // we do things the old way temporarily
                    InputStream in = new ByteArrayInputStream(data);
                    Message message = messageFactory.read(in, Network.MULTICAST, HEADER_BUF, datagram.getSocketAddress());
                    if(message == null)
                        continue;
                    messageDispatcher.get().dispatchMulticast(message, (InetSocketAddress)datagram.getSocketAddress());
                }
                catch (IOException e) {
                    continue;
                }
                catch (BadPacketException e) {
                    continue;
                }
                // ----------------------------*
            }
        } catch(Throwable t) {
            ErrorService.error(t);
        }
	}

	/**
	 * Sends the <tt>Message</tt> using UDPService to the multicast
	 * address/port.
     *
	 * @param msg  the <tt>Message</tt> to send
	 */
    public synchronized void send(Message msg) {
        // only send the msg if we've initialized the port.
        if( _port != -1 ) {
            udpService.get().send(msg, _group, _port);
        }
	}

	/**
	 * Returns whether or not the Multicast socket is listening for incoming
	 * messsages.
	 *
	 * @return <tt>true</tt> if the Multicast socket is listening for incoming
	 *  Multicast messages, <tt>false</tt> otherwise
	 */
	public boolean isListening() {
		if(_socket == null) return false;
		return (_socket.getLocalPort() != -1);
	}

	/** 
	 * Overrides Object.toString to give more informative information
	 * about the class.
	 *
	 * @return the <tt>MulticastSocket</tt> data
	 */
	public String toString() {
		return "MulticastService\r\nsocket: "+_socket;
	}

}
