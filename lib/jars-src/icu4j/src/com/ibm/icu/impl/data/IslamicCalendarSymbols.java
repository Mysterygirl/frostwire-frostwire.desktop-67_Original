/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/data/Attic/IslamicCalendarSymbols.java,v $ 
 * $Date: 2002/02/16 03:05:48 $ 
 * $Revision: 1.4 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.impl.data;

import java.util.ListResourceBundle;

/**
 * Default Date Format symbols for the Islamic Calendar
 */
public class IslamicCalendarSymbols extends ListResourceBundle {
    
    private static String copyright = "Copyright \u00a9 1998-1999 IBM Corp. All Rights Reserved.";

    static final Object[][] fContents = {
        { "MonthNames", new String[] { 
                "Muharram",
                "Safar",
                "Rabi' I",
                "Rabi' II",
                "Jumada I",
                "Jumada I",
                "Rajab",
                "Sha'ban",
                "Ramadan",
                "Shawwal",
                "Dhu'l-Qi'dah",
                "Dhu'l-Hijjah",
            } },
        { "Eras", new String[] { 
                "AH"
            } },
    };
        
    public synchronized Object[][] getContents() {
        return fContents;
    }
};
