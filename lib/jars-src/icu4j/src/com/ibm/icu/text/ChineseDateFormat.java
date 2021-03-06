/*********************************************************************
 * Copyright (C) 2000-2003, International Business Machines Corporation and
 * others. All Rights Reserved.
 *********************************************************************
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/ChineseDateFormat.java,v $
 * $Date: 2003/06/03 18:49:33 $
 * $Revision: 1.9 $
 */
package com.ibm.icu.text;
import com.ibm.icu.util.*;
import com.ibm.icu.impl.Utility;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Locale;

/**
 * A concrete {@link DateFormat} for {@link com.ibm.icu.util.ChineseCalendar}.
 * This class handles a <code>ChineseCalendar</code>-specific field,
 * <code>ChineseCalendar.IS_LEAP_MONTH</code>.  It also redefines the
 * handling of two fields, <code>ERA</code> and <code>YEAR</code>.  The
 * former is displayed numerically, instead of symbolically, since it is
 * the numeric cycle number in <code>ChineseCalendar</code>.  The latter is
 * numeric, as before, but has no special 2-digit Y2K behavior.
 *
 * <p>With regard to <code>ChineseCalendar.IS_LEAP_MONTH</code>, this
 * class handles parsing specially.  If no string symbol is found at all,
 * this is taken as equivalent to an <code>IS_LEAP_MONTH</code> value of
 * zero.  This allows formats to display a special string (e.g., "*") for
 * leap months, but no string for normal months.
 *
 * <p>Summary of field changes vs. {@link SimpleDateFormat}:<pre>
 * Symbol   Meaning                 Presentation        Example
 * ------   -------                 ------------        -------
 * G        cycle                   (Number)            78
 * y        year of cycle (1..60)   (Number)            17
 * l        is leap month           (Text)              4637
 * </pre>
 *
 * @see com.ibm.icu.util.ChineseCalendar
 * @see ChineseDateFormatSymbols
 * @author Alan Liu
 * @stable ICU 2.0
 */
public class ChineseDateFormat extends SimpleDateFormat {

    // TODO Finish the constructors

    /**
     * Construct a ChineseDateFormat from a date format pattern and locale
     * @param pattern the pattern
     * @param locale the locale
     * @stable ICU 2.0
     */
   public ChineseDateFormat(String pattern, Locale locale) {
        super(pattern, new ChineseDateFormatSymbols(locale));
    }

    /**
     * @stable ICU 2.0
     */
    protected String subFormat(char ch, int count, int beginOffset,
                               FieldPosition pos, DateFormatSymbols formatData,
                               Calendar cal)  {
        switch (ch) {
        case 'G': // 'G' - ERA
            return zeroPaddingNumber(cal.get(Calendar.ERA), 1, 9);
        case 'l': // 'l' - IS_LEAP_MONTH
            {
                ChineseDateFormatSymbols symbols =
                    (ChineseDateFormatSymbols) formatData;
                return symbols.getLeapMonth(cal.get(
                               ChineseCalendar.IS_LEAP_MONTH));
            }
        default:
            return super.subFormat(ch, count, beginOffset, pos, formatData, cal);
        }
    }    

    /**
     * @stable ICU 2.0
     */
    protected int subParse(String text, int start, char ch, int count,
                           boolean obeyCount, boolean allowNegative, boolean[] ambiguousYear, Calendar cal) {
        if (ch != 'G' && ch != 'l' && ch != 'y') {
            return super.subParse(text, start, ch, count, obeyCount, allowNegative, ambiguousYear, cal);
        }

        // Skip whitespace
        start = Utility.skipWhitespace(text, start);

        ParsePosition pos = new ParsePosition(start);

        switch (ch) {
        case 'G': // 'G' - ERA
        case 'y': // 'y' - YEAR, but without the 2-digit Y2K adjustment
            {
                Number number = null;
                if (obeyCount) {
                    if ((start+count) > text.length()) {
                        return -start;
                    }
                    number = numberFormat.parse(text.substring(0, start+count), pos);
                } else {
                    number = numberFormat.parse(text, pos);
                }
                if (number == null) {
                    return -start;
                }
                int value = number.intValue();
                cal.set(ch == 'G' ? Calendar.ERA : Calendar.YEAR, value);
                return pos.getIndex();
            }
        case 'l': // 'l' - IS_LEAP_MONTH
            {
                ChineseDateFormatSymbols symbols =
                    (ChineseDateFormatSymbols) getSymbols();
                int result = matchString(text, start, ChineseCalendar.IS_LEAP_MONTH,
                                         symbols.isLeapMonth, cal);
                // Treat the absence of any matching string as setting
                // IS_LEAP_MONTH to false.
                if (result<0) {
                    cal.set(ChineseCalendar.IS_LEAP_MONTH, 0);
                    result = start;
                }
                return result;
            }
        default:
            return 0; // This can never happen
        }
    }
}
