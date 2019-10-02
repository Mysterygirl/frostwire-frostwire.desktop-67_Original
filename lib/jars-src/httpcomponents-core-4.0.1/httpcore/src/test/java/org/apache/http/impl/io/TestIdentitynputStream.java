/*
 * $HeadURL: https://svn.apache.org/repos/asf/httpcomponents/httpcore/tags/4.0.1/httpcore/src/test/java/org/apache/http/impl/io/TestIdentitynputStream.java $
 * $Revision: 744517 $
 * $Date: 2009-02-14 17:39:33 +0100 (Sat, 14 Feb 2009) $
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

package org.apache.http.impl.io;

import org.apache.http.impl.io.IdentityInputStream;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.mockup.SessionInputBufferMockup;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Simple tests for {@link IdentityInputStream}.
 *
 */
public class TestIdentitynputStream extends TestCase {

    // ------------------------------------------------------------ Constructor
    public TestIdentitynputStream(String testName) {
        super(testName);
    }

    // ------------------------------------------------------------------- Main
    public static void main(String args[]) {
        String[] testCaseName = { TestIdentitynputStream.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    // ------------------------------------------------------- TestCase Methods

    public static Test suite() {
        return new TestSuite(TestIdentitynputStream.class);
    }

    public void testConstructor() throws Exception {
        SessionInputBuffer receiver = new SessionInputBufferMockup(new byte[] {});
        new IdentityInputStream(receiver);
        try {
            new IdentityInputStream(null);
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException ex) {
            //expected
        }
    }
    
    public void testBasicRead() throws Exception {
        byte[] input = new byte[] {'a', 'b', 'c'};
        SessionInputBufferMockup receiver = new SessionInputBufferMockup(input);
        IdentityInputStream instream = new IdentityInputStream(receiver);
        byte[] tmp = new byte[2];
        assertEquals(2, instream.read(tmp, 0, tmp.length));
        assertEquals('a', tmp[0]);
        assertEquals('b', tmp[1]);
        assertEquals('c', instream.read());
        assertEquals(-1, instream.read(tmp, 0, tmp.length));
        assertEquals(-1, instream.read());
        assertEquals(-1, instream.read(tmp, 0, tmp.length));
        assertEquals(-1, instream.read());        
    }
    
    public void testClosedCondition() throws Exception {
        byte[] input = new byte[] {'a', 'b', 'c'};
        SessionInputBufferMockup receiver = new SessionInputBufferMockup(input);
        IdentityInputStream instream = new IdentityInputStream(receiver);

        instream.close();
        instream.close();
        
        assertTrue(instream.available() == 0);
        byte[] tmp = new byte[2];
        assertEquals(-1, instream.read(tmp, 0, tmp.length));
        assertEquals(-1, instream.read());
        assertEquals(-1, instream.read(tmp, 0, tmp.length));
        assertEquals(-1, instream.read());        
    }

    public void testAvailable() throws Exception {
        byte[] input = new byte[] {'a', 'b', 'c'};
        SessionInputBufferMockup receiver = new SessionInputBufferMockup(input);
        IdentityInputStream instream = new IdentityInputStream(receiver);
        assertTrue(instream.available() > 0);        
    }
    
}
