/*
 *******************************************************************************
 * Copyright (C) 1996-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/NumberFormat.java,v $ 
 * $Date: 2003/06/06 21:09:43 $ 
 * $Revision: 1.29 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.text;

import java.io.InvalidObjectException; //Bug 4185761 [Richard/GCL]
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import com.ibm.icu.util.Currency;
import com.ibm.icu.impl.ICULocaleData;
import com.ibm.icu.impl.LocaleUtility;

/**
 * <code>NumberFormat</code> is the abstract base class for all number
 * formats. This class provides the interface for formatting and parsing
 * numbers. <code>NumberFormat</code> also provides methods for determining
 * which locales have number formats, and what their names are.
 *
 * <p><strong>This is an enhanced version of <code>NumberFormat</code> that
 * is based on the standard version in the JDK.  New or changed functionality
 * is labeled
 * <strong><font face=helvetica color=red>NEW</font></strong> or
 * <strong><font face=helvetica color=red>CHANGED</font></strong>.</strong>
 *
 * <p>
 * <code>NumberFormat</code> helps you to format and parse numbers for any locale.
 * Your code can be completely independent of the locale conventions for
 * decimal points, thousands-separators, or even the particular decimal
 * digits used, or whether the number format is even decimal.
 *
 * <p>
 * To format a number for the current Locale, use one of the factory
 * class methods:
 * <blockquote>
 * <pre>
 *  myString = NumberFormat.getInstance().format(myNumber);
 * </pre>
 * </blockquote>
 * If you are formatting multiple numbers, it is
 * more efficient to get the format and use it multiple times so that
 * the system doesn't have to fetch the information about the local
 * language and country conventions multiple times.
 * <blockquote>
 * <pre>
 * NumberFormat nf = NumberFormat.getInstance();
 * for (int i = 0; i < a.length; ++i) {
 *     output.println(nf.format(myNumber[i]) + "; ");
 * }
 * </pre>
 * </blockquote>
 * To format a number for a different Locale, specify it in the
 * call to <code>getInstance</code>.
 * <blockquote>
 * <pre>
 * NumberFormat nf = NumberFormat.getInstance(Locale.FRENCH);
 * </pre>
 * </blockquote>
 * You can also use a <code>NumberFormat</code> to parse numbers:
 * <blockquote>
 * <pre>
 * myNumber = nf.parse(myString);
 * </pre>
 * </blockquote>
 * Use <code>getInstance</code> or <code>getNumberInstance</code> to get the
 * normal number format. Use <code>getIntegerInstance</code> to get an
 * integer number format. Use <code>getCurrencyInstance</code> to get the
 * currency number format. And use <code>getPercentInstance</code> to get a
 * format for displaying percentages. With this format, a fraction like
 * 0.53 is displayed as 53%.
 *
 * <p>
 * You can also control the display of numbers with such methods as
 * <code>setMinimumFractionDigits</code>.
 * If you want even more control over the format or parsing,
 * or want to give your users more control,
 * you can try casting the <code>NumberFormat</code> you get from the factory methods
 * to a <code>DecimalFormat</code>. This will work for the vast majority
 * of locales; just remember to put it in a <code>try</code> block in case you
 * encounter an unusual one.
 *
 * <p>
 * NumberFormat is designed such that some controls
 * work for formatting and others work for parsing.  The following is
 * the detailed description for each these control methods,
 * <p>
 * setParseIntegerOnly : only affects parsing, e.g.
 * if true,  "3456.78" -> 3456 (and leaves the parse position just after '6')
 * if false, "3456.78" -> 3456.78 (and leaves the parse position just after '8')
 * This is independent of formatting.  If you want to not show a decimal point
 * where there might be no digits after the decimal point, use
 * setDecimalSeparatorAlwaysShown on DecimalFormat.
 * <p>
 * You can also use forms of the <code>parse</code> and <code>format</code>
 * methods with <code>ParsePosition</code> and <code>FieldPosition</code> to
 * allow you to:
 * <ul>
 * <li> progressively parse through pieces of a string
 * <li> align the decimal point and other areas
 * </ul>
 * For example, you can align numbers in two ways:
 * <ol>
 * <li> If you are using a monospaced font with spacing for alignment,
 *      you can pass the <code>FieldPosition</code> in your format call, with
 *      <code>field</code> = <code>INTEGER_FIELD</code>. On output,
 *      <code>getEndIndex</code> will be set to the offset between the
 *      last character of the integer and the decimal. Add
 *      (desiredSpaceCount - getEndIndex) spaces at the front of the string.
 *
 * <li> If you are using proportional fonts,
 *      instead of padding with spaces, measure the width
 *      of the string in pixels from the start to <code>getEndIndex</code>.
 *      Then move the pen by
 *      (desiredPixelWidth - widthToAlignmentPoint) before drawing the text.
 *      It also works where there is no decimal, but possibly additional
 *      characters at the end, e.g., with parentheses in negative
 *      numbers: "(12)" for -12.
 * </ol>
 *
 * <h4>Synchronization</h4>
 * <p>
 * Number formats are generally not synchronized. It is recommended to create 
 * separate format instances for each thread. If multiple threads access a format
 * concurrently, it must be synchronized externally. 
 * <p>
 *
 * <h4>DecimalFormat</h4>
 * <p>DecimalFormat is the concrete implementation of NumberFormat, and the
 * NumberFormat API is essentially an abstraction from DecimalFormat's API.
 * Refer to DecimalFormat for more information about this API.</p>
 *
 * see          DecimalFormat
 * see          java.text.ChoiceFormat
 * @author       Mark Davis
 * @author       Helena Shih
 * @author       Alan Liu
 * @stable ICU 2.0
 */
public abstract class NumberFormat extends Format {

    // Constants used by factory methods to specify a style of format.
    private static final int NUMBERSTYLE = 0;
    private static final int CURRENCYSTYLE = 1;
    private static final int PERCENTSTYLE = 2;
    private static final int SCIENTIFICSTYLE = 3;
    private static final int INTEGERSTYLE = 4;

    /**
     * Field constant used to construct a FieldPosition object. Signifies that
     * the position of the integer part of a formatted number should be returned.
     * @see java.text.FieldPosition
     * @stable ICU 2.0
     */
    public static final int INTEGER_FIELD = 0;

    /**
     * Field constant used to construct a FieldPosition object. Signifies that
     * the position of the fraction part of a formatted number should be returned.
     * @see java.text.FieldPosition
     * @stable ICU 2.0
     */
    public static final int FRACTION_FIELD = 1;

    /**
     * <strong><font face=helvetica color=red>CHANGED</font></strong>
     * Format an object.  Change: recognizes <code>BigInteger</code>
     * and <code>BigDecimal</code> objects.
     * @stable ICU 2.0
     */
    public final StringBuffer format(Object number,
                                     StringBuffer toAppendTo,
                                     FieldPosition pos)
    {
        if (number instanceof Long) {
            return format(((Long)number).longValue(), toAppendTo, pos);
        } else if (number instanceof BigInteger) {
            return format((BigInteger) number, toAppendTo, pos);
        } else if (number instanceof java.math.BigDecimal) {
            return format((java.math.BigDecimal) number, toAppendTo, pos);
        } else if (number instanceof com.ibm.icu.math.BigDecimal) {
            return format((com.ibm.icu.math.BigDecimal) number, toAppendTo, pos);
        } else if (number instanceof Number) {
            return format(((Number)number).doubleValue(), toAppendTo, pos);
        } else {
            throw new IllegalArgumentException("Cannot format given Object as a Number");
        }
    }

    /**
     * @stable ICU 2.0
     */
    public final Object parseObject(String source,
                                    ParsePosition parsePosition)
    {
        return parse(source, parsePosition);
    }

   /**
     * Specialization of format.
     * @see java.text.Format#format(Object)
     * @stable ICU 2.0
     */
    public final String format (double number) {
        return format(number,new StringBuffer(),
                      new FieldPosition(0)).toString();
    }

   /**
     * Specialization of format.
     * @see java.text.Format#format(Object)
     * @stable ICU 2.0
     */
    public final String format (long number) {
        return format(number,new StringBuffer(),
                      new FieldPosition(0)).toString();
    }

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Convenience method to format a BigInteger.
     * @stable ICU 2.0
     */
    public final String format(BigInteger number) {
        return format(number, new StringBuffer(),
                      new FieldPosition(0)).toString();
    }

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Convenience method to format a BigDecimal.
     * @stable ICU 2.0
     */
    public final String format(java.math.BigDecimal number) {
        return format(number, new StringBuffer(),
                      new FieldPosition(0)).toString();
    }

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Convenience method to format an ICU BigDecimal.
     * @stable ICU 2.0
     */
    public final String format(com.ibm.icu.math.BigDecimal number) {
        return format(number, new StringBuffer(),
                      new FieldPosition(0)).toString();
    }

   /**
     * Specialization of format.
     * @see java.text.Format#format(Object, StringBuffer, FieldPosition)
     * @stable ICU 2.0
     */
    public abstract StringBuffer format(double number,
                                        StringBuffer toAppendTo,
                                        FieldPosition pos);

   /**
     * Specialization of format.
     * @see java.text.Format#format(Object, StringBuffer, FieldPosition)
     * @stable ICU 2.0
     */
    public abstract StringBuffer format(long number,
                                        StringBuffer toAppendTo,
                                        FieldPosition pos);

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Format a BigInteger.
     * @see java.text.Format#format(Object, StringBuffer, FieldPosition)
     * @stable ICU 2.0
     */
    public abstract StringBuffer format(BigInteger number,
                                        StringBuffer toAppendTo,
                                        FieldPosition pos); 

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Format a BigDecimal.
     * @see java.text.Format#format(Object, StringBuffer, FieldPosition)
     * @stable ICU 2.0
     */
    public abstract StringBuffer format(java.math.BigDecimal number,
                                        StringBuffer toAppendTo,
                                        FieldPosition pos);

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Format a BigDecimal.
     * @see java.text.Format#format(Object, StringBuffer, FieldPosition)
     * @stable ICU 2.0
     */
    public abstract StringBuffer format(com.ibm.icu.math.BigDecimal number,
                                        StringBuffer toAppendTo,
                                        FieldPosition pos);
   
   /**
     * Returns a Long if possible (e.g., within the range [Long.MIN_VALUE,
     * Long.MAX_VALUE] and with no decimals), otherwise a Double.
     * If IntegerOnly is set, will stop at a decimal
     * point (or equivalent; e.g., for rational numbers "1 2/3", will stop
     * after the 1).
     * Does not throw an exception; if no object can be parsed, index is
     * unchanged!
     * @see #isParseIntegerOnly
     * @see java.text.Format#parseObject(String, ParsePosition)
     * @stable ICU 2.0
     */
    public abstract Number parse(String text, ParsePosition parsePosition);

    /**
     * Parses text from the beginning of the given string to produce a number.
     * The method might not use the entire text of the given string.
     *
     * @param text A String whose beginning should be parsed.
     * @return A Number parsed from the string.
     * @exception ParseException if the beginning of the specified string 
     * cannot be parsed.
     * @see #format
     * @stable ICU 2.0
     */
    //Bug 4375399 [Richard/GCL]
    public Number parse(String text) throws ParseException {
        ParsePosition parsePosition = new ParsePosition(0);
        Number result = parse(text, parsePosition);
        if (parsePosition.getIndex() == 0) {
            throw new ParseException("Unparseable number: \"" + text + "\"",
                                     //PP:parsePosition.errorIndex);
                                     0);
        }
        return result;
    }

    /**
     * Returns true if this format will parse numbers as integers only.
     * For example in the English locale, with ParseIntegerOnly true, the
     * string "1234." would be parsed as the integer value 1234 and parsing
     * would stop at the "." character.  The decimal separator accepted
     * by the parse operation is locale-dependent and determined by the
     * subclass.
     * @return true if this will parse integers only
     * @stable ICU 2.0
     */
    public boolean isParseIntegerOnly() {
        return parseIntegerOnly;
    }

    /**
     * Sets whether or not numbers should be parsed as integers only.
     * @param value true if this should parse integers only
     * @see #isParseIntegerOnly
     * @stable ICU 2.0
     */
    public void setParseIntegerOnly(boolean value) {
        parseIntegerOnly = value;
    }

    //============== Locale Stuff =====================

    /**
     * Returns the default number format for the current default locale.
     * The default format is one of the styles provided by the other
     * factory methods: getNumberInstance, getIntegerInstance,
     * getCurrencyInstance or getPercentInstance.
     * Exactly which one is locale-dependent.
     * @stable ICU 2.0
     */
    //Bug 4408066 [Richard/GCL]
    public final static NumberFormat getInstance() {
        return getInstance(Locale.getDefault(), NUMBERSTYLE);
    }

    /**
     * Returns the default number format for the specified locale.
     * The default format is one of the styles provided by the other
     * factory methods: getNumberInstance, getCurrencyInstance or getPercentInstance.
     * Exactly which one is locale-dependent.
     * @stable ICU 2.0
     */
    public static NumberFormat getInstance(Locale inLocale) {
        return getInstance(inLocale, NUMBERSTYLE);
    }

    /**
     * Returns a general-purpose number format for the current default locale.
     * @stable ICU 2.0
     */
    public final static NumberFormat getNumberInstance() {
        return getInstance(Locale.getDefault(), NUMBERSTYLE);
    }

    /**
     * Returns a general-purpose number format for the specified locale.
     * @stable ICU 2.0
     */
    public static NumberFormat getNumberInstance(Locale inLocale) {
        return getInstance(inLocale, NUMBERSTYLE);
    }

    /**
     * Returns an integer number format for the current default locale. The
     * returned number format is configured to round floating point numbers
     * to the nearest integer using IEEE half-even rounding (see {@link 
     * com.ibm.icu.math.BigDecimal#ROUND_HALF_EVEN ROUND_HALF_EVEN}) for formatting,
     * and to parse only the integer part of an input string (see {@link
     * #isParseIntegerOnly isParseIntegerOnly}).
     *
     * @return a number format for integer values
     * @stable ICU 2.0
     */
    //Bug 4408066 [Richard/GCL]
    public final static NumberFormat getIntegerInstance() {
        return getInstance(Locale.getDefault(), INTEGERSTYLE);
    }

    /**
     * Returns an integer number format for the specified locale. The
     * returned number format is configured to round floating point numbers
     * to the nearest integer using IEEE half-even rounding (see {@link 
     * com.ibm.icu.math.BigDecimal#ROUND_HALF_EVEN ROUND_HALF_EVEN}) for formatting,
     * and to parse only the integer part of an input string (see {@link
     * #isParseIntegerOnly isParseIntegerOnly}).
     *
     * @param inLocale the locale for which a number format is needed
     * @return a number format for integer values
     * @stable ICU 2.0
     */
    //Bug 4408066 [Richard/GCL]
    public static NumberFormat getIntegerInstance(Locale inLocale) {
        return getInstance(inLocale, INTEGERSTYLE);
    }

    /**
     * Returns a currency format for the current default locale.
     * @return a number format for currency
     * @stable ICU 2.0
     */
    public final static NumberFormat getCurrencyInstance() {
        return getInstance(Locale.getDefault(), CURRENCYSTYLE);
    }

    /**
     * Returns a currency format for the specified locale.
     * @return a number format for currency
     * @stable ICU 2.0
     */
    public static NumberFormat getCurrencyInstance(Locale inLocale) {
        return getInstance(inLocale, CURRENCYSTYLE);
    }

    /**
     * Returns a percentage format for the current default locale.
     * @return a number format for percents
     * @stable ICU 2.0
     */
    public final static NumberFormat getPercentInstance() {
        return getInstance(Locale.getDefault(), PERCENTSTYLE);
    }

    /**
     * Returns a percentage format for the specified locale.
     * @return a number format for percents
     * @stable ICU 2.0
     */
    public static NumberFormat getPercentInstance(Locale inLocale) {
        return getInstance(inLocale, PERCENTSTYLE);
    }

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Returns a scientific format for the current default locale.
     * @return a scientific number format
     * @stable ICU 2.0
     */
    public final static NumberFormat getScientificInstance() {
        return getInstance(Locale.getDefault(), SCIENTIFICSTYLE);
    }

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Returns a scientific format for the specified locale.
     * @return a scientific number format
     * @stable ICU 2.0
     */
    public static NumberFormat getScientificInstance(Locale inLocale) {
        return getInstance(inLocale, SCIENTIFICSTYLE);
    }

    // ===== Factory stuff =====
    /**
     * A NumberFormatFactory is used to register new number formats.  The factory
     * should be able to create any of the predefined formats for each locale it
     * supports.  When registered, the locales it supports extend or override the
     * locales already supported by ICU.
     *
     * @draft ICU 2.6
     */
    public static abstract class NumberFormatFactory {
        /**
         * Value passed to format requesting a default number format.
         * @draft ICU 2.6
         */
        public static final int FORMAT_NUMBER = NUMBERSTYLE;

        /**
         * Value passed to format requesting a currency format.
         * @draft ICU 2.6
         */
        public static final int FORMAT_CURRENCY = CURRENCYSTYLE;

        /**
         * Value passed to format requesting a percent format.
         * @draft ICU 2.6
         */
        public static final int FORMAT_PERCENT = PERCENTSTYLE;

        /**
         * Value passed to format requesting a scientific format.
         * @draft ICU 2.6
         */
        public static final int FORMAT_SCIENTIFIC = SCIENTIFICSTYLE;

        /**
         * Value passed to format requesting an integer format.
         * @draft ICU 2.6
         */
        public static final int FORMAT_INTEGER = INTEGERSTYLE;

        /**
         * Returns true if this factory is visible.  Default is true.
         * If not visible, the locales supported by this factory will not
         * be listed by getAvailableLocales.  This value must not change.
         * @return true if the factory is visible.
         * @draft ICU 2.6
         */
	///CLOVER:OFF
        public boolean visible() {
            return true;
        }
	///CLOVER:ON

        /**
         * Returns an immutable collection of the locale names directly 
         * supported by this factory.
         * @return the supported locale names.
         * @draft ICU 2.6
         */
         public abstract Set getSupportedLocaleNames();

        /**
         * Returns a number format of the appropriate type.  If the locale
         * is not supported, return null.  If the locale is supported, but
         * the type is not provided by this service, return null.  Otherwise
         * return an appropriate instance of NumberFormat.
         * @param loc the locale for which to create the format
         * @param formatType the type of format
         * @return the NumberFormat, or null.
         * @draft ICU 2.6
         */
        public abstract NumberFormat createFormat(Locale loc, int formatType);

        /**
         * @draft ICU 2.6
         */
        protected NumberFormatFactory() {
        }
    }

    /**
     * A NumberFormatFactory that supports a single locale.  It can be visible or invisible.
     * @draft ICU 2.6
     */
    public static abstract class SimpleNumberFormatFactory extends NumberFormatFactory {
        final Set localeNames;
        final boolean visible;

        /**
         * @draft ICU 2.6
         */
        public SimpleNumberFormatFactory(Locale locale) {
            this(locale, true);
        }
        
        /**
         * @draft ICU 2.6
         */
        public SimpleNumberFormatFactory(Locale locale, boolean visible) {
            localeNames = Collections.singleton(LocaleUtility.canonicalLocaleString(locale));
            this.visible = visible;
        }

        /**
         * @draft ICU 2.6
         */
        public final boolean visible() {
            return visible;
        }

        /**
         * @draft ICU 2.6
         */
        public final Set getSupportedLocaleNames() {
            return localeNames;
        }
    }

    // shim so we can build without service code
    static abstract class NumberFormatShim {
        abstract Locale[] getAvailableLocales();
        abstract Object registerFactory(NumberFormatFactory f);
        abstract boolean unregister(Object k);
        abstract NumberFormat createInstance(Locale l, int k);
    }

    private static NumberFormatShim shim;
    private static NumberFormatShim getShim() {
        // Note: this instantiation is safe on loose-memory-model configurations
        // despite lack of synchronization, since the shim instance has no state--
        // it's all in the class init.  The worst problem is we might instantiate
        // two shim instances, but they'll share the same state so that's ok.
        if (shim == null) {
            try {
                Class cls = Class.forName("com.ibm.icu.text.NumberFormatServiceShim");
                shim = (NumberFormatShim)cls.newInstance();
            }
            catch (Exception e) {
		///CLOVER:OFF
                e.printStackTrace();
                throw new RuntimeException(e.getMessage());
		///CLOVER:ON
            }
        }
        return shim;
    }

    /**
     * Get the list of Locales for which NumberFormats are available.
     * @return the available locales
     * @stable ICU 2.0
     */
    public static Locale[] getAvailableLocales() {
        if (shim == null) {
            return ICULocaleData.getAvailableLocales();
        }
        return getShim().getAvailableLocales();
    }

    /**
     * Registers a new NumberFormatFactory.  The factory is adopted by
     * the service and must not be modified.  The returned object is a
     * key that can be used to unregister this factory.
     * @param factory the factory to register
     * @return a key with which to unregister the factory
     * @draft ICU 2.6 
     */
    public static Object registerFactory(NumberFormatFactory factory) {
        if (factory == null) {
            throw new IllegalArgumentException("factory must not be null");
        }
        return getShim().registerFactory(factory);
    }

    /**
     * Unregister the factory or instance associated with this key (obtained from
     * registerInstance or registerFactory).
     * @param registryKey a key obtained from registerFactory
     * @return true if the object was successfully unregistered
     * @draft ICU 2.6
     */
    public static boolean unregister(Object registryKey) {
        if (registryKey == null) {
            throw new IllegalArgumentException("registryKey must not be null");
        }

        if (shim == null) {
            return false;
        }

        return shim.unregister(registryKey);
    }

    // ===== End of factory stuff =====

    /**
     * Overrides hashCode
     * @stable ICU 2.0
     */
    public int hashCode() {
        return maximumIntegerDigits * 37 + maxFractionDigits;
        // just enough fields for a reasonable distribution
    }

    /**
     * Overrides equals.  Two NumberFormats are equal if they are of the same class
     * and the settings (groupingUsed, parseIntegerOnly, maximumIntegerDigits, etc.
     * are equal.
     * @param obj the object to compare against
     * @return true if the object is equal to this.
     * @stable ICU 2.0
     */
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        NumberFormat other = (NumberFormat) obj;
        return (maximumIntegerDigits == other.maximumIntegerDigits
            && minimumIntegerDigits == other.minimumIntegerDigits
            && maximumFractionDigits == other.maximumFractionDigits
            && minimumFractionDigits == other.minimumFractionDigits
            && groupingUsed == other.groupingUsed
            && parseIntegerOnly == other.parseIntegerOnly);
    }

    /**
     * Overrides Cloneable.
     * @stable ICU 2.0
     */
    public Object clone()
    {
        NumberFormat other = (NumberFormat) super.clone();
        return other;
    }

    /**
     * Returns true if grouping is used in this format. For example, in the
     * en_US locale, with grouping on, the number 1234567 will be formatted
     * as "1,234,567". The grouping separator as well as the size of each group
     * is locale-dependent and is determined by subclasses of NumberFormat.
     * Grouping affects both parsing and formatting.
     * @return true if grouping is used
     * @see #setGroupingUsed
     * @stable ICU 2.0
     */
    public boolean isGroupingUsed() {
        return groupingUsed;
    }

    /**
     * Sets whether or not grouping will be used in this format.  Grouping
     * affects both parsing and formatting.
     * @see #isGroupingUsed
     * @param newValue true to use grouping.
     * @stable ICU 2.0
     */
    public void setGroupingUsed(boolean newValue) {
        groupingUsed = newValue;
    }

    /**
     * Returns the maximum number of digits allowed in the integer portion of a
     * number.  The default value is 40, which subclasses can override.
     * When formatting, the exact behavior when this value is exceeded is
     * subclass-specific.  When parsing, this has no effect.
     * @return the maximum number of integer digits
     * @see #setMaximumIntegerDigits
     * @stable ICU 2.0
     */
    public int getMaximumIntegerDigits() {
        return maximumIntegerDigits;
    }

    /**
     * Sets the maximum number of digits allowed in the integer portion of a
     * number. This must be >= minimumIntegerDigits.  If the
     * new value for maximumIntegerDigits is less than the current value
     * of minimumIntegerDigits, then minimumIntegerDigits will also be set to
     * the new value.
     * @param newValue the maximum number of integer digits to be shown; if
     * less than zero, then zero is used.  Subclasses might enforce an
     * upper limit to this value appropriate to the numeric type being formatted.
     * @see #getMaximumIntegerDigits
     * @stable ICU 2.0
     */
    public void setMaximumIntegerDigits(int newValue) {
        maximumIntegerDigits = Math.max(0,newValue);
        if (minimumIntegerDigits > maximumIntegerDigits)
            minimumIntegerDigits = maximumIntegerDigits;
    }

    /**
     * Returns the minimum number of digits allowed in the integer portion of a
     * number.  The default value is 1, which subclasses can override.
     * When formatting, if this value is not reached, numbers are padded on the
     * left with the locale-specific '0' character to ensure at least this
     * number of integer digits.  When parsing, this has no effect.
     * @return the minimum number of integer digits
     * @see #setMinimumIntegerDigits
     * @stable ICU 2.0
     */
    public int getMinimumIntegerDigits() {
        return minimumIntegerDigits;
    }

    /**
     * Sets the minimum number of digits allowed in the integer portion of a
     * number.  This must be <= maximumIntegerDigits.  If the
     * new value for minimumIntegerDigits is more than the current value
     * of maximumIntegerDigits, then maximumIntegerDigits will also be set to
     * the new value.
     * @param newValue the minimum number of integer digits to be shown; if
     * less than zero, then zero is used. Subclasses might enforce an
     * upper limit to this value appropriate to the numeric type being formatted.
     * @see #getMinimumIntegerDigits
     * @stable ICU 2.0
     */
    public void setMinimumIntegerDigits(int newValue) {
        minimumIntegerDigits = Math.max(0,newValue);
        if (minimumIntegerDigits > maximumIntegerDigits)
            maximumIntegerDigits = minimumIntegerDigits;
    }

    /**
     * Returns the maximum number of digits allowed in the fraction
     * portion of a number.  The default value is 3, which subclasses
     * can override.  When formatting, the exact behavior when this
     * value is exceeded is subclass-specific.  When parsing, this has 
     * no effect.
     * @return the maximum number of fraction digits
     * @see #setMaximumFractionDigits
     * @stable ICU 2.0 
     */
    public int getMaximumFractionDigits() {
        return maximumFractionDigits;
    }

    /**
     * Sets the maximum number of digits allowed in the fraction portion of a
     * number. This must be >= minimumFractionDigits.  If the
     * new value for maximumFractionDigits is less than the current value
     * of minimumFractionDigits, then minimumFractionDigits will also be set to
     * the new value.
     * @param newValue the maximum number of fraction digits to be shown; if
     * less than zero, then zero is used. The concrete subclass may enforce an
     * upper limit to this value appropriate to the numeric type being formatted.
     * @see #getMaximumFractionDigits
     * @stable ICU 2.0
     */
    public void setMaximumFractionDigits(int newValue) {
        maximumFractionDigits = Math.max(0,newValue);
        if (maximumFractionDigits < minimumFractionDigits)
            minimumFractionDigits = maximumFractionDigits;
    }

    /**
     * Returns the minimum number of digits allowed in the fraction portion of a
     * number.  The default value is 0, which subclasses can override.
     * When formatting, if this value is not reached, numbers are padded on
     * the right with the locale-specific '0' character to ensure at least
     * this number of fraction digits.  When parsing, this has no effect.
     * @return the minimum number of fraction digits
     * @see #setMinimumFractionDigits
     * @stable ICU 2.0
     */
    public int getMinimumFractionDigits() {
        return minimumFractionDigits;
    }

    /**
     * Sets the minimum number of digits allowed in the fraction portion of a
     * number.  This must be <= maximumFractionDigits.  If the
     * new value for minimumFractionDigits exceeds the current value
     * of maximumFractionDigits, then maximumFractionDigits will also be set to
     * the new value.
     * @param newValue the minimum number of fraction digits to be shown; if
     * less than zero, then zero is used.  Subclasses might enforce an
     * upper limit to this value appropriate to the numeric type being formatted.
     * @see #getMinimumFractionDigits
     * @stable ICU 2.0
     */
    public void setMinimumFractionDigits(int newValue) {
        minimumFractionDigits = Math.max(0,newValue);
        if (maximumFractionDigits < minimumFractionDigits)
            maximumFractionDigits = minimumFractionDigits;
    }

    /**
     * Sets the <tt>Currency</tt> object used to display currency
     * amounts.  This takes effect immediately, if this format is a
     * currency format.  If this format is not a currency format, then
     * the currency object is used if and when this object becomes a
     * currency format.
     * @param theCurrency new currency object to use.  May be null for
     * some subclasses.
     * @draft ICU 2.6
     */
    public void setCurrency(Currency theCurrency) {
        currency = theCurrency;
    }

    /**
     * Gets the <tt>Currency</tt> object used to display currency
     * amounts.  This may be null.
     * @draft ICU 2.6
     */
    public Currency getCurrency() {
        return currency;
    }
    
    // =======================privates===============================

    // Hook for service
    private static NumberFormat getInstance(Locale desiredLocale, int choice) {
        if (shim == null) {
            return createInstance(desiredLocale, choice);
        } else {
            return getShim().createInstance(desiredLocale, choice);
        }
    }

    // [NEW]
    static NumberFormat createInstance(Locale desiredLocale,
                                            int choice) {
        String pattern = getPattern(desiredLocale, choice);
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(desiredLocale);
        DecimalFormat format = new DecimalFormat(pattern, symbols);
        // System.out.println("loc: " + desiredLocale + " choice: " + choice + " pat: " + pattern + " sym: " + symbols + " result: " + format);
                                 
        /*Bug 4408066
         Add codes for the new method getIntegerInstance() [Richard/GCL]
        */
        if (choice == INTEGERSTYLE) {
            format.setMaximumFractionDigits(0);
            format.setDecimalSeparatorAlwaysShown(false);
            format.setParseIntegerOnly(true);
        }
        return format;
    }

    /**
     * Returns the pattern for the provided locale and choice.
     * @param forLocale the locale of the data.
     * @param choice the pattern format.
     * @return the pattern
     * @draft ICU 2.2
     */
    // [NEW]
    protected static String getPattern(Locale forLocale, int choice) {

        /* The following code takes care of a few cases where the
         * resource data in the underlying JDK lags the new features
         * we have added to ICU4J: scientific notation, rounding, and
         * secondary grouping.
         *
         * We detect these cases here and return various hard-coded
         * resource data.  This is the simplest solution for now, but
         * it is not a good long-term mechanism.
         * 
         * We should replace this code with a data-driven mechanism
         * that reads the bundle com.ibm.icu.impl.data.LocaleElements
         * and parses an exception table that overrides the standard
         * data at java.text.resource.LocaleElements*.java.
         * Alternatively, we should create our own copy of the
         * resource data, and use that exclusively.
         */

        // TEMPORARY, until we get scientific patterns into the main
        // resources:  Retrieve scientific patterns from our resources.
        if (choice == SCIENTIFICSTYLE) {
            // Temporarily hard code; retrieve from resource later
            /*For ICU compatibility [Richard/GCL]*/
            return "#E0";
            // return NumberFormat.getBaseStringArray("NumberPatterns")[SCIENTIFICSTYLE];
        }
        // TEMPORARY: Use rounding for Swiss currency
        //if (choice == CURRENCYSTYLE &&
        //    forLocale.getCountry().equals("CH")) {
        //    return "'Fr. '#,##0.05;'Fr.-'#,##0.05";
        //}
        // TEMPORARY: Special case IN number format
        //if (choice == NUMBERSTYLE &&
        //    forLocale.getCountry().equals("IN")) {
        //    return "#,##,##0.###";
        //}

		// {dlf}
		ResourceBundle rb = ICULocaleData.getLocaleElements(forLocale);
		String[] numberPatterns = rb.getStringArray("NumberPatterns");

		/* {dlf}
        // Try the cache first
        String[] numberPatterns = (String[]) cachedLocaleData.get(forLocale);
        if (numberPatterns == null) {
            OverlayBundle resource = new OverlayBundle(new String[]
                { "com.ibm.icu.impl.data.LocaleElements", RESOURCE_BASE }, forLocale);
            numberPatterns = resource.getStringArray("NumberPatterns");
            // Update the cache
            cachedLocaleData.put(forLocale, numberPatterns); 
        }
		*/

        /*Bug 4408066
         Add codes for the new method getIntegerInstance() [Richard/GCL]
        */
        int entry = (choice == INTEGERSTYLE) ? NUMBERSTYLE : choice; //[Richard/GCL]
        return numberPatterns[entry]; //[Richard/GCL]
    }

    /**
     * First, read in the default serializable data.
     *
     * Then, if <code>serialVersionOnStream</code> is less than 1, indicating that
     * the stream was written by JDK 1.1,
     * set the <code>int</code> fields such as <code>maximumIntegerDigits</code>
     * to be equal to the <code>byte</code> fields such as <code>maxIntegerDigits</code>,
     * since the <code>int</code> fields were not present in JDK 1.1.
     * Finally, set serialVersionOnStream back to the maximum allowed value so that
     * default serialization will work properly if this object is streamed out again.
     */
    private void readObject(ObjectInputStream stream)
         throws IOException, ClassNotFoundException
    {
        stream.defaultReadObject();
        if (serialVersionOnStream < 1) {
            // Didn't have additional int fields, reassign to use them.
            maximumIntegerDigits = maxIntegerDigits;
            minimumIntegerDigits = minIntegerDigits;
            maximumFractionDigits = maxFractionDigits;
            minimumFractionDigits = minFractionDigits;
        }
        /*Bug 4185761
          Validate the min and max fields [Richard/GCL]
        */
        if (minimumIntegerDigits > maximumIntegerDigits ||
            minimumFractionDigits > maximumFractionDigits ||
            minimumIntegerDigits < 0 || minimumFractionDigits < 0) {
            throw new InvalidObjectException("Digit count range invalid");
        }
        serialVersionOnStream = currentSerialVersion;
    }

    /**
     * Write out the default serializable data, after first setting
     * the <code>byte</code> fields such as <code>maxIntegerDigits</code> to be
     * equal to the <code>int</code> fields such as <code>maximumIntegerDigits</code>
     * (or to <code>Byte.MAX_VALUE</code>, whichever is smaller), for compatibility
     * with the JDK 1.1 version of the stream format.
     */
    private void writeObject(ObjectOutputStream stream)
         throws IOException
    {
        maxIntegerDigits = (maximumIntegerDigits > Byte.MAX_VALUE) ? Byte.MAX_VALUE :
            (byte)maximumIntegerDigits;
        minIntegerDigits = (minimumIntegerDigits > Byte.MAX_VALUE) ? Byte.MAX_VALUE :
            (byte)minimumIntegerDigits;
        maxFractionDigits = (maximumFractionDigits > Byte.MAX_VALUE) ? Byte.MAX_VALUE :
            (byte)maximumFractionDigits;
        minFractionDigits = (minimumFractionDigits > Byte.MAX_VALUE) ? Byte.MAX_VALUE :
            (byte)minimumFractionDigits;
        stream.defaultWriteObject();
    }

// Unused -- Alan 2003-05
//    /**
//     * Cache to hold the NumberPatterns of a Locale.
//     */
//    private static final Hashtable cachedLocaleData = new Hashtable(3);

    /*Bug 4408066
      Add Field for the new method getIntegerInstance() [Richard/GCL]
    */

    /**
     * True if the the grouping (i.e. thousands) separator is used when
     * formatting and parsing numbers.
     *
     * @serial
     * @see #isGroupingUsed
     */
    private boolean groupingUsed = true;

    /**
     * The maximum number of digits allowed in the integer portion of a
     * number.  <code>maxIntegerDigits</code> must be greater than or equal to
     * <code>minIntegerDigits</code>.
     * <p>
     * <strong>Note:</strong> This field exists only for serialization
     * compatibility with JDK 1.1.  In JDK 1.2 and higher, the new
     * <code>int</code> field <code>maximumIntegerDigits</code> is used instead.
     * When writing to a stream, <code>maxIntegerDigits</code> is set to
     * <code>maximumIntegerDigits</code> or <code>Byte.MAX_VALUE</code>,
     * whichever is smaller.  When reading from a stream, this field is used
     * only if <code>serialVersionOnStream</code> is less than 1. 
     *
     * @serial
     * @see #getMaximumIntegerDigits
     */
    private byte    maxIntegerDigits = 40;

    /**
     * The minimum number of digits allowed in the integer portion of a
     * number.  <code>minimumIntegerDigits</code> must be less than or equal to
     * <code>maximumIntegerDigits</code>.
     * <p>
     * <strong>Note:</strong> This field exists only for serialization
     * compatibility with JDK 1.1.  In JDK 1.2 and higher, the new
     * <code>int</code> field <code>minimumIntegerDigits</code> is used instead.
     * When writing to a stream, <code>minIntegerDigits</code> is set to
     * <code>minimumIntegerDigits</code> or <code>Byte.MAX_VALUE</code>,
     * whichever is smaller.  When reading from a stream, this field is used
     * only if <code>serialVersionOnStream</code> is less than 1. 
     *
     * @serial
     * @see #getMinimumIntegerDigits
     */
    private byte    minIntegerDigits = 1;

    /**
     * The maximum number of digits allowed in the fractional portion of a
     * number.  <code>maximumFractionDigits</code> must be greater than or equal to
     * <code>minimumFractionDigits</code>.
     * <p>
     * <strong>Note:</strong> This field exists only for serialization
     * compatibility with JDK 1.1.  In JDK 1.2 and higher, the new
     * <code>int</code> field <code>maximumFractionDigits</code> is used instead.
     * When writing to a stream, <code>maxFractionDigits</code> is set to
     * <code>maximumFractionDigits</code> or <code>Byte.MAX_VALUE</code>,
     * whichever is smaller.  When reading from a stream, this field is used
     * only if <code>serialVersionOnStream</code> is less than 1. 
     *
     * @serial
     * @see #getMaximumFractionDigits
     */
    private byte    maxFractionDigits = 3;    // invariant, >= minFractionDigits

    /**
     * The minimum number of digits allowed in the fractional portion of a
     * number.  <code>minimumFractionDigits</code> must be less than or equal to
     * <code>maximumFractionDigits</code>.
     * <p>
     * <strong>Note:</strong> This field exists only for serialization
     * compatibility with JDK 1.1.  In JDK 1.2 and higher, the new
     * <code>int</code> field <code>minimumFractionDigits</code> is used instead.
     * When writing to a stream, <code>minFractionDigits</code> is set to
     * <code>minimumFractionDigits</code> or <code>Byte.MAX_VALUE</code>,
     * whichever is smaller.  When reading from a stream, this field is used
     * only if <code>serialVersionOnStream</code> is less than 1. 
     *
     * @serial
     * @see #getMinimumFractionDigits
     */
    private byte    minFractionDigits = 0;

    /**
     * True if this format will parse numbers as integers only.
     *
     * @serial
     * @see #isParseIntegerOnly
     */
    private boolean parseIntegerOnly = false;

    // new fields for 1.2.  byte is too small for integer digits.

    /**
     * The maximum number of digits allowed in the integer portion of a
     * number.  <code>maximumIntegerDigits</code> must be greater than or equal to
     * <code>minimumIntegerDigits</code>.
     *
     * @serial
     * @see #getMaximumIntegerDigits
     */
    private int    maximumIntegerDigits = 40;

    /**
     * The minimum number of digits allowed in the integer portion of a
     * number.  <code>minimumIntegerDigits</code> must be less than or equal to
     * <code>maximumIntegerDigits</code>.
     *
     * @serial
     * @see #getMinimumIntegerDigits
     */
    private int    minimumIntegerDigits = 1;

    /**
     * The maximum number of digits allowed in the fractional portion of a
     * number.  <code>maximumFractionDigits</code> must be greater than or equal to
     * <code>minimumFractionDigits</code>.
     *
     * @serial
     * @see #getMaximumFractionDigits
     */
    private int    maximumFractionDigits = 3;    // invariant, >= minFractionDigits

    /**
     * The minimum number of digits allowed in the fractional portion of a
     * number.  <code>minimumFractionDigits</code> must be less than or equal to
     * <code>maximumFractionDigits</code>.
     *
     * @serial
     * @see #getMinimumFractionDigits
     */
    private int    minimumFractionDigits = 0;

    /**
     * Currency object used to format currencies.  Subclasses may
     * ignore this if they are not currency formats.  This will be
     * null unless a subclass sets it to a non-null value.
     * @since ICU 2.6
     */
    private Currency currency;

    static final int currentSerialVersion = 1;

    /**
     * Describes the version of <code>NumberFormat</code> present on the stream.
     * Possible values are:
     * <ul>
     * <li><b>0</b> (or uninitialized): the JDK 1.1 version of the stream format.
     *     In this version, the <code>int</code> fields such as
     *     <code>maximumIntegerDigits</code> were not present, and the <code>byte</code>
     *     fields such as <code>maxIntegerDigits</code> are used instead.
     *
     * <li><b>1</b>: the JDK 1.2 version of the stream format.  The values of the
     *     <code>byte</code> fields such as <code>maxIntegerDigits</code> are ignored,
     *     and the <code>int</code> fields such as <code>maximumIntegerDigits</code>
     *     are used instead.
     * </ul>
     * When streaming out a <code>NumberFormat</code>, the most recent format
     * (corresponding to the highest allowable <code>serialVersionOnStream</code>)
     * is always written.
     *
     * @serial
     */
    private int serialVersionOnStream = currentSerialVersion;

    // Removed "implements Cloneable" clause.  Needs to update serialization
    // ID for backward compatibility.
    static final long serialVersionUID = -2308460125733713944L;

    /**
     * Empty constructor.
     * @draft ICU 2.6
     */
    protected NumberFormat() {
    }
}
