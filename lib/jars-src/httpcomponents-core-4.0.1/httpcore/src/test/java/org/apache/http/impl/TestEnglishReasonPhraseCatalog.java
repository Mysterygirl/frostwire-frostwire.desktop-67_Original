/*
 * $HeadURL: https://svn.apache.org/repos/asf/httpcomponents/httpcore/tags/4.0.1/httpcore/src/test/java/org/apache/http/impl/TestEnglishReasonPhraseCatalog.java $
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

package org.apache.http.impl;

import junit.framework.*;
import java.lang.reflect.*;

import org.apache.http.HttpStatus;


/**
 *
 * Unit tests for {@link EnglishReasonPhraseCatalog}
 *
 */
public class TestEnglishReasonPhraseCatalog extends TestCase {

    // ------------------------------------------------------------ Constructor
    public TestEnglishReasonPhraseCatalog(String testName) {
        super(testName);
    }

    // ------------------------------------------------------------------- Main
    public static void main(String args[]) {
        String[] testCaseName = { TestEnglishReasonPhraseCatalog.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    // ------------------------------------------------------- TestCase Methods

    public static Test suite() {
        return new TestSuite(TestEnglishReasonPhraseCatalog.class);
    }


    // ----------------------------------------------------------- Test Methods

    public void testReasonPhrases() throws IllegalAccessException {
    Field[] publicFields = HttpStatus.class.getFields();

    assertTrue( publicFields != null );

    assertTrue( publicFields.length > 0 );

    for (int i = 0; i < publicFields.length; i++)
    {
        final Field f = publicFields[i];

        final int modifiers = f.getModifiers();

        if ( (f.getType() == int.class)
            && Modifier.isPublic(modifiers)
            && Modifier.isFinal(modifiers)
            && Modifier.isStatic(modifiers) )
        {
            final int iValue = f.getInt(null);
            final String text = EnglishReasonPhraseCatalog.
                            INSTANCE.getReason(iValue, null);
            assertTrue("text is null for HttpStatus."+f.getName(), 
                                   (text != null));
            assertTrue(text.length() > 0);
        }
    }
    }


    public void testStatusInvalid() throws Exception {
        try {
            EnglishReasonPhraseCatalog.INSTANCE.getReason(-1, null);
            fail("IllegalArgumentException must have been thrown (-1)");
        } catch (IllegalArgumentException expected) {
        }
        try {
            EnglishReasonPhraseCatalog.INSTANCE.getReason(99, null);
            fail("IllegalArgumentException must have been thrown (99)");
        } catch (IllegalArgumentException expected) {
        }
        try {
            EnglishReasonPhraseCatalog.INSTANCE.getReason(600, null);
            fail("IllegalArgumentException must have been thrown (600)");
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testStatusAll() throws Exception {
        for (int i = 100; i < 600; i++) {
            EnglishReasonPhraseCatalog.INSTANCE.getReason(i, null);
        }
    }
}
