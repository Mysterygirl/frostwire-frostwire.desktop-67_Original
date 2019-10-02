/*
 * $HeadURL: https://svn.apache.org/repos/asf/httpcomponents/httpcore/tags/4.0.1/httpcore/src/test/java/org/apache/http/params/TestBasicHttpParams.java $
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

package org.apache.http.params;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit tests for {@link BasicHttpParams}.
 *
 */
public class TestBasicHttpParams extends TestCase {

    public TestBasicHttpParams(String testName) {
        super(testName);
    }

    public static void main(String args[]) {
        String[] testCaseName = { TestBasicHttpParams.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    public static Test suite() {
        return new TestSuite(TestBasicHttpParams.class);
    }



    public void testCopyParams() {
        HttpParams parent = new BasicHttpParams();
        HttpParams child  = new DefaultedHttpParams(
                new BasicHttpParams(), parent);
        parent.setParameter("parent", "something");
        child.setParameter("child", "something");

        HttpParams copy = child.copy();
        assertSame("copied parameters have wrong class",
                   child.getClass(), copy.getClass());
        assertEquals("local parameter missing in copy",
                     "something", copy.getParameter("child"));
        assertEquals("default parameter missing in copy",
                     "something", copy.getParameter("parent"));

        // now modify stuff to make sure the copy is a copy
        child.setParameter("child", "else-child");
        assertEquals("modification in child reflected in copy",
                     "something", copy.getParameter("child"));
        child.setParameter("child+", "something");
        assertNull("new parameter in child reflected in copy",
                   copy.getParameter("child+"));

        copy.setParameter("child", "else-copy");
        assertEquals("modification in copy reflected in child",
                     "else-child", child.getParameter("child"));
        copy.setParameter("copy+", "something");
        assertNull("new parameter in copy reflected in child",
                   child.getParameter("copy+"));

        // and modify the parent to make sure there is only one
        parent.setParameter("parent+", "something");
        assertEquals("parent parameter not known in child",
                     "something", child.getParameter("parent+"));
        assertEquals("parent parameter not known in copy",
                     "something", copy.getParameter("parent+"));
    }
    
    public void testRemoveParam() {
        BasicHttpParams params = new BasicHttpParams();
        params.setParameter("param1", "paramValue1");
        assertTrue("The parameter should be removed successfully", 
                params.removeParameter("param1"));
        assertFalse("The parameter should not be present", 
                params.removeParameter("param1"));
        
        //try a remove from an empty params
        params = new BasicHttpParams();
        assertFalse("The parameter should not be present", 
                params.removeParameter("param1"));
    }

}
