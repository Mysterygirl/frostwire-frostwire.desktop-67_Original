/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/data/Attic/HebrewCalendarSymbols.java,v $ 
 * $Date: 2002/02/16 03:05:43 $ 
 * $Revision: 1.4 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.impl.data;

import java.util.ListResourceBundle;

/**
 * Default Date Format symbols for the Hebrew Calendar
 */
public class HebrewCalendarSymbols extends ListResourceBundle {

    private static String copyright = "Copyright \u00a9 1998 IBM Corp. All Rights Reserved.";

    static final Object[][] fContents = {
        { "MonthNames", new String[] { 
                "Tishri",
                "Heshvan",
                "Kislev",
                "Tevet",
                "Shevat",
                "Adar I",       // Leap years only
                "Adar",
                "Nisan",
                "Iyar",
                "Sivan",
                "Tamuz",
                "Av",
                "Elul",
            } },
        { "Eras", new String[] { 
                "AM"
            } },
    };
        
    public synchronized Object[][] getContents() {
        return fContents;
    }
};
