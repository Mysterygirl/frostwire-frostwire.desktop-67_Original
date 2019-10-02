/*
 * $HeadURL: https://svn.apache.org/repos/asf/httpcomponents/httpcore/tags/4.0.1/httpcore-nio/src/test/java/org/apache/http/impl/nio/reactor/TestDefaultListeningIOReactor.java $
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

package org.apache.http.impl.nio.reactor;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.nio.DefaultServerIOEventDispatch;
import org.apache.http.nio.protocol.BufferingHttpServiceHandler;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorExceptionHandler;
import org.apache.http.nio.reactor.IOReactorStatus;
import org.apache.http.nio.reactor.ListenerEndpoint;
import org.apache.http.nio.reactor.ListeningIOReactor;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;

/**
 * Basic tests for {@link DefaultListeningIOReactor}.
 *
 * 
 * @version $Id: TestDefaultListeningIOReactor.java 744515 2009-02-14 16:36:56Z sebb $
 */
public class TestDefaultListeningIOReactor extends TestCase {

    // ------------------------------------------------------------ Constructor
    public TestDefaultListeningIOReactor(String testName) {
        super(testName);
    }

    // ------------------------------------------------------------------- Main
    public static void main(String args[]) {
        String[] testCaseName = { TestDefaultListeningIOReactor.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    // ------------------------------------------------------- TestCase Methods

    public static Test suite() {
        return new TestSuite(TestDefaultListeningIOReactor.class);
    }
    
    public void testEndpointUpAndDown() throws Exception {
        
        HttpParams params = new BasicHttpParams();
        
        BasicHttpProcessor httpproc = new BasicHttpProcessor();
        httpproc.addInterceptor(new ResponseDate());
        httpproc.addInterceptor(new ResponseServer());
        httpproc.addInterceptor(new ResponseContent());
        httpproc.addInterceptor(new ResponseConnControl());

        final BufferingHttpServiceHandler serviceHandler = new BufferingHttpServiceHandler(
                httpproc,
                new DefaultHttpResponseFactory(),
                new DefaultConnectionReuseStrategy(),
                params);
        
        final IOEventDispatch eventDispatch = new DefaultServerIOEventDispatch(
                serviceHandler, 
                params);
        
        final ListeningIOReactor ioreactor = new DefaultListeningIOReactor(1, params);
        
        Thread t = new Thread(new Runnable() {
            
            public void run() {
                try {
                    ioreactor.execute(eventDispatch);
                } catch (IOException ex) {
                }
            }
            
        });
        
        t.start();
        
        Set<ListenerEndpoint> endpoints = ioreactor.getEndpoints();
        assertNotNull(endpoints);
        assertEquals(0, endpoints.size());
        
        ListenerEndpoint port9998 = ioreactor.listen(new InetSocketAddress(9998));
        port9998.waitFor();

        ListenerEndpoint port9999 = ioreactor.listen(new InetSocketAddress(9999));
        port9999.waitFor();

        endpoints = ioreactor.getEndpoints();
        assertNotNull(endpoints);
        assertEquals(2, endpoints.size());
        
        port9998.close();

        endpoints = ioreactor.getEndpoints();
        assertNotNull(endpoints);
        assertEquals(1, endpoints.size());
        
        ListenerEndpoint endpoint = endpoints.iterator().next();
        
        assertEquals(9999, ((InetSocketAddress) endpoint.getAddress()).getPort());
        
        ioreactor.shutdown(1000);
        t.join(1000);
        
        assertEquals(IOReactorStatus.SHUT_DOWN, ioreactor.getStatus());
    }

    public void testEndpointAlreadyBoundFatal() throws Exception {
        
        HttpParams params = new BasicHttpParams();
        
        BasicHttpProcessor httpproc = new BasicHttpProcessor();
        httpproc.addInterceptor(new ResponseDate());
        httpproc.addInterceptor(new ResponseServer());
        httpproc.addInterceptor(new ResponseContent());
        httpproc.addInterceptor(new ResponseConnControl());

        final BufferingHttpServiceHandler serviceHandler = new BufferingHttpServiceHandler(
                httpproc,
                new DefaultHttpResponseFactory(),
                new DefaultConnectionReuseStrategy(),
                params);
        
        final IOEventDispatch eventDispatch = new DefaultServerIOEventDispatch(
                serviceHandler, 
                params);
        
        final ListeningIOReactor ioreactor = new DefaultListeningIOReactor(1, params);
        
        final CountDownLatch latch = new CountDownLatch(1);
        
        Thread t = new Thread(new Runnable() {
            
            public void run() {
                try {
                    ioreactor.execute(eventDispatch);
                    fail("IOException should have been thrown");
                } catch (IOException ex) {
                    latch.countDown();
                }
            }
            
        });
        
        t.start();
        
        ListenerEndpoint endpoint1 = ioreactor.listen(new InetSocketAddress(9999));
        endpoint1.waitFor();

        ListenerEndpoint endpoint2 = ioreactor.listen(new InetSocketAddress(9999));
        endpoint2.waitFor();
        assertNotNull(endpoint2.getException());

        // I/O reactor is now expected to be shutting down
        latch.await(2000, TimeUnit.MILLISECONDS);
        assertEquals(IOReactorStatus.SHUT_DOWN, ioreactor.getStatus());
        
        Set<ListenerEndpoint> endpoints = ioreactor.getEndpoints();
        assertNotNull(endpoints);
        assertEquals(0, endpoints.size());
        
        ioreactor.shutdown(1000);
        t.join(1000);
        
        assertEquals(IOReactorStatus.SHUT_DOWN, ioreactor.getStatus());
    }
    
    public void testEndpointAlreadyBoundNonFatal() throws Exception {
        
        HttpParams params = new BasicHttpParams();
        
        BasicHttpProcessor httpproc = new BasicHttpProcessor();
        httpproc.addInterceptor(new ResponseDate());
        httpproc.addInterceptor(new ResponseServer());
        httpproc.addInterceptor(new ResponseContent());
        httpproc.addInterceptor(new ResponseConnControl());

        final BufferingHttpServiceHandler serviceHandler = new BufferingHttpServiceHandler(
                httpproc,
                new DefaultHttpResponseFactory(),
                new DefaultConnectionReuseStrategy(),
                params);
        
        final IOEventDispatch eventDispatch = new DefaultServerIOEventDispatch(
                serviceHandler, 
                params);
        
        final DefaultListeningIOReactor ioreactor = new DefaultListeningIOReactor(1, params);
        
        ioreactor.setExceptionHandler(new IOReactorExceptionHandler() {

            public boolean handle(final IOException ex) {
                return (ex instanceof BindException);
            }

            public boolean handle(final RuntimeException ex) {
                return false;
            }
            
        });
        
        Thread t = new Thread(new Runnable() {
            
            public void run() {
                try {
                    ioreactor.execute(eventDispatch);
                } catch (IOException ex) {
                }
            }
            
        });
        
        t.start();
        
        ListenerEndpoint endpoint1 = ioreactor.listen(new InetSocketAddress(9999));
        endpoint1.waitFor();

        ListenerEndpoint endpoint2 = ioreactor.listen(new InetSocketAddress(9999));
        endpoint2.waitFor();
        assertNotNull(endpoint2.getException());

        // Sleep a little to make sure the I/O reactor is not shutting down
        Thread.sleep(500);
        
        assertEquals(IOReactorStatus.ACTIVE, ioreactor.getStatus());
        
        ioreactor.shutdown(1000);
        t.join(1000);
        
        assertEquals(IOReactorStatus.SHUT_DOWN, ioreactor.getStatus());
    }

}
