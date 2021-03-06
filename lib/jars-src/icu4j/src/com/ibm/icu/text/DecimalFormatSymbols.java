/*
 *******************************************************************************
 * Copyright (C) 1996-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/DecimalFormatSymbols.java,v $ 
 * $Date: 2003/06/03 18:49:34 $ 
 * $Revision: 1.11 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.text;

import com.ibm.icu.impl.ICULocaleData;
import com.ibm.icu.util.Currency;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Locale;
import java.util.ResourceBundle;
import java.text.ChoiceFormat;

/**
 * This class represents the set of symbols (such as the decimal separator, the
 * grouping separator, and so on) needed by <code>DecimalFormat</code> to format
 * numbers. <code>DecimalFormat</code> creates for itself an instance of
 * <code>DecimalFormatSymbols</code> from its locale data.  If you need to
 * change any of these symbols, you can get the
 * <code>DecimalFormatSymbols</code> object from your <code>DecimalFormat</code>
 * and modify it.
 *
 * <p><strong>This is an enhanced version of <code>DecimalFormatSymbols</code> that
 * is based on the standard version in the JDK.  New or changed functionality
 * is labeled
 * <strong><font face=helvetica color=red>NEW</font></strong>.</strong>
 *
 * @see          java.util.Locale
 * @see          DecimalFormat
 * @author       Mark Davis
 * @author       Alan Liu
 * @stable ICU 2.0
 */

final public class DecimalFormatSymbols implements Cloneable, Serializable {

    /**
     * Create a DecimalFormatSymbols object for the default locale.
     * @stable ICU 2.0
     */
    public DecimalFormatSymbols() {
        initialize( Locale.getDefault() );
    }

    /**
     * Create a DecimalFormatSymbols object for the given locale.
     * @param locale the locale
     * @stable ICU 2.0
     */
    public DecimalFormatSymbols( Locale locale ) {
        initialize( locale );
    }

    /**
     * Return the character used for zero. Different for Arabic, etc.
     * @return the character
     * @stable ICU 2.0
     */
    public char getZeroDigit() {
        return zeroDigit;
    }

    /**
     * Set the character used for zero.
     * @param zeroDigit the zero character.
     * @stable ICU 2.0
     */
    public void setZeroDigit(char zeroDigit) {
        this.zeroDigit = zeroDigit;
    }

    /**
     * Return the character used for thousands separator. Different for French, etc.
     * @return the thousands character
     * @stable ICU 2.0
     */
    public char getGroupingSeparator() {
        return groupingSeparator;
    }

    /**
     * Set the character used for thousands separator. Different for French, etc.
     * @param groupingSeparator the thousands character
     * @stable ICU 2.0
     */
    public void setGroupingSeparator(char groupingSeparator) {
        this.groupingSeparator = groupingSeparator;
    }

    /**
     * Return the character used for decimal sign. Different for French, etc.
     * @return the decimal character
     * @stable ICU 2.0
     */
    public char getDecimalSeparator() {
        return decimalSeparator;
    }

    /**
     * Set the character used for decimal sign. Different for French, etc.
     * @param decimalSeparator the decimal character
     * @stable ICU 2.0
     */
    public void setDecimalSeparator(char decimalSeparator) {
        this.decimalSeparator = decimalSeparator;
    }

    /**
     * Return the character used for mille percent sign. Different for Arabic, etc.
     * @return the mille percent character
     * @stable ICU 2.0
     */
    public char getPerMill() {
        return perMill;
    }

    /**
     * Set the character used for mille percent sign. Different for Arabic, etc.
     * @param perMill the mille percent character
     * @stable ICU 2.0
     */
    public void setPerMill(char perMill) {
        this.perMill = perMill;
    }

    /**
     * Return the character used for percent sign. Different for Arabic, etc.
     * @return the percent character
     * @stable ICU 2.0
     */
    public char getPercent() {
        return percent;
    }

    /**
     * Set the character used for percent sign. Different for Arabic, etc.
     * @param percent the percent character
     * @stable ICU 2.0
     */
    public void setPercent(char percent) {
        this.percent = percent;
    }

    /**
     * Return the character used for a digit in a pattern.
     * @return the digit pattern character
     * @stable ICU 2.0
     */
    public char getDigit() {
        return digit;
    }

    /**
     * Set the character used for a digit in a pattern.
     * @param digit the digit pattern character
     * @stable ICU 2.0
     */
    public void setDigit(char digit) {
        this.digit = digit;
    }

    /**
     * Return the character used to separate positive and negative subpatterns
     * in a pattern.
     * @return the pattern separator character
     * @stable ICU 2.0
     */
    public char getPatternSeparator() {
        return patternSeparator;
    }

    /**
     * Set the character used to separate positive and negative subpatterns
     * in a pattern.
     * @param patternSeparator the pattern separator character
     * @stable ICU 2.0
     */
    public void setPatternSeparator(char patternSeparator) {
        this.patternSeparator = patternSeparator;
    }

    /**
     * Return the String used to represent infinity. Almost always left
     * unchanged.
     * @return the Infinity string
     * @stable ICU 2.0
     */
     //Bug 4194173 [Richard/GCL]

    public String getInfinity() {
        return infinity;
    }

    /**
     * Set the String used to represent infinity. Almost always left
     * unchanged.
     * @param infinity the Infinity String
     * @stable ICU 2.0
     */
    public void setInfinity(String infinity) {
        this.infinity = infinity;
    }

    /**
     * Return the String used to represent NaN. Almost always left
     * unchanged.
     * @return the NaN String
     * @stable ICU 2.0
     */
     //Bug 4194173 [Richard/GCL]
    public String getNaN() {
        return NaN;
    }

    /**
     * Set the String used to represent NaN. Almost always left
     * unchanged.
     * @param NaN the NaN String
     * @stable ICU 2.0
     */
    public void setNaN(String NaN) {
        this.NaN = NaN;
    }

    /**
     * Return the character used to represent minus sign. If no explicit
     * negative format is specified, one is formed by prefixing
     * minusSign to the positive format.
     * @return the minus sign character
     * @stable ICU 2.0
     */
    public char getMinusSign() {
        return minusSign;
    }

    /**
     * Set the character used to represent minus sign. If no explicit
     * negative format is specified, one is formed by prefixing
     * minusSign to the positive format.
     * @param minusSign the minus sign character
     * @stable ICU 2.0
     */
    public void setMinusSign(char minusSign) {
        this.minusSign = minusSign;
    }

    /**
     * Return the string denoting the local currency.
     * @return the local currency String.
     * @stable ICU 2.0
     */
    public String getCurrencySymbol()
    {
        return currencySymbol;
    }

    /**
     * Set the string denoting the local currency.
     * @param currency the local currency String.
     * @stable ICU 2.0
     */
    public void setCurrencySymbol(String currency)
    {
        currencySymbol = currency;
    }

    /**
     * Return the international string denoting the local currency.
     * @return the international string denoting the local currency
     * @stable ICU 2.0
     */
    public String getInternationalCurrencySymbol()
    {
        return intlCurrencySymbol;
    }

    /**
     * Set the international string denoting the local currency.
     * @param currency the international string denoting the local currency.
     * @stable ICU 2.0
     */
    public void setInternationalCurrencySymbol(String currency)
    {
        intlCurrencySymbol = currency;
    }

    /**
     * Return the monetary decimal separator.
     * @return the monetary decimal separator character
     * @stable ICU 2.0
     */
    public char getMonetaryDecimalSeparator()
    {
        return monetarySeparator;
    }

    /**
     * Set the monetary decimal separator.
     * @param sep the monetary decimal separator character
     * @stable ICU 2.0
     */
    public void setMonetaryDecimalSeparator(char sep)
    {
        monetarySeparator = sep;
    }

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Return the string used to separate the mantissa from the exponent.
     * Examples: "x10^" for 1.23x10^4, "E" for 1.23E4.
     * @return the localized exponent symbol, used in localized patterns
     * and formatted strings
     * @see #setExponentSeparator
     * @stable ICU 2.0
     */
    public String getExponentSeparator()
    {
        return exponentSeparator;
    }

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Set the string used to separate the mantissa from the exponent.
     * Examples: "x10^" for 1.23x10^4, "E" for 1.23E4.
     * @param exp the localized exponent symbol, used in localized patterns
     * and formatted strings
     * @see #getExponentSeparator
     * @stable ICU 2.0
     */
    public void setExponentSeparator(String exp)
    {
        exponentSeparator = exp;
    }

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Return the localized plus sign.
     * @return the plus sign, used in localized patterns and formatted
     * strings
     * @see #setPlusSign
     * @see #setMinusSign
     * @see #getMinusSign
     * @stable ICU 2.0
     */
    public char getPlusSign() {
        return plusSign;
    }

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Set the localized plus sign.
     * @param plus the plus sign, used in localized patterns and formatted
     * strings
     * @see #getPlusSign
     * @see #setMinusSign
     * @see #getMinusSign
     * @stable ICU 2.0
     */
    public void setPlusSign(char plus) {
        plusSign = plus;
    }

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Return the character used to pad numbers out to a specified width.  This
     * is not the pad character itself; rather, it is the special pattern
     * character <em>preceding</em> the pad character.  In the pattern
     * "*_#,##0", '*' is the pad escape, and '_' is the pad character.
     * @return the character 
     * @see #setPadEscape
     * @see DecimalFormat#getFormatWidth
     * @see DecimalFormat#getPadPosition
     * @see DecimalFormat#getPadCharacter
     * @stable ICU 2.0
     */
    public char getPadEscape() {
        return padEscape;
    }

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Set the character used to pad numbers out to a specified width.  This is
     * not the pad character itself; rather, it is the special pattern character
     * <em>preceding</em> the pad character.  In the pattern "*_#,##0", '*' is
     * the pad escape, and '_' is the pad character.
     * @see #getPadEscape
     * @see DecimalFormat#setFormatWidth
     * @see DecimalFormat#setPadPosition
     * @see DecimalFormat#setPadCharacter
     * @stable ICU 2.0
     */
    public void setPadEscape(char c) {
        padEscape = c;
    }

    /**
     * Returns the locale for which this object was constructed.
     * @return the locale for which this object was constructed
     * @stable ICU 2.0
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Standard override.
     * @stable ICU 2.0
     */
    public Object clone() {
        try {
            return (DecimalFormatSymbols)super.clone();
            // other fields are bit-copied
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    /**
     * Override equals.
     * @stable ICU 2.0
     */
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (getClass() != obj.getClass()) return false;
        DecimalFormatSymbols other = (DecimalFormatSymbols) obj;
        return (zeroDigit == other.zeroDigit &&
        groupingSeparator == other.groupingSeparator &&
        decimalSeparator == other.decimalSeparator &&
        percent == other.percent &&
        perMill == other.perMill &&
        digit == other.digit &&
        minusSign == other.minusSign &&
        patternSeparator == other.patternSeparator &&
        infinity.equals(other.infinity) &&
        NaN.equals(other.NaN) &&
        currencySymbol.equals(other.currencySymbol) &&
        intlCurrencySymbol.equals(other.intlCurrencySymbol) &&
        padEscape == other.padEscape && // [NEW]
        plusSign == other.plusSign && // [NEW]
        exponentSeparator.equals(other.exponentSeparator) && // [NEW]
        monetarySeparator == other.monetarySeparator);
    }

    /**
     * Override hashCode
     * @stable ICU 2.0
     */
    public int hashCode() {
            int result = zeroDigit;
            result = result * 37 + groupingSeparator;
            result = result * 37 + decimalSeparator;
            return result;
    }

    /**
     * Initializes the symbols from the LocaleElements resource bundle.
     * Note: The organization of LocaleElements badly needs to be
     * cleaned up.
     */
    private void initialize( Locale locale ) {
        this.locale = locale;
        /* try the cache first */
        String[][] data = (String[][]) cachedLocaleData.get(locale);
        String[] numberElements;
        if (data == null) {  /* cache miss */
            data = new String[1][];
            ResourceBundle rb = ICULocaleData.getLocaleElements(locale);
            data[0] = rb.getStringArray("NumberElements");
            /* update cache */
            cachedLocaleData.put(locale, data);
        }
        numberElements = data[0];

	// {dlf} clean up below now that we have our own resource data
        decimalSeparator = numberElements[0].charAt(0);
        groupingSeparator = numberElements[1].charAt(0);
        // [NEW] Temporary hack to support old JDK 1.1 resources
        patternSeparator = numberElements[2].length() > 0 ?
            numberElements[2].charAt(0) : ';';
        // patternSeparator = numberElements[2].charAt(0);
        percent = numberElements[3].charAt(0);
        zeroDigit = numberElements[4].charAt(0); //different for Arabic,etc.
        digit = numberElements[5].charAt(0);
        minusSign = numberElements[6].charAt(0);
        
        // [NEW] Temporary hack to support JDK versions before 1.1.6 (?)
        exponentSeparator = numberElements.length >= 9 ?
            numberElements[7] : DecimalFormat.PATTERN_EXPONENT;
        perMill = numberElements.length >= 9 ?
            numberElements[8].charAt(0) : '\u2030';
        infinity  = numberElements.length >= 10 ?
            numberElements[9] : "\u221e";
        NaN = numberElements.length >= 11 ?
            numberElements[10] : "\ufffd";
        
        // [NEW] Temporarily hard code; retrieve from resource later
        plusSign  = DecimalFormat.PATTERN_PLUS_SIGN;
        padEscape = DecimalFormat.PATTERN_PAD_ESCAPE;

        // Obtain currency data from the currency API.  This is strictly
        // for backward compatibility; we don't use DecimalFormatSymbols
        // for currency data anymore.
        String currname = null;
        Currency curr = Currency.getInstance(locale);
        if (curr != null) {
            intlCurrencySymbol = curr.getCurrencyCode();
            boolean[] isChoiceFormat = new boolean[1];
            currname = curr.getName(locale,
                                    Currency.SYMBOL_NAME,
                                    isChoiceFormat);
            // If this is a ChoiceFormat currency, then format an
            // arbitrary value; pick something != 1; more common.
            currencySymbol = isChoiceFormat[0]
                ? new ChoiceFormat(currname).format(2.0)
                : currname;
        } else {
            intlCurrencySymbol = "XXX";
            currencySymbol = "\u00A4"; // 'OX' currency symbol
        }
        // If there is a currency decimal, use it.
        monetarySeparator =
            numberElements[numberElements.length >= 12 ? 11 : 0].charAt(0);
    }

    /**
     * Read the default serializable fields, then if <code>serialVersionOnStream</code>
     * is less than 1, initialize <code>monetarySeparator</code> to be
     * the same as <code>decimalSeparator</code> and <code>exponential</code>
     * to be 'E'.
     * Finally, set serialVersionOnStream back to the maximum allowed value so that
     * default serialization will work properly if this object is streamed out again.
     *
     */
    private void readObject(ObjectInputStream stream)
        throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        if (serialVersionOnStream < 1) {
            // Didn't have monetarySeparator or exponential field;
            // use defaults.
            monetarySeparator = decimalSeparator;
            exponential = 'E';
        }
        if (serialVersionOnStream < 2) { // [NEW]
            padEscape = DecimalFormat.PATTERN_PAD_ESCAPE;
            plusSign = DecimalFormat.PATTERN_PLUS_SIGN;
            exponentSeparator = String.valueOf(exponential);
            // Although we read the exponential field on stream to create the
            // exponentSeparator, we don't do the reverse, since scientific
            // notation isn't supported by the old classes, even though the
            // symbol is there.
        }
        if (serialVersionOnStream < 3) {
            // Resurrected objects from old streams will have no
            // locale.  There is no 100% fix for this.  A
            // 90% fix is to construct a mapping of data back to
            // locale, perhaps a hash of all our members.  This is
            // expensive and doesn't seem worth it.
            locale = Locale.getDefault();
        }
        serialVersionOnStream = currentSerialVersion;
    }

    /**
     * Character used for zero.
     *
     * @serial
     * @see #getZeroDigit
     */
    private  char    zeroDigit;

    /**
     * Character used for thousands separator.
     *
     * @serial
     * @see #getGroupingSeparator
     */
    private  char    groupingSeparator;

    /**
     * Character used for decimal sign.
     *
     * @serial
     * @see #getDecimalSeparator
     */
    private  char    decimalSeparator;

    /**
     * Character used for mille percent sign.
     *
     * @serial
     * @see #getPerMill
     */
    private  char    perMill;

    /**
     * Character used for percent sign.
     * @serial
     * @see #getPercent
     */
    private  char    percent;

    /**
     * Character used for a digit in a pattern.
     *
     * @serial
     * @see #getDigit
     */
    private  char    digit;

    /**
     * Character used to separate positive and negative subpatterns
     * in a pattern.
     *
     * @serial
     * @see #getPatternSeparator
     */
    private  char    patternSeparator;

    /**
     * Character used to represent infinity.
     * @serial
     * @see #getInfinity
     */
    private  String  infinity;

    /**
     * Character used to represent NaN.
     * @serial
     * @see #getNaN
     */
    private  String  NaN;

    /**
     * Character used to represent minus sign.
     * @serial
     * @see #getMinusSign
     */
    private  char    minusSign;

    /**
     * String denoting the local currency, e.g. "$".
     * @serial
     * @see #getCurrencySymbol
     */
    private  String  currencySymbol;

    /**
     * International string denoting the local currency, e.g. "USD".
     * @serial
     * @see #getInternationalCurrencySymbol
     */
    private  String  intlCurrencySymbol;

    /**
     * The decimal separator used when formatting currency values.
     * @serial
     * @see #getMonetaryDecimalSeparator
     */
    private  char    monetarySeparator; // Field new in JDK 1.1.6

    /**
     * The character used to distinguish the exponent in a number formatted
     * in exponential notation, e.g. 'E' for a number such as "1.23E45".
     * <p>
     * Note that this field has been superseded by <code>exponentSeparator</code>.
     * It is retained for backward compatibility.
     *
     * @serial
     */
    private  char    exponential;       // Field new in JDK 1.1.6

    /**
     * The string used to separate the mantissa from the exponent.
     * Examples: "x10^" for 1.23x10^4, "E" for 1.23E4.
     * <p>
     * Note that this supersedes the <code>exponential</code> field.
     *
     * @serial
     * @since AlphaWorks
     */
    private String exponentSeparator;

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * The character used to indicate a padding character in a format,
     * e.g., '*' in a pattern such as "$*_#,##0.00".
     * @serial
     * @since AlphaWorks
     */
    private char padEscape;

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * The character used to indicate a plus sign.
     * @serial
     * @since AlphaWorks
     */
    private char plusSign;

    /**
     * The locale for which this object was constructed.  Set to the
     * default locale for objects resurrected from old streams.
     * @since ICU 2.2
     */
    private Locale locale;

    // Proclaim JDK 1.1 FCS compatibility
    static final long serialVersionUID = 5772796243397350300L;

    // The internal serial version which says which version was written
    // - 0 (default) for version up to JDK 1.1.5
    // - 1 for version from JDK 1.1.6, which includes two new fields:
    //     monetarySeparator and exponential.
    // - 2 for version from AlphaWorks, which includes 3 new fields:
    //     padEscape, exponentSeparator, and plusSign.
    // - 3 for ICU 2.2, which includes the locale field
    private static final int currentSerialVersion = 3;
    
    /**
     * Describes the version of <code>DecimalFormatSymbols</code> present on the stream.
     * Possible values are:
     * <ul>
     * <li><b>0</b> (or uninitialized): versions prior to JDK 1.1.6.
     *
     * <li><b>1</b>: Versions written by JDK 1.1.6 or later, which includes
     *      two new fields: <code>monetarySeparator</code> and <code>exponential</code>.
     * <li><b>2</b>: Version for AlphaWorks.  Adds padEscape, exponentSeparator,
     *      and plusSign.
     * </ul>
     * When streaming out a <code>DecimalFormatSymbols</code>, the most recent format
     * (corresponding to the highest allowable <code>serialVersionOnStream</code>)
     * is always written.
     *
     * @serial
     */
    private int serialVersionOnStream = currentSerialVersion;

    /**
     * cache to hold the NumberElements of a Locale.
     */
    private static final Hashtable cachedLocaleData = new Hashtable(3);
}
