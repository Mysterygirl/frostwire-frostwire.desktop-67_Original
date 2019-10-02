/*
 * $HeadURL: https://svn.apache.org/repos/asf/httpcomponents/httpcore/tags/4.0.1/httpcore/src/main/java/org/apache/http/protocol/HttpService.java $
 * $Revision: 744532 $
 * $Date: 2009-02-14 18:12:18 +0100 (Sat, 14 Feb 2009) $
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

package org.apache.http.protocol;

import java.io.IOException;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseFactory;
import org.apache.http.HttpServerConnection;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.ProtocolException;
import org.apache.http.ProtocolVersion;
import org.apache.http.UnsupportedHttpVersionException;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.params.HttpParams;
import org.apache.http.params.DefaultedHttpParams;
import org.apache.http.util.EncodingUtils;

/**
 * HttpService is a server side HTTP protocol handler based in the blocking 
 * I/O model that implements the essential requirements of the HTTP protocol 
 * for the server side message processing as described by RFC 2616. 
 * <br>
 * HttpService relies on {@link HttpProcessor} to generate mandatory protocol 
 * headers for all outgoing messages and apply common, cross-cutting message 
 * transformations to all incoming and outgoing messages, whereas individual 
 * {@link HttpRequestHandler}s are expected to take care of application specific 
 * content generation and processing.
 * <br>
 * HttpService relies on {@link HttpRequestHandler} to resolve matching request 
 * handler for a particular request URI of an incoming HTTP request.
 * <br>
 * HttpService can use optional {@link HttpExpectationVerifier} to ensure that 
 * incoming requests meet server's expectations.
 *
 *
 * @version $Revision: 744532 $
 *
 * @since 4.0
 */
public class HttpService {

    private HttpParams params = null;
    private HttpProcessor processor = null;
    private HttpRequestHandlerResolver handlerResolver = null;
    private ConnectionReuseStrategy connStrategy = null;
    private HttpResponseFactory responseFactory = null;
    private HttpExpectationVerifier expectationVerifier = null;
    
    /**
     * Create a new HTTP service.
     *
     * @param proc             the processor to use on requests and responses
     * @param connStrategy     the connection reuse strategy
     * @param responseFactory  the response factory
     */
    public HttpService(
            final HttpProcessor proc,
            final ConnectionReuseStrategy connStrategy,
            final HttpResponseFactory responseFactory) {
        super();
        setHttpProcessor(proc);
        setConnReuseStrategy(connStrategy);
        setResponseFactory(responseFactory);
    }
    
    public void setHttpProcessor(final HttpProcessor processor) {
        if (processor == null) {
            throw new IllegalArgumentException("HTTP processor may not be null");
        }
        this.processor = processor;
    }

    public void setConnReuseStrategy(final ConnectionReuseStrategy connStrategy) {
        if (connStrategy == null) {
            throw new IllegalArgumentException("Connection reuse strategy may not be null");
        }
        this.connStrategy = connStrategy;
    }

    public void setResponseFactory(final HttpResponseFactory responseFactory) {
        if (responseFactory == null) {
            throw new IllegalArgumentException("Response factory may not be null");
        }
        this.responseFactory = responseFactory;
    }
    
    public void setHandlerResolver(final HttpRequestHandlerResolver handlerResolver) {
        this.handlerResolver = handlerResolver;
    }

    public void setExpectationVerifier(final HttpExpectationVerifier expectationVerifier) {
        this.expectationVerifier = expectationVerifier;
    }

    public HttpParams getParams() {
        return this.params;
    }
    
    public void setParams(final HttpParams params) {
        this.params = params;
    }
    
    /**
     * Handles receives one HTTP request over the given connection within the 
     * given execution context and sends a response back to the client.
     * 
     * @param conn the active connection to the client
     * @param context the actual execution context.
     * @throws IOException in case of an I/O error.
     * @throws HttpException in case of HTTP protocol violation or a processing 
     *   problem.
     */
    public void handleRequest(
            final HttpServerConnection conn, 
            final HttpContext context) throws IOException, HttpException { 
        
        context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);

        HttpResponse response = null;
        
        try {

            HttpRequest request = conn.receiveRequestHeader();
            request.setParams(
                    new DefaultedHttpParams(request.getParams(), this.params));
            
            ProtocolVersion ver =
                request.getRequestLine().getProtocolVersion();
            if (!ver.lessEquals(HttpVersion.HTTP_1_1)) {
                // Downgrade protocol version if greater than HTTP/1.1 
                ver = HttpVersion.HTTP_1_1;
            }

            if (request instanceof HttpEntityEnclosingRequest) {

                if (((HttpEntityEnclosingRequest) request).expectContinue()) {
                    response = this.responseFactory.newHttpResponse(ver, 
                            HttpStatus.SC_CONTINUE, context);
                    response.setParams(
                            new DefaultedHttpParams(response.getParams(), this.params));
                    
                    if (this.expectationVerifier != null) {
                        try {
                            this.expectationVerifier.verify(request, response, context);
                        } catch (HttpException ex) {
                            response = this.responseFactory.newHttpResponse(HttpVersion.HTTP_1_0, 
                                    HttpStatus.SC_INTERNAL_SERVER_ERROR, context);
                            response.setParams(
                                    new DefaultedHttpParams(response.getParams(), this.params));
                            handleException(ex, response);
                        }
                    }
                    if (response.getStatusLine().getStatusCode() < 200) {
                        // Send 1xx response indicating the server expections
                        // have been met
                        conn.sendResponseHeader(response);
                        conn.flush();
                        response = null;
                        conn.receiveRequestEntity((HttpEntityEnclosingRequest) request);
                    }
                } else {
                    conn.receiveRequestEntity((HttpEntityEnclosingRequest) request);
                }
            }

            if (response == null) {
                response = this.responseFactory.newHttpResponse(ver, HttpStatus.SC_OK, context);
                response.setParams(
                        new DefaultedHttpParams(response.getParams(), this.params));

                context.setAttribute(ExecutionContext.HTTP_REQUEST, request);
                context.setAttribute(ExecutionContext.HTTP_RESPONSE, response);

                this.processor.process(request, context);
                doService(request, response, context);
            }
            
            // Make sure the request content is fully consumed
            if (request instanceof HttpEntityEnclosingRequest) {
                HttpEntity entity = ((HttpEntityEnclosingRequest)request).getEntity();
                if (entity != null) {
                    entity.consumeContent();
                }
            }
            
        } catch (HttpException ex) {
            response = this.responseFactory.newHttpResponse
                (HttpVersion.HTTP_1_0, HttpStatus.SC_INTERNAL_SERVER_ERROR,
                 context);
            response.setParams(
                    new DefaultedHttpParams(response.getParams(), this.params));
            handleException(ex, response);
        }
        
        this.processor.process(response, context);
        conn.sendResponseHeader(response);
        conn.sendResponseEntity(response);
        conn.flush();
        
        if (!this.connStrategy.keepAlive(response, context)) {
            conn.close();
        }
    }

    /**
     * Handles the given exception and generates an HTTP response to be sent 
     * back to the client to inform about the exceptional condition encountered
     * in the course of the request processing.
     * 
     * @param ex the exception.
     * @param response the HTTP response.
     */
    protected void handleException(final HttpException ex, final HttpResponse response) {
        if (ex instanceof MethodNotSupportedException) {
            response.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
        } else if (ex instanceof UnsupportedHttpVersionException) {
            response.setStatusCode(HttpStatus.SC_HTTP_VERSION_NOT_SUPPORTED);
        } else if (ex instanceof ProtocolException) {
            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
        } else {
            response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
        byte[] msg = EncodingUtils.getAsciiBytes(ex.getMessage());
        ByteArrayEntity entity = new ByteArrayEntity(msg);
        entity.setContentType("text/plain; charset=US-ASCII");
        response.setEntity(entity);
    }
    
    /**
     * The default implementation of this method attempts to resolve an 
     * {@link HttpRequestHandler} for the request URI of the given request
     * and, if found, executes its
     * {@link HttpRequestHandler#handle(HttpRequest, HttpResponse, HttpContext)}
     * method.
     * <p>
     * Super-classes can override this method in order to provide a custom
     * implementation of the request processing logic.
     * 
     * @param request the HTTP request.
     * @param response the HTTP response.
     * @param context the execution context.
     * @throws IOException in case of an I/O error.
     * @throws HttpException in case of HTTP protocol violation or a processing 
     *   problem.
     */
    protected void doService(
            final HttpRequest request, 
            final HttpResponse response,
            final HttpContext context) throws HttpException, IOException {
        HttpRequestHandler handler = null;
        if (this.handlerResolver != null) {
            String requestURI = request.getRequestLine().getUri();
            handler = this.handlerResolver.lookup(requestURI);
        }
        if (handler != null) {
            handler.handle(request, response, context);
        } else {
            response.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
        }
    }
    
}
