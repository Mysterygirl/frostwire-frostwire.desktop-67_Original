/*
 *******************************************************************************
 * Copyright (C) 1996-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/RuleBasedNumberFormat.java,v $ 
 * $Date: 2003/06/03 18:49:35 $ 
 * $Revision: 1.16 $
 *
 *****************************************************************************************
 */

package com.ibm.icu.text;

import com.ibm.icu.impl.ICULocaleData;
import com.ibm.icu.impl.UCharacterProperty;

import java.math.BigInteger;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Locale;
import java.util.ResourceBundle;


/**
 * <p>A class that formats numbers according to a set of rules. This number formatter is
 * typically used for spelling out numeric values in words (e.g., 25,3476 as
 * &quot;twenty-five thousand three hundred seventy-six&quot; or &quot;vingt-cinq mille trois
 * cents soixante-seize&quot; or
 * &quot;funfundzwanzigtausenddreihundertsechsundsiebzig&quot;), but can also be used for
 * other complicated formatting tasks, such as formatting a number of seconds as hours,
 * minutes and seconds (e.g., 3,730 as &quot;1:02:10&quot;).</p>
 *
 * <p>The resources contain three predefined formatters for each locale: spellout, which
 * spells out a value in words (123 is &quot;one hundred twenty-three&quot;); ordinal, which
 * appends an ordinal suffix to the end of a numeral (123 is &quot;123rd&quot;); and
 * duration, which shows a duration in seconds as hours, minutes, and seconds (123 is
 * &quot;2:03&quot;).&nbsp; The client can also define more specialized <tt>RuleBasedNumberFormat</tt>s
 * by supplying programmer-defined rule sets.</p>
 *
 * <p>The behavior of a <tt>RuleBasedNumberFormat</tt> is specified by a textual description
 * that is either passed to the constructor as a <tt>String</tt> or loaded from a resource
 * bundle. In its simplest form, the description consists of a semicolon-delimited list of <em>rules.</em>
 * Each rule has a string of output text and a value or range of values it is applicable to.
 * In a typical spellout rule set, the first twenty rules are the words for the numbers from
 * 0 to 19:</p>
 *
 * <pre>zero; one; two; three; four; five; six; seven; eight; nine;
 * ten; eleven; twelve; thirteen; fourteen; fifteen; sixteen; seventeen; eighteen; nineteen;</pre>
 *
 * <p>For larger numbers, we can use the preceding set of rules to format the ones place, and
 * we only have to supply the words for the multiples of 10:</p>
 *
 * <pre>20: twenty[-&gt;&gt;];
 * 30: thirty{-&gt;&gt;];
 * 40: forty[-&gt;&gt;];
 * 50: fifty[-&gt;&gt;];
 * 60: sixty[-&gt;&gt;];
 * 70: seventy[-&gt;&gt;];
 * 80: eighty[-&gt;&gt;];
 * 90: ninety[-&gt;&gt;];</pre>
 *
 * <p>In these rules, the <em>base value</em> is spelled out explicitly and set off from the
 * rule's output text with a colon. The rules are in a sorted list, and a rule is applicable
 * to all numbers from its own base value to one less than the next rule's base value. The
 * &quot;&gt;&gt;&quot; token is called a <em>substitution</em> and tells the fomatter to
 * isolate the number's ones digit, format it using this same set of rules, and place the
 * result at the position of the &quot;&gt;&gt;&quot; token. Text in brackets is omitted if
 * the number being formatted is an even multiple of 10 (the hyphen is a literal hyphen; 24
 * is &quot;twenty-four,&quot; not &quot;twenty four&quot;).</p>
 *
 * <p>For even larger numbers, we can actually look up several parts of the number in the
 * list:</p>
 *
 * <pre>100: &lt;&lt; hundred[ &gt;&gt;];</pre>
 *
 * <p>The &quot;&lt;&lt;&quot; represents a new kind of substitution. The &lt;&lt; isolates
 * the hundreds digit (and any digits to its left), formats it using this same rule set, and
 * places the result where the &quot;&lt;&lt;&quot; was. Notice also that the meaning of
 * &gt;&gt; has changed: it now refers to both the tens and the ones digits. The meaning of
 * both substitutions depends on the rule's base value. The base value determines the rule's <em>divisor,</em>
 * which is the highest power of 10 that is less than or equal to the base value (the user
 * can change this). To fill in the substitutions, the formatter divides the number being
 * formatted by the divisor. The integral quotient is used to fill in the &lt;&lt;
 * substitution, and the remainder is used to fill in the &gt;&gt; substitution. The meaning
 * of the brackets changes similarly: text in brackets is omitted if the value being
 * formatted is an even multiple of the rule's divisor. The rules are applied recursively, so
 * if a substitution is filled in with text that includes another substitution, that
 * substitution is also filled in.</p>
 *
 * <p>This rule covers values up to 999, at which point we add another rule:</p>
 *
 * <pre>1000: &lt;&lt; thousand[ &gt;&gt;];</pre>
 *
 * <p>Again, the meanings of the brackets and substitution tokens shift because the rule's
 * base value is a higher power of 10, changing the rule's divisor. This rule can actually be
 * used all the way up to 999,999. This allows us to finish out the rules as follows:</p>
 *
 * <pre>1,000,000: &lt;&lt; million[ &gt;&gt;];
 * 1,000,000,000: &lt;&lt; billion[ &gt;&gt;];
 * 1,000,000,000,000: &lt;&lt; trillion[ &gt;&gt;];
 * 1,000,000,000,000,000: OUT OF RANGE!;</pre>
 *
 * <p>Commas, periods, and spaces can be used in the base values to improve legibility and
 * are ignored by the rule parser. The last rule in the list is customarily treated as an
 * &quot;overflow rule,&quot; applying to everything from its base value on up, and often (as
 * in this example) being used to print out an error message or default representation.
 * Notice also that the size of the major groupings in large numbers is controlled by the
 * spacing of the rules: because in English we group numbers by thousand, the higher rules
 * are separated from each other by a factor of 1,000.</p>
 *
 * <p>To see how these rules actually work in practice, consider the following example:
 * Formatting 25,430 with this rule set would work like this:</p>
 *
 * <table border="0" width="630">
 *   <tr>
 *     <td width="21"></td>
 *     <td width="257" valign="top"><strong>&lt;&lt; thousand &gt;&gt;</strong></td>
 *     <td width="340" valign="top">[the rule whose base value is 1,000 is applicable to 25,340]</td>
 *   </tr>
 *   <tr>
 *     <td width="21"></td>
 *     <td width="257" valign="top"><strong>twenty-&gt;&gt;</strong> thousand &gt;&gt;</td>
 *     <td width="340" valign="top">[25,340 over 1,000 is 25. The rule for 20 applies.]</td>
 *   </tr>
 *   <tr>
 *     <td width="21"></td>
 *     <td width="257" valign="top">twenty-<strong>five</strong> thousand &gt;&gt;</td>
 *     <td width="340" valign="top">[25 mod 10 is 5. The rule for 5 is &quot;five.&quot;</td>
 *   </tr>
 *   <tr>
 *     <td width="21"></td>
 *     <td width="257" valign="top">twenty-five thousand <strong>&lt;&lt; hundred &gt;&gt;</strong></td>
 *     <td width="340" valign="top">[25,340 mod 1,000 is 340. The rule for 100 applies.]</td>
 *   </tr>
 *   <tr>
 *     <td width="21"></td>
 *     <td width="257" valign="top">twenty-five thousand <strong>three</strong> hundred &gt;&gt;</td>
 *     <td width="340" valign="top">[340 over 100 is 3. The rule for 3 is &quot;three.&quot;]</td>
 *   </tr>
 *   <tr>
 *     <td width="21"></td>
 *     <td width="257" valign="top">twenty-five thousand three hundred <strong>forty</strong></td>
 *     <td width="340" valign="top">[340 mod 100 is 40. The rule for 40 applies. Since 40 divides
 *     evenly by 10, the hyphen and substitution in the brackets are omitted.]</td>
 *   </tr>
 * </table>
 *
 * <p>The above syntax suffices only to format positive integers. To format negative numbers,
 * we add a special rule:</p>
 *
 * <pre>-x: minus &gt;&gt;;</pre>
 *
 * <p>This is called a <em>negative-number rule,</em> and is identified by &quot;-x&quot;
 * where the base value would be. This rule is used to format all negative numbers. the
 * &gt;&gt; token here means &quot;find the number's absolute value, format it with these
 * rules, and put the result here.&quot;</p>
 *
 * <p>We also add a special rule called a <em>fraction rule </em>for numbers with fractional
 * parts:</p>
 *
 * <pre>x.x: &lt;&lt; point &gt;&gt;;</pre>
 *
 * <p>This rule is used for all positive non-integers (negative non-integers pass through the
 * negative-number rule first and then through this rule). Here, the &lt;&lt; token refers to
 * the number's integral part, and the &gt;&gt; to the number's fractional part. The
 * fractional part is formatted as a series of single-digit numbers (e.g., 123.456 would be
 * formatted as &quot;one hundred twenty-three point four five six&quot;).</p>
 *
 * <p>To see how this rule syntax is applied to various languages, examine the resource data.</p>
 *
 * <p>There is actually much more flexibility built into the rule language than the
 * description above shows. A formatter may own multiple rule sets, which can be selected by
 * the caller, and which can use each other to fill in their substitutions. Substitutions can
 * also be filled in with digits, using a DecimalFormat object. There is syntax that can be
 * used to alter a rule's divisor in various ways. And there is provision for much more
 * flexible fraction handling. A complete description of the rule syntax follows:</p>
 *
 * <hr>
 *
 * <p>The description of a <tt>RuleBasedNumberFormat</tt>'s behavior consists of one or more <em>rule
 * sets.</em> Each rule set consists of a name, a colon, and a list of <em>rules.</em> A rule
 * set name must begin with a % sign. Rule sets with names that begin with a single % sign
 * are <em>public:</em> the caller can specify that they be used to format and parse numbers.
 * Rule sets with names that begin with %% are <em>private:</em> they exist only for the use
 * of other rule sets. If a formatter only has one rule set, the name may be omitted.</p>
 *
 * <p>The user can also specify a special &quot;rule set&quot; named <tt>%%lenient-parse</tt>.
 * The body of <tt>%%lenient-parse</tt> isn't a set of number-formatting rules, but a <tt>RuleBasedCollator</tt>
 * description which is used to define equivalences for lenient parsing. For more information
 * on the syntax, see <tt>RuleBasedCollator</tt>. For more information on lenient parsing,
 * see <tt>setLenientParse()</tt>. <em>Note:</em> symbols that have syntactic meaning
 * in collation rules, such as '&amp;', have no particular meaning when appearing outside
 * of the <tt>lenient-parse</tt> rule set.</p>
 *
 * <p>The body of a rule set consists of an ordered, semicolon-delimited list of <em>rules.</em>
 * Internally, every rule has a base value, a divisor, rule text, and zero, one, or two <em>substitutions.</em>
 * These parameters are controlled by the description syntax, which consists of a <em>rule
 * descriptor,</em> a colon, and a <em>rule body.</em></p>
 *
 * <p>A rule descriptor can take one of the following forms (text in <em>italics</em> is the
 * name of a token):</p>
 *
 * <table border="0" width="100%">
 *   <tr>
 *     <td width="5%" valign="top"></td>
 *     <td width="8%" valign="top"><em>bv</em>:</td>
 *     <td valign="top"><em>bv</em> specifies the rule's base value. <em>bv</em> is a decimal
 *     number expressed using ASCII digits. <em>bv</em> may contain spaces, period, and commas,
 *     which are irgnored. The rule's divisor is the highest power of 10 less than or equal to
 *     the base value.</td>
 *   </tr>
 *   <tr>
 *     <td width="5%" valign="top"></td>
 *     <td width="8%" valign="top"><em>bv</em>/<em>rad</em>:</td>
 *     <td valign="top"><em>bv</em> specifies the rule's base value. The rule's divisor is the
 *     highest power of <em>rad</em> less than or equal to the base value.</td>
 *   </tr>
 *   <tr>
 *     <td width="5%" valign="top"></td>
 *     <td width="8%" valign="top"><em>bv</em>&gt;:</td>
 *     <td valign="top"><em>bv</em> specifies the rule's base value. To calculate the divisor,
 *     let the radix be 10, and the exponent be the highest exponent of the radix that yields a
 *     result less than or equal to the base value. Every &gt; character after the base value
 *     decreases the exponent by 1. If the exponent is positive or 0, the divisor is the radix
 *     raised to the power of the exponent; otherwise, the divisor is 1.</td>
 *   </tr>
 *   <tr>
 *     <td width="5%" valign="top"></td>
 *     <td width="8%" valign="top"><em>bv</em>/<em>rad</em>&gt;:</td>
 *     <td valign="top"><em>bv</em> specifies the rule's base value. To calculate the divisor,
 *     let the radix be <em>rad</em>, and the exponent be the highest exponent of the radix that
 *     yields a result less than or equal to the base value. Every &gt; character after the radix
 *     decreases the exponent by 1. If the exponent is positive or 0, the divisor is the radix
 *     raised to the power of the exponent; otherwise, the divisor is 1.</td>
 *   </tr>
 *   <tr>
 *     <td width="5%" valign="top"></td>
 *     <td width="8%" valign="top">-x:</td>
 *     <td valign="top">The rule is a negative-number rule.</td>
 *   </tr>
 *   <tr>
 *     <td width="5%" valign="top"></td>
 *     <td width="8%" valign="top">x.x:</td>
 *     <td valign="top">The rule is an <em>improper fraction rule.</em></td>
 *   </tr>
 *   <tr>
 *     <td width="5%" valign="top"></td>
 *     <td width="8%" valign="top">0.x:</td>
 *     <td valign="top">The rule is a <em>proper fraction rule.</em></td>
 *   </tr>
 *   <tr>
 *     <td width="5%" valign="top"></td>
 *     <td width="8%" valign="top">x.0:</td>
 *     <td valign="top">The rule is a <em>master rule.</em></td>
 *   </tr>
 *   <tr>
 *     <td width="5%" valign="top"></td>
 *     <td width="8%" valign="top"><em>nothing</em></td>
 *     <td valign="top">If the rule's rule descriptor is left out, the base value is one plus the
 *     preceding rule's base value (or zero if this is the first rule in the list) in a normal
 *     rule set.&nbsp; In a fraction rule set, the base value is the same as the preceding rule's
 *     base value.</td>
 *   </tr>
 * </table>
 *
 * <p>A rule set may be either a regular rule set or a <em>fraction rule set,</em> depending
 * on whether it is used to format a number's integral part (or the whole number) or a
 * number's fractional part. Using a rule set to format a rule's fractional part makes it a
 * fraction rule set.</p>
 *
 * <p>Which rule is used to format a number is defined according to one of the following
 * algorithms: If the rule set is a regular rule set, do the following:
 *
 * <ul>
 *   <li>If the rule set includes a master rule (and the number was passed in as a <tt>double</tt>),
 *     use the master rule.&nbsp; (If the number being formatted was passed in as a <tt>long</tt>,
 *     the master rule is ignored.)</li>
 *   <li>If the number is negative, use the negative-number rule.</li>
 *   <li>If the number has a fractional part and is greater than 1, use the improper fraction
 *     rule.</li>
 *   <li>If the number has a fractional part and is between 0 and 1, use the proper fraction
 *     rule.</li>
 *   <li>Binary-search the rule list for the rule with the highest base value less than or equal
 *     to the number. If that rule has two substitutions, its base value is not an even multiple
 *     of its divisor, and the number <em>is</em> an even multiple of the rule's divisor, use the
 *     rule that precedes it in the rule list. Otherwise, use the rule itself.</li>
 * </ul>
 *
 * <p>If the rule set is a fraction rule set, do the following:
 *
 * <ul>
 *   <li>Ignore negative-number and fraction rules.</li>
 *   <li>For each rule in the list, multiply the number being formatted (which will always be
 *     between 0 and 1) by the rule's base value. Keep track of the distance between the result
 *     the nearest integer.</li>
 *   <li>Use the rule that produced the result closest to zero in the above calculation. In the
 *     event of a tie or a direct hit, use the first matching rule encountered. (The idea here is
 *     to try each rule's base value as a possible denominator of a fraction. Whichever
 *     denominator produces the fraction closest in value to the number being formatted wins.) If
 *     the rule following the matching rule has the same base value, use it if the numerator of
 *     the fraction is anything other than 1; if the numerator is 1, use the original matching
 *     rule. (This is to allow singular and plural forms of the rule text without a lot of extra
 *     hassle.)</li>
 * </ul>
 *
 * <p>A rule's body consists of a string of characters terminated by a semicolon. The rule
 * may include zero, one, or two <em>substitution tokens,</em> and a range of text in
 * brackets. The brackets denote optional text (and may also include one or both
 * substitutions). The exact meanings of the substitution tokens, and under what conditions
 * optional text is omitted, depend on the syntax of the substitution token and the context.
 * The rest of the text in a rule body is literal text that is output when the rule matches
 * the number being formatted.</p>
 *
 * <p>A substitution token begins and ends with a <em>token character.</em> The token
 * character and the context together specify a mathematical operation to be performed on the
 * number being formatted. An optional <em>substitution descriptor </em>specifies how the
 * value resulting from that operation is used to fill in the substitution. The position of
 * the substitution token in the rule body specifies the location of the resultant text in
 * the original rule text.</p>
 *
 * <p>The meanings of the substitution token characters are as follows:</p>
 *
 * <table border="0" width="100%">
 *   <tr>
 *     <td width="37"></td>
 *     <td width="23">&gt;&gt;</td>
 *     <td width="165" valign="top">in normal rule</td>
 *     <td>Divide the number by the rule's divisor and format the remainder</td>
 *   </tr>
 *   <tr>
 *     <td width="37"></td>
 *     <td width="23"></td>
 *     <td width="165" valign="top">in negative-number rule</td>
 *     <td>Find the absolute value of the number and format the result</td>
 *   </tr>
 *   <tr>
 *     <td width="37"></td>
 *     <td width="23"></td>
 *     <td width="165" valign="top">in fraction or master rule</td>
 *     <td>Isolate the number's fractional part and format it.</td>
 *   </tr>
 *   <tr>
 *     <td width="37"></td>
 *     <td width="23"></td>
 *     <td width="165" valign="top">in rule in fraction rule set</td>
 *     <td>Not allowed.</td>
 *   </tr>
 *   <tr>
 *     <td width="37"></td>
 *     <td width="23">&gt;&gt;&gt;</td>
 *     <td width="165" valign="top">in normal rule</td>
 *     <td>Divide the number by the rule's divisor and format the remainder,
 *       but bypass the normal rule-selection process and just use the
 *       rule that precedes this one in this rule list.</td>
 *   </tr>
 *   <tr>
 *     <td width="37"></td>
 *     <td width="23"></td>
 *     <td width="165" valign="top">in all other rules</td>
 *     <td>Not allowed.</td>
 *   </tr>
 *   <tr>
 *     <td width="37"></td>
 *     <td width="23">&lt;&lt;</td>
 *     <td width="165" valign="top">in normal rule</td>
 *     <td>Divide the number by the rule's divisor and format the quotient</td>
 *   </tr>
 *   <tr>
 *     <td width="37"></td>
 *     <td width="23"></td>
 *     <td width="165" valign="top">in negative-number rule</td>
 *     <td>Not allowed.</td>
 *   </tr>
 *   <tr>
 *     <td width="37"></td>
 *     <td width="23"></td>
 *     <td width="165" valign="top">in fraction or master rule</td>
 *     <td>Isolate the number's integral part and format it.</td>
 *   </tr>
 *   <tr>
 *     <td width="37"></td>
 *     <td width="23"></td>
 *     <td width="165" valign="top">in rule in fraction rule set</td>
 *     <td>Multiply the number by the rule's base value and format the result.</td>
 *   </tr>
 *   <tr>
 *     <td width="37"></td>
 *     <td width="23">==</td>
 *     <td width="165" valign="top">in all rule sets</td>
 *     <td>Format the number unchanged</td>
 *   </tr>
 *   <tr>
 *     <td width="37"></td>
 *     <td width="23">[]</td>
 *     <td width="165" valign="top">in normal rule</td>
 *     <td>Omit the optional text if the number is an even multiple of the rule's divisor</td>
 *   </tr>
 *   <tr>
 *     <td width="37"></td>
 *     <td width="23"></td>
 *     <td width="165" valign="top">in negative-number rule</td>
 *     <td>Not allowed.</td>
 *   </tr>
 *   <tr>
 *     <td width="37"></td>
 *     <td width="23"></td>
 *     <td width="165" valign="top">in improper-fraction rule</td>
 *     <td>Omit the optional text if the number is between 0 and 1 (same as specifying both an
 *     x.x rule and a 0.x rule)</td>
 *   </tr>
 *   <tr>
 *     <td width="37"></td>
 *     <td width="23"></td>
 *     <td width="165" valign="top">in master rule</td>
 *     <td>Omit the optional text if the number is an integer (same as specifying both an x.x
 *     rule and an x.0 rule)</td>
 *   </tr>
 *   <tr>
 *     <td width="37"></td>
 *     <td width="23"></td>
 *     <td width="165" valign="top">in proper-fraction rule</td>
 *     <td>Not allowed.</td>
 *   </tr>
 *   <tr>
 *     <td width="37"></td>
 *     <td width="23"></td>
 *     <td width="165" valign="top">in rule in fraction rule set</td>
 *     <td>Omit the optional text if multiplying the number by the rule's base value yields 1.</td>
 *   </tr>
 * </table>
 *
 * <p>The substitution descriptor (i.e., the text between the token characters) may take one
 * of three forms:</p>
 *
 * <table border="0" width="100%">
 *   <tr>
 *     <td width="42"></td>
 *     <td width="166" valign="top">a rule set name</td>
 *     <td>Perform the mathematical operation on the number, and format the result using the
 *     named rule set.</td>
 *   </tr>
 *   <tr>
 *     <td width="42"></td>
 *     <td width="166" valign="top">a DecimalFormat pattern</td>
 *     <td>Perform the mathematical operation on the number, and format the result using a
 *     DecimalFormat with the specified pattern.&nbsp; The pattern must begin with 0 or #.</td>
 *   </tr>
 *   <tr>
 *     <td width="42"></td>
 *     <td width="166" valign="top">nothing</td>
 *     <td>Perform the mathematical operation on the number, and format the result using the rule
 *     set containing the current rule, except:<ul>
 *       <li>You can't have an empty substitution descriptor with a == substitution.</li>
 *       <li>If you omit the substitution descriptor in a &gt;&gt; substitution in a fraction rule,
 *         format the result one digit at a time using the rule set containing the current rule.</li>
 *       <li>If you omit the substitution descriptor in a &lt;&lt; substitution in a rule in a
 *         fraction rule set, format the result using the default rule set for this formatter.</li>
 *     </ul>
 *     </td>
 *   </tr>
 * </table>
 *
 * <p>Whitespace is ignored between a rule set name and a rule set body, between a rule
 * descriptor and a rule body, or between rules. If a rule body begins with an apostrophe,
 * the apostrophe is ignored, but all text after it becomes significant (this is how you can
 * have a rule's rule text begin with whitespace). There is no escape function: the semicolon
 * is not allowed in rule set names or in rule text, and the colon is not allowed in rule set
 * names. The characters beginning a substitution token are always treated as the beginning
 * of a substitution token.</p>
 *
 * <p>See the resource data and the demo program for annotated examples of real rule sets
 * using these features.</p>
 *
 * @author Richard Gillam
 * $RCSfile: RuleBasedNumberFormat.java,v $ $Revision: 1.16 $ $Date: 2003/06/03 18:49:35 $
 * @see NumberFormat
 * @see DecimalFormat
 * @stable ICU 2.0
 */
public final class RuleBasedNumberFormat extends NumberFormat {

    //-----------------------------------------------------------------------
    // constants
    //-----------------------------------------------------------------------

    /**
     * Puts a copyright in the .class file
     */
    private static final String copyrightNotice
        = "Copyright \u00a91997-1998 IBM Corp.  All rights reserved.";

    /**
     * Selector code that tells the constructor to create a spellout formatter
     * @stable ICU 2.0
     */
    public static final int SPELLOUT = 1;

    /**
     * Selector code that tells the constructor to create an ordinal formatter
     * @stable ICU 2.0
     */
    public static final int ORDINAL = 2;

    /**
     * Selector code that tells the constructor to create a duration formatter
     * @stable ICU 2.0
     */
    public static final int DURATION = 3;

    //-----------------------------------------------------------------------
    // data members
    //-----------------------------------------------------------------------

    /**
     * The formatter's rule sets.
     */
    private NFRuleSet[] ruleSets = null;

    /**
     * A pointer to the formatter's default rule set.  This is always included
     * in ruleSets.
     */
    private NFRuleSet defaultRuleSet = null;

    /**
     * The formatter's locale.  This is used to create DecimalFormatSymbols and
     * Collator objects.
     */
    private Locale locale = null;

    /**
     * Collator to be used in lenient parsing.  This variable is lazy-evaluated:
     * the collator is actually created the first time the client does a parse
     * with lenient-parse mode turned on.
     */
    private Collator collator = null;

    /**
     * The DecimalFormatSymbols object that any DecimalFormat objects this
     * formatter uses should use.  This variable is lazy-evaluated: it isn't
     * filled in if the rule set never uses a DecimalFormat pattern.
     */
    private DecimalFormatSymbols decimalFormatSymbols = null;

    /**
     * Flag specifying whether lenient parse mode is on or off.  Off by default.
     */
    private boolean lenientParse = false;

    /**
     * If the description specifies lenient-parse rules, they're stored here until
     * the collator is created.
     */
    private String lenientParseRules = null;

    //-----------------------------------------------------------------------
    // constructors
    //-----------------------------------------------------------------------

    /**
     * Creates a RuleBasedNumberFormat that behaves according to the description
     * passed in.  The formatter uses the default locale.
     * @param description A description of the formatter's desired behavior.
     * See the class documentation for a complete explanation of the description
     * syntax.
     * @stable ICU 2.0
     */
    public RuleBasedNumberFormat(String description) {
        locale = Locale.getDefault();
        init(description);
    }

    /**
     * Creates a RuleBasedNumberFormat that behaves according to the description
     * passed in.  The formatter uses the specified locale to determine the
     * characters to use when formatting in numerals, and to define equivalences
     * for lenient parsing.
     * @param description A description of the formatter's desired behavior.
     * See the class documentation for a complete explanation of the description
     * syntax.
     * @param locale A locale, which governs which characters are used for
     * formatting values in numerals, and which characters are equivalent in
     * lenient parsing.
     * @stable ICU 2.0
     */
    public RuleBasedNumberFormat(String description, Locale locale) {
        this.locale = locale;
        init(description);
    }

    /**
     * Creates a RuleBasedNumberFormat from a predefined description.  The selector
     * code choosed among three possible predefined formats: spellout, ordinal,
     * and duration.
     * @param locale The locale for the formatter.
     * @param format A selector code specifying which kind of formatter to create for that
     * locale.  There are three legal values: SPELLOUT, which creates a formatter that
     * spells out a value in words in the desired language, ORDINAL, which attaches
     * an ordinal suffix from the desired language to the end of a number (e.g. "123rd"),
     * and DURATION, which formats a duration in seconds as hours, minutes, and seconds.
     * @stable ICU 2.0
     */
    public RuleBasedNumberFormat(Locale locale, int format) {
        this.locale = locale;

        // load up the resource bundle containing the description
        // from the specified locale
	//        ResourceBundle bundle = ICULocaleData.getResourceBundle("NumberFormatRules", locale);
        ResourceBundle bundle = ICULocaleData.getResourceBundle("LocaleElements", locale);
        String description = "";

        // pick a description from the resource bundle based on the
        // kind of formatter the user asked for
        switch (format) {
            case SPELLOUT:
                description = bundle.getString("SpelloutRules");
                break;

            case ORDINAL:
                description = bundle.getString("OrdinalRules");
                break;

            case DURATION:
                description = bundle.getString("DurationRules");
                break;
        }

        // construct the formatter based on the description
        init(description);
    }

    /**
     * Creates a RuleBasedNumberFormat from a predefined description.  Uses the
     * default locale.
     * @param format A selector code specifying which kind of formatter to create.
     * There are three legal values: SPELLOUT, which creates a formatter that spells
     * out a value in words in the default locale's langyage, ORDINAL, which attaches
     * an ordinal suffix from the default locale's language to a numeral, and
     * DURATION, which formats a duration in seconds as hours, minutes, and seconds.
     * @stable ICU 2.0
     */
    public RuleBasedNumberFormat(int format) {
        this(Locale.getDefault(), format);
    }

    //-----------------------------------------------------------------------
    // boilerplate
    //-----------------------------------------------------------------------

    /**
     * Duplicates this formatter.
     * @return A RuleBasedNumberFormat that is equal to this one.
     * @stable ICU 2.0
     */
    public Object clone() {
        return super.clone();
    }

    /**
     * Tests two RuleBasedNumberFormats for equality.
     * @param that The formatter to compare against this one.
     * @return true if the two formatters have identical behavior.
     * @stable ICU 2.0
     */
    public boolean equals(Object that) {
        // if the other object isn't a RuleBasedNumberFormat, that's
        // all we need to know
        if (!(that instanceof RuleBasedNumberFormat)) {
            return false;
        } else {
            // cast the other object's pointer to a pointer to a
            // RuleBasedNumberFormat
            RuleBasedNumberFormat that2 = (RuleBasedNumberFormat)that;

            // compare their locales and lenient-parse modes
            if (!locale.equals(that2.locale) || lenientParse != that2.lenientParse) {
                return false;
            }

            // if that succeeds, then compare their rule set lists
            if (ruleSets.length != that2.ruleSets.length) {
                return false;
            }
            for (int i = 0; i < ruleSets.length; i++) {
                if (!ruleSets[i].equals(that2.ruleSets[i])) {
                    return false;
                }
            }

            return true;
        }
    }

    /**
     * Generates a textual description of this formatter.
     * @return a String containing a rule set that will produce a RuleBasedNumberFormat
     * with identical behavior to this one.  This won't necessarily be identical
     * to the rule set description that was originally passed in, but will produce
     * the same result.
     * @stable ICU 2.0
     */
    public String toString() {

        // accumulate the descriptions of all the rule sets in a
        // StringBuffer, then cast it to a String and return it
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < ruleSets.length; i++) {
            result.append(ruleSets[i].toString());
        }
        return result.toString();
    }

    /**
     * Writes this object to a stream.
     * @param out The stream to write to.
     */
    private void writeObject(java.io.ObjectOutputStream out)
                    throws java.io.IOException {
        // we just write the textual description to the stream, so we
        // have an implementation-independent streaming format
        out.writeUTF(this.toString());
    }

    /**
     * Reads this object in from a stream.
     * @param in The stream to read from.
     */
    private void readObject(java.io.ObjectInputStream in)
                    throws java.io.IOException {

        // read the description in from the stream
        String description = in.readUTF();

        // build a brand-new RuleBasedNumberFormat from the description,
        // then steal its substructure.  This object's substructure and
        // the temporary RuleBasedNumberFormat drop on the floor and
        // get swept up by the garbage collector
        RuleBasedNumberFormat temp = new RuleBasedNumberFormat(description);
        ruleSets = temp.ruleSets;
        defaultRuleSet = temp.defaultRuleSet;
    }


    //-----------------------------------------------------------------------
    // public API functions
    //-----------------------------------------------------------------------

    /**
     * Returns a list of the names of all of this formatter's public rule sets.
     * @return A list of the names of all of this formatter's public rule sets.
     * @stable ICU 2.0
     */
    public String[] getRuleSetNames() {
        // preflight the iteration, counting the number of public rule sets
        // (public rule sets have names that begin with % instead of %%)
        int count = 0;
        for (int i = 0; i < ruleSets.length; i++) {
            if (!ruleSets[i].getName().startsWith("%%")) {
                ++count;
            }
        }

        // then new up an array of the proper size and copy the names into it
        String[] result = new String[count];
        count = 0;
        for (int i = ruleSets.length - 1; i >= 0; i--) {
            if (!ruleSets[i].getName().startsWith("%%")) {
                result[count++] = ruleSets[i].getName();
            }
        }

        return result;
    }

    /**
     * Formats the specified number according to the specified rule set.
     * @param number The number to format.
     * @param ruleSet The name of the rule set to format the number with.
     * This must be the name of a valid public rule set for this formatter.
     * @return A textual representation of the number.
     * @stable ICU 2.0
     */
    public String format(double number, String ruleSet) throws IllegalArgumentException {
        if (ruleSet.startsWith("%%")) {
            throw new IllegalArgumentException("Can't use internal rule set");
        }
        return format(number, findRuleSet(ruleSet));
    }

    /**
     * Formats the specified number according to the specified rule set.
     * (If the specified rule set specifies a master ["x.0"] rule, this function
     * ignores it.  Convert the number to a double first if you ned it.)  This
     * function preserves all the precision in the long-- it doesn't convert it
     * to a double.
     * @param number The number to format.
     * @param ruleSet The name of the rule set to format the number with.
     * This must be the name of a valid public rule set for this formatter.
     * @return A textual representation of the number.
     * @stable ICU 2.0
     */
    public String format(long number, String ruleSet) throws IllegalArgumentException {
        if (ruleSet.startsWith("%%")) {
            throw new IllegalArgumentException("Can't use internal rule set");
        }
        return format(number, findRuleSet(ruleSet));
    }

    /**
     * Formats the specified number using the formatter's default rule set.
     * (The default rule set is the last public rule set defined in the description.)
     * @param number The number to format.
     * @param toAppendTo A StringBuffer that the result should be appended to.
     * @param ignore This function doesn't examine or update the field position.
     * @return toAppendTo
     * @stable ICU 2.0
     */
    public StringBuffer format(double number,
                               StringBuffer toAppendTo,
                               FieldPosition ignore) {
        // this is one of the inherited format() methods.  Since it doesn't
        // have a way to select the rule set to use, it just uses the
        // default one
        toAppendTo.append(format(number, defaultRuleSet));
        return toAppendTo;
    }

    /**
     * Formats the specified number using the formatter's default rule set.
     * (The default rule set is the last public rule set defined in the description.)
     * (If the specified rule set specifies a master ["x.0"] rule, this function
     * ignores it.  Convert the number to a double first if you ned it.)  This
     * function preserves all the precision in the long-- it doesn't convert it
     * to a double.
     * @param number The number to format.
     * @param toAppendTo A StringBuffer that the result should be appended to.
     * @param ignore This function doesn't examine or update the field position.
     * @return toAppendTo
     * @stable ICU 2.0
     */
    public StringBuffer format(long number,
                               StringBuffer toAppendTo,
                               FieldPosition ignore) {
        // this is one of the inherited format() methods.  Since it doesn't
        // have a way to select the rule set to use, it just uses the
        // default one
        toAppendTo.append(format(number, defaultRuleSet));
        return toAppendTo;
    }

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Implement com.ibm.icu.text.NumberFormat:
     * Format a BigInteger.
     * @stable ICU 2.0
     */
    public StringBuffer format(BigInteger number,
                               StringBuffer toAppendTo,
                               FieldPosition pos) {
        return format(new com.ibm.icu.math.BigDecimal(number), toAppendTo, pos);
    }
    
    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Implement com.ibm.icu.text.NumberFormat:
     * Format a BigDecimal.
     * @stable ICU 2.0
     */
    public StringBuffer format(java.math.BigDecimal number,
                               StringBuffer toAppendTo,
                               FieldPosition pos) {
        return format(new com.ibm.icu.math.BigDecimal(number), toAppendTo, pos);
    }

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Implement com.ibm.icu.text.NumberFormat:
     * Format a BigDecimal.
     * @stable ICU 2.0
     */
    public StringBuffer format(com.ibm.icu.math.BigDecimal number,
                               StringBuffer toAppendTo,
                               FieldPosition pos) {
        // TEMPORARY:
        return format(number.doubleValue(), toAppendTo, pos);
    }

    /**
     * Parses the specfied string, beginning at the specified position, according
     * to this formatter's rules.  This will match the string against all of the
     * formatter's public rule sets and return the value corresponding to the longest
     * parseable substring.  This function's behavior is affected by the lenient
     * parse mode.
     * @param text The string to parse
     * @param parsePosition On entry, contains the position of the first character
     * in "text" to examine.  On exit, has been updated to contain the position
     * of the first character in "text" that wasn't consumed by the parse.
     * @return The number that corresponds to the parsed text.  This will be an
     * instance of either Long or Double, depending on whether the result has a
     * fractional part.
     * @see #setLenientParseMode
     * @stable ICU 2.0
     */
    public Number parse(String text, ParsePosition parsePosition) {

        // parsePosition tells us where to start parsing.  We copy the
        // text in the string from here to the end inro a new string,
        // and create a new ParsePosition and result variable to use
        // for the duration of the parse operation
        String workingText = text.substring(parsePosition.getIndex());
        ParsePosition workingPos = new ParsePosition(0);
        Number tempResult = null;

        // keep track of the largest number of characters consumed in
        // the various trials, and the result that corresponds to it
        Number result = new Long(0);
        ParsePosition highWaterMark = new ParsePosition(workingPos.getIndex());

        // iterate over the public rule sets (beginning with the default one)
        // and try parsing the text with each of them.  Keep track of which
        // one consumes the most characters: that's the one that determines
        // the result we return
        for (int i = ruleSets.length - 1; i >= 0; i--) {
            // skip private rule sets
            if (ruleSets[i].getName().startsWith("%%")) {
                continue;
            }

            // try parsing the string with the rule set.  If it gets past the
            // high-water mark, update the high-water mark and the result
            tempResult = ruleSets[i].parse(workingText, workingPos, Double.MAX_VALUE);
            if (workingPos.getIndex() > highWaterMark.getIndex()) {
                result = tempResult;
                highWaterMark.setIndex(workingPos.getIndex());
            }
// commented out because this API on ParsePosition doesn't exist in 1.1.x
//            if (workingPos.getErrorIndex() > highWaterMark.getErrorIndex()) {
//                highWaterMark.setErrorIndex(workingPos.getErrorIndex());
//            }

            // if we manage to use up all the characters in the string,
            // we don't have to try any more rule sets
            if (highWaterMark.getIndex() == workingText.length()) {
                break;
            }

            // otherwise, reset our internal parse position to the
            // beginning and try again with the next rule set
            workingPos.setIndex(0);
        }

        // add the high water mark to our original parse position and
        // return the result
        parsePosition.setIndex(parsePosition.getIndex() + highWaterMark.getIndex());
// commented out because this API on ParsePosition doesn't exist in 1.1.x
//        if (highWaterMark.getIndex() == 0) {
//            parsePosition.setErrorIndex(parsePosition.getIndex() + highWaterMark.getErrorIndex());
//        }
        return result;
    }

    /**
     * Turns lenient parse mode on and off.
     *
     * When in lenient parse mode, the formatter uses a Collator for parsing the text.
     * Only primary differences are treated as significant.  This means that case
     * differences, accent differences, alternate spellings of the same letter
     * (e.g., ae and a-umlaut in German), ignorable characters, etc. are ignored in
     * matching the text.  In many cases, numerals will be accepted in place of words
     * or phrases as well.
     *
     * For example, all of the following will correctly parse as 255 in English in
     * lenient-parse mode:
     * <br>"two hundred fifty-five"
     * <br>"two hundred fifty five"
     * <br>"TWO HUNDRED FIFTY-FIVE"
     * <br>"twohundredfiftyfive"
     * <br>"2 hundred fifty-5"
     *
     * The Collator used is determined by the locale that was
     * passed to this object on construction.  The description passed to this object
     * on construction may supply additional collation rules that are appended to the
     * end of the default collator for the locale, enabling additional equivalences
     * (such as adding more ignorable characters or permitting spelled-out version of
     * symbols; see the demo program for examples).
     *
     * It's important to emphasize that even strict parsing is relatively lenient: it
     * will accept some text that it won't produce as output.  In English, for example,
     * it will correctly parse "two hundred zero" and "fifteen hundred".
     *
     * @param enabled If true, turns lenient-parse mode on; if false, turns it off.
     * @see java.text.RuleBasedCollator
     * @stable ICU 2.0
     */
    public void setLenientParseMode(boolean enabled) {
        lenientParse = enabled;

        // if we're leaving lenient-parse mode, throw away the collator
        // we've been using
        if (!enabled) {
            collator = null;
        }
    }

    /**
     * Returns true if lenient-parse mode is turned on.  Lenient parsing is off
     * by default.
     * @return true if lenient-parse mode is turned on.
     * @see #setLenientParseMode
     * @stable ICU 2.0
     */
    public boolean lenientParseEnabled() {
        return lenientParse;
    }

    /**
     * Override the default rule set to use.  If ruleSetName is null, reset
     * to the initial default rule set.
     * @param ruleSetName the name of the rule set, or null to reset the initial default.
     * @throws IllegalArgumentException if ruleSetName is not the name of a public ruleset.
     * @stable ICU 2.0
     */
    public void setDefaultRuleSet(String ruleSetName) {
        if (ruleSetName == null) {
            initDefaultRuleSet();
        } else if (ruleSetName.startsWith("%%")) {
            throw new IllegalArgumentException("cannot use private rule set: " + ruleSetName);
        } else {
            defaultRuleSet = findRuleSet(ruleSetName);
        }
    }

    //-----------------------------------------------------------------------
    // package-internal API
    //-----------------------------------------------------------------------

    /**
     * Returns a reference to the formatter's default rule set.  The default
     * rule set is the last public rule set in the description, or the one
     * most recently set by setDefaultRuleSet.
     * @return The formatter's default rule set.
     */
    NFRuleSet getDefaultRuleSet() {
        return defaultRuleSet;
    }

    /**
     * Returns the collator to use for lenient parsing.  The collator is lazily created:
     * this function creates it the first time it's called.
     * @return The collator to use for lenient parsing, or null if lenient parsing
     * is turned off.
     */
    Collator getCollator() {
        // lazy-evaulate the collator
        if (collator == null && lenientParse) {
            try {
                // create a default collator based on the formatter's locale,
                // then pull out that collator's rules, append any additional
                // rules specified in the description, and create a _new_
                // collator based on the combinaiton of those rules
                RuleBasedCollator temp = (RuleBasedCollator)Collator.getInstance(locale);
                String rules = temp.getRules() + lenientParseRules;

                collator = new RuleBasedCollator(rules);
                collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
            }
            catch (Exception e) {
                // If we get here, it means we have a malformed set of
                // collation rules, which hopefully won't happen
                e.printStackTrace();
                collator = null;
            }
        }

        // if lenient-parse mode is off, this will be null
        // (see setLenientParseMode())
        return collator;
    }

    /**
     * Returns the DecimalFormatSymbols object that should be used by all DecimalFormat
     * instances owned by this formatter.  This object is lazily created: this function
     * creates it the first time it's called.
     * @return The DecimalFormatSymbols object that should be used by all DecimalFormat
     * instances owned by this formatter.
     */
    DecimalFormatSymbols getDecimalFormatSymbols() {
        // lazy-evaluate the DecimalFormatSymbols object.  This object
        // is shared by all DecimalFormat instances belonging to this
        // formatter
        if (decimalFormatSymbols == null) {
            decimalFormatSymbols = new DecimalFormatSymbols(locale);
        }
        return decimalFormatSymbols;
    }

    //-----------------------------------------------------------------------
    // construction implementation
    //-----------------------------------------------------------------------

    /**
     * This function parses the description and uses it to build all of
     * internal data structures that the formatter uses to do formatting
     * @param description The description of the formatter's desired behavior.
     * This is either passed in by the caller or loaded out of a resource
     * by one of the constructors, and is in the description format specified
     * in the class docs.
     */
    private void init(String description) {
        // start by stripping the trailing whitespace from all the rules
        // (this is all the whitespace follwing each semicolon in the
        // description).  This allows us to look for rule-set boundaries
        // by searching for ";%" without having to worry about whitespace
        // between the ; and the %
        description = stripWhitespace(description);

        // check to see if there's a set of lenient-parse rules.  If there
        // is, pull them out into our temporary holding place for them,
        // and delete them from the description before the real desciption-
        // parsing code sees them
        int lp = description.indexOf("%%lenient-parse:");
        if (lp != -1) {
            // we've got to make sure we're not in the middle of a rule
            // (where "%%lenient-parse" would actually get treated as
            // rule text)
            if (lp == 0 || description.charAt(lp - 1) == ';') {
                // locate the beginning and end of the actual collation
                // rules (there may be whitespace between the name and
                // the first token in the description)
                int lpEnd = description.indexOf(";%", lp);

                if (lpEnd == -1) {
                    lpEnd = description.length() - 1;
                }
                int lpStart = lp + "%%lenient-parse:".length();
                while (UCharacterProperty.isRuleWhiteSpace(description.charAt(lpStart))) {
                    ++lpStart;
                }

                // copy out the lenient-parse rules and delete them
                // from the description
                lenientParseRules = description.substring(lpStart, lpEnd);

                StringBuffer temp = new StringBuffer(description.substring(0, lp));
                if (lpEnd + 1 < description.length()) {
                    temp.append(description.substring(lpEnd + 1));
                }
                description = temp.toString();
            }
        }

        // pre-flight parsing the description and count the number of
        // rule sets (";%" marks the end of one rule set and the beginning
        // of the next)
        int numRuleSets = 0;
        for (int p = description.indexOf(";%"); p != -1; p = description.indexOf(";%", p)) {
            ++numRuleSets;
            ++p;
        }
        ++numRuleSets;

        // our rule list is an array of the apprpriate size
        ruleSets = new NFRuleSet[numRuleSets];

        // divide up the descriptions into individual rule-set descriptions
        // and store them in a temporary array.  At each step, we also
        // new up a rule set, but all this does is initialize its name
        // and remove it from its description.  We can't actually parse
        // the rest of the descriptions and finish initializing everything
        // because we have to know the names and locations of all the rule
        // sets before we can actually set everything up
        String[] ruleSetDescriptions = new String[numRuleSets];

        int curRuleSet = 0;
        int start = 0;
        for (int p = description.indexOf(";%"); p != -1; p = description.indexOf(";%", start)) {
            ruleSetDescriptions[curRuleSet] = description.substring(start, p + 1);
            ruleSets[curRuleSet] = new NFRuleSet(ruleSetDescriptions, curRuleSet);
            ++curRuleSet;
            start = p + 1;
        }
        ruleSetDescriptions[curRuleSet] = description.substring(start);
        ruleSets[curRuleSet] = new NFRuleSet(ruleSetDescriptions, curRuleSet);

        // now we can take note of the formatter's default rule set, which
        // is the last public rule set in the description (it's the last
        // rather than the first so that a user can create a new formatter
        // from an existing formatter and change its default bevhaior just
        // by appending more rule sets to the end)
        initDefaultRuleSet();

        // finally, we can go back through the temporary descriptions
        // list and finish seting up the substructure (and we throw
        // away the temporary descriptions as we go)
        for (int i = 0; i < ruleSets.length; i++) {
            ruleSets[i].parseRules(ruleSetDescriptions[i], this);
            ruleSetDescriptions[i] = null;
        }
    }

    /**
     * This function is used by init() to strip whitespace between rules (i.e.,
     * after semicolons).
     * @param description The formatter description
     * @return The description with all the whitespace that follows semicolons
     * taken out.
     */
    private String stripWhitespace(String description) {
        // since we don't have a method that deletes characters (why?!!)
        // create a new StringBuffer to copy the text into
        StringBuffer result = new StringBuffer();

        // iterate through the characters...
        int start = 0;
        while (start != -1 && start < description.length()) {
            // seek to the first non-whitespace character...
            while (start < description.length()
                   && UCharacterProperty.isRuleWhiteSpace(description.charAt(start))) {
                ++start;
            }

            //if the first non-whitespace character is semicolon, skip it and continue
            if (start < description.length() && description.charAt(start) == ';') {
                start += 1;
                continue;
            }

            // locate the next semicolon in the text and copy the text from
            // our current position up to that semicolon into the result
            int p;
            p = description.indexOf(';', start);
            if (p == -1) {
                // or if we don't find a semicolon, just copy the rest of
                // the string into the result
                result.append(description.substring(start));
                start = -1;
            }
            else if (p < description.length()) {
                result.append(description.substring(start, p + 1));
                start = p + 1;
            }

            // when we get here, we've seeked off the end of the sring, and
            // we terminate the loop (we continue until *start* is -1 rather
            // than until *p* is -1, because otherwise we'd miss the last
            // rule in the description)
            else {
                start = -1;
            }
        }
        return result.toString();
    }

    /**
     * This function is called ONLY DURING CONSTRUCTION to fill in the
     * defaultRuleSet variable once we've set up all the rule sets.
     * The default rule set is the last public rule set in the description.
     * (It's the last rather than the first so that a caller can append
     * text to the end of an existing formatter description to change its
     * behavior.)
     */
    private void initDefaultRuleSet() {
        // seek backward from the end of the list until we reach a rule set
        // whose name DOESN'T begin with %%.  That's the default rule set
        for (int i = ruleSets.length - 1; i >= 0; --i) {
            if (!ruleSets[i].getName().startsWith("%%")) {
                defaultRuleSet = ruleSets[i];
                return;
            }
        }
        defaultRuleSet = ruleSets[ruleSets.length - 1];
    }

    //-----------------------------------------------------------------------
    // formatting implementation
    //-----------------------------------------------------------------------

    /**
     * Bottleneck through which all the public format() methods
     * that take a double pass. By the time we get here, we know
     * which rule set we're using to do the formatting.
     * @param number The number to format
     * @param ruleSet The rule set to use to format the number
     * @return The text that resulted from formatting the number
     */
    String format(double number, NFRuleSet ruleSet) {
        // all API format() routines that take a double vector through
        // here.  Create an empty string buffer where the result will
        // be built, and pass it to the rule set (along with an insertion
        // position of 0 and the number being formatted) to the rule set
        // for formatting
        StringBuffer result = new StringBuffer();
        ruleSet.format(number, result, 0);
        return result.toString();
    }

    /**
     * Bottleneck through which all the public format() methods
     * that take a long pass. By the time we get here, we know
     * which rule set we're using to do the formatting.
     * @param number The number to format
     * @param ruleSet The rule set to use to format the number
     * @return The text that resulted from formatting the number
     */
    String format(long number, NFRuleSet ruleSet) {
        // all API format() routines that take a double vector through
        // here.  We have these two identical functions-- one taking a
        // double and one taking a long-- the couple digits of precision
        // that long has but double doesn't (both types are 8 bytes long,
        // but double has to borrow some of the mantissa bits to hold
        // the exponent).
        // Create an empty string buffer where the result will
        // be built, and pass it to the rule set (along with an insertion
        // position of 0 and the number being formatted) to the rule set
        // for formatting
        StringBuffer result = new StringBuffer();
        ruleSet.format(number, result, 0);
        return result.toString();
    }

    /**
     * Returns the named rule set.  Throws an IllegalArgumentException
     * if this formatter doesn't have a rule set with that name.
     * @param name The name of the desired rule set
     * @return The rule set with that name
     */
    NFRuleSet findRuleSet(String name) throws IllegalArgumentException {
        for (int i = 0; i < ruleSets.length; i++) {
            if (ruleSets[i].getName().equals(name)) {
                return ruleSets[i];
            }
        }
        throw new IllegalArgumentException("No rule set named " + name);
    }
}

