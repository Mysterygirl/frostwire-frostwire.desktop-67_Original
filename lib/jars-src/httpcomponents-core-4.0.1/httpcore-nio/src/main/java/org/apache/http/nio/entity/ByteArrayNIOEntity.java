/*
 * $HeadURL: https://svn.apache.org/repos/asf/httpcomponents/httpcore/tags/4.0.1/httpcore-nio/src/main/java/org/apache/http/nio/entity/ByteArrayNIOEntity.java $
 * $Revision: 744570 $
 * $Date: 2009-02-14 22:20:20 +0100 (Sat, 14 Feb 2009) $
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

package org.apache.http.nio.entity;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;

/**
 * An entity whose content is retrieved from a byte array. In addition to the 
 * standard {@link HttpEntity} interface this class also implements NIO specific 
 * {@link HttpNIOEntity}.
 *
 * @deprecated Use {@link NByteArrayEntity}
 * 
 * @version $Revision: 744570 $
 * 
 * @since 4.0
 */
@Deprecated
public class ByteArrayNIOEntity extends ByteArrayEntity implements HttpNIOEntity {

    public ByteArrayNIOEntity(final byte[] b) {
        super(b);
    }

    public ReadableByteChannel getChannel() throws IOException {
        return Channels.newChannel(getContent());
    }

}
