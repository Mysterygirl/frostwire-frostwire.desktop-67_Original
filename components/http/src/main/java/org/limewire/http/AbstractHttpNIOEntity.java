package org.limewire.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.limewire.nio.NIODispatcher;
import org.limewire.nio.channel.InterestWritableByteChannel;
import org.limewire.nio.observer.Shutdownable;
import org.limewire.nio.timeout.StalledUploadWatchdog;

/**
 * Provides a default implementation of an event based HTTP entity that
 * implements {@link InterestWritableByteChannel}. Inherited methods from
 * {@HttpEntity} for streaming throw {@link UnsupportedOperationException}.
 * <p>
 * NOTE: This class only supports writing, reading has not been implemented.
 */
public abstract class AbstractHttpNIOEntity extends AbstractHttpEntity
        implements HttpNIOEntity {

    /** Cancels the transfer if inactivity for too long. */
    private StalledUploadWatchdog watchdog;

    private long timeout = StalledUploadWatchdog.DELAY_TIME;
    
    private boolean initialized = false;
    
    /** shutdownable to shut off in case of a timeout */
    private final Shutdownable timeoutable = new Shutdownable() {
        public void shutdown() {
            timeout();
        }
    };
    
    protected void activateTimeout() {
        if (this.watchdog == null) {
            this.watchdog = new StalledUploadWatchdog(timeout, NIODispatcher.instance().getScheduledExecutorService());
        }
        this.watchdog.activate(timeoutable);
    }

    protected void deactivateTimeout() {
        if (this.watchdog != null) {
            this.watchdog.deactivate();
        }
    }

    /**
     * Throws <code>UnsupportedOperationException</code>.
     */
    public InputStream getContent() throws IOException, IllegalStateException {
        throw new UnsupportedOperationException();
    }

    public abstract long getContentLength();
    
    /**
     * Throws <code>UnsupportedOperationException</code>.
     */
    public boolean isRepeatable() {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws <code>UnsupportedOperationException</code>.
     */
    public boolean isStreaming() {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws <code>UnsupportedOperationException</code>.
     */
    public void writeTo(OutputStream outstream) throws IOException {
        throw new UnsupportedOperationException();
    }

    public int consumeContent(ContentDecoder decoder, IOControl ioctrl)
            throws IOException {
        throw new RuntimeException("Not supported");
    }

    public void produceContent(ContentEncoder encoder, IOControl ioctrl) throws IOException {
        if (!initialized) {
            initialized = true;
            initialize(encoder, ioctrl);
        }
        
        if (!writeContent(encoder, ioctrl)) {
            encoder.complete();
        }
    }

    /**
     * Sub-classes need to implement this and and write data to the
     * ContentEncoder.
     * 
     * @throws IOException indicates an I/O error which will abort the
     *         connection
     * @return true, if more data is expected; false, if the transfer is
     *         complete
     */
    public abstract boolean writeContent(ContentEncoder contentEncoder, IOControl ioctrl)
            throws IOException;

    /**
     * Invoked before the first call to
     * {@link #writeContent(ContentEncoder, IOControl)}.
     * @param contentEncoder TODO
     * @param ioctrl TODO
     * 
     * @throws IOException indicates an I/O error which will abort the
     *         connection
     */
    public abstract void initialize(ContentEncoder contentEncoder, IOControl ioctrl) throws IOException;

    /**
     * Invoked after transfer has completed. This is true if either
     * {@link #writeContent(ContentEncoder, IOControl)} returns false or {@link #writeContent(ContentEncoder, IOControl)} or
     * {@link #initialize(ContentEncoder, IOControl)} thrown an exception.
     */
    public abstract void finished();

    /**
     * Invoked when the transfer times out. Needs to close the underlying
     * connection.
     */
    public abstract void timeout();
}
