/*********************************************************************
 * Copyright (C) 2000, International Business Machines Corporation and
 * others. All Rights Reserved.
 *********************************************************************
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/ChineseDateFormatSymbols.java,v $
 * $Date: 2002/12/05 01:21:31 $
 * $Revision: 1.4 $
 */
package com.ibm.icu.text;
import com.ibm.icu.util.*;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.MissingResourceException;

/**
 * A subclass of {@link DateFormatSymbols} for {@link ChineseDateFormat}.
 * This class contains additional symbols corresponding to the
 * <code>ChineseCalendar.IS_LEAP_MONTH</code> field.
 *
 * @see ChineseDateFormat
 * @see com.ibm.icu.util.ChineseCalendar
 * @author Alan Liu
 * @stable ICU 2.0
 */
public class ChineseDateFormatSymbols extends DateFormatSymbols {
    
    /**
     * Package-private array that ChineseDateFormat needs to be able to
     * read.
     */
    String isLeapMonth[]; // Do NOT add =null initializer

    /**
     * Construct a ChineseDateFormatSymbols for the default locale.
     * @stable ICU 2.0
     */
    public ChineseDateFormatSymbols() {
        this(Locale.getDefault());
    }

    /**
     * Construct a ChineseDateFormatSymbols for the provided locale.
     * @param locale the locale
     * @stable ICU 2.0
     */
    public ChineseDateFormatSymbols(Locale locale) {
        super(ChineseCalendar.class, locale);
    }

    /**
     * Construct a ChineseDateFormatSymbols for the provided calendar and locale.
     * @param cal the Calendar
     * @param locale the locale
     * @stable ICU 2.0
     */
    public ChineseDateFormatSymbols(Calendar cal, Locale locale) {
        super(cal==null?null:cal.getClass(), locale);
    }

    // New API
    /**
     * @stable ICU 2.0
     */
    public String getLeapMonth(int isLeapMonth) {
        return this.isLeapMonth[isLeapMonth];
    }

    /**
     * Override DateFormatSymbols.
     * @stable ICU 2.0
     */
    protected void constructCalendarSpecific(ResourceBundle bundle) {
        super.constructCalendarSpecific(bundle);
        if (bundle != null) {
            try {
                isLeapMonth = bundle.getStringArray("IsLeapMonth");
            } catch (MissingResourceException e) {}
        }
    }
}
