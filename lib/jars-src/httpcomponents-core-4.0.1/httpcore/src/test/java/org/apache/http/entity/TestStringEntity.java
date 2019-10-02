/*
 * $HeadURL: https://svn.apache.org/repos/asf/httpcomponents/httpcore/tags/4.0.1/httpcore/src/test/java/org/apache/http/entity/TestStringEntity.java $
 * $Revision: 744517 $
 * $Date: 2009-02-14 17:39:33 +0100 (Sat, 14 Feb 2009) $
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

package org.apache.http.entity;

import java.io.ByteArrayOutputStream;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.http.protocol.HTTP;

/**
 * Unit tests for {@link StringEntity}.
 *
 */
public class TestStringEntity extends TestCase {

    public TestStringEntity(String testName) {
        super(testName);
    }

    public static void main(String args[]) {
        String[] testCaseName = { TestStringEntity.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    public static Test suite() {
        return new TestSuite(TestStringEntity.class);
    }

    public void testBasics() throws Exception {
        String s = "Message content";
        StringEntity httpentity = new StringEntity(s, HTTP.ISO_8859_1);
        
        byte[] bytes = s.getBytes(HTTP.ISO_8859_1);
        assertEquals(bytes.length, httpentity.getContentLength());
        assertNotNull(httpentity.getContent());
        assertTrue(httpentity.isRepeatable());
        assertFalse(httpentity.isStreaming());        
    }

    public void testIllegalConstructor() throws Exception {
        try {
            new StringEntity(null);
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    public void testDefaultContent() throws Exception {
        String s = "Message content";
        StringEntity httpentity = new StringEntity(s, HTTP.US_ASCII);
        assertEquals("text/plain; charset=US-ASCII", 
                httpentity.getContentType().getValue());
        httpentity = new StringEntity(s);
        assertEquals("text/plain; charset=ISO-8859-1", 
                httpentity.getContentType().getValue());
    }

    public void testWriteTo() throws Exception {
        String s = "Message content";
        byte[] bytes = s.getBytes(HTTP.ISO_8859_1);
        StringEntity httpentity = new StringEntity(s);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        httpentity.writeTo(out);
        byte[] bytes2 = out.toByteArray();
        assertNotNull(bytes2);
        assertEquals(bytes.length, bytes2.length);
        for (int i = 0; i < bytes.length; i++) {
            assertEquals(bytes[i], bytes2[i]);
        }

        out = new ByteArrayOutputStream();
        httpentity.writeTo(out);
        bytes2 = out.toByteArray();
        assertNotNull(bytes2);
        assertEquals(bytes.length, bytes2.length);
        for (int i = 0; i < bytes.length; i++) {
            assertEquals(bytes[i], bytes2[i]);
        }
        
        try {
            httpentity.writeTo(null);
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }
        
}
