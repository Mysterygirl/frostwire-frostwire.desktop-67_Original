/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/data/HolidayBundle_de_AT.java,v $ 
 * $Date: 2002/02/16 03:05:45 $ 
 * $Revision: 1.4 $
 *
 *****************************************************************************************
 */

package com.ibm.icu.impl.data;

import com.ibm.icu.util.*;
import java.util.Calendar;
import java.util.ListResourceBundle;

public class HolidayBundle_de_AT extends ListResourceBundle {
    static private final Holiday[] fHolidays = {
        SimpleHoliday.NEW_YEARS_DAY,
        SimpleHoliday.EPIPHANY,
        EasterHoliday.GOOD_FRIDAY,
        EasterHoliday.EASTER_SUNDAY,
        EasterHoliday.EASTER_MONDAY,
        EasterHoliday.ASCENSION,
        EasterHoliday.WHIT_SUNDAY,
        EasterHoliday.WHIT_MONDAY,
        EasterHoliday.CORPUS_CHRISTI,
        SimpleHoliday.ASSUMPTION,
        SimpleHoliday.ALL_SAINTS_DAY,
        SimpleHoliday.IMMACULATE_CONCEPTION,
        SimpleHoliday.CHRISTMAS,
        SimpleHoliday.ST_STEPHENS_DAY,

        new SimpleHoliday(Calendar.MAY,        1,  0,               "National Holiday"),
        new SimpleHoliday(Calendar.OCTOBER,   31, -Calendar.MONDAY, "National Holiday"),
    };

    static private final Object[][] fContents = {
        { "holidays",   fHolidays },

        // Only holidays names different from those used in Germany are listed here
        {   "Christmas",        "Christtag" },
        {   "New Year's Day",   "Neujahrstag" },
    };
    public synchronized Object[][] getContents() { return fContents; }
};
