/*
 *******************************************************************************
 * Copyright (C) 1996-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/format/TestAll.java,v $
 * $Date: 2003/06/03 18:49:29 $
 * $Revision: 1.4 $
 *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.format;

import com.ibm.icu.dev.test.TestFmwk.TestGroup;

/**
 * Top level test used to run all other tests as a batch.
 */

public class TestAll extends TestGroup {

    public static void main(String[] args) {
        new TestAll().run(args);
    }

    public TestAll() {
        super(new String[] {
                  "TestAll$RBNF",
                  "TestAll$NumberFormat",
                  "TestAll$DateFormat",
                  "com.ibm.icu.dev.test.format.BigNumberFormatTest"
              },
              "Formatting Tests");
    }

    public static class RBNF extends TestGroup {
        public RBNF() {
            super(new String[] {
                "RbnfTest",
                "RbnfRoundTripTest",
            });
        }
    }

    public static class NumberFormat extends TestGroup {
        public NumberFormat() {
            super(new String[] {
                "IntlTestNumberFormat",
                "IntlTestNumberFormatAPI",
                "NumberFormatTest",
                "NumberFormatRegistrationTest",
                "NumberFormatRoundTripTest",
                "NumberRegression",
                "NumberFormatRegressionTest",
                "IntlTestDecimalFormatAPI",
                "IntlTestDecimalFormatAPIC",
                "IntlTestDecimalFormatSymbols",
                "IntlTestDecimalFormatSymbolsC",
            });
        }
    }

    public static class DateFormat extends TestGroup {
        public DateFormat() {
            super(new String[] {
                "DateFormatMiscTests",
                "DateFormatRegressionTest",
                "DateFormatRoundTripTest",
                "DateFormatTest",
                "IntlTestDateFormat",
                "IntlTestDateFormatAPI",
                "IntlTestDateFormatAPIC",
                "IntlTestDateFormatSymbols",
                "IntlTestSimpleDateFormatAPI",
                "DateFormatRegressionTestJ",
            });
        }
    }

    public static final String CLASS_TARGET_NAME = "Format";
}

