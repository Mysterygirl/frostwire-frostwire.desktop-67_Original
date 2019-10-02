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
 */

package org.apache.http.impl.client;

import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.UserTokenHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.localserver.ServerTestBase;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

/**
 * Unit tests for {@link DefaultRequestDirector}
 */
public class TestStatefulConnManagement extends ServerTestBase {

    public TestStatefulConnManagement(final String testName) {
        super(testName);
    }

    public static void main(String args[]) {
        String[] testCaseName = { TestStatefulConnManagement.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    public static Test suite() {
        return new TestSuite(TestStatefulConnManagement.class);
    }
    
    private class SimpleService implements HttpRequestHandler {
        
        public SimpleService() {
            super();
        }

        public void handle(
                final HttpRequest request, 
                final HttpResponse response, 
                final HttpContext context) throws HttpException, IOException {
            response.setStatusCode(HttpStatus.SC_OK);
            StringEntity entity = new StringEntity("Whatever");
            response.setEntity(entity);
        }
    }

    public void testStatefulConnections() throws Exception {

        int workerCount = 5;
        int requestCount = 5;
        
        int port = this.localServer.getServicePort();
        this.localServer.register("*", new SimpleService());

        HttpHost target = new HttpHost("localhost", port);
        
        HttpParams params = defaultParams.copy();
        ConnManagerParams.setMaxTotalConnections(params, workerCount);
        ConnManagerParams.setMaxConnectionsPerRoute(params, 
                new ConnPerRouteBean(workerCount));
        ConnManagerParams.setTimeout(params, 10L);
        
        ThreadSafeClientConnManager mgr = new ThreadSafeClientConnManager(
                params, supportedSchemes);        
        
        DefaultHttpClient client = new DefaultHttpClient(mgr, params); 
        
        HttpContext[] contexts = new HttpContext[workerCount];
        HttpWorker[] workers = new HttpWorker[workerCount];
        for (int i = 0; i < contexts.length; i++) {
            HttpContext context = new BasicHttpContext();
            context.setAttribute("user", Integer.valueOf(i));
            contexts[i] = context;
            workers[i] = new HttpWorker(context, requestCount, target, client);
        }
        
        client.setUserTokenHandler(new UserTokenHandler() {

            public Object getUserToken(final HttpContext context) {
                Integer id = (Integer) context.getAttribute("user");
                return id;
            }
            
        });
        
        
        for (int i = 0; i < workers.length; i++) {
            workers[i].start();
        }
        for (int i = 0; i < workers.length; i++) {
            workers[i].join(10000);
        }
        for (int i = 0; i < workers.length; i++) {
            Exception ex = workers[i].getException();
            if (ex != null) {
                throw ex;
            }
            assertEquals(requestCount, workers[i].getCount());
        }
        
        for (int i = 0; i < contexts.length; i++) {
            HttpContext context = contexts[i];
            Integer id = (Integer) context.getAttribute("user");
            
            for (int r = 0; r < requestCount; r++) {
                Integer state = (Integer) context.getAttribute("r" + r);
                assertNotNull(state);
                assertEquals(id, state);
            }
        }
        
    }
    
    static class HttpWorker extends Thread {

        private final HttpContext context;
        private final int requestCount;
        private final HttpHost target;
        private final HttpClient httpclient;
        
        private volatile Exception exception;
        private volatile int count;
        
        public HttpWorker(
                final HttpContext context, 
                int requestCount,
                final HttpHost target,
                final HttpClient httpclient) {
            super();
            this.context = context;
            this.requestCount = requestCount;
            this.target = target;
            this.httpclient = httpclient;
            this.count = 0;
        }
        
        public int getCount() {
            return this.count;
        }

        public Exception getException() {
            return this.exception;
        }

        @Override
        public void run() {
            try {
                for (int r = 0; r < this.requestCount; r++) {
                    HttpGet httpget = new HttpGet("/");
                    HttpResponse response = this.httpclient.execute(
                            this.target, 
                            httpget, 
                            this.context);
                    this.count++;

                    ManagedClientConnection conn = (ManagedClientConnection) this.context.getAttribute(
                            ExecutionContext.HTTP_CONNECTION);
                    
                    this.context.setAttribute("r" + r, conn.getState());
                    
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        entity.consumeContent();
                    }
                }
                
            } catch (Exception ex) {
                this.exception = ex;
            }
        }
        
    }

}
