/*
 * $HeadURL: https://svn.apache.org/repos/asf/httpcomponents/httpcore/tags/4.0.1/httpcore-nio/src/test/java/org/apache/http/mockup/RequestCount.java $
 * $Revision: 632854 $
 * $Date: 2008-03-02 22:51:00 +0100 (Sun, 02 Mar 2008) $
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

package org.apache.http.mockup;

import java.io.IOException;

public class RequestCount {

    private volatile boolean aborted;
    private volatile int value;
    private volatile Exception ex;
    
    public RequestCount(int initialValue) {
        this.value = initialValue;
        this.aborted = false;
    }
    
    public int getValue() {
        return this.value;
    }
    
    public void decrement() {
        synchronized (this) {
            if (!this.aborted) {
                this.value--;
            }
            notifyAll();
        }
    }

    public void abort() {
        synchronized (this) {
            this.aborted = true;
            notifyAll();
        }
    }

    public void failure(final Exception ex) {
        synchronized (this) {
            this.aborted = true;
            this.ex = ex;
            notifyAll();
        }
    }

    public boolean isAborted() {
        return this.aborted;
    }
    
    public void await(int count, long timeout) throws InterruptedException, IOException {
        synchronized (this) {
            long deadline = System.currentTimeMillis() + timeout;
            long remaining = timeout;
            while (!this.aborted && this.value > count) {
                wait(remaining);
                if (timeout > 0) {
                    remaining = deadline - System.currentTimeMillis();
                    if (remaining <= 0) {
                        break;
                    }
                }
            }
        }
        if (this.ex != null) {
            if (this.ex instanceof IOException) {
                throw (IOException) this.ex;
            } else if (this.ex instanceof RuntimeException) {
                throw (RuntimeException) this.ex;
            } else {
                throw new RuntimeException("Unexpected exception", ex);
            }
        }
    }
    
    public void await(long timeout) throws InterruptedException, IOException {
        await(0, timeout);
    }
    
}
