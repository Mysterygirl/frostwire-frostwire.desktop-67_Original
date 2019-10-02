/*
 * $HeadURL: https://svn.apache.org/repos/asf/httpcomponents/httpcore/tags/4.0.1/httpcore-nio/src/test/java/org/apache/http/nio/protocol/TestNIOSSLHttp.java $
 * $Revision: 744515 $
 * $Date: 2009-02-14 17:36:56 +0100 (Sat, 14 Feb 2009) $
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

package org.apache.http.nio.protocol;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.http.HttpCoreNIOSSLTestBase;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.mockup.ByteSequence;
import org.apache.http.mockup.RequestCount;
import org.apache.http.mockup.SimpleEventListener;
import org.apache.http.nio.NHttpClientHandler;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.NHttpServiceHandler;
import org.apache.http.nio.entity.NByteArrayEntity;
import org.apache.http.nio.entity.NHttpEntityWrapper;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.protocol.HttpRequestExecutionHandler;
import org.apache.http.nio.reactor.ListenerEndpoint;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;

/**
 * HttpCore NIO SSL tests.
 *
 * 
 * @version $Id: TestNIOSSLHttp.java 744515 2009-02-14 16:36:56Z sebb $
 */
public class TestNIOSSLHttp extends HttpCoreNIOSSLTestBase {

    // ------------------------------------------------------------ Constructor
    public TestNIOSSLHttp(String testName) {
        super(testName);
    }

    // ------------------------------------------------------------------- Main
    public static void main(String args[]) {
        String[] testCaseName = { TestNIOSSLHttp.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    // ------------------------------------------------------- TestCase Methods

    public static Test suite() {
        return new TestSuite(TestNIOSSLHttp.class);
    }

    /**
     * This test case executes a series of simple (non-pipelined) GET requests 
     * over multiple connections. 
     */
    public void testSimpleHttpGets() throws Exception {
        final int connNo = 3;
        final int reqNo = 20;
        final RequestCount requestCount = new RequestCount(connNo * reqNo); 
        
        final ByteSequence requestData = new ByteSequence();
        requestData.rnd(reqNo);
        
        List<ByteSequence> responseData = new ArrayList<ByteSequence>(connNo);
        for (int i = 0; i < connNo; i++) {
            responseData.add(new ByteSequence());
        }
        
        HttpRequestHandler requestHandler = new HttpRequestHandler() {

            public void handle(
                    final HttpRequest request, 
                    final HttpResponse response, 
                    final HttpContext context) throws HttpException, IOException {
                
                String s = request.getRequestLine().getUri();
                URI uri;
                try {
                    uri = new URI(s);
                } catch (URISyntaxException ex) {
                    throw new HttpException("Invalid request URI: " + s);
                }
                int index = Integer.parseInt(uri.getQuery());
                byte[] bytes = requestData.getBytes(index);
                NByteArrayEntity entity = new NByteArrayEntity(bytes); 
                response.setEntity(entity);
            }
            
        };
        
        HttpRequestExecutionHandler requestExecutionHandler = new HttpRequestExecutionHandler() {

            public void initalizeContext(final HttpContext context, final Object attachment) {
                context.setAttribute("LIST", attachment);
                context.setAttribute("REQ-COUNT", Integer.valueOf(0));
                context.setAttribute("RES-COUNT", Integer.valueOf(0));
            }

            public void finalizeContext(final HttpContext context) {
            }

            public HttpRequest submitRequest(final HttpContext context) {
                int i = ((Integer) context.getAttribute("REQ-COUNT")).intValue();
                BasicHttpRequest get = null;
                if (i < reqNo) {
                    get = new BasicHttpRequest("GET", "/?" + i);
                    context.setAttribute("REQ-COUNT", Integer.valueOf(i + 1));
                }
                return get;
            }
            
            public void handleResponse(final HttpResponse response, final HttpContext context) {
                NHttpConnection conn = (NHttpConnection) context.getAttribute(
                        ExecutionContext.HTTP_CONNECTION);
                
                ByteSequence list = (ByteSequence) context.getAttribute("LIST");
                int i = ((Integer) context.getAttribute("RES-COUNT")).intValue();
                i++;
                context.setAttribute("RES-COUNT", Integer.valueOf(i));

                try {
                    HttpEntity entity = response.getEntity();
                    byte[] data = EntityUtils.toByteArray(entity);
                    list.addBytes(data);
                    requestCount.decrement();
                } catch (IOException ex) {
                    requestCount.abort();
                }

                if (i < reqNo) {
                    conn.requestInput();
                }
            }
            
        };
        
        NHttpServiceHandler serviceHandler = createHttpServiceHandler(
                requestHandler, 
                null,
                new SimpleEventListener());

        NHttpClientHandler clientHandler = createHttpClientHandler(
                requestExecutionHandler, 
                new SimpleEventListener());

        this.server.setRequestCount(requestCount);
        this.client.setRequestCount(requestCount);
        
        this.server.start(serviceHandler);
        this.client.start(clientHandler);

        ListenerEndpoint endpoint = this.server.getListenerEndpoint();
        endpoint.waitFor();
        InetSocketAddress serverAddress = (InetSocketAddress) endpoint.getAddress();
        
        for (int i = 0; i < responseData.size(); i++) {
            this.client.openConnection(
                    new InetSocketAddress("localhost", serverAddress.getPort()), 
                    responseData.get(i));
        }
     
        requestCount.await(10000);
        assertEquals(0, requestCount.getValue());
        
        this.client.shutdown();
        this.server.shutdown();

        for (int c = 0; c < responseData.size(); c++) {
            ByteSequence receivedPackets = responseData.get(c);
            ByteSequence expectedPackets = requestData;
            assertEquals(expectedPackets.size(), receivedPackets.size());
            for (int p = 0; p < requestData.size(); p++) {
                byte[] expected = requestData.getBytes(p);
                byte[] received = receivedPackets.getBytes(p);
                
                assertEquals(expected.length, received.length);
                for (int i = 0; i < expected.length; i++) {
                    assertEquals(expected[i], received[i]);
                }
            }
        }
        
    }

    /**
     * This test case executes a series of simple (non-pipelined) POST requests 
     * with content length delimited content over multiple connections. 
     */
    public void testSimpleBasicHttpEntityEnclosingRequestsWithContentLength() throws Exception {
        
        final int connNo = 3;
        final int reqNo = 20;
        final RequestCount requestCount = new RequestCount(connNo * reqNo); 
        
        final ByteSequence requestData = new ByteSequence();
        requestData.rnd(reqNo);
        
        List<ByteSequence> responseData = new ArrayList<ByteSequence>(connNo);
        for (int i = 0; i < connNo; i++) {
            responseData.add(new ByteSequence());
        }
        
        HttpRequestHandler requestHandler = new HttpRequestHandler() {

            public void handle(
                    final HttpRequest request, 
                    final HttpResponse response, 
                    final HttpContext context) throws HttpException, IOException {
                
                if (request instanceof HttpEntityEnclosingRequest) {
                    HttpEntity incoming = ((HttpEntityEnclosingRequest) request).getEntity();
                    byte[] data = EntityUtils.toByteArray(incoming);
                    
                    NByteArrayEntity outgoing = new NByteArrayEntity(data);
                    outgoing.setChunked(false);
                    response.setEntity(outgoing);
                } else {
                    NStringEntity outgoing = new NStringEntity("No content"); 
                    response.setEntity(outgoing);
                }
            }
            
        };
        
        HttpRequestExecutionHandler requestExecutionHandler = new HttpRequestExecutionHandler() {

            public void initalizeContext(final HttpContext context, final Object attachment) {
                context.setAttribute("LIST", attachment);
                context.setAttribute("REQ-COUNT", Integer.valueOf(0));
                context.setAttribute("RES-COUNT", Integer.valueOf(0));
            }

            public void finalizeContext(final HttpContext context) {
            }

            public HttpRequest submitRequest(final HttpContext context) {
                int i = ((Integer) context.getAttribute("REQ-COUNT")).intValue();
                BasicHttpEntityEnclosingRequest post = null;
                if (i < reqNo) {
                    post = new BasicHttpEntityEnclosingRequest("POST", "/?" + i);
                    byte[] bytes = requestData.getBytes(i);
                    NByteArrayEntity outgoing = new NByteArrayEntity(bytes);
                    post.setEntity(outgoing);
                    
                    context.setAttribute("REQ-COUNT", Integer.valueOf(i + 1));
                }
                return post;
            }
            
            public void handleResponse(final HttpResponse response, final HttpContext context) {
                NHttpConnection conn = (NHttpConnection) context.getAttribute(
                        ExecutionContext.HTTP_CONNECTION);
                
                ByteSequence list = (ByteSequence) context.getAttribute("LIST");
                int i = ((Integer) context.getAttribute("RES-COUNT")).intValue();
                i++;
                context.setAttribute("RES-COUNT", Integer.valueOf(i));

                try {
                    HttpEntity entity = response.getEntity();
                    byte[] data = EntityUtils.toByteArray(entity);
                    list.addBytes(data);
                    requestCount.decrement();
                } catch (IOException ex) {
                    requestCount.abort();
                }

                if (i < reqNo) {
                    conn.requestInput();
                }
            }
            
        };
        
        NHttpServiceHandler serviceHandler = createHttpServiceHandler(
                requestHandler, 
                null,
                new SimpleEventListener());

        NHttpClientHandler clientHandler = createHttpClientHandler(
                requestExecutionHandler, 
                new SimpleEventListener());

        this.server.setRequestCount(requestCount);
        this.client.setRequestCount(requestCount);
        
        this.server.start(serviceHandler);
        this.client.start(clientHandler);

        ListenerEndpoint endpoint = this.server.getListenerEndpoint();
        endpoint.waitFor();
        InetSocketAddress serverAddress = (InetSocketAddress) endpoint.getAddress();
        
        for (int i = 0; i < responseData.size(); i++) {
            this.client.openConnection(
                    new InetSocketAddress("localhost", serverAddress.getPort()), 
                    responseData.get(i));
        }
     
        requestCount.await(10000);
        assertEquals(0, requestCount.getValue());
        
        this.client.shutdown();
        this.server.shutdown();

        for (int c = 0; c < responseData.size(); c++) {
            ByteSequence receivedPackets = responseData.get(c);
            ByteSequence expectedPackets = requestData;
            assertEquals(expectedPackets.size(), receivedPackets.size());
            for (int p = 0; p < requestData.size(); p++) {
                byte[] expected = requestData.getBytes(p);
                byte[] received = receivedPackets.getBytes(p);
                
                assertEquals(expected.length, received.length);
                for (int i = 0; i < expected.length; i++) {
                    assertEquals(expected[i], received[i]);
                }
            }
        }
        
    }

    /**
     * This test case executes a series of simple (non-pipelined) POST requests 
     * with chunk coded content content over multiple connections. 
     */
    public void testSimpleBasicHttpEntityEnclosingRequestsChunked() throws Exception {
        
        final int connNo = 3;
        final int reqNo = 20;
        final RequestCount requestCount = new RequestCount(connNo * reqNo); 
        
        final ByteSequence requestData = new ByteSequence();
        requestData.rnd(reqNo);
        
        List<ByteSequence> responseData = new ArrayList<ByteSequence>(connNo);
        for (int i = 0; i < connNo; i++) {
            responseData.add(new ByteSequence());
        }
        
        HttpRequestHandler requestHandler = new HttpRequestHandler() {

            public void handle(
                    final HttpRequest request, 
                    final HttpResponse response, 
                    final HttpContext context) throws HttpException, IOException {
                
                if (request instanceof HttpEntityEnclosingRequest) {
                    HttpEntity incoming = ((HttpEntityEnclosingRequest) request).getEntity();
                    byte[] data = EntityUtils.toByteArray(incoming);
                    NByteArrayEntity outgoing = new NByteArrayEntity(data);
                    outgoing.setChunked(true);
                    response.setEntity(outgoing);
                } else {
                    NStringEntity outgoing = new NStringEntity("No content"); 
                    response.setEntity(outgoing);
                }
            }
            
        };
        
        HttpRequestExecutionHandler requestExecutionHandler = new HttpRequestExecutionHandler() {

            public void initalizeContext(final HttpContext context, final Object attachment) {
                context.setAttribute("LIST", attachment);
                context.setAttribute("REQ-COUNT", Integer.valueOf(0));
                context.setAttribute("RES-COUNT", Integer.valueOf(0));
            }

            public void finalizeContext(final HttpContext context) {
            }

            public HttpRequest submitRequest(final HttpContext context) {
                int i = ((Integer) context.getAttribute("REQ-COUNT")).intValue();
                BasicHttpEntityEnclosingRequest post = null;
                if (i < reqNo) {
                    post = new BasicHttpEntityEnclosingRequest("POST", "/?" + i);
                    byte[] bytes = requestData.getBytes(i);
                    NByteArrayEntity outgoing = new NByteArrayEntity(bytes);
                    outgoing.setChunked(true);
                    post.setEntity(outgoing);
                    
                    context.setAttribute("REQ-COUNT", Integer.valueOf(i + 1));
                }
                return post;
            }
            
            public void handleResponse(final HttpResponse response, final HttpContext context) {
                NHttpConnection conn = (NHttpConnection) context.getAttribute(
                        ExecutionContext.HTTP_CONNECTION);
                
                ByteSequence list = (ByteSequence) context.getAttribute("LIST");
                int i = ((Integer) context.getAttribute("RES-COUNT")).intValue();
                i++;
                context.setAttribute("RES-COUNT", Integer.valueOf(i));
                
                try {
                    HttpEntity entity = response.getEntity();
                    byte[] data = EntityUtils.toByteArray(entity);
                    list.addBytes(data);
                    requestCount.decrement();
                } catch (IOException ex) {
                    requestCount.abort();
                }

                if (i < reqNo) {
                    conn.requestInput();
                }
            }
            
        };
        
        SimpleEventListener serverEventListener = new SimpleEventListener();
        SimpleEventListener clientEventListener = new SimpleEventListener();
        
        NHttpServiceHandler serviceHandler = createHttpServiceHandler(
                requestHandler, 
                null,
                serverEventListener);

        NHttpClientHandler clientHandler = createHttpClientHandler(
                requestExecutionHandler, 
                clientEventListener);

        this.server.setRequestCount(requestCount);
        this.client.setRequestCount(requestCount);
        
        this.server.start(serviceHandler);
        this.client.start(clientHandler);

        ListenerEndpoint endpoint = this.server.getListenerEndpoint();
        endpoint.waitFor();
        InetSocketAddress serverAddress = (InetSocketAddress) endpoint.getAddress();
        
        for (int i = 0; i < responseData.size(); i++) {
            this.client.openConnection(
                    new InetSocketAddress("localhost", serverAddress.getPort()), 
                    responseData.get(i));
        }
     
        requestCount.await(10000);
        assertEquals(0, requestCount.getValue());
        
        this.client.shutdown();
        this.server.shutdown();

        for (int c = 0; c < responseData.size(); c++) {
            ByteSequence receivedPackets = responseData.get(c);
            ByteSequence expectedPackets = requestData;
            assertEquals(expectedPackets.size(), receivedPackets.size());
            for (int p = 0; p < requestData.size(); p++) {
                byte[] expected = requestData.getBytes(p);
                byte[] received = receivedPackets.getBytes(p);
                
                assertEquals(expected.length, received.length);
                for (int i = 0; i < expected.length; i++) {
                    assertEquals(expected[i], received[i]);
                }
            }
        }
        
    }

    /**
     * This test case executes a series of simple (non-pipelined) HTTP/1.0 
     * POST requests over multiple persistent connections. 
     */
    public void testSimpleBasicHttpEntityEnclosingRequestsHTTP10() throws Exception {
        
        final int connNo = 3;
        final int reqNo = 20;
        final RequestCount requestCount = new RequestCount(connNo * reqNo); 
        
        final ByteSequence requestData = new ByteSequence();
        requestData.rnd(reqNo);
        
        List<ByteSequence> responseData = new ArrayList<ByteSequence>(connNo);
        for (int i = 0; i < connNo; i++) {
            responseData.add(new ByteSequence());
        }
        
        HttpRequestHandler requestHandler = new HttpRequestHandler() {
            

            public void handle(
                    final HttpRequest request, 
                    final HttpResponse response, 
                    final HttpContext context) throws HttpException, IOException {
                
                if (request instanceof HttpEntityEnclosingRequest) {
                    HttpEntity incoming = ((HttpEntityEnclosingRequest) request).getEntity();
                    byte[] data = EntityUtils.toByteArray(incoming);
                    
                    NByteArrayEntity outgoing = new NByteArrayEntity(data);
                    outgoing.setChunked(false);
                    response.setEntity(outgoing);
                } else {
                    NStringEntity outgoing = new NStringEntity("No content"); 
                    response.setEntity(outgoing);
                }
            }
            
        };
        
        // Set protocol level to HTTP/1.0
        this.client.getParams().setParameter(
                CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_0);
        
        HttpRequestExecutionHandler requestExecutionHandler = new HttpRequestExecutionHandler() {

            public void initalizeContext(final HttpContext context, final Object attachment) {
                context.setAttribute("LIST", attachment);
                context.setAttribute("REQ-COUNT", Integer.valueOf(0));
                context.setAttribute("RES-COUNT", Integer.valueOf(0));
            }

            public void finalizeContext(final HttpContext context) {
            }

            public HttpRequest submitRequest(final HttpContext context) {
                int i = ((Integer) context.getAttribute("REQ-COUNT")).intValue();
                BasicHttpEntityEnclosingRequest post = null;
                if (i < reqNo) {
                    post = new BasicHttpEntityEnclosingRequest("POST", "/?" + i);
                    byte[] bytes = requestData.getBytes(i);
                    NByteArrayEntity outgoing = new NByteArrayEntity(bytes);
                    post.setEntity(outgoing);
                    
                    context.setAttribute("REQ-COUNT", Integer.valueOf(i + 1));
                }
                return post;
            }
            
            public void handleResponse(final HttpResponse response, final HttpContext context) {
                NHttpConnection conn = (NHttpConnection) context.getAttribute(
                        ExecutionContext.HTTP_CONNECTION);

                ByteSequence list = (ByteSequence) context.getAttribute("LIST");
                int i = ((Integer) context.getAttribute("RES-COUNT")).intValue();
                i++;
                context.setAttribute("RES-COUNT", Integer.valueOf(i));

                try {
                    HttpEntity entity = response.getEntity();
                    byte[] data = EntityUtils.toByteArray(entity);
                    list.addBytes(data);
                    requestCount.decrement();
                } catch (IOException ex) {
                    requestCount.abort();
                }

                if (i < reqNo) {
                    conn.requestInput();
                }
            }
            
        };
        
        SimpleEventListener serverEventListener = new SimpleEventListener();
        SimpleEventListener clientEventListener = new SimpleEventListener();
        
        NHttpServiceHandler serviceHandler = createHttpServiceHandler(
                requestHandler, 
                null,
                serverEventListener);

        NHttpClientHandler clientHandler = createHttpClientHandler(
                requestExecutionHandler, 
                clientEventListener);

        this.server.setRequestCount(requestCount);
        this.client.setRequestCount(requestCount);
        
        this.server.start(serviceHandler);
        this.client.start(clientHandler);

        ListenerEndpoint endpoint = this.server.getListenerEndpoint();
        endpoint.waitFor();
        InetSocketAddress serverAddress = (InetSocketAddress) endpoint.getAddress();
        
        for (int i = 0; i < responseData.size(); i++) {
            this.client.openConnection(
                    new InetSocketAddress("localhost", serverAddress.getPort()), 
                    responseData.get(i));
        }
     
        requestCount.await(10000);
        assertEquals(0, requestCount.getValue());
        
        this.client.shutdown();
        this.server.shutdown();

        for (int c = 0; c < responseData.size(); c++) {
            ByteSequence receivedPackets = responseData.get(c);
            ByteSequence expectedPackets = requestData;
            assertEquals(expectedPackets.size(), receivedPackets.size());
            for (int p = 0; p < requestData.size(); p++) {
                byte[] expected = requestData.getBytes(p);
                byte[] received = receivedPackets.getBytes(p);
                
                assertEquals(expected.length, received.length);
                for (int i = 0; i < expected.length; i++) {
                    assertEquals(expected[i], received[i]);
                }
            }
        }
        
    }
    
    /**
     * This test case executes a series of simple (non-pipelined) POST requests 
     * with chunk coded content over multiple connections using 
     * {@link NHttpEntityWrapper}. 
     */
    public void testSimpleBasicHttpEntityEnclosingRequestsChunkedWithWrapper() throws Exception {
        
        final int connNo = 1;
        final int reqNo = 2;
        final RequestCount requestCount = new RequestCount(connNo * reqNo); 
        
        final ByteSequence requestData = new ByteSequence();
        requestData.rnd(reqNo);
        
        List<ByteSequence> responseData = new ArrayList<ByteSequence>(connNo);
        for (int i = 0; i < connNo; i++) {
            responseData.add(new ByteSequence());
        }
        
        HttpRequestHandler requestHandler = new HttpRequestHandler() {

            public void handle(
                    final HttpRequest request, 
                    final HttpResponse response, 
                    final HttpContext context) throws HttpException, IOException {
                
                if (request instanceof HttpEntityEnclosingRequest) {
                    HttpEntity incoming = ((HttpEntityEnclosingRequest) request).getEntity();
                    byte[] data = EntityUtils.toByteArray(incoming);
                    ByteArrayEntity outgoing = new ByteArrayEntity(data);
                    outgoing.setChunked(true);
                    response.setEntity(new NHttpEntityWrapper(outgoing));
                } else {
                    StringEntity outgoing = new StringEntity("No content"); 
                    response.setEntity(new NHttpEntityWrapper(outgoing));
                }
            }
            
        };
        
        HttpRequestExecutionHandler requestExecutionHandler = new HttpRequestExecutionHandler() {

            public void initalizeContext(final HttpContext context, final Object attachment) {
                context.setAttribute("LIST", attachment);
                context.setAttribute("REQ-COUNT", Integer.valueOf(0));
                context.setAttribute("RES-COUNT", Integer.valueOf(0));
            }

            public void finalizeContext(final HttpContext context) {
            }

            public HttpRequest submitRequest(final HttpContext context) {
                int i = ((Integer) context.getAttribute("REQ-COUNT")).intValue();
                BasicHttpEntityEnclosingRequest post = null;
                if (i < reqNo) {
                    post = new BasicHttpEntityEnclosingRequest("POST", "/?" + i);
                    byte[] bytes = requestData.getBytes(i);
                    ByteArrayEntity outgoing = new ByteArrayEntity(bytes);
                    outgoing.setChunked(true);
                    post.setEntity(outgoing);
                    
                    context.setAttribute("REQ-COUNT", Integer.valueOf(i + 1));
                }
                return post;
            }
            
            public void handleResponse(final HttpResponse response, final HttpContext context) {
                NHttpConnection conn = (NHttpConnection) context.getAttribute(
                        ExecutionContext.HTTP_CONNECTION);
                
                ByteSequence list = (ByteSequence) context.getAttribute("LIST");
                int i = ((Integer) context.getAttribute("RES-COUNT")).intValue();
                i++;
                context.setAttribute("RES-COUNT", Integer.valueOf(i));
                
                try {
                    HttpEntity entity = response.getEntity();
                    byte[] data = EntityUtils.toByteArray(entity);
                    list.addBytes(data);
                    requestCount.decrement();
                } catch (IOException ex) {
                    requestCount.abort();
                }

                if (i < reqNo) {
                    conn.requestInput();
                }
            }
            
        };
        
        SimpleEventListener serverEventListener = new SimpleEventListener();
        SimpleEventListener clientEventListener = new SimpleEventListener();
        
        NHttpServiceHandler serviceHandler = createHttpServiceHandler(
                requestHandler, 
                null,
                serverEventListener);

        NHttpClientHandler clientHandler = createHttpClientHandler(
                requestExecutionHandler, 
                clientEventListener);

        this.server.setRequestCount(requestCount);
        this.client.setRequestCount(requestCount);
        
        this.server.start(serviceHandler);
        this.client.start(clientHandler);

        ListenerEndpoint endpoint = this.server.getListenerEndpoint();
        endpoint.waitFor();
        InetSocketAddress serverAddress = (InetSocketAddress) endpoint.getAddress();
        
        for (int i = 0; i < responseData.size(); i++) {
            this.client.openConnection(
                    new InetSocketAddress("localhost", serverAddress.getPort()), 
                    responseData.get(i));
        }
     
        requestCount.await(10000);
        assertEquals(0, requestCount.getValue());
        
        this.client.shutdown();
        this.server.shutdown();

        for (int c = 0; c < responseData.size(); c++) {
            ByteSequence receivedPackets = responseData.get(c);
            ByteSequence expectedPackets = requestData;
            assertEquals(expectedPackets.size(), receivedPackets.size());
            for (int p = 0; p < requestData.size(); p++) {
                byte[] expected = requestData.getBytes(p);
                byte[] received = receivedPackets.getBytes(p);
                
                assertEquals(expected.length, received.length);
                for (int i = 0; i < expected.length; i++) {
                    assertEquals(expected[i], received[i]);
                }
            }
        }
        
    }

}
