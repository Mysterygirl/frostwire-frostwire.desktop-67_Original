/*
 * $HeadURL: https://svn.apache.org/repos/asf/httpcomponents/httpcore/tags/4.0.1/httpcore-nio/src/main/java/org/apache/http/impl/nio/codecs/AbstractMessageParser.java $
 * $Revision: 744538 $
 * $Date: 2009-02-14 18:20:23 +0100 (Sat, 14 Feb 2009) $
 *
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.http.impl.nio.codecs;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpException;
import org.apache.http.HttpMessage;
import org.apache.http.ParseException;
import org.apache.http.ProtocolException;
import org.apache.http.message.LineParser;
import org.apache.http.message.BasicLineParser;
import org.apache.http.nio.NHttpMessageParser;
import org.apache.http.nio.reactor.SessionInputBuffer;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.util.CharArrayBuffer;

/**
 * Abstract {@link NHttpMessageParser} that serves as a base for all message 
 * parser implementations.
 *
 *
 * @version $Revision: 744538 $
 *
 * @since 4.0
 */
public abstract class AbstractMessageParser implements NHttpMessageParser {
    
    private final SessionInputBuffer sessionBuffer;    
    
    private static final int READ_HEAD_LINE = 0;
    private static final int READ_HEADERS   = 1;
    private static final int COMPLETED      = 2;
    
    private int state;
    private boolean endOfStream;

    private HttpMessage message;
    private CharArrayBuffer lineBuf;
    private final List<CharArrayBuffer> headerBufs;

    private int maxLineLen = -1;
    private int maxHeaderCount = -1;
    protected final LineParser lineParser;

    /**
     * Creates an instance of this class.
     * <p>
     * The following HTTP parameters affect the initialization:
     * <p>
     * {@link CoreConnectionPNames#MAX_HEADER_COUNT} parameter determines 
     * the maximum HTTP header count allowed. If set to a positive value, 
     * the number of HTTP headers received from the data stream exceeding 
     * this limit will cause an IOException. A negative or zero value will 
     * effectively disable the check. Per default the check is disabled. 
     * <p>
     * {@link CoreConnectionPNames#MAX_LINE_LENGTH} parameter determines 
     * the maximum line length limit. If set to a positive value, any HTTP line 
     * exceeding this limit will cause an IOException. A negative or zero value
     * will effectively disable the check the check. Per default the check is 
     * disabled.
     * 
     * @param buffer the session input buffer.
     * @param parser the line parser.
     * @param params HTTP parameters.
     */
    public AbstractMessageParser(final SessionInputBuffer buffer, final LineParser parser, final HttpParams params) {
        super();
        if (buffer == null) {
            throw new IllegalArgumentException("Session input buffer may not be null");
        }
        if (params == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        this.sessionBuffer = buffer;
        this.state = READ_HEAD_LINE;
        this.endOfStream = false;
        this.headerBufs = new ArrayList<CharArrayBuffer>();        
        this.maxLineLen = params.getIntParameter(
                CoreConnectionPNames.MAX_LINE_LENGTH, -1);
        this.maxHeaderCount = params.getIntParameter(
                CoreConnectionPNames.MAX_HEADER_COUNT, -1);
        this.lineParser = (parser != null) ? parser : BasicLineParser.DEFAULT;
    }
    
    public void reset() {
        this.state = READ_HEAD_LINE;
        this.endOfStream = false;
        this.headerBufs.clear();
        this.message = null;
    }
    
    public int fillBuffer(final ReadableByteChannel channel) throws IOException {
        int bytesRead = this.sessionBuffer.fill(channel);
        if (bytesRead == -1) {
            this.endOfStream = true;
        }
        return bytesRead;
    }
    
    /**
     * Creates {@link HttpMessage} instance based on the content of the input
     *  buffer containing the first line of the incoming HTTP message.
     *  
     * @param buffer the line buffer.
     * @return HTTP message.
     * @throws HttpException in case of HTTP protocol violation
     * @throws ParseException in case of a parse error.
     */
    protected abstract HttpMessage createMessage(CharArrayBuffer buffer) 
        throws HttpException, ParseException;
    
    private void parseHeadLine() throws HttpException, ParseException {
        this.message = createMessage(this.lineBuf);
    }
    
    private void parseHeader() throws IOException {
        CharArrayBuffer current = this.lineBuf;
        int count = this.headerBufs.size();
        if ((this.lineBuf.charAt(0) == ' ' || this.lineBuf.charAt(0) == '\t') && count > 0) {
            // Handle folded header line
            CharArrayBuffer previous = this.headerBufs.get(count - 1);
            int i = 0;
            while (i < current.length()) {
                char ch = current.charAt(i);
                if (ch != ' ' && ch != '\t') {
                    break;
                }
                i++;
            }
            if (this.maxLineLen > 0 
                    && previous.length() + 1 + current.length() - i > this.maxLineLen) {
                throw new IOException("Maximum line length limit exceeded");
            }
            previous.append(' ');
            previous.append(current, i, current.length() - i);
        } else {
            this.headerBufs.add(current);
            this.lineBuf = null;
        }
    }

    public HttpMessage parse() throws IOException, HttpException {
        while (this.state != COMPLETED) {
            if (this.lineBuf == null) {
                this.lineBuf = new CharArrayBuffer(64);
            } else {
                this.lineBuf.clear();
            }
            boolean lineComplete = this.sessionBuffer.readLine(this.lineBuf, this.endOfStream);
            if (this.maxLineLen > 0 && 
                    (this.lineBuf.length() > this.maxLineLen || 
                            (!lineComplete && this.sessionBuffer.length() > this.maxLineLen))) {
                throw new IOException("Maximum line length limit exceeded");
            }
            if (!lineComplete) {
                break;
            }

            switch (this.state) {
            case READ_HEAD_LINE:
                try {
                    parseHeadLine();
                } catch (ParseException px) {
                    throw new ProtocolException(px.getMessage(), px);
                }
                this.state = READ_HEADERS;
                break;
            case READ_HEADERS:
                if (this.lineBuf.length() > 0) {
                    if (this.maxHeaderCount > 0 && headerBufs.size() >= this.maxHeaderCount) {
                        throw new IOException("Maximum header count exceeded");
                    }
                    
                    parseHeader();
                } else {
                    this.state = COMPLETED;
                }
                break;
            }
            if (this.endOfStream && !this.sessionBuffer.hasData()) {
                this.state = COMPLETED;
            }
        }
        if (this.state == COMPLETED) {
            for (int i = 0; i < this.headerBufs.size(); i++) {
                CharArrayBuffer buffer = this.headerBufs.get(i);
                try {
                    this.message.addHeader(lineParser.parseHeader(buffer));
                } catch (ParseException ex) {
                    throw new ProtocolException(ex.getMessage(), ex);
                }
            }
            return this.message;
        } else {
            return null;
        }
    }

}
