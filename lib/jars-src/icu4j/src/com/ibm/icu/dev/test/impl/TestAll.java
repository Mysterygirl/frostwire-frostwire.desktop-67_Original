/*
 *******************************************************************************
 * Copyright (C) 1996-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/impl/TestAll.java,v $
 * $Date: 2003/06/03 18:49:29 $
 * $Revision: 1.2 $
 *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.impl;

import com.ibm.icu.dev.test.TestFmwk.TestGroup;

/**
 * Top level test used to run all other tests as a batch.
 */
public class TestAll extends TestGroup {
    public static void main(String[] args) throws Exception {
        new TestAll().run(args);
    }

    public TestAll() {
        super("com.ibm.icu.dev.test.util",
              new String[] {
                  "ICUServiceTest",
                  "ICUServiceThreadTest",
                  "ICUBinaryTest"
              },
              "Test miscellaneous implementation utilities");
    }

    public static final String CLASS_TARGET_NAME = "Impl";
}


