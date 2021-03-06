package com.limegroup.gnutella.messages;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IOUtils;
import org.limewire.util.Base32;
import org.limewire.util.CommonUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.util.Data;

@Singleton
public final class StaticMessages {
    
    private static final Log LOG = LogFactory.getLog(StaticMessages.class);

    private volatile QueryReply updateReply;
    private volatile QueryReply limeReply;
    
    private final QueryReplyFactory queryReplyFactory;
    
    
    @Inject
    public StaticMessages(QueryReplyFactory queryReplyFactory) {
        this.queryReplyFactory = queryReplyFactory;

    }
   
    public void initialize() {
        reloadMessages();
    }
    
    private void reloadMessages() {
        updateReply = readUpdateReply();
        limeReply = createLimeReply();
    }
    
    private QueryReply readUpdateReply() {
        try {
            return createReply(new FileInputStream(new File(CommonUtils.getUserSettingsDir(), "data.ser")));
        } catch (FileNotFoundException bad) {
            return null;
        }
    }
    
    private QueryReply createLimeReply() {
        byte [] reply = Base32.decode(SearchSettings.LIME_SIGNED_RESPONSE.getValue());
        return createReply(new ByteArrayInputStream(reply));
    }
    
    private QueryReply createReply(InputStream source) {
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(source);
            byte[] payload = ((Data) in.readObject()).data;
            return queryReplyFactory.createFromNetwork(new byte[16], (byte) 1,
                    (byte) 0, payload);
        } catch (Throwable t) {
            LOG.error("Unable to read serialized data", t);
            return null;
        } finally {
            IOUtils.close(in);
        }
    }
    
    public QueryReply getUpdateReply() {
        return updateReply;
    }
    
    public QueryReply getLimeReply() {
        return limeReply;
    }
}
