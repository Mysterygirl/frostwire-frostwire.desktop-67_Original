/*
 * ====================================================================
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.http.impl.conn;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSession;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpConnectionMetrics;
import org.apache.http.conn.OperatedClientConnection;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.ClientConnectionManager;

/**
 * Abstract adapter from {@link OperatedClientConnection operated} to
 * {@link ManagedClientConnection managed} client connections.
 * Read and write methods are delegated to the wrapped connection.
 * Operations affecting the connection state have to be implemented
 * by derived classes. Operations for querying the connection state
 * are delegated to the wrapped connection if there is one, or
 * return a default value if there is none.
 * <p>
 * This adapter tracks the checkpoints for reusable communication states,
 * as indicated by {@link #markReusable markReusable} and queried by
 * {@link #isMarkedReusable isMarkedReusable}.
 * All send and receive operations will automatically clear the mark.
 * <p>
 * Connection release calls are delegated to the connection manager,
 * if there is one. {@link #abortConnection abortConnection} will
 * clear the reusability mark first. The connection manager is
 * expected to tolerate multiple calls to the release method.
 *
 * @since 4.0
 */
public abstract class AbstractClientConnAdapter implements ManagedClientConnection {

    /** Thread that requested this connection. */
    private final Thread executionThread; 
    
    /**
     * The connection manager, if any.
     * This attribute MUST NOT be final, so the adapter can be detached
     * from the connection manager without keeping a hard reference there.
     */
    private volatile ClientConnectionManager connManager;

    /** The wrapped connection. */
    private volatile OperatedClientConnection wrappedConnection;

    /** The reusability marker. */
    private volatile boolean markedReusable;

    /** True if the connection has been aborted. */
    private volatile boolean aborted;
    
    /** The duration this is valid for while idle (in ms). */
    private volatile long duration;

    /**
     * Creates a new connection adapter.
     * The adapter is initially <i>not</i>
     * {@link #isMarkedReusable marked} as reusable.
     *
     * @param mgr       the connection manager, or <code>null</code>
     * @param conn      the connection to wrap, or <code>null</code>
     */
    protected AbstractClientConnAdapter(ClientConnectionManager mgr,
                                        OperatedClientConnection conn) {
        super();
        executionThread = Thread.currentThread();
        connManager = mgr;
        wrappedConnection = conn;
        markedReusable = false;
        aborted = false;
        duration = Long.MAX_VALUE;
    } // <constructor>


    /**
     * Detaches this adapter from the wrapped connection.
     * This adapter becomes useless.
     */
    protected void detach() {
        wrappedConnection = null;
        connManager = null; // base class attribute
        duration = Long.MAX_VALUE;
    }

    protected OperatedClientConnection getWrappedConnection() {
        return wrappedConnection;
    }
    
    protected ClientConnectionManager getManager() {
        return connManager;
    }
    
    /**
     * Asserts that the connection has not been aborted.
     *
     * @throws InterruptedIOException   if the connection has been aborted
     */
    protected final void assertNotAborted() throws InterruptedIOException {
        if (aborted) {
            throw new InterruptedIOException("Connection has been shut down.");
        }
    }

    /**
     * Asserts that there is a wrapped connection to delegate to.
     *
     * @throws IllegalStateException    if there is no wrapped connection
     *                                  or connection has been aborted
     */
    protected final void assertValid(
            final OperatedClientConnection wrappedConn) {
        if (wrappedConn == null) {
            throw new IllegalStateException("No wrapped connection.");
        }
    }

    public boolean isOpen() {
        OperatedClientConnection conn = getWrappedConnection();
        if (conn == null)
            return false;

        return conn.isOpen();
    }

    public boolean isStale() {
        if (aborted)
            return true;
        OperatedClientConnection conn = getWrappedConnection();
        if (conn == null)
            return true;

        return conn.isStale();
    }

    public void setSocketTimeout(int timeout) {
        OperatedClientConnection conn = getWrappedConnection();
        assertValid(conn);
        conn.setSocketTimeout(timeout);
    }

    public int getSocketTimeout() {
        OperatedClientConnection conn = getWrappedConnection();
        assertValid(conn);
        return conn.getSocketTimeout();
    }

    public HttpConnectionMetrics getMetrics() {
        OperatedClientConnection conn = getWrappedConnection();
        assertValid(conn);
        return conn.getMetrics();
    }

    public void flush()
        throws IOException {

        assertNotAborted();
        OperatedClientConnection conn = getWrappedConnection();
        assertValid(conn);

        conn.flush();
    }

    public boolean isResponseAvailable(int timeout)
        throws IOException {

        assertNotAborted();
        OperatedClientConnection conn = getWrappedConnection();
        assertValid(conn);

        return conn.isResponseAvailable(timeout);
    }

    public void receiveResponseEntity(HttpResponse response)
        throws HttpException, IOException {

        assertNotAborted();
        OperatedClientConnection conn = getWrappedConnection();
        assertValid(conn);

        unmarkReusable();
        conn.receiveResponseEntity(response);
    }

    public HttpResponse receiveResponseHeader()
        throws HttpException, IOException {

        assertNotAborted();
        OperatedClientConnection conn = getWrappedConnection();
        assertValid(conn);

        unmarkReusable();
        return conn.receiveResponseHeader();
    }

    public void sendRequestEntity(HttpEntityEnclosingRequest request)
        throws HttpException, IOException {

        assertNotAborted();
        OperatedClientConnection conn = getWrappedConnection();
        assertValid(conn);

        unmarkReusable();
        conn.sendRequestEntity(request);
    }

    public void sendRequestHeader(HttpRequest request)
        throws HttpException, IOException {

        assertNotAborted();
        OperatedClientConnection conn = getWrappedConnection();
        assertValid(conn);
        
        unmarkReusable();
        conn.sendRequestHeader(request);
    }

    public InetAddress getLocalAddress() {
        OperatedClientConnection conn = getWrappedConnection();
        assertValid(conn);
        return conn.getLocalAddress();
    }

    public int getLocalPort() {
        OperatedClientConnection conn = getWrappedConnection();
        assertValid(conn);
        return conn.getLocalPort();
    }

    public InetAddress getRemoteAddress() {
        OperatedClientConnection conn = getWrappedConnection();
        assertValid(conn);
        return conn.getRemoteAddress();
    }

    public int getRemotePort() {
        OperatedClientConnection conn = getWrappedConnection();
        assertValid(conn);
        return conn.getRemotePort();
    }

    public boolean isSecure() {
        OperatedClientConnection conn = getWrappedConnection();
        assertValid(conn);
        return conn.isSecure();
    }

    public SSLSession getSSLSession() {
        OperatedClientConnection conn = getWrappedConnection();
        assertValid(conn);
        if (!isOpen())
            return null;

        SSLSession result = null;
        Socket    sock    = conn.getSocket();
        if (sock instanceof SSLSocket) {
            result = ((SSLSocket)sock).getSession();
        }
        return result;
    }

    public void markReusable() {
        markedReusable = true;
    }

    public void unmarkReusable() {
        markedReusable = false;
    }

    public boolean isMarkedReusable() {
        return markedReusable;
    }
    
    public void setIdleDuration(long duration, TimeUnit unit) {
        if(duration > 0) {
            this.duration = unit.toMillis(duration);
        } else {
            this.duration = -1;
        }
    }

    public void releaseConnection() {
        if (connManager != null) {
            connManager.releaseConnection(this, duration, TimeUnit.MILLISECONDS);
        }
    }

    public void abortConnection() {
        if (aborted) {
            return;
        }
        aborted = true;
        unmarkReusable();
        try {
            shutdown();
        } catch (IOException ignore) {
        }
        // Usually #abortConnection() is expected to be called from 
        // a helper thread in order to unblock the main execution thread 
        // blocked in an I/O operation. It may be unsafe to call 
        // #releaseConnection() from the helper thread, so we have to rely
        // on an IOException thrown by the closed socket on the main thread 
        // to trigger the release of the connection back to the 
        // connection manager.
        // 
        // However, if this method is called from the main execution thread 
        // it should be safe to release the connection immediately. Besides, 
        // this also helps ensure the connection gets released back to the 
        // manager if #abortConnection() is called from the main execution 
        // thread while there is no blocking I/O operation.
        if (executionThread.equals(Thread.currentThread())) {
            releaseConnection();
        }
    }

}
