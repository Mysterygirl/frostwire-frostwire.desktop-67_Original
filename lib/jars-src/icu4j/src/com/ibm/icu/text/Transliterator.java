/*
 *******************************************************************************
 * Copyright (C) 1996-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/Transliterator.java,v $
 * $Date: 2003/06/03 18:49:35 $
 * $Revision: 1.91 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.text;

import com.ibm.icu.impl.ICULocaleData;
import com.ibm.icu.impl.data.ResourceReader;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.impl.UtilityExtensions;
import com.ibm.icu.util.CaseInsensitiveString;
import com.ibm.icu.impl.UCharacterProperty;

import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.util.Vector;

/**
 * <code>Transliterator</code> is an abstract class that
 * transliterates text from one format to another.  The most common
 * kind of transliterator is a script, or alphabet, transliterator.
 * For example, a Russian to Latin transliterator changes Russian text
 * written in Cyrillic characters to phonetically equivalent Latin
 * characters.  It does not <em>translate</em> Russian to English!
 * Transliteration, unlike translation, operates on characters, without
 * reference to the meanings of words and sentences.
 *
 * <p>Although script conversion is its most common use, a
 * transliterator can actually perform a more general class of tasks.
 * In fact, <code>Transliterator</code> defines a very general API
 * which specifies only that a segment of the input text is replaced
 * by new text.  The particulars of this conversion are determined
 * entirely by subclasses of <code>Transliterator</code>.
 *
 * <p><b>Transliterators are stateless</b>
 *
 * <p><code>Transliterator</code> objects are <em>stateless</em>; they
 * retain no information between calls to
 * <code>transliterate()</code>.  As a result, threads may share
 * transliterators without synchronizing them.  This might seem to
 * limit the complexity of the transliteration operation.  In
 * practice, subclasses perform complex transliterations by delaying
 * the replacement of text until it is known that no other
 * replacements are possible.  In other words, although the
 * <code>Transliterator</code> objects are stateless, the source text
 * itself embodies all the needed information, and delayed operation
 * allows arbitrary complexity.
 *
 * <p><b>Batch transliteration</b>
 *
 * <p>The simplest way to perform transliteration is all at once, on a
 * string of existing text.  This is referred to as <em>batch</em>
 * transliteration.  For example, given a string <code>input</code>
 * and a transliterator <code>t</code>, the call
 *
 * <blockquote><code>String result = t.transliterate(input);
 * </code></blockquote>
 *
 * will transliterate it and return the result.  Other methods allow
 * the client to specify a substring to be transliterated and to use
 * {@link Replaceable} objects instead of strings, in order to
 * preserve out-of-band information (such as text styles).
 *
 * <p><b>Keyboard transliteration</b>
 *
 * <p>Somewhat more involved is <em>keyboard</em>, or incremental
 * transliteration.  This is the transliteration of text that is
 * arriving from some source (typically the user's keyboard) one
 * character at a time, or in some other piecemeal fashion.
 *
 * <p>In keyboard transliteration, a <code>Replaceable</code> buffer
 * stores the text.  As text is inserted, as much as possible is
 * transliterated on the fly.  This means a GUI that displays the
 * contents of the buffer may show text being modified as each new
 * character arrives.
 *
 * <p>Consider the simple <code>RuleBasedTransliterator</code>:
 *
 * <blockquote><code>
 * th&gt;{theta}<br>
 * t&gt;{tau}
 * </code></blockquote>
 *
 * When the user types 't', nothing will happen, since the
 * transliterator is waiting to see if the next character is 'h'.  To
 * remedy this, we introduce the notion of a cursor, marked by a '|'
 * in the output string:
 *
 * <blockquote><code>
 * t&gt;|{tau}<br>
 * {tau}h&gt;{theta}
 * </code></blockquote>
 *
 * Now when the user types 't', tau appears, and if the next character
 * is 'h', the tau changes to a theta.  This is accomplished by
 * maintaining a cursor position (independent of the insertion point,
 * and invisible in the GUI) across calls to
 * <code>transliterate()</code>.  Typically, the cursor will
 * be coincident with the insertion point, but in a case like the one
 * above, it will precede the insertion point.
 *
 * <p>Keyboard transliteration methods maintain a set of three indices
 * that are updated with each call to
 * <code>transliterate()</code>, including the cursor, start,
 * and limit.  These indices are changed by the method, and they are
 * passed in and out via a Position object. The <code>start</code> index
 * marks the beginning of the substring that the transliterator will
 * look at.  It is advanced as text becomes committed (but it is not
 * the committed index; that's the <code>cursor</code>).  The
 * <code>cursor</code> index, described above, marks the point at
 * which the transliterator last stopped, either because it reached
 * the end, or because it required more characters to disambiguate
 * between possible inputs.  The <code>cursor</code> can also be
 * explicitly set by rules in a <code>RuleBasedTransliterator</code>.
 * Any characters before the <code>cursor</code> index are frozen;
 * future keyboard transliteration calls within this input sequence
 * will not change them.  New text is inserted at the
 * <code>limit</code> index, which marks the end of the substring that
 * the transliterator looks at.
 *
 * <p>Because keyboard transliteration assumes that more characters
 * are to arrive, it is conservative in its operation.  It only
 * transliterates when it can do so unambiguously.  Otherwise it waits
 * for more characters to arrive.  When the client code knows that no
 * more characters are forthcoming, perhaps because the user has
 * performed some input termination operation, then it should call
 * <code>finishTransliteration()</code> to complete any
 * pending transliterations.
 *
 * <p><b>Inverses</b>
 *
 * <p>Pairs of transliterators may be inverses of one another.  For
 * example, if transliterator <b>A</b> transliterates characters by
 * incrementing their Unicode value (so "abc" -> "def"), and
 * transliterator <b>B</b> decrements character values, then <b>A</b>
 * is an inverse of <b>B</b> and vice versa.  If we compose <b>A</b>
 * with <b>B</b> in a compound transliterator, the result is the
 * indentity transliterator, that is, a transliterator that does not
 * change its input text.
 *
 * The <code>Transliterator</code> method <code>getInverse()</code>
 * returns a transliterator's inverse, if one exists, or
 * <code>null</code> otherwise.  However, the result of
 * <code>getInverse()</code> usually will <em>not</em> be a true
 * mathematical inverse.  This is because true inverse transliterators
 * are difficult to formulate.  For example, consider two
 * transliterators: <b>AB</b>, which transliterates the character 'A'
 * to 'B', and <b>BA</b>, which transliterates 'B' to 'A'.  It might
 * seem that these are exact inverses, since
 *
 * <blockquote>"A" x <b>AB</b> -> "B"<br>
 * "B" x <b>BA</b> -> "A"</blockquote>
 *
 * where 'x' represents transliteration.  However,
 *
 * <blockquote>"ABCD" x <b>AB</b> -> "BBCD"<br>
 * "BBCD" x <b>BA</b> -> "AACD"</blockquote>
 *
 * so <b>AB</b> composed with <b>BA</b> is not the
 * identity. Nonetheless, <b>BA</b> may be usefully considered to be
 * <b>AB</b>'s inverse, and it is on this basis that
 * <b>AB</b><code>.getInverse()</code> could legitimately return
 * <b>BA</b>.
 *
 * <p><b>IDs and display names</b>
 *
 * <p>A transliterator is designated by a short identifier string or
 * <em>ID</em>.  IDs follow the format <em>source-destination</em>,
 * where <em>source</em> describes the entity being replaced, and
 * <em>destination</em> describes the entity replacing
 * <em>source</em>.  The entities may be the names of scripts,
 * particular sequences of characters, or whatever else it is that the
 * transliterator converts to or from.  For example, a transliterator
 * from Russian to Latin might be named "Russian-Latin".  A
 * transliterator from keyboard escape sequences to Latin-1 characters
 * might be named "KeyboardEscape-Latin1".  By convention, system
 * entity names are in English, with the initial letters of words
 * capitalized; user entity names may follow any format so long as
 * they do not contain dashes.
 *
 * <p>In addition to programmatic IDs, transliterator objects have
 * display names for presentation in user interfaces, returned by
 * {@link #getDisplayName}.
 *
 * <p><b>Factory methods and registration</b>
 *
 * <p>In general, client code should use the factory method
 * <code>getInstance()</code> to obtain an instance of a
 * transliterator given its ID.  Valid IDs may be enumerated using
 * <code>getAvailableIDs()</code>.  Since transliterators are
 * stateless, multiple calls to <code>getInstance()</code> with the
 * same ID will return the same object.
 *
 * <p>In addition to the system transliterators registered at startup,
 * user transliterators may be registered by calling
 * <code>registerInstance()</code> at run time.  To register a
 * transliterator subclass without instantiating it (until it is
 * needed), users may call <code>registerClass()</code>.
 *
 * <p><b>Composed transliterators</b>
 *
 * <p>In addition to built-in system transliterators like
 * "Latin-Greek", there are also built-in <em>composed</em>
 * transliterators.  These are implemented by composing two or more
 * component transliterators.  For example, if we have scripts "A",
 * "B", "C", and "D", and we want to transliterate between all pairs
 * of them, then we need to write 12 transliterators: "A-B", "A-C",
 * "A-D", "B-A",..., "D-A", "D-B", "D-C".  If it is possible to
 * convert all scripts to an intermediate script "M", then instead of
 * writing 12 rule sets, we only need to write 8: "A~M", "B~M", "C~M",
 * "D~M", "M~A", "M~B", "M~C", "M~D".  (This might not seem like a big
 * win, but it's really 2<em>n</em> vs. <em>n</em><sup>2</sup> -
 * <em>n</em>, so as <em>n</em> gets larger the gain becomes
 * significant.  With 9 scripts, it's 18 vs. 72 rule sets, a big
 * difference.)  Note the use of "~" rather than "-" for the script
 * separator here; this indicates that the given transliterator is
 * intended to be composed with others, rather than be used as is.
 *
 * <p>Composed transliterators can be instantiated as usual.  For
 * example, the system transliterator "Devanagari-Gujarati" is a
 * composed transliterator built internally as
 * "Devanagari~InterIndic;InterIndic~Gujarati".  When this
 * transliterator is instantiated, it appears externally to be a
 * standard transliterator (e.g., getID() returns
 * "Devanagari-Gujarati").
 *
 * <p><b>Subclassing</b>
 *
 * <p>Subclasses must implement the abstract method
 * <code>handleTransliterate()</code>.  <p>Subclasses should override
 * the <code>transliterate()</code> method taking a
 * <code>Replaceable</code> and the <code>transliterate()</code>
 * method taking a <code>String</code> and <code>StringBuffer</code>
 * if the performance of these methods can be improved over the
 * performance obtained by the default implementations in this class.
 *
 * <p>Copyright &copy; IBM Corporation 1999.  All rights reserved.
 *
 * @author Alan Liu
 * @stable ICU 2.0
 */
public abstract class Transliterator {
    /**
     * Direction constant indicating the forward direction in a transliterator,
     * e.g., the forward rules of a RuleBasedTransliterator.  An "A-B"
     * transliterator transliterates A to B when operating in the forward
     * direction, and B to A when operating in the reverse direction.
     * @see RuleBasedTransliterator
     * @see CompoundTransliterator
     * @stable ICU 2.0
     */
    public static final int FORWARD = 0;

    /**
     * Direction constant indicating the reverse direction in a transliterator,
     * e.g., the reverse rules of a RuleBasedTransliterator.  An "A-B"
     * transliterator transliterates A to B when operating in the forward
     * direction, and B to A when operating in the reverse direction.
     * @see RuleBasedTransliterator
     * @see CompoundTransliterator
     * @stable ICU 2.0
     */
    public static final int REVERSE = 1;

    /**
     * Position structure for incremental transliteration.  This data
     * structure defines two substrings of the text being
     * transliterated.  The first region, [contextStart,
     * contextLimit), defines what characters the transliterator will
     * read as context.  The second region, [start, limit), defines
     * what characters will actually be transliterated.  The second
     * region should be a subset of the first.
     *
     * <p>After a transliteration operation, some of the indices in this
     * structure will be modified.  See the field descriptions for
     * details.
     *
     * <p>contextStart <= start <= limit <= contextLimit
     *
     * <p>Note: All index values in this structure must be at code point
     * boundaries.  That is, none of them may occur between two code units
     * of a surrogate pair.  If any index does split a surrogate pair,
     * results are unspecified.
     * @stable ICU 2.0
     */
    public static class Position {

        /**
         * Beginning index, inclusive, of the context to be considered for
         * a transliteration operation.  The transliterator will ignore
         * anything before this index.  INPUT/OUTPUT parameter: This parameter
         * is updated by a transliteration operation to reflect the maximum
         * amount of antecontext needed by a transliterator.
         * @stable ICU 2.0
         */
        public int contextStart;

        /**
         * Ending index, exclusive, of the context to be considered for a
         * transliteration operation.  The transliterator will ignore
         * anything at or after this index.  INPUT/OUTPUT parameter: This
         * parameter is updated to reflect changes in the length of the
         * text, but points to the same logical position in the text.
         * @stable ICU 2.0
         */
        public int contextLimit;

        /**
         * Beginning index, inclusive, of the text to be transliteratd.
         * INPUT/OUTPUT parameter: This parameter is advanced past
         * characters that have already been transliterated by a
         * transliteration operation.
         * @stable ICU 2.0
         */
        public int start;

        /**
         * Ending index, exclusive, of the text to be transliteratd.
         * INPUT/OUTPUT parameter: This parameter is updated to reflect
         * changes in the length of the text, but points to the same
         * logical position in the text.
         * @stable ICU 2.0
         */
        public int limit;

        /**
         * Constructs a Position object with start, limit,
         * contextStart, and contextLimit all equal to zero.
         * @stable ICU 2.0
         */
        public Position() {
            this(0, 0, 0, 0);
        }

        /**
         * Constructs a Position object with the given start,
         * contextStart, and contextLimit.  The limit is set to the
         * contextLimit.
         * @stable ICU 2.0
         */
        public Position(int contextStart, int contextLimit, int start) {
            this(contextStart, contextLimit, start, contextLimit);
        }

        /**
         * Constructs a Position object with the given start, limit,
         * contextStart, and contextLimit.
         * @stable ICU 2.0
         */
        public Position(int contextStart, int contextLimit,
                        int start, int limit) {
            this.contextStart = contextStart;
            this.contextLimit = contextLimit;
            this.start = start;
            this.limit = limit;
        }

        /**
         * Constructs a Position object that is a copy of another.
         * @draft ICU 2.6
         */
        public Position(Position pos) {
            set(pos);
        }

        /**
         * Copies the indices of this position from another.
         * @draft ICU 2.6
         */
        public void set(Position pos) {
            contextStart = pos.contextStart;
            contextLimit = pos.contextLimit;
            start = pos.start;
            limit = pos.limit;
        }

        /**
         * Returns true if this Position is equal to the given object.
         * @draft ICU 2.6
         */
        public boolean equals(Object obj) {
            if (obj instanceof Position) {
                Position pos = (Position) obj;
                return contextStart == pos.contextStart &&
                    contextLimit == pos.contextLimit &&
                    start == pos.start &&
                    limit == pos.limit;
            }
            return false;
        }

        /**
         * Returns a string representation of this Position.
         * @draft ICU 2.6
         */
        public String toString() {
            return "[cs=" + contextStart
    			+ ", s=" + start
    			+ ", l=" + limit
    			+ ", cl=" + contextLimit
    			+ "]";
        }

        /**
         * Check all bounds.  If they are invalid, throw an exception.
         * @param length the length of the string this object applies to
         * @exception IllegalArgumentException if any indices are out
         * of bounds
         * @stable ICU 2.0
         */
        public final void validate(int length) {
            if (contextStart < 0 ||
                start < contextStart ||
                limit < start ||
                contextLimit < limit ||
                length < contextLimit) {
                throw new IllegalArgumentException("Invalid Position {cs=" +
                                                   contextStart + ", s=" +
                                                   start + ", l=" +
                                                   limit + ", cl=" +
                                                   contextLimit + "}, len=" +
                                                   length);
            }
        }
    }

    /**
     * Programmatic name, e.g., "Latin-Arabic".
     */
    private String ID;

    /**
     * This transliterator's filter.  Any character for which
     * <tt>filter.contains()</tt> returns <tt>false</tt> will not be
     * altered by this transliterator.  If <tt>filter</tt> is
     * <tt>null</tt> then no filtering is applied.
     */
    private UnicodeFilter filter;

    private int maximumContextLength = 0;

    /**
     * System transliterator registry.
     */
    private static TransliteratorRegistry registry;

    private static Hashtable displayNameCache;

    /**
     * Prefix for resource bundle key for the display name for a
     * transliterator.  The ID is appended to this to form the key.
     * The resource bundle value should be a String.
     */
    private static final String RB_DISPLAY_NAME_PREFIX = "%Translit%%";

    /**
     * Prefix for resource bundle key for the display name for a
     * transliterator SCRIPT.  The ID is appended to this to form the key.
     * The resource bundle value should be a String.
     */
    private static final String RB_SCRIPT_DISPLAY_NAME_PREFIX = "%Translit%";

    /**
     * Resource bundle key for display name pattern.
     * The resource bundle value should be a String forming a
     * MessageFormat pattern, e.g.:
     * "{0,choice,0#|1#{1} Transliterator|2#{1} to {2} Transliterator}".
     */
    private static final String RB_DISPLAY_NAME_PATTERN = "TransliteratorNamePattern";

    /**
     * Delimiter between elements in a compound ID.
     * @deprecated ICU 2.4 To be made package private after 2003-12-01.
     */
    protected static final char ID_DELIM = ';';

    /**
     * Delimiter before target in an ID.
     * @deprecated ICU 2.4 To be made package private after 2003-12-01.
     */
    protected static final char ID_SEP = '-';

    /**
     * Delimiter before variant in an ID.
     * @deprecated ICU 2.4 To be made package private after 2003-12-01.
     */
    protected static final char VARIANT_SEP = '/';

    /**
     * To enable debugging output in the Transliterator component, set
     * DEBUG to true.
     *
     * N.B. Make sure to recompile all of the com.ibm.icu.text package
     * after changing this.  Easiest way to do this is 'ant clean
     * core' ('ant' will NOT pick up the dependency automatically).
     *
     * <<This generates a lot of output.>>
     */
    static final boolean DEBUG = false;

    private static final String COPYRIGHT =
        "\u00A9 IBM Corporation 1999. All rights reserved.";

    /**
     * Default constructor.
     * @param ID the string identifier for this transliterator
     * @param filter the filter.  Any character for which
     * <tt>filter.contains()</tt> returns <tt>false</tt> will not be
     * altered by this transliterator.  If <tt>filter</tt> is
     * <tt>null</tt> then no filtering is applied.
     * @stable ICU 2.0
     */
    protected Transliterator(String ID, UnicodeFilter filter) {
        if (ID == null) {
            throw new NullPointerException();
        }
        this.ID = ID;
        this.filter = filter;
    }

    /**
     * Transliterates a segment of a string, with optional filtering.
     *
     * @param text the string to be transliterated
     * @param start the beginning index, inclusive; <code>0 <= start
     * <= limit</code>.
     * @param limit the ending index, exclusive; <code>start <= limit
     * <= text.length()</code>.
     * @return The new limit index.  The text previously occupying <code>[start,
     * limit)</code> has been transliterated, possibly to a string of a different
     * length, at <code>[start, </code><em>new-limit</em><code>)</code>, where
     * <em>new-limit</em> is the return value. If the input offsets are out of bounds,
     * the returned value is -1 and the input string remains unchanged.
     * @stable ICU 2.0
     */
    public final int transliterate(Replaceable text, int start, int limit) {
        if (start < 0 ||
            limit < start ||
            text.length() < limit) {
            return -1;
        }

        Position pos = new Position(start, limit, start);
        filteredTransliterate(text, pos, false, true);
        return pos.limit;
    }

    /**
     * Transliterates an entire string in place. Convenience method.
     * @param text the string to be transliterated
     * @stable ICU 2.0
     */
    public final void transliterate(Replaceable text) {
        transliterate(text, 0, text.length());
    }

    /**
     * Transliterate an entire string and returns the result. Convenience method.
     *
     * @param text the string to be transliterated
     * @return The transliterated text
     * @stable ICU 2.0
     */
    public final String transliterate(String text) {
        ReplaceableString result = new ReplaceableString(text);
        transliterate(result);
        return result.toString();
    }

    /**
     * Transliterates the portion of the text buffer that can be
     * transliterated unambiguosly after new text has been inserted,
     * typically as a result of a keyboard event.  The new text in
     * <code>insertion</code> will be inserted into <code>text</code>
     * at <code>index.contextLimit</code>, advancing
     * <code>index.contextLimit</code> by <code>insertion.length()</code>.
     * Then the transliterator will try to transliterate characters of
     * <code>text</code> between <code>index.start</code> and
     * <code>index.contextLimit</code>.  Characters before
     * <code>index.start</code> will not be changed.
     *
     * <p>Upon return, values in <code>index</code> will be updated.
     * <code>index.contextStart</code> will be advanced to the first
     * character that future calls to this method will read.
     * <code>index.start</code> and <code>index.contextLimit</code> will
     * be adjusted to delimit the range of text that future calls to
     * this method may change.
     *
     * <p>Typical usage of this method begins with an initial call
     * with <code>index.contextStart</code> and <code>index.contextLimit</code>
     * set to indicate the portion of <code>text</code> to be
     * transliterated, and <code>index.start == index.contextStart</code>.
     * Thereafter, <code>index</code> can be used without
     * modification in future calls, provided that all changes to
     * <code>text</code> are made via this method.
     *
     * <p>This method assumes that future calls may be made that will
     * insert new text into the buffer.  As a result, it only performs
     * unambiguous transliterations.  After the last call to this
     * method, there may be untransliterated text that is waiting for
     * more input to resolve an ambiguity.  In order to perform these
     * pending transliterations, clients should call {@link
     * #finishTransliteration} after the last call to this
     * method has been made.
     *
     * @param text the buffer holding transliterated and untransliterated text
     * @param index the start and limit of the text, the position
     * of the cursor, and the start and limit of transliteration.
     * @param insertion text to be inserted and possibly
     * transliterated into the translation buffer at
     * <code>index.contextLimit</code>.  If <code>null</code> then no text
     * is inserted.
     * @see #handleTransliterate
     * @exception IllegalArgumentException if <code>index</code>
     * is invalid
     * @stable ICU 2.0
     */
    public final void transliterate(Replaceable text, Position index,
                                    String insertion) {
        index.validate(text.length());

//        int originalStart = index.contextStart;
        if (insertion != null) {
            text.replace(index.limit, index.limit, insertion);
            index.limit += insertion.length();
            index.contextLimit += insertion.length();
        }

        if (index.limit > 0 &&
            UTF16.isLeadSurrogate(text.charAt(index.limit - 1))) {
            // Oops, there is a dangling lead surrogate in the buffer.
            // This will break most transliterators, since they will
            // assume it is part of a pair.  Don't transliterate until
            // more text comes in.
            return;
        }

        filteredTransliterate(text, index, true, true);

// TODO
// This doesn't work once we add quantifier support.  Need to rewrite
// this code to support quantifiers and 'use maximum backup <n>;'.
//
//        index.contextStart = Math.max(index.start - getMaximumContextLength(),
//                                      originalStart);
    }

    /**
     * Transliterates the portion of the text buffer that can be
     * transliterated unambiguosly after a new character has been
     * inserted, typically as a result of a keyboard event.  This is a
     * convenience method; see {@link #transliterate(Replaceable,
     * Transliterator.Position, String)} for details.
     * @param text the buffer holding transliterated and
     * untransliterated text
     * @param index the start and limit of the text, the position
     * of the cursor, and the start and limit of transliteration.
     * @param insertion text to be inserted and possibly
     * transliterated into the translation buffer at
     * <code>index.contextLimit</code>.
     * @see #transliterate(Replaceable, Transliterator.Position, String)
     * @stable ICU 2.0
     */
    public final void transliterate(Replaceable text, Position index,
                                    int insertion) {
        transliterate(text, index, UTF16.valueOf(insertion));
    }

    /**
     * Transliterates the portion of the text buffer that can be
     * transliterated unambiguosly.  This is a convenience method; see
     * {@link #transliterate(Replaceable, Transliterator.Position,
     * String)} for details.
     * @param text the buffer holding transliterated and
     * untransliterated text
     * @param index the start and limit of the text, the position
     * of the cursor, and the start and limit of transliteration.
     * @see #transliterate(Replaceable, Transliterator.Position, String)
     * @stable ICU 2.0
     */
    public final void transliterate(Replaceable text, Position index) {
        transliterate(text, index, null);
    }

    /**
     * Finishes any pending transliterations that were waiting for
     * more characters.  Clients should call this method as the last
     * call after a sequence of one or more calls to
     * <code>transliterate()</code>.
     * @param text the buffer holding transliterated and
     * untransliterated text.
     * @param index the array of indices previously passed to {@link
     * #transliterate}
     * @stable ICU 2.0
     */
    public final void finishTransliteration(Replaceable text,
                                            Position index) {
        index.validate(text.length());
        filteredTransliterate(text, index, false, true);
    }

    /**
     * Abstract method that concrete subclasses define to implement
     * their transliteration algorithm.  This method handles both
     * incremental and non-incremental transliteration.  Let
     * <code>originalStart</code> refer to the value of
     * <code>pos.start</code> upon entry.
     *
     * <ul>
     *  <li>If <code>incremental</code> is false, then this method
     *  should transliterate all characters between
     *  <code>pos.start</code> and <code>pos.limit</code>. Upon return
     *  <code>pos.start</code> must == <code> pos.limit</code>.</li>
     *
     *  <li>If <code>incremental</code> is true, then this method
     *  should transliterate all characters between
     *  <code>pos.start</code> and <code>pos.limit</code> that can be
     *  unambiguously transliterated, regardless of future insertions
     *  of text at <code>pos.limit</code>.  Upon return,
     *  <code>pos.start</code> should be in the range
     *  [<code>originalStart</code>, <code>pos.limit</code>).
     *  <code>pos.start</code> should be positioned such that
     *  characters [<code>originalStart</code>, <code>
     *  pos.start</code>) will not be changed in the future by this
     *  transliterator and characters [<code>pos.start</code>,
     *  <code>pos.limit</code>) are unchanged.</li>
     * </ul>
     *
     * <p>Implementations of this method should also obey the
     * following invariants:</p>
     *
     * <ul>
     *  <li> <code>pos.limit</code> and <code>pos.contextLimit</code>
     *  should be updated to reflect changes in length of the text
     *  between <code>pos.start</code> and <code>pos.limit</code>. The
     *  difference <code> pos.contextLimit - pos.limit</code> should
     *  not change.</li>
     *
     *  <li><code>pos.contextStart</code> should not change.</li>
     *
     *  <li>Upon return, neither <code>pos.start</code> nor
     *  <code>pos.limit</code> should be less than
     *  <code>originalStart</code>.</li>
     *
     *  <li>Text before <code>originalStart</code> and text after
     *  <code>pos.limit</code> should not change.</li>
     *
     *  <li>Text before <code>pos.contextStart</code> and text after
     *  <code> pos.contextLimit</code> should be ignored.</li>
     * </ul>
     *
     * <p>Subclasses may safely assume that all characters in
     * [<code>pos.start</code>, <code>pos.limit</code>) are filtered.
     * In other words, the filter has already been applied by the time
     * this method is called.  See
     * <code>filteredTransliterate()</code>.
     *
     * <p>This method is <b>not</b> for public consumption.  Calling
     * this method directly will transliterate
     * [<code>pos.start</code>, <code>pos.limit</code>) without
     * applying the filter. End user code should call <code>
     * transliterate()</code> instead of this method. Subclass code
     * should call <code>filteredTransliterate()</code> instead of
     * this method.<p>
     *
     * @param text the buffer holding transliterated and
     * untransliterated text
     *
     * @param pos the indices indicating the start, limit, context
     * start, and context limit of the text.
     *
     * @param incremental if true, assume more text may be inserted at
     * <code>pos.limit</code> and act accordingly.  Otherwise,
     * transliterate all text between <code>pos.start</code> and
     * <code>pos.limit</code> and move <code>pos.start</code> up to
     * <code>pos.limit</code>.
     *
     * @see #transliterate
     * @stable ICU 2.0
     */
    protected abstract void handleTransliterate(Replaceable text,
                                                Position pos, boolean incremental);

    /**
     * Top-level transliteration method, handling filtering, incremental and
     * non-incremental transliteration, and rollback.  All transliteration
     * public API methods eventually call this method with a rollback argument
     * of TRUE.  Other entities may call this method but rollback should be
     * FALSE.
     *
     * <p>If this transliterator has a filter, break up the input text into runs
     * of unfiltered characters.  Pass each run to
     * <subclass>.handleTransliterate().
     *
     * <p>In incremental mode, if rollback is TRUE, perform a special
     * incremental procedure in which several passes are made over the input
     * text, adding one character at a time, and committing successful
     * transliterations as they occur.  Unsuccessful transliterations are rolled
     * back and retried with additional characters to give correct results.
     *
     * @param text the text to be transliterated
     * @param index the position indices
     * @param incremental if TRUE, then assume more characters may be inserted
     * at index.limit, and postpone processing to accomodate future incoming
     * characters
     * @param rollback if TRUE and if incremental is TRUE, then perform special
     * incremental processing, as described above, and undo partial
     * transliterations where necessary.  If incremental is FALSE then this
     * parameter is ignored.
     */
    private void filteredTransliterate(Replaceable text,
                                       Position index,
                                       boolean incremental,
                                       boolean rollback) {
        // Short circuit path for transliterators with no filter in
        // non-incremental mode.
        if (filter == null && !rollback) {
            handleTransliterate(text, index, incremental);
            return;
        }

        //----------------------------------------------------------------------
        // This method processes text in two groupings:
        //
        // RUNS -- A run is a contiguous group of characters which are contained
        // in the filter for this transliterator (filter.contains(ch) == true).
        // Text outside of runs may appear as context but it is not modified.
        // The start and limit Position values are narrowed to each run.
        //
        // PASSES (incremental only) -- To make incremental mode work correctly,
        // each run is broken up into n passes, where n is the length (in code
        // points) of the run.  Each pass contains the first n characters.  If a
        // pass is completely transliterated, it is committed, and further passes
        // include characters after the committed text.  If a pass is blocked,
        // and does not transliterate completely, then this method rolls back
        // the changes made during the pass, extends the pass by one code point,
        // and tries again.
        //----------------------------------------------------------------------

        // globalLimit is the limit value for the entire operation.  We
        // set index.limit to the end of each unfiltered run before
        // calling handleTransliterate(), so we need to maintain the real
        // value of index.limit here.  After each transliteration, we
        // update globalLimit for insertions or deletions that have
        // happened.
        int globalLimit = index.limit;

        // If there is a non-null filter, then break the input text up.  Say the
        // input text has the form:
        //   xxxabcxxdefxx
        // where 'x' represents a filtered character (filter.contains('x') ==
        // false).  Then we break this up into:
        //   xxxabc xxdef xx
        // Each pass through the loop consumes a run of filtered
        // characters (which are ignored) and a subsequent run of
        // unfiltered characters (which are transliterated).

        StringBuffer log = null;
        if (DEBUG) {
            log = new StringBuffer();
        }

        for (;;) {

            if (filter != null) {
                // Narrow the range to be transliterated to the first run
                // of unfiltered characters at or after index.start.

                // Advance past filtered chars
                int c;
                while (index.start < globalLimit &&
                       !filter.contains(c=text.char32At(index.start))) {
                    index.start += UTF16.getCharCount(c);
                }

                // Find the end of this run of unfiltered chars
                index.limit = index.start;
                while (index.limit < globalLimit &&
                       filter.contains(c=text.char32At(index.limit))) {
                    index.limit += UTF16.getCharCount(c);
                }
            }

            // Check to see if the unfiltered run is empty.  This only
            // happens at the end of the string when all the remaining
            // characters are filtered.
            if (index.start == index.limit) {
                break;
            }

            // Is this run incremental?  If there is additional
            // filtered text (if limit < globalLimit) then we pass in
            // an incremental value of FALSE to force the subclass to
            // complete the transliteration for this run.
            boolean isIncrementalRun =
                (index.limit < globalLimit ? false : incremental);

            int delta;

            // Implement rollback.  To understand the need for rollback,
            // consider the following transliterator:
            //
            //  "t" is "a > A;"
            //  "u" is "A > b;"
            //  "v" is a compound of "t; NFD; u" with a filter [:Ll:]
            //
            // Now apply "v" to the input text "a".  The result is "b".  But if
            // the transliteration is done incrementally, then the NFD holds
            // things up after "t" has already transformed "a" to "A".  When
            // finishTransliterate() is called, "A" is _not_ processed because
            // it gets excluded by the [:Ll:] filter, and the end result is "A"
            // -- incorrect.  The problem is that the filter is applied to a
            // partially-transliterated result, when we only want it to apply to
            // input text.  Although this example describes a compound
            // transliterator containing NFD and a specific filter, it can
            // happen with any transliterator which does a partial
            // transformation in incremental mode into characters outside its
            // filter.
            //
            // To handle this, when in incremental mode we supply characters to
            // handleTransliterate() in several passes.  Each pass adds one more
            // input character to the input text.  That is, for input "ABCD", we
            // first try "A", then "AB", then "ABC", and finally "ABCD".  If at
            // any point we block (upon return, start < limit) then we roll
            // back.  If at any point we complete the run (upon return start ==
            // limit) then we commit that run.

            if (rollback && isIncrementalRun) {

                if (DEBUG) {
                    log.setLength(0);
                    System.out.println("filteredTransliterate{"+getID()+"}i: IN=" +
                                       UtilityExtensions.formatInput(text, index));
                }

                int runStart = index.start;
                int runLimit = index.limit;
                int runLength =  runLimit - runStart;

                // Make a rollback copy at the end of the string
                int rollbackOrigin = text.length();
                text.copy(runStart, runLimit, rollbackOrigin);

                // Variables reflecting the commitment of completely
                // transliterated text.  passStart is the runStart, advanced
                // past committed text.  rollbackStart is the rollbackOrigin,
                // advanced past rollback text that corresponds to committed
                // text.
                int passStart = runStart;
                int rollbackStart = rollbackOrigin;

                // The limit for each pass; we advance by one code point with
                // each iteration.
                int passLimit = index.start;

                // Total length, in 16-bit code units, of uncommitted text.
                // This is the length to be rolled back.
                int uncommittedLength = 0;

                // Total delta (change in length) for all passes
                int totalDelta = 0;

                // PASS MAIN LOOP -- Start with a single character, and extend
                // the text by one character at a time.  Roll back partial
                // transliterations and commit complete transliterations.
                for (;;) {
                    // Length of additional code point, either one or two
                    int charLength =
                        UTF16.getCharCount(text.char32At(passLimit));
                    passLimit += charLength;
                    if (passLimit > runLimit) {
                        break;
                    }
                    uncommittedLength += charLength;

                    index.limit = passLimit;

                    if (DEBUG) {
                        log.setLength(0);
                        log.append("filteredTransliterate{"+getID()+"}i: ");
                        UtilityExtensions.formatInput(log, text, index);
                    }

                    // Delegate to subclass for actual transliteration.  Upon
                    // return, start will be updated to point after the
                    // transliterated text, and limit and contextLimit will be
                    // adjusted for length changes.
                    handleTransliterate(text, index, true);

                    if (DEBUG) {
                        log.append(" => ");
                        UtilityExtensions.formatInput(log, text, index);
                    }

                    delta = index.limit - passLimit; // change in length

                    // We failed to completely transliterate this pass.
                    // Roll back the text.  Indices remain unchanged; reset
                    // them where necessary.
                    if (index.start != index.limit) {
                        // Find the rollbackStart, adjusted for length changes
                        // and the deletion of partially transliterated text.
                        int rs = rollbackStart + delta - (index.limit - passStart);

                        // Delete the partially transliterated text
                        text.replace(passStart, index.limit, "");

                        // Copy the rollback text back
                        text.copy(rs, rs + uncommittedLength, passStart);

                        // Restore indices to their original values
                        index.start = passStart;
                        index.limit = passLimit;
                        index.contextLimit -= delta;

                        if (DEBUG) {
                            log.append(" (ROLLBACK)");
                        }
                    }

                    // We did completely transliterate this pass.  Update the
                    // commit indices to record how far we got.  Adjust indices
                    // for length change.
                    else {
                        // Move the pass indices past the committed text.
                        passStart = passLimit = index.start;

                        // Adjust the rollbackStart for length changes and move
                        // it past the committed text.  All characters we've
                        // processed to this point are committed now, so zero
                        // out the uncommittedLength.
                        rollbackStart += delta + uncommittedLength;
                        uncommittedLength = 0;

                        // Adjust indices for length changes.
                        runLimit += delta;
                        totalDelta += delta;
                    }

                    if (DEBUG) {
                        System.out.println(Utility.escape(log.toString()));
                    }
                }

                // Adjust overall limit and rollbackOrigin for insertions and
                // deletions.  Don't need to worry about contextLimit because
                // handleTransliterate() maintains that.
                rollbackOrigin += totalDelta;
                globalLimit += totalDelta;

                // Delete the rollback copy
                text.replace(rollbackOrigin, rollbackOrigin + runLength, "");

                // Move start past committed text
                index.start = passStart;
            }

            else {
                // Delegate to subclass for actual transliteration.
                if (DEBUG) {
                    log.setLength(0);
                    log.append("filteredTransliterate{"+getID()+"}: ");
                    UtilityExtensions.formatInput(log, text, index);
                }

                int limit = index.limit;
                handleTransliterate(text, index, isIncrementalRun);
                delta = index.limit - limit; // change in length

                if (DEBUG) {
                    log.append(" => ");
                    UtilityExtensions.formatInput(log, text, index);
                }

                // In a properly written transliterator, start == limit after
                // handleTransliterate() returns when incremental is false.
                // Catch cases where the subclass doesn't do this, and throw
                // an exception.  (Just pinning start to limit is a bad idea,
                // because what's probably happening is that the subclass
                // isn't transliterating all the way to the end, and it should
                // in non-incremental mode.)
                if (!isIncrementalRun && index.start != index.limit) {
                    throw new RuntimeException("ERROR: Incomplete non-incremental transliteration by " + getID());
                }

                // Adjust overall limit for insertions/deletions.  Don't need
                // to worry about contextLimit because handleTransliterate()
                // maintains that.
                globalLimit += delta;

                if (DEBUG) {
                    System.out.println(Utility.escape(log.toString()));
                }
            }

            if (filter == null || isIncrementalRun) {
                break;
            }

            // If we did completely transliterate this
            // run, then repeat with the next unfiltered run.
        }

        // Start is valid where it is.  Limit needs to be put back where
        // it was, modulo adjustments for deletions/insertions.
        index.limit = globalLimit;

        if (DEBUG) {
            System.out.println("filteredTransliterate{"+getID()+"}: OUT=" +
                               UtilityExtensions.formatInput(text, index));
        }
    }

    /**
     * Transliterate a substring of text, as specified by index, taking filters
     * into account.  This method is for subclasses that need to delegate to
     * another transliterator, such as CompoundTransliterator.
     * @param text the text to be transliterated
     * @param index the position indices
     * @param incremental if TRUE, then assume more characters may be inserted
     * at index.limit, and postpone processing to accomodate future incoming
     * characters
     * @stable ICU 2.0
     */
    protected void filteredTransliterate(Replaceable text,
                                         Position index,
                                         boolean incremental) {
        filteredTransliterate(text, index, incremental, false);
    }

    /**
     * Returns the length of the longest context required by this transliterator.
     * This is <em>preceding</em> context.  The default value is zero, but
     * subclasses can change this by calling <code>setMaximumContextLength()</code>.
     * For example, if a transliterator translates "ddd" (where
     * d is any digit) to "555" when preceded by "(ddd)", then the preceding
     * context length is 5, the length of "(ddd)".
     *
     * @return The maximum number of preceding context characters this
     * transliterator needs to examine
     * @stable ICU 2.0
     */
    protected final int getMaximumContextLength() {
        return maximumContextLength;
    }

    /**
     * Method for subclasses to use to set the maximum context length.
     * @see #getMaximumContextLength
     * @stable ICU 2.0
     */
    protected void setMaximumContextLength(int a) {
        if (a < 0) {
            throw new IllegalArgumentException("Invalid context length " + a);
        }
        maximumContextLength = a;
    }

    /**
     * Returns a programmatic identifier for this transliterator.
     * If this identifier is passed to <code>getInstance()</code>, it
     * will return this object, if it has been registered.
     * @see #registerClass
     * @see #getAvailableIDs
     * @stable ICU 2.0
     */
    public final String getID() {
        return ID;
    }

    /**
     * Set the programmatic identifier for this transliterator.  Only
     * for use by subclasses.
     * @stable ICU 2.0
     */
    protected final void setID(String id) {
        ID = id;
    }

    /**
     * Returns a name for this transliterator that is appropriate for
     * display to the user in the default locale.  See {@link
     * #getDisplayName(String,Locale)} for details.
     * @stable ICU 2.0
     */
    public final static String getDisplayName(String ID) {
        return getDisplayName(ID, Locale.getDefault());
    }

    /**
     * Returns a name for this transliterator that is appropriate for
     * display to the user in the given locale.  This name is taken
     * from the locale resource data in the standard manner of the
     * <code>java.text</code> package.
     *
     * <p>If no localized names exist in the system resource bundles,
     * a name is synthesized using a localized
     * <code>MessageFormat</code> pattern from the resource data.  The
     * arguments to this pattern are an integer followed by one or two
     * strings.  The integer is the number of strings, either 1 or 2.
     * The strings are formed by splitting the ID for this
     * transliterator at the first '-'.  If there is no '-', then the
     * entire ID forms the only string.
     * @param inLocale the Locale in which the display name should be
     * localized.
     * @see java.text.MessageFormat
     * @stable ICU 2.0
     */
    public static String getDisplayName(String id, Locale inLocale) {

        // Resource bundle containing display name keys and the
        // RB_RULE_BASED_IDS array.
        //
        //If we ever integrate this with the Sun JDK, the resource bundle
        // root will change to sun.text.resources.LocaleElements

        ResourceBundle bundle = ICULocaleData.getLocaleElements(inLocale);

        // Normalize the ID
        String stv[] = TransliteratorIDParser.IDtoSTV(id);
        if (stv == null) {
            // No target; malformed id
            return "";
        }
        String ID = stv[0] + '-' + stv[1];
        if (stv[2] != null && stv[2].length() > 0) {
            ID = ID + '/' + stv[2];
        }

        // Use the registered display name, if any
        String n = (String) displayNameCache.get(new CaseInsensitiveString(ID));
        if (n != null) {
            return n;
        }

        // Use display name for the entire transliterator, if it
        // exists.
        try {
            return bundle.getString(RB_DISPLAY_NAME_PREFIX + ID);
        } catch (MissingResourceException e) {}

        try {
            // Construct the formatter first; if getString() fails
            // we'll exit the try block
            MessageFormat format = new MessageFormat(
                    bundle.getString(RB_DISPLAY_NAME_PATTERN));
            // Construct the argument array
            Object[] args = new Object[] { new Integer(2), stv[0], stv[1] };

            // Use display names for the scripts, if they exist
            for (int j=1; j<=2; ++j) {
                try {
                    args[j] = bundle.getString(RB_SCRIPT_DISPLAY_NAME_PREFIX +
                                               (String) args[j]);
                } catch (MissingResourceException e) {}
            }

            // Format it using the pattern in the resource
            return (stv[2].length() > 0) ?
                (format.format(args) + '/' + stv[2]) :
                format.format(args);
        } catch (MissingResourceException e2) {}

        // We should not reach this point unless there is something
        // wrong with the build or the RB_DISPLAY_NAME_PATTERN has
        // been deleted from the root RB_LOCALE_ELEMENTS resource.
        throw new RuntimeException();
    }

    /**
     * Returns the filter used by this transliterator, or <tt>null</tt>
     * if this transliterator uses no filter.
     * @stable ICU 2.0
     */
    public final UnicodeFilter getFilter() {
        return filter;
    }

    /**
     * Changes the filter used by this transliterator.  If the filter
     * is set to <tt>null</tt> then no filtering will occur.
     *
     * <p>Callers must take care if a transliterator is in use by
     * multiple threads.  The filter should not be changed by one
     * thread while another thread may be transliterating.
     * @stable ICU 2.0
     */
    public void setFilter(UnicodeFilter filter) {
        this.filter = filter;
    }

    /**
     * Returns a <code>Transliterator</code> object given its ID.
     * The ID must be either a system transliterator ID or a ID registered
     * using <code>registerClass()</code>.
     *
     * @param ID a valid ID, as enumerated by <code>getAvailableIDs()</code>
     * @return A <code>Transliterator</code> object with the given ID
     * @exception IllegalArgumentException if the given ID is invalid.
     * @stable ICU 2.0
     */
    public static final Transliterator getInstance(String ID) {
        return getInstance(ID, FORWARD);
    }

    /**
     * Returns a <code>Transliterator</code> object given its ID.
     * The ID must be either a system transliterator ID or a ID registered
     * using <code>registerClass()</code>.
     *
     * @param ID a valid ID, as enumerated by <code>getAvailableIDs()</code>
     * @param dir either FORWARD or REVERSE.  If REVERSE then the
     * inverse of the given ID is instantiated.
     * @return A <code>Transliterator</code> object with the given ID
     * @exception IllegalArgumentException if the given ID is invalid.
     * @see #registerClass
     * @see #getAvailableIDs
     * @see #getID
     * @stable ICU 2.0
     */
    public static Transliterator getInstance(String ID,
                                             int dir) {
        StringBuffer canonID = new StringBuffer();
        Vector list = new Vector();
        UnicodeSet[] globalFilter = new UnicodeSet[1];
        if (!TransliteratorIDParser.parseCompoundID(ID, dir, canonID, list, globalFilter)) {
            throw new IllegalArgumentException("Invalid ID " + ID);
        }

        TransliteratorIDParser.instantiateList(list, null, -1);

        // assert(list.size() > 0);
        Transliterator t = null;
        switch (list.size()) {
        case 1:
            t = (Transliterator) list.elementAt(0);
            break;
        default:
            t = new CompoundTransliterator(list);
            break;
        }
        t.setID(canonID.toString());
        if (globalFilter[0] != null) {
            t.setFilter(globalFilter[0]);
        }
        return t;
    }

    /**
     * Create a transliterator from a basic ID.  This is an ID
     * containing only the forward direction source, target, and
     * variant.
     * @param id a basic ID of the form S-T or S-T/V.
     * @param canonID canonical ID to apply to the result, or
     * null to leave the ID unchanged
     * @return a newly created Transliterator or null if the ID is
     * invalid.
     */
    static Transliterator getBasicInstance(String id, String canonID) {
        StringBuffer s = new StringBuffer();
        Transliterator t = registry.get(id, s);
        if (s.length() != 0) {
            // assert(t==0);
            // Instantiate an alias
            t = getInstance(s.toString(), FORWARD);
        }
        if (t != null && canonID != null) {
            t.setID(canonID);
        }
        return t;
    }

    /**
     * Returns a <code>Transliterator</code> object constructed from
     * the given rule string.  This will be a RuleBasedTransliterator,
     * if the rule string contains only rules, or a
     * CompoundTransliterator, if it contains ID blocks, or a
     * NullTransliterator, if it contains ID blocks which parse as
     * empty for the given direction.
     * @stable ICU 2.0
     */
    public static final Transliterator createFromRules(String ID, String rules, int dir) {
        Transliterator t = null;

        TransliteratorParser parser = new TransliteratorParser();
        parser.parse(rules, dir);

        // NOTE: The logic here matches that in TransliteratorRegistry.
        if (parser.idBlock.length() == 0) {
            if (parser.data == null) {
                // No idBlock, no data -- this is just an
                // alias for Null
                t = new NullTransliterator();
            } else {
                // No idBlock, data != 0 -- this is an
                // ordinary RBT_DATA.
                t = new RuleBasedTransliterator(ID, parser.data, null);
            }
        } else {
            if (parser.data == null) {
                // idBlock, no data -- this is an alias.  The ID has
                // been munged from reverse into forward mode, if
                // necessary, so instantiate the ID in the forward
                // direction.
                t = getInstance(parser.idBlock);
                if (t != null) {
                    t.setID(ID);
                }
            } else {
                // idBlock and data -- this is a compound
                // RBT
                t = new RuleBasedTransliterator("_", parser.data, null);
                t = new CompoundTransliterator(ID, parser.idBlock, parser.idSplitPoint,
                                               t);
                if (parser.compoundFilter != null) {
                    t.setFilter(parser.compoundFilter);
                }
            }
        }

        return t;
    }

    /**
     * Returns a rule string for this transliterator.
     * @param escapeUnprintable if true, then unprintable characters
     * will be converted to escape form backslash-'u' or
     * backslash-'U'.
     * @stable ICU 2.0
     */
    public String toRules(boolean escapeUnprintable) {
        return baseToRules(escapeUnprintable);
    }

    /**
     * Returns a rule string for this transliterator.  This is
     * a non-overrideable base class implementation that subclasses
     * may call.  It simply munges the ID into the correct format,
     * that is, "foo" => "::foo".
     * @param escapeUnprintable if true, then unprintable characters
     * will be converted to escape form backslash-'u' or
     * backslash-'U'.
     * @stable ICU 2.0
     */
    protected final String baseToRules(boolean escapeUnprintable) {
        // The base class implementation of toRules munges the ID into
        // the correct format.  That is: foo => ::foo
        // KEEP in sync with rbt_pars
        if (escapeUnprintable) {
            StringBuffer rulesSource = new StringBuffer();
            String id = getID();
            for (int i=0; i<id.length();) {
                int c = UTF16.charAt(id, i);
                if (!Utility.escapeUnprintable(rulesSource, c)) {
                    UTF16.append(rulesSource, c);
                }
                i += UTF16.getCharCount(c);
            }
            rulesSource.insert(0, "::");
            rulesSource.append(ID_DELIM);
            return rulesSource.toString();
        }
        return "::" + getID() + ID_DELIM;
    }

    /**
     * Returns the set of all characters that may be modified in the
     * input text by this Transliterator.  This incorporates this
     * object's current filter; if the filter is changed, the return
     * value of this function will change.  The default implementation
     * returns an empty set.  Some subclasses may override {@link
     * #handleGetSourceSet} to return a more precise result.  The
     * return result is approximate in any case and is intended for
     * use by tests, tools, or utilities.
     * @see #getTargetSet
     * @see #handleGetSourceSet
     * @draft ICU 2.2
     */
    public final UnicodeSet getSourceSet() {
        UnicodeSet set = handleGetSourceSet();
        if (filter != null) {
            UnicodeSet filterSet;
            // Most, but not all filters will be UnicodeSets.  Optimize for
            // the high-runner case.
            try {
                filterSet = (UnicodeSet) filter;
            } catch (ClassCastException e) {
                filterSet = new UnicodeSet();
                filter.addMatchSetTo(filterSet);
            }
            set.retainAll(filterSet);
        }
        return set;
    }

    /**
     * Framework method that returns the set of all characters that
     * may be modified in the input text by this Transliterator,
     * ignoring the effect of this object's filter.  The base class
     * implementation returns the empty set.  Subclasses that wish to
     * implement this should override this method.
     * @return the set of characters that this transliterator may
     * modify.  The set may be modified, so subclasses should return a
     * newly-created object.
     * @see #getSourceSet
     * @see #getTargetSet
     * @draft ICU 2.2
     */
    protected UnicodeSet handleGetSourceSet() {
        return new UnicodeSet();
    }

    /**
     * Returns the set of all characters that may be generated as
     * replacement text by this transliterator.  The default
     * implementation returns the empty set.  Some subclasses may
     * override this method to return a more precise result.  The
     * return result is approximate in any case and is intended for
     * use by tests, tools, or utilities requiring such
     * meta-information.
     * @see #getTargetSet
     * @draft ICU 2.2
     */
    public UnicodeSet getTargetSet() {
        return new UnicodeSet();
    }

    /**
     * Returns this transliterator's inverse.  See the class
     * documentation for details.  This implementation simply inverts
     * the two entities in the ID and attempts to retrieve the
     * resulting transliterator.  That is, if <code>getID()</code>
     * returns "A-B", then this method will return the result of
     * <code>getInstance("B-A")</code>, or <code>null</code> if that
     * call fails.
     *
     * <p>Subclasses with knowledge of their inverse may wish to
     * override this method.
     *
     * @return a transliterator that is an inverse, not necessarily
     * exact, of this transliterator, or <code>null</code> if no such
     * transliterator is registered.
     * @see #registerClass
     * @stable ICU 2.0
     */
    public final Transliterator getInverse() {
        return getInstance(ID, REVERSE);
    }

    /**
     * Registers a subclass of <code>Transliterator</code> with the
     * system.  This subclass must have a public constructor taking no
     * arguments.  When that constructor is called, the resulting
     * object must return the <code>ID</code> passed to this method if
     * its <code>getID()</code> method is called.
     *
     * @param ID the result of <code>getID()</code> for this
     * transliterator
     * @param transClass a subclass of <code>Transliterator</code>
     * @see #unregister
     * @stable ICU 2.0
     */
    public static void registerClass(String ID, Class transClass, String displayName) {
        registry.put(ID, transClass, true);
        if (displayName != null) {
            displayNameCache.put(new CaseInsensitiveString(ID), displayName);
        }
    }

    /**
     * Register a factory object with the given ID.  The factory
     * method should return a new instance of the given transliterator.
     * @param ID the ID of this transliterator
     * @param factory the factory object
     * @stable ICU 2.0
     */
    public static void registerFactory(String ID, Factory factory) {
        registry.put(ID, factory, true);
    }

    /**
     * Register a Transliterator object with the given ID.
     * @param ID the ID of this transliterator
     * @param trans the Transliterator object
     * @draft ICU 2.2
     */
    public static void registerInstance(Transliterator trans) {
        registry.put(trans.getID(), trans, true);
    }

    /**
     * Register a Transliterator object with the given ID.
     * @param ID the ID of this transliterator
     * @param trans the Transliterator object
     */
    static void registerInstance(Transliterator trans, boolean visible) {
        registry.put(trans.getID(), trans, visible);
    }

    /**
     * Register two targets as being inverses of one another.  For
     * example, calling registerSpecialInverse("NFC", "NFD", true) causes
     * Transliterator to form the following inverse relationships:
     *
     * <pre>NFC => NFD
     * Any-NFC => Any-NFD
     * NFD => NFC
     * Any-NFD => Any-NFC</pre>
     *
     * (Without the special inverse registration, the inverse of NFC
     * would be NFC-Any.)  Note that NFD is shorthand for Any-NFD, but
     * that the presence or absence of "Any-" is preserved.
     *
     * <p>The relationship is symmetrical; registering (a, b) is
     * equivalent to registering (b, a).
     *
     * <p>The relevant IDs must still be registered separately as
     * factories or classes.
     *
     * <p>Only the targets are specified.  Special inverses always
     * have the form Any-Target1 <=> Any-Target2.  The target should
     * have canonical casing (the casing desired to be produced when
     * an inverse is formed) and should contain no whitespace or other
     * extraneous characters.
     *
     * @param target the target against which to register the inverse
     * @param inverseTarget the inverse of target, that is
     * Any-target.getInverse() => Any-inverseTarget
     * @param bidirectional if true, register the reverse relation
     * as well, that is, Any-inverseTarget.getInverse() => Any-target
     */
    static void registerSpecialInverse(String target,
                                       String inverseTarget,
                                       boolean bidirectional) {
        TransliteratorIDParser.registerSpecialInverse(target, inverseTarget, bidirectional);
    }

    /**
     * Unregisters a transliterator or class.  This may be either
     * a system transliterator or a user transliterator or class.
     *
     * @param ID the ID of the transliterator or class
     * @see #registerClass
     * @stable ICU 2.0
     */
    public static void unregister(String ID) {
        displayNameCache.remove(new CaseInsensitiveString(ID));
        registry.remove(ID);
    }

    /**
     * Returns an enumeration over the programmatic names of registered
     * <code>Transliterator</code> objects.  This includes both system
     * transliterators and user transliterators registered using
     * <code>registerClass()</code>.  The enumerated names may be
     * passed to <code>getInstance()</code>.
     *
     * @return An <code>Enumeration</code> over <code>String</code> objects
     * @see #getInstance
     * @see #registerClass
     * @stable ICU 2.0
     */
    public static final Enumeration getAvailableIDs() {
        return registry.getAvailableIDs();
    }

    /**
     * Returns an enumeration over the source names of registered
     * transliterators.  Source names may be passed to
     * getAvailableTargets() to obtain available targets for each
     * source.
     * @stable ICU 2.0
     */
    public static final Enumeration getAvailableSources() {
        return registry.getAvailableSources();
    }

    /**
     * Returns an enumeration over the target names of registered
     * transliterators having a given source name.  Target names may
     * be passed to getAvailableVariants() to obtain available
     * variants for each source and target pair.
     * @stable ICU 2.0
     */
    public static final Enumeration getAvailableTargets(String source) {
        return registry.getAvailableTargets(source);
    }

    /**
     * Returns an enumeration over the variant names of registered
     * transliterators having a given source name and target name.
     * @stable ICU 2.0
     */
    public static final Enumeration getAvailableVariants(String source,
                                                         String target) {
        return registry.getAvailableVariants(source, target);
    }

// REMOVED: ICU 2.4
//  /**
//   * Method for subclasses to use to obtain a character in the given
//   * string, with filtering.  If the character at the given offset
//   * is excluded by this transliterator's filter, then U+FFFE is returned.
//   *
//   * <p><b>Note:</b> Most subclasses that implement
//   * handleTransliterator() will <em>not</em> want to use this
//   * method, since characters they see are already filtered.
//   *
//   * @deprecated the new architecture provides filtering at the top
//   * level.  This method will be removed Dec 31 2001.
//   */
//  protected char filteredCharAt(Replaceable text, int i) {
//      char c;
//      UnicodeFilter filter = getFilter();
//      return (filter == null) ? text.charAt(i) :
//          (filter.contains(c = text.charAt(i)) ? c : '\uFFFE');
//  }

    static {
        registry = new TransliteratorRegistry();

        // The display name cache starts out empty
        displayNameCache = new Hashtable();

        // Read the index file and populate the registry.
        // Each line of the index file is either blank, a '#' comment,
        // or a colon-delimited line.  In the latter case the first
        // field is the ID being defined.  The second field is one of
        // three strings: "file", "internal", or "alias".  Remaining
        // fields vary according the value fo the second field.  See
        // the index file itself for further documentation.
        ResourceReader r = new ResourceReader("Transliterator_index.txt");
        for (;;) {
            String line = null;
            try {
                line = r.readLine();
            } catch (java.io.IOException e) {
                throw new RuntimeException("Can't read Transliterator_index.txt");
            }
            if (line == null) {
                break;
            }
            try {
                // Skip over whitespace
                int pos = 0;
                while (pos < line.length() &&
                       UCharacterProperty.isRuleWhiteSpace(line.charAt(pos))) {
                    ++pos;
                }
                // Ignore blank lines and comments
                if (pos == line.length() || line.charAt(pos) == '#') {
                    continue;
                }
                // Parse colon-delimited line
                int colon = line.indexOf(':', pos);
                String ID = line.substring(pos, colon);
                pos = colon+1;
                colon = line.indexOf(':', pos);
                String type = line.substring(pos, colon);
                pos = colon+1;

                if (type.equals("file") || type.equals("internal")) {
                    // Rest of line is <resource>:<encoding>:<direction>
                    //                pos       colon      c2
                    colon = line.indexOf(':', pos);
                    int c2 = line.indexOf(':', colon+1);
                    int dir;
                    switch (line.charAt(c2+1)) {
                    case 'F':
                        dir = FORWARD;
                        break;
                    case 'R':
                        dir = REVERSE;
                        break;
                    default:
                        throw new RuntimeException("Can't parse line: " + line);
                    }
                    registry.put(ID,
                                 line.substring(pos, colon), // resource
                                 line.substring(colon+1, c2), // encoding
                                 dir,
                                 !type.equals("internal"));
                } else if (type.equals("alias")) {
                    // Rest of line is the <getInstanceArg>
                    registry.put(ID, line.substring(pos), true);
                } else {
                    // Unknown type
                    throw new RuntimeException("Can't parse line: " + line);
                }
            } catch (StringIndexOutOfBoundsException e) {
                throw new RuntimeException("Can't parse line: " + line);
            }
        }

        registerSpecialInverse(NullTransliterator.SHORT_ID, NullTransliterator.SHORT_ID, false);

        // Register non-rule-based transliterators
        registerClass(NullTransliterator._ID,
                      NullTransliterator.class, null);
        RemoveTransliterator.register();
        EscapeTransliterator.register();
        UnescapeTransliterator.register();
        LowercaseTransliterator.register();
        UppercaseTransliterator.register();
        TitlecaseTransliterator.register();
        UnicodeNameTransliterator.register();
        NameUnicodeTransliterator.register();
        NormalizationTransliterator.register();
        BreakTransliterator.register();
        AnyTransliterator.register(); // do this last!
    }

    /**
     * The factory interface for transliterators.  Transliterator
     * subclasses can register factory objects for IDs using the
     * registerFactory() method of Transliterator.  When invoked, the
     * factory object will be passed the ID being instantiated.  This
     * makes it possible to register one factory method to more than
     * one ID, or for a factory method to parameterize its result
     * based on the variant.
     * @stable ICU 2.0
     */
    public static interface Factory {
        /**
         * Return a transliterator for the given ID.
         * @stable ICU 2.0
         */
        Transliterator getInstance(String ID);
    }
}
