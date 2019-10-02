/*
 * $HeadURL: https://svn.apache.org/repos/asf/httpcomponents/httpcore/tags/4.0.1/httpcore/src/main/java/org/apache/http/HttpResponseInterceptor.java $
 * $Revision: 744522 $
 * $Date: 2009-02-14 17:56:03 +0100 (Sat, 14 Feb 2009) $
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

package org.apache.http;

import java.io.IOException;

import org.apache.http.protocol.HttpContext;

/**
 * HTTP protocol interceptor is a routine that implements a specific aspect of 
 * the HTTP protocol. Usually protocol interceptors are expected to act upon 
 * one specific header or a group of related headers of the incoming message 
 * or populate the outgoing message with one specific header or a group of 
 * related headers. Protocol 
 * <p>
 * Interceptors can also manipulate content entities enclosed with messages.
 * Usually this is accomplished by using the 'Decorator' pattern where a wrapper 
 * entity class is used to decorate the original entity. 
 * <p>
 * Protocol interceptors must be implemented as thread-safe. Similarly to 
 * servlets, protocol interceptors should not use instance variables unless 
 * access to those variables is synchronized.
 *
 *
 * <!-- empty lines above to avoid 'svn diff' context problems -->
 * @version $Revision: 744522 $
 * 
 * @since 4.0
 */
public interface HttpResponseInterceptor {

    /**
     * Processes a response.
     * On the server side, this step is performed before the response is
     * sent to the client. On the client side, this step is performed
     * on incoming messages before the message body is evaluated.
     *
     * @param response  the response to postprocess
     * @param context   the context for the request
     *
     * @throws HttpException in case of an HTTP protocol violation
     * @throws IOException in case of an I/O error
     */
    void process(HttpResponse response, HttpContext context) 
        throws HttpException, IOException;

}
