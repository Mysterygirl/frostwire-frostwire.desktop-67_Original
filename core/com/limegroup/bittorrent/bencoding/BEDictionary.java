package com.limegroup.bittorrent.bencoding;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.Map;

/**
 * A token used to represent a bencoded Dictionary (Mapping) of keys to values.
 */
class BEDictionary extends BEAbstractCollection<Map<String, Object>> {
    
    BEDictionary(ReadableByteChannel chan) {
        super(chan);
    }
    
    public int getType() {
        return DICTIONARY;
    }
    
    protected Map<String, Object> createCollection() {
        return new HashMap<String, Object>();
    }
    
    protected void add(Object o) {
        BEEntry e = (BEEntry)o;
        result.put(e.key, e.value);
    }
    
    protected Token<?> getNewElement() {
        return new BEEntry(chan);
    }
    
    /**
     * A token used to represent a bencoded mapping of a Key -> Value
     * The Key must be a String.
     */
    private static class BEEntry extends Token<Object> {
        /** Token for the parsing of the key */
        private BEString keyToken;
        /** The key itself */
        private String key;
        /** Token for the parsing of the value */
        private Token valueToken;
        /** The value itself */
        private Object value;
        /** Whether this is the last entry in the map */
        private boolean lastEntry;
        
        BEEntry (ReadableByteChannel chan) {
            super(chan);
            result = this;
        }
        
        public void handleRead() throws IOException {
            if (keyToken == null && key == null) {
                Token t = getNextToken(chan);
                if (t != null) {
                    if (t instanceof BEString) { 
                        keyToken = (BEString)t;
                    } else if (t == Token.TERMINATOR) {
                        lastEntry = true;
                        return;
                    } else
                        throw new IOException("invalid entry - key not a string");
                } else 
                    return; // try again next time
            }
            
            if (key == null) {
                keyToken.handleRead();
                if (keyToken.getResult() != null) {
                	// technically keys don't necesssarily need to be String objects
                	// but in practice they are
                    key = new String(keyToken.getResult(),Token.ASCII);
                    keyToken = null; 
                }
                else
                    return; // try again next time
            }
            
            // if we got here we have fully read the key
            
            if (valueToken == null && value == null) {
                Token t = getNextToken(chan);
                if (t != null) 
                    valueToken = t;
                else
                    return; // try to figure out which type of token the value is next time
            }
         
            // we've read the type of the value, but not the value itself
            if (value == null) {
                valueToken.handleRead();
                value = valueToken.getResult();
                if (value == Token.TERMINATOR)
                    throw new IOException("missing value");
                if (value != null)
                    valueToken = null; //clean the ref
            } else
                throw new IllegalStateException("token is done - don't read to it "+key+" "+value);
        } 
        
        protected boolean isDone() {
            return key != null && value != null; 
        }
        
        public Object getResult() {
            if (lastEntry)
                return Token.TERMINATOR;
            return super.getResult();
        }
    }
}
