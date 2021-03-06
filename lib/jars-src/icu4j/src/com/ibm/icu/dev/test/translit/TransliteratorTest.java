/*
 *******************************************************************************
 * Copyright (C) 1996-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/translit/TransliteratorTest.java,v $
 * $Date: 2003/06/11 20:00:12 $
 * $Revision: 1.126 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.dev.test.translit;
import com.ibm.icu.lang.*;
import com.ibm.icu.text.*;
import com.ibm.icu.dev.test.*;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.impl.UtilityExtensions;
import com.ibm.icu.util.CaseInsensitiveString;
import java.util.*;

/***********************************************************************

                     HOW TO USE THIS TEST FILE
                               -or-
                  How I developed on two platforms
                without losing (too much of) my mind


1. Add new tests by copying/pasting/changing existing tests.  On Java,
   any public void method named Test...() taking no parameters becomes
   a test.  On C++, you need to modify the header and add a line to
   the runIndexedTest() dispatch method.

2. Make liberal use of the expect() method; it is your friend.

3. The tests in this file exactly match those in a sister file on the
   other side.  The two files are:

   icu4j:  src/com.ibm.icu.dev.test/translit/TransliteratorTest.java
   icu4c:  source/test/intltest/transtst.cpp

                  ==> THIS IS THE IMPORTANT PART <==

   When you add a test in this file, add it in transtst.cpp too.
   Give it the same name and put it in the same relative place.  This
   makes maintenance a lot simpler for any poor soul who ends up
   trying to synchronize the tests between icu4j and icu4c.

4. If you MUST enter a test that is NOT paralleled in the sister file,
   then add it in the special non-mirrored section.  These are
   labeled

     "icu4j ONLY"

   or

     "icu4c ONLY"

   Make sure you document the reason the test is here and not there.


Thank you.
The Management
***********************************************************************/

/**
 * @test
 * @summary General test of Transliterator
 */
public class TransliteratorTest extends TestFmwk {

    public static void main(String[] args) throws Exception {
        new TransliteratorTest().run(args);
    }

    public void TestInstantiation() {
        long ms = System.currentTimeMillis();
        String ID;
        for (Enumeration e = Transliterator.getAvailableIDs(); e.hasMoreElements(); ) {
            ID = (String) e.nextElement();
            if (ID.equals("Latin-Han/definition")) {
                System.out.println("\nTODO: disabling Latin-Han/definition check for now: fix later");
                continue;
            }
            Transliterator t = null;
            try {
                t = Transliterator.getInstance(ID);
                // We should get a new instance if we try again
                Transliterator t2 = null;
                // This is true only of RBT
                if (t instanceof RuleBasedTransliterator) {
                    t = Transliterator.getInstance(ID);
                }
                if (t != t2) {
                    logln("OK: " + Transliterator.getDisplayName(ID) + " (" + ID + "): " + t);
                } else {
                    errln("FAIL: " + ID + " returned identical instances");
                    t = null;
                }
            } catch (IllegalArgumentException ex) {
                errln("FAIL: " + ID);
                throw ex;
            }

            if (t != null) {
                // Now test toRules
                String rules = null;
                try {
                    rules = t.toRules(true);

                    Transliterator u = Transliterator.createFromRules("x",
                                           rules, Transliterator.FORWARD);
                } catch (IllegalArgumentException ex2) {
                    errln("FAIL: " + ID + ".toRules() => bad rules: " +
                          rules);
                    throw ex2;
                }
            }
        }

        // Now test the failure path
        try {
            ID = "<Not a valid Transliterator ID>";
            Transliterator t = Transliterator.getInstance(ID);
            errln("FAIL: " + ID + " returned " + t);
        } catch (IllegalArgumentException ex) {
            logln("OK: Bogus ID handled properly");
        }

        ms = System.currentTimeMillis() - ms;
        logln("Elapsed time: " + ms + " ms");
    }

    public void TestSimpleRules() {
        /* Example: rules 1. ab>x|y
         *                2. yc>z
         *
         * []|eabcd  start - no match, copy e to tranlated buffer
         * [e]|abcd  match rule 1 - copy output & adjust cursor
         * [ex|y]cd  match rule 2 - copy output & adjust cursor
         * [exz]|d   no match, copy d to transliterated buffer
         * [exzd]|   done
         */
        expect("ab>x|y;" +
               "yc>z",
               "eabcd", "exzd");

        /* Another set of rules:
         *    1. ab>x|yzacw
         *    2. za>q
         *    3. qc>r
         *    4. cw>n
         *
         * []|ab       Rule 1
         * [x|yzacw]   No match
         * [xy|zacw]   Rule 2
         * [xyq|cw]    Rule 4
         * [xyqn]|     Done
         */
        expect("ab>x|yzacw;" +
               "za>q;" +
               "qc>r;" +
               "cw>n",
               "ab", "xyqn");

        /* Test categories
         */
        Transliterator t = new RuleBasedTransliterator("<ID>",
                                                       "$dummy=\uE100;" +
                                                       "$vowel=[aeiouAEIOU];" +
                                                       "$lu=[:Lu:];" +
                                                       "$vowel } $lu > '!';" +
                                                       "$vowel > '&';" +
                                                       "'!' { $lu > '^';" +
                                                       "$lu > '*';" +
                                                       "a>ERROR");
        expect(t, "abcdefgABCDEFGU", "&bcd&fg!^**!^*&");
    }

    /**
     * Test inline set syntax and set variable syntax.
     */
    public void TestInlineSet() {
        expect("{ [:Ll:] } x > y; [:Ll:] > z;", "aAbxq", "zAyzz");
        expect("a[0-9]b > qrs", "1a7b9", "1qrs9");

        expect("$digit = [0-9];" +
               "$alpha = [a-zA-Z];" +
               "$alphanumeric = [$digit $alpha];" + // ***
               "$special = [^$alphanumeric];" +     // ***
               "$alphanumeric > '-';" +
               "$special > '*';",

               "thx-1138", "---*----");
    }

    /**
     * Create some inverses and confirm that they work.  We have to be
     * careful how we do this, since the inverses will not be true
     * inverses -- we can't throw any random string at the composition
     * of the transliterators and expect the identity function.  F x
     * F' != I.  However, if we are careful about the input, we will
     * get the expected results.
     */
    public void TestRuleBasedInverse() {
        String RULES =
            "abc>zyx;" +
            "ab>yz;" +
            "bc>zx;" +
            "ca>xy;" +
            "a>x;" +
            "b>y;" +
            "c>z;" +

            "abc<zyx;" +
            "ab<yz;" +
            "bc<zx;" +
            "ca<xy;" +
            "a<x;" +
            "b<y;" +
            "c<z;" +

            "";

        String[] DATA = {
            // Careful here -- random strings will not work.  If we keep
            // the left side to the domain and the right side to the range
            // we will be okay though (left, abc; right xyz).
            "a", "x",
            "abcacab", "zyxxxyy",
            "caccb", "xyzzy",
        };

        Transliterator fwd = new RuleBasedTransliterator("<ID>", RULES);
        Transliterator rev = new RuleBasedTransliterator("<ID>", RULES,
                                     RuleBasedTransliterator.REVERSE, null);
        for (int i=0; i<DATA.length; i+=2) {
            expect(fwd, DATA[i], DATA[i+1]);
            expect(rev, DATA[i+1], DATA[i]);
        }
    }

    /**
     * Basic test of keyboard.
     */
    public void TestKeyboard() {
        Transliterator t = new RuleBasedTransliterator("<ID>",
                                                       "psch>Y;"
                                                       +"ps>y;"
                                                       +"ch>x;"
                                                       +"a>A;");
        String DATA[] = {
            // insertion, buffer
            "a", "A",
            "p", "Ap",
            "s", "Aps",
            "c", "Apsc",
            "a", "AycA",
            "psch", "AycAY",
            null, "AycAY", // null means finishKeyboardTransliteration
        };

        keyboardAux(t, DATA);
    }

    /**
     * Basic test of keyboard with cursor.
     */
    public void TestKeyboard2() {
        Transliterator t = new RuleBasedTransliterator("<ID>",
                                                       "ych>Y;"
                                                       +"ps>|y;"
                                                       +"ch>x;"
                                                       +"a>A;");
        String DATA[] = {
            // insertion, buffer
            "a", "A",
            "p", "Ap",
            "s", "Aps", // modified for rollback - "Ay",
            "c", "Apsc", // modified for rollback - "Ayc",
            "a", "AycA",
            "p", "AycAp",
            "s", "AycAps", // modified for rollback - "AycAy",
            "c", "AycApsc", // modified for rollback - "AycAyc",
            "h", "AycAY",
            null, "AycAY", // null means finishKeyboardTransliteration
        };

        keyboardAux(t, DATA);
    }

    /**
     * Test keyboard transliteration with back-replacement.
     */
    public void TestKeyboard3() {
        // We want th>z but t>y.  Furthermore, during keyboard
        // transliteration we want t>y then yh>z if t, then h are
        // typed.
        String RULES =
            "t>|y;" +
            "yh>z;" +
            "";

        String[] DATA = {
            // Column 1: characters to add to buffer (as if typed)
            // Column 2: expected appearance of buffer after
            //           keyboard xliteration.
            "a", "a",
            "b", "ab",
            "t", "abt", // modified for rollback - "aby",
            "c", "abyc",
            "t", "abyct", // modified for rollback - "abycy",
            "h", "abycz",
            null, "abycz", // null means finishKeyboardTransliteration
        };

        Transliterator t = new RuleBasedTransliterator("<ID>", RULES);
        keyboardAux(t, DATA);
    }

    private void keyboardAux(Transliterator t, String[] DATA) {
        Transliterator.Position index = new Transliterator.Position();
        ReplaceableString s = new ReplaceableString();
        for (int i=0; i<DATA.length; i+=2) {
            StringBuffer log;
            if (DATA[i] != null) {
                log = new StringBuffer(s.toString() + " + "
                                       + DATA[i]
                                       + " -> ");
                t.transliterate(s, index, DATA[i]);
            } else {
                log = new StringBuffer(s.toString() + " => ");
                t.finishTransliteration(s, index);
            }
            UtilityExtensions.formatInput(log, s, index);
            if (s.toString().equals(DATA[i+1])) {
                logln(log.toString());
            } else {
                errln("FAIL: " + log.toString() + ", expected " + DATA[i+1]);
            }
        }
    }

    // Latin-Arabic has been temporarily removed until it can be
    // done correctly.

//  public void TestArabic() {
//      String DATA[] = {
//          "Arabic",
//              "\u062a\u062a\u0645\u062a\u0639 "+
//              "\u0627\u0644\u0644\u063a\u0629 "+
//              "\u0627\u0644\u0639\u0631\u0628\u0628\u064a\u0629 "+
//              "\u0628\u0628\u0646\u0638\u0645 "+
//              "\u0643\u062a\u0627\u0628\u0628\u064a\u0629 "+
//              "\u062c\u0645\u064a\u0644\u0629"
//      };

//      Transliterator t = Transliterator.getInstance("Latin-Arabic");
//      for (int i=0; i<DATA.length; i+=2) {
//          expect(t, DATA[i], DATA[i+1]);
//      }
//  }

    /**
     * Compose the Kana transliterator forward and reverse and try
     * some strings that should come out unchanged.
     */
    public void TestCompoundKana() {
        Transliterator t = new CompoundTransliterator("Latin-Katakana;Katakana-Latin");
        expect(t, "aaaaa", "aaaaa");
    }

    /**
     * Compose the hex transliterators forward and reverse.
     */
    public void TestCompoundHex() {
        Transliterator a = Transliterator.getInstance("Any-Hex");
        Transliterator b = Transliterator.getInstance("Hex-Any");
        Transliterator[] trans = { a, b };
        Transliterator ab = new CompoundTransliterator(trans);

        // Do some basic tests of b
        expect(b, "\\u0030\\u0031", "01");

        String s = "abcde";
        expect(ab, s, s);

        trans = new Transliterator[] { b, a };
        Transliterator ba = new CompoundTransliterator(trans);
        ReplaceableString str = new ReplaceableString(s);
        a.transliterate(str);
        expect(ba, str.toString(), str.toString());
    }

    /**
     * Do some basic tests of filtering.
     */
    public void TestFiltering() {
        Transliterator hex = Transliterator.getInstance("Any-Hex");
        hex.setFilter(new UnicodeFilter() {
            public boolean contains(int c) {
                return c != 'c';
            }
            public String toPattern(boolean escapeUnprintable) {
                return "";
            }
            public boolean matchesIndexValue(int v) {
                return false;
            }
            public void addMatchSetTo(UnicodeSet toUnionTo) {}
        });
        String s = "abcde";
        String out = hex.transliterate(s);
        String exp = "\\u0061\\u0062c\\u0064\\u0065";
        if (out.equals(exp)) {
            logln("Ok:   \"" + exp + "\"");
        } else {
            logln("FAIL: \"" + out + "\", wanted \"" + exp + "\"");
        }
    }

    /**
     * Test anchors
     */
    public void TestAnchors() {
        expect("^ab  > 01 ;" +
               " ab  > |8 ;" +
               "  b  > k ;" +
               " 8x$ > 45 ;" +
               " 8x  > 77 ;",

               "ababbabxabx",
               "018k7745");
        expect("$s = [z$] ;" +
               "$s{ab    > 01 ;" +
               "   ab    > |8 ;" +
               "    b    > k ;" +
               "   8x}$s > 45 ;" +
               "   8x    > 77 ;",

               "abzababbabxzabxabx",
               "01z018k45z01x45");
    }

    /**
     * Test pattern quoting and escape mechanisms.
     */
    public void TestPatternQuoting() {
        // Array of 3n items
        // Each item is <rules>, <input>, <expected output>
        String[] DATA = {
            "\u4E01>'[male adult]'", "\u4E01", "[male adult]",
        };

        for (int i=0; i<DATA.length; i+=3) {
            logln("Pattern: " + Utility.escape(DATA[i]));
            Transliterator t = new RuleBasedTransliterator("<ID>", DATA[i]);
            expect(t, DATA[i+1], DATA[i+2]);
        }
    }

    /**
     * Regression test for bugs found in Greek transliteration.
     */
    public void TestJ277() {
        Transliterator gl = Transliterator.getInstance("Greek-Latin; NFD; [:M:]Remove; NFC");

        char sigma = (char)0x3C3;
        char upsilon = (char)0x3C5;
        char nu = (char)0x3BD;
        // not used char PHI = (char)0x3A6;
        char alpha = (char)0x3B1;
        // not used char omega = (char)0x3C9;
        // not used char omicron = (char)0x3BF;
        // not used char epsilon = (char)0x3B5;

        // sigma upsilon nu -> syn
        StringBuffer buf = new StringBuffer();
        buf.append(sigma).append(upsilon).append(nu);
        String syn = buf.toString();
        expect(gl, syn, "syn");

        // sigma alpha upsilon nu -> saun
        buf.setLength(0);
        buf.append(sigma).append(alpha).append(upsilon).append(nu);
        String sayn = buf.toString();
        expect(gl, sayn, "saun");

        // Again, using a smaller rule set
        String rules =
                    "$alpha   = \u03B1;" +
                    "$nu      = \u03BD;" +
                    "$sigma   = \u03C3;" +
                    "$ypsilon = \u03C5;" +
                    "$vowel   = [aeiouAEIOU$alpha$ypsilon];" +
                    "s <>           $sigma;" +
                    "a <>           $alpha;" +
                    "u <>  $vowel { $ypsilon;" +
                    "y <>           $ypsilon;" +
                    "n <>           $nu;";
        RuleBasedTransliterator mini = new RuleBasedTransliterator
            ("mini", rules, Transliterator.REVERSE, null);
        expect(mini, syn, "syn");
        expect(mini, sayn, "saun");

//|    // Transliterate the Greek locale data
//|    Locale el("el");
//|    DateFormatSymbols syms(el, status);
//|    if (U_FAILURE(status)) { errln("FAIL: Transliterator constructor failed"); return; }
//|    int32_t i, count;
//|    const UnicodeString* data = syms.getMonths(count);
//|    for (i=0; i<count; ++i) {
//|        if (data[i].length() == 0) {
//|            continue;
//|        }
//|        UnicodeString out(data[i]);
//|        gl->transliterate(out);
//|        bool_t ok = TRUE;
//|        if (data[i].length() >= 2 && out.length() >= 2 &&
//|            u_isupper(data[i].charAt(0)) && u_islower(data[i].charAt(1))) {
//|            if (!(u_isupper(out.charAt(0)) && u_islower(out.charAt(1)))) {
//|                ok = FALSE;
//|            }
//|        }
//|        if (ok) {
//|            logln(prettify(data[i] + " -> " + out));
//|        } else {
//|            errln(UnicodeString("FAIL: ") + prettify(data[i] + " -> " + out));
//|        }
//|    }
    }

    /**
     * Prefix, suffix support in hex transliterators
     */
    public void TestJ243() {
        // Test default Hex-Any, which should handle
        // \\u, \\U, u+, and U+
        HexToUnicodeTransliterator hex = new HexToUnicodeTransliterator();
        expect(hex, "\\u0041+\\U0042,u+0043uu+0044z", "A+B,CuDz");

        // Try a custom Hex-Any
        // \\uXXXX and &#xXXXX;
        HexToUnicodeTransliterator hex2 = new HexToUnicodeTransliterator("\\\\u###0;&\\#x###0\\;");
        expect(hex2, "\\u61\\u062\\u0063\\u00645\\u66x&#x30;&#x031;&#x0032;&#x00033;",
               "abcd5fx012&#x00033;");

        // Try custom Any-Hex (default is tested elsewhere)
        UnicodeToHexTransliterator hex3 = new UnicodeToHexTransliterator("&\\#x###0;");
        expect(hex3, "012", "&#x30;&#x31;&#x32;");
    }

    public void TestJ329() {

        Object[] DATA = {
            new Boolean(false), "a > b; c > d",
            new Boolean(true),  "a > b; no operator; c > d",
        };

        for (int i=0; i<DATA.length; i+=2) {
            String err = null;
            try {
                Transliterator t = new
                    RuleBasedTransliterator("<ID>",
                                            (String) DATA[i+1],
                                            Transliterator.FORWARD,
                                            null);
            } catch (IllegalArgumentException e) {
                err = e.getMessage();
            }
            boolean gotError = (err != null);
            String desc = (String) DATA[i+1] +
                (gotError ? (" -> error: " + err) : " -> no error");
            if ((err != null) == ((Boolean)DATA[i]).booleanValue()) {
                logln("Ok:   " + desc);
            } else {
                errln("FAIL: " + desc);
            }
        }
    }

    /**
     * Test segments and segment references.
     */
    public void TestSegments() {
        // Array of 3n items
        // Each item is <rules>, <input>, <expected output>
        String[] DATA = {
            "([a-z]) '.' ([0-9]) > $2 '-' $1",
            "abc.123.xyz.456",
            "ab1-c23.xy4-z56",
        };

        for (int i=0; i<DATA.length; i+=3) {
            logln("Pattern: " + Utility.escape(DATA[i]));
            Transliterator t = new RuleBasedTransliterator("<ID>", DATA[i]);
            expect(t, DATA[i+1], DATA[i+2]);
        }
    }

    /**
     * Test cursor positioning outside of the key
     */
    public void TestCursorOffset() {
        // Array of 3n items
        // Each item is <rules>, <input>, <expected output>
        String[] DATA = {
            "pre {alpha} post > | @ ALPHA ;" +
            "eALPHA > beta ;" +
            "pre {beta} post > BETA @@ | ;" +
            "post > xyz",

            "prealphapost prebetapost",
            "prbetaxyz preBETApost",
        };

        for (int i=0; i<DATA.length; i+=3) {
            logln("Pattern: " + Utility.escape(DATA[i]));
            Transliterator t = new RuleBasedTransliterator("<ID>", DATA[i]);
            expect(t, DATA[i+1], DATA[i+2]);
        }
    }

    /**
     * Test zero length and > 1 char length variable values.  Test
     * use of variable refs in UnicodeSets.
     */
    public void TestArbitraryVariableValues() {
        // Array of 3n items
        // Each item is <rules>, <input>, <expected output>
        String[] DATA = {
            "$abe = ab;" +
            "$pat = x[yY]z;" +
            "$ll  = 'a-z';" +
            "$llZ = [$ll];" +
            "$llY = [$ll$pat];" +
            "$emp = ;" +

            "$abe > ABE;" +
            "$pat > END;" +
            "$llZ > 1;" +
            "$llY > 2;" +
            "7$emp 8 > 9;" +
            "",

            "ab xYzxyz stY78",
            "ABE ENDEND 1129",
        };

        for (int i=0; i<DATA.length; i+=3) {
            logln("Pattern: " + Utility.escape(DATA[i]));
            Transliterator t = new RuleBasedTransliterator("<ID>", DATA[i]);
            expect(t, DATA[i+1], DATA[i+2]);
        }
    }

    /**
     * Confirm that the contextStart, contextLimit, start, and limit
     * behave correctly.
     */
    public void TestPositionHandling() {
        // Array of 3n items
        // Each item is <rules>, <input>, <expected output>
        String[] DATA = {
            "a{t} > SS ; {t}b > UU ; {t} > TT ;",
            "xtat txtb", // pos 0,9,0,9
            "xTTaSS TTxUUb",

            "a{t} > SS ; {t}b > UU ; {t} > TT ;",
            "xtat txtb", // pos 2,9,3,8
            "xtaSS TTxUUb",

            "a{t} > SS ; {t}b > UU ; {t} > TT ;",
            "xtat txtb", // pos 3,8,3,8
            "xtaTT TTxTTb",
        };

        // Array of 4n positions -- these go with the DATA array
        // They are: contextStart, contextLimit, start, limit
        int[] POS = {
            0, 9, 0, 9,
            2, 9, 3, 8,
            3, 8, 3, 8,
        };

        int n = DATA.length/3;
        for (int i=0; i<n; i++) {
            Transliterator t = new RuleBasedTransliterator("<ID>", DATA[3*i]);
            Transliterator.Position pos = new Transliterator.Position(
                POS[4*i], POS[4*i+1], POS[4*i+2], POS[4*i+3]);
            ReplaceableString rsource = new ReplaceableString(DATA[3*i+1]);
            t.transliterate(rsource, pos);
            t.finishTransliteration(rsource, pos);
            String result = rsource.toString();
            String exp = DATA[3*i+2];
            expectAux(Utility.escape(DATA[3*i]),
                      DATA[3*i+1],
                      result,
                      result.equals(exp),
                      exp);
        }
    }

    /**
     * Test the Hiragana-Katakana transliterator.
     */
    public void TestHiraganaKatakana() {
        Transliterator hk = Transliterator.getInstance("Hiragana-Katakana");
        Transliterator kh = Transliterator.getInstance("Katakana-Hiragana");

        // Array of 3n items
        // Each item is "hk"|"kh"|"both", <Hiragana>, <Katakana>
        String[] DATA = {
            "both",
            "\u3042\u3090\u3099\u3092\u3050",
            "\u30A2\u30F8\u30F2\u30B0",

            "kh",
            "\u307C\u3051\u3060\u3042\u3093\u30FC",
            "\u30DC\u30F6\u30C0\u30FC\u30F3\u30FC",
        };

        for (int i=0; i<DATA.length; i+=3) {
            switch (DATA[i].charAt(0)) {
            case 'h': // Hiragana-Katakana
                expect(hk, DATA[i+1], DATA[i+2]);
                break;
            case 'k': // Katakana-Hiragana
                expect(kh, DATA[i+2], DATA[i+1]);
                break;
            case 'b': // both
                expect(hk, DATA[i+1], DATA[i+2]);
                expect(kh, DATA[i+2], DATA[i+1]);
                break;
            }
        }

    }

    public void TestCopyJ476() {
        // This is a C++-only copy constructor test
    }

    /**
     * Test inter-Indic transliterators.  These are composed.
     */
    public void TestInterIndic() {
        String ID = "Devanagari-Gujarati";
        Transliterator dg = Transliterator.getInstance(ID);
        if (dg == null) {
            errln("FAIL: getInstance(" + ID + ") returned null");
            return;
        }
        String id = dg.getID();
        if (!id.equals(ID)) {
            errln("FAIL: getInstance(" + ID + ").getID() => " + id);
        }
        String dev = "\u0901\u090B\u0925";
        String guj = "\u0A81\u0A8B\u0AA5";
        expect(dg, dev, guj);
    }

    /**
     * Test filter syntax in IDs. (J23)
     */
    public void TestFilterIDs() {
        String[] DATA = {
            "[aeiou]Any-Hex", // ID
            "[aeiou]Hex-Any", // expected inverse ID
            "quizzical",      // src
            "q\\u0075\\u0069zz\\u0069c\\u0061l", // expected ID.translit(src)

            "[aeiou]Any-Hex;[^5]Hex-Any",
            "[^5]Any-Hex;[aeiou]Hex-Any",
            "quizzical",
            "q\\u0075izzical",

            "[abc]Null",
            "[abc]Null",
            "xyz",
            "xyz",
        };

        for (int i=0; i<DATA.length; i+=4) {
            String ID = DATA[i];
            Transliterator t = Transliterator.getInstance(ID);
            expect(t, DATA[i+2], DATA[i+3]);

            // Check the ID
            if (!ID.equals(t.getID())) {
                errln("FAIL: getInstance(" + ID + ").getID() => " +
                      t.getID());
            }

            // Check the inverse
            String uID = DATA[i+1];
            Transliterator u = t.getInverse();
            if (u == null) {
                errln("FAIL: " + ID + ".getInverse() returned NULL");
            } else if (!u.getID().equals(uID)) {
                errln("FAIL: " + ID + ".getInverse().getID() => " +
                      u.getID() + ", expected " + uID);
            }
        }
    }

    /**
     * Test the case mapping transliterators.
     */
    public void TestCaseMap() {
        Transliterator toUpper =
            Transliterator.getInstance("Any-Upper[^xyzXYZ]");
        Transliterator toLower =
            Transliterator.getInstance("Any-Lower[^xyzXYZ]");
        Transliterator toTitle =
            Transliterator.getInstance("Any-Title[^xyzXYZ]");

        expect(toUpper, "The quick brown fox jumped over the lazy dogs.",
               "THE QUICK BROWN FOx JUMPED OVER THE LAzy DOGS.");
        expect(toLower, "The quIck brown fOX jUMPED OVER THE LAzY dogs.",
               "the quick brown foX jumped over the lazY dogs.");
        expect(toTitle, "the quick brown foX caN'T jump over the laZy dogs.",
               "The Quick Brown FoX Can't Jump Over The LaZy Dogs.");
    }

    /**
     * Test the name mapping transliterators.
     */
    public void TestNameMap() {
        Transliterator uni2name =
            Transliterator.getInstance("Any-Name[^abc]");
        Transliterator name2uni =
            Transliterator.getInstance("Name-Any");

        expect(uni2name, "\u00A0abc\u4E01\u00B5\u0A81\uFFFD\u0004\u0009\u0081\uFFFF",
               "\\N{NO-BREAK SPACE}abc\\N{CJK UNIFIED IDEOGRAPH-4E01}\\N{MICRO SIGN}\\N{GUJARATI SIGN CANDRABINDU}\\N{REPLACEMENT CHARACTER}\\N{END OF TRANSMISSION}\\N{CHARACTER TABULATION}\\N{<control-0081>}\\N{<noncharacter-FFFF>}");
        expect(name2uni, "{\\N { NO-BREAK SPACE}abc\\N{  CJK UNIFIED  IDEOGRAPH-4E01  }\\N{x\\N{MICRO SIGN}\\N{GUJARATI SIGN CANDRABINDU}\\N{REPLACEMENT CHARACTER}\\N{END OF TRANSMISSION}\\N{CHARACTER TABULATION}\\N{<control-0081>}\\N{<noncharacter-FFFF>}\\N{<control-0004>}\\N{",
               "{\u00A0abc\u4E01\\N{x\u00B5\u0A81\uFFFD\u0004\u0009\u0081\uFFFF\u0004\\N{");

        // round trip
        Transliterator t = Transliterator.getInstance("Any-Name;Name-Any");

        String s = "{\u00A0abc\u4E01\\N{x\u00B5\u0A81\uFFFD\u0004\u0009\u0081\uFFFF\u0004\\N{";
        expect(t, s, s);
    }

    /**
     * Test liberalized ID syntax.  1006c
     */
    public void TestLiberalizedID() {
        // Some test cases have an expected getID() value of NULL.  This
        // means I have disabled the test case for now.  This stuff is
        // still under development, and I haven't decided whether to make
        // getID() return canonical case yet.  It will all get rewritten
        // with the move to Source-Target/Variant IDs anyway. [aliu]
        String DATA[] = {
            "latin-greek", null /*"Latin-Greek"*/, "case insensitivity",
            "  Null  ", "Null", "whitespace",
            " Latin[a-z]-Greek  ", "[a-z]Latin-Greek", "inline filter",
            "  null  ; latin-greek  ", null /*"Null;Latin-Greek"*/, "compound whitespace",
        };

        for (int i=0; i<DATA.length; i+=3) {
            try {
                Transliterator t = Transliterator.getInstance(DATA[i]);
                if (DATA[i+1] == null || DATA[i+1].equals(t.getID())) {
                    logln("Ok: " + DATA[i+2] +
                          " create ID \"" + DATA[i] + "\" => \"" +
                          t.getID() + "\"");
                } else {
                    errln("FAIL: " + DATA[i+2] +
                          " create ID \"" + DATA[i] + "\" => \"" +
                          t.getID() + "\", exp \"" + DATA[i+1] + "\"");
                }
            } catch (IllegalArgumentException e) {
                errln("FAIL: " + DATA[i+2] +
                      " create ID \"" + DATA[i] + "\"");
            }
        }
    }

    public void TestCreateInstance() {
        String FORWARD = "F";
        String REVERSE = "R";
        String DATA[] = {
            // Column 1: id
            // Column 2: direction
            // Column 3: expected ID, or "" if expect failure
            "Latin-Hangul", REVERSE, "Hangul-Latin", // JB#912
            
            // JB#2689: bad compound causes crash
            "InvalidSource-InvalidTarget", FORWARD, "",
            "InvalidSource-InvalidTarget", REVERSE, "",
            "Hex-Any;InvalidSource-InvalidTarget", FORWARD, "",
            "Hex-Any;InvalidSource-InvalidTarget", REVERSE, "",
            "InvalidSource-InvalidTarget;Hex-Any", FORWARD, "",
            "InvalidSource-InvalidTarget;Hex-Any", REVERSE, "",
            
            null
        };
        
        for (int i=0; DATA[i]!=null; i+=3) {
            String id=DATA[i];
            int dir = (DATA[i+1]==FORWARD)?
                Transliterator.FORWARD:Transliterator.REVERSE;
            String expID=DATA[i+2];
            Exception e = null;
            Transliterator t;
            try {
                t = Transliterator.getInstance(id,dir);
            } catch (Exception e1) {
                e = e1;
                t = null;
            }
            String newID = (t!=null)?t.getID():"";
            boolean ok = (newID.equals(expID));
            if (t==null) {
                newID = e.getMessage();
            }
            if (ok) {
                logln("Ok: createInstance(" +
                      id + "," + DATA[i+1] + ") => " + newID);
            } else {
                errln("FAIL: createInstance(" +
                      id + "," + DATA[i+1] + ") => " + newID +
                      ", expected " + expID);
            }
        }
    }

    /**
     * Test the normalization transliterator.
     */
    public void TestNormalizationTransliterator() {
        // THE FOLLOWING TWO TABLES ARE COPIED FROM com.ibm.icu.dev.test.normalizer.BasicTest
        // PLEASE KEEP THEM IN SYNC WITH BasicTest.
        String[][] CANON = {
            // Input               Decomposed            Composed
            {"cat",                "cat",                "cat"               },
            {"\u00e0ardvark",      "a\u0300ardvark",     "\u00e0ardvark"     },

            {"\u1e0a",             "D\u0307",            "\u1e0a"            }, // D-dot_above
            {"D\u0307",            "D\u0307",            "\u1e0a"            }, // D dot_above

            {"\u1e0c\u0307",       "D\u0323\u0307",      "\u1e0c\u0307"      }, // D-dot_below dot_above
            {"\u1e0a\u0323",       "D\u0323\u0307",      "\u1e0c\u0307"      }, // D-dot_above dot_below
            {"D\u0307\u0323",      "D\u0323\u0307",      "\u1e0c\u0307"      }, // D dot_below dot_above

            {"\u1e10\u0307\u0323", "D\u0327\u0323\u0307","\u1e10\u0323\u0307"}, // D dot_below cedilla dot_above
            {"D\u0307\u0328\u0323","D\u0328\u0323\u0307","\u1e0c\u0328\u0307"}, // D dot_above ogonek dot_below

            {"\u1E14",             "E\u0304\u0300",      "\u1E14"            }, // E-macron-grave
            {"\u0112\u0300",       "E\u0304\u0300",      "\u1E14"            }, // E-macron + grave
            {"\u00c8\u0304",       "E\u0300\u0304",      "\u00c8\u0304"      }, // E-grave + macron

            {"\u212b",             "A\u030a",            "\u00c5"            }, // angstrom_sign
            {"\u00c5",             "A\u030a",            "\u00c5"            }, // A-ring

            {"\u00fdffin",         "y\u0301ffin",        "\u00fdffin"        }, //updated with 3.0
            {"\u00fd\uFB03n",      "y\u0301\uFB03n",     "\u00fd\uFB03n"     }, //updated with 3.0

            {"Henry IV",           "Henry IV",           "Henry IV"          },
            {"Henry \u2163",       "Henry \u2163",       "Henry \u2163"      },

            {"\u30AC",             "\u30AB\u3099",       "\u30AC"            }, // ga (Katakana)
            {"\u30AB\u3099",       "\u30AB\u3099",       "\u30AC"            }, // ka + ten
            {"\uFF76\uFF9E",       "\uFF76\uFF9E",       "\uFF76\uFF9E"      }, // hw_ka + hw_ten
            {"\u30AB\uFF9E",       "\u30AB\uFF9E",       "\u30AB\uFF9E"      }, // ka + hw_ten
            {"\uFF76\u3099",       "\uFF76\u3099",       "\uFF76\u3099"      }, // hw_ka + ten

            {"A\u0300\u0316",      "A\u0316\u0300",      "\u00C0\u0316"      },
        };

        String[][] COMPAT = {
            // Input               Decomposed            Composed
            {"\uFB4f",             "\u05D0\u05DC",       "\u05D0\u05DC"      }, // Alef-Lamed vs. Alef, Lamed

            {"\u00fdffin",         "y\u0301ffin",        "\u00fdffin"        }, //updated for 3.0
            {"\u00fd\uFB03n",      "y\u0301ffin",        "\u00fdffin"        }, // ffi ligature -> f + f + i

            {"Henry IV",           "Henry IV",           "Henry IV"          },
            {"Henry \u2163",       "Henry IV",           "Henry IV"          },

            {"\u30AC",             "\u30AB\u3099",       "\u30AC"            }, // ga (Katakana)
            {"\u30AB\u3099",       "\u30AB\u3099",       "\u30AC"            }, // ka + ten

            {"\uFF76\u3099",       "\u30AB\u3099",       "\u30AC"            }, // hw_ka + ten
        };

        Transliterator NFD = Transliterator.getInstance("NFD");
        Transliterator NFC = Transliterator.getInstance("NFC");
        for (int i=0; i<CANON.length; ++i) {
            String in = CANON[i][0];
            String expd = CANON[i][1];
            String expc = CANON[i][2];
            expect(NFD, in, expd);
            expect(NFC, in, expc);
        }

        Transliterator NFKD = Transliterator.getInstance("NFKD");
        Transliterator NFKC = Transliterator.getInstance("NFKC");
        for (int i=0; i<COMPAT.length; ++i) {
            String in = COMPAT[i][0];
            String expkd = COMPAT[i][1];
            String expkc = COMPAT[i][2];
            expect(NFKD, in, expkd);
            expect(NFKC, in, expkc);
        }

        Transliterator t = Transliterator.getInstance("NFD; [x]Remove");
        expect(t, "\u010dx", "c\u030C");
    }

    /**
     * Test compound RBT rules.
     */
    public void TestCompoundRBT() {
        // Careful with spacing and ';' here:  Phrase this exactly
        // as toRules() is going to return it.  If toRules() changes
        // with regard to spacing or ';', then adjust this string.
        String rule = "::Hex-Any;\n" +
                      "::Any-Lower;\n" +
                      "a > '.A.';\n" +
                      "b > '.B.';\n" +
                      "::[^t]Any-Upper;";
        Transliterator t = Transliterator.createFromRules("Test", rule, Transliterator.FORWARD);
        if (t == null) {
            errln("FAIL: createFromRules failed");
            return;
        }
        expect(t, "\u0043at in the hat, bat on the mat",
               "C.A.t IN tHE H.A.t, .B..A.t ON tHE M.A.t");
        String r = t.toRules(true);
        if (r.equals(rule)) {
            logln("OK: toRules() => " + r);
        } else {
            errln("FAIL: toRules() => " + r +
                  ", expected " + rule);
        }

        // Now test toRules
        t = Transliterator.getInstance("Greek-Latin; Latin-Cyrillic", Transliterator.FORWARD);
        if (t == null) {
            errln("FAIL: createInstance failed");
            return;
        }
        String exp = "::Greek-Latin;\n::Latin-Cyrillic;";
        r = t.toRules(true);
        if (!r.equals(exp)) {
            errln("FAIL: toRules() => " + r +
                  ", expected " + exp);
        } else {
            logln("OK: toRules() => " + r);
        }

        // Round trip the result of toRules
        t = Transliterator.createFromRules("Test", r, Transliterator.FORWARD);
        if (t == null) {
            errln("FAIL: createFromRules #2 failed");
            return;
        } else {
            logln("OK: createFromRules(" + r + ") succeeded");
        }

        // Test toRules again
        r = t.toRules(true);
        if (!r.equals(exp)) {
            errln("FAIL: toRules() => " + r +
                  ", expected " + exp);
        } else {
            logln("OK: toRules() => " + r);
        }

        // Test Foo(Bar) IDs.  Careful with spacing in id; make it conform
        // to what the regenerated ID will look like.
        String id = "Upper(Lower);(NFKC)";
        t = Transliterator.getInstance(id, Transliterator.FORWARD);
        if (t == null) {
            errln("FAIL: createInstance #2 failed");
            return;
        }
        if (t.getID().equals(id)) {
            logln("OK: created " + id);
        } else {
            errln("FAIL: createInstance(" + id +
                  ").getID() => " + t.getID());
        }

        Transliterator u = t.getInverse();
        if (u == null) {
            errln("FAIL: createInverse failed");
            return;
        }
        exp = "NFKC();Lower(Upper)";
        if (u.getID().equals(exp)) {
            logln("OK: createInverse(" + id + ") => " +
                  u.getID());
        } else {
            errln("FAIL: createInverse(" + id + ") => " +
                  u.getID());
        }
    }

    /**
     * Compound filter semantics were orginially not implemented
     * correctly.  Originally, each component filter f(i) is replaced by
     * f'(i) = f(i) && g, where g is the filter for the compound
     * transliterator.
     *
     * From Mark:
     *
     * Suppose and I have a transliterator X. Internally X is
     * "Greek-Latin; Latin-Cyrillic; Any-Lower". I use a filter [^A].
     *
     * The compound should convert all greek characters (through latin) to
     * cyrillic, then lowercase the result. The filter should say "don't
     * touch 'A' in the original". But because an intermediate result
     * happens to go through "A", the Greek Alpha gets hung up.
     */
    public void TestCompoundFilter() {
        Transliterator t = Transliterator.getInstance
            ("Greek-Latin; Latin-Greek; Lower", Transliterator.FORWARD);
        t.setFilter(new UnicodeSet("[^A]"));

        // Only the 'A' at index 1 should remain unchanged
        expect(t,
               CharsToUnicodeString("BA\\u039A\\u0391"),
               CharsToUnicodeString("\\u03b2A\\u03ba\\u03b1"));
    }

    /**
     * Test the "Remove" transliterator.
     */
    public void TestRemove() {
        Transliterator t = Transliterator.getInstance("Remove[aeiou]");
        expect(t, "The quick brown fox.",
               "Th qck brwn fx.");
    }

    public void TestToRules() {
        String RBT = "rbt";
        String SET = "set";
        String[] DATA = {
            RBT,
            "$a=\\u4E61; [$a] > A;",
            "[\\u4E61] > A;",

            RBT,
            "$white=[[:Zs:][:Zl:]]; $white{a} > A;",
            "[[:Zs:][:Zl:]]{a} > A;",

            SET,
            "[[:Zs:][:Zl:]]",
            "[[:Zs:][:Zl:]]",

            SET,
            "[:Ps:]",
            "[:Ps:]",

            SET,
            "[:L:]",
            "[:L:]",

            SET,
            "[[:L:]-[A]]",
            "[[:L:]-[A]]",

            SET,
            "[~[:Lu:][:Ll:]]",
            "[~[:Lu:][:Ll:]]",

            SET,
            "[~[a-z]]",
            "[~[a-z]]",

            RBT,
            "$white=[:Zs:]; $black=[^$white]; $black{a} > A;",
            "[^[:Zs:]]{a} > A;",

            RBT,
            "$a=[:Zs:]; $b=[[a-z]-$a]; $b{a} > A;",
            "[[a-z]-[:Zs:]]{a} > A;",

            RBT,
            "$a=[:Zs:]; $b=[$a&[a-z]]; $b{a} > A;",
            "[[:Zs:]&[a-z]]{a} > A;",

            RBT,
            "$a=[:Zs:]; $b=[x$a]; $b{a} > A;",
            "[x[:Zs:]]{a} > A;",

            RBT,
            "$accentMinus = [ [\\u0300-\\u0345] & [:M:] - [\\u0338]] ;"+
            "$macron = \\u0304 ;"+
            "$evowel = [aeiouyAEIOUY] ;"+
            "$iotasub = \\u0345 ;"+
            "($evowel $macron $accentMinus *) i > | $1 $iotasub ;",
            "([AEIOUYaeiouy]\\u0304[[\\u0300-\\u0345]&[:M:]-[\\u0338]]*)i > | $1 \\u0345;",

            RBT,
            "([AEIOUYaeiouy]\\u0304[[:M:]-[\\u0304\\u0345]]*)i > | $1 \\u0345;",
            "([AEIOUYaeiouy]\\u0304[[:M:]-[\\u0304\\u0345]]*)i > | $1 \\u0345;",
        };

        for (int d=0; d < DATA.length; d+=3) {
            if (DATA[d] == RBT) {
                // Transliterator test
                Transliterator t = Transliterator.createFromRules("ID",
                                       DATA[d+1], Transliterator.FORWARD);
                if (t == null) {
                    errln("FAIL: createFromRules failed");
                    return;
                }
                String rules, escapedRules;
                rules = t.toRules(false);
                escapedRules = t.toRules(true);
                String expRules = Utility.unescape(DATA[d+2]);
                String expEscapedRules = DATA[d+2];
                if (rules.equals(expRules)) {
                    logln("Ok: " + DATA[d+1] +
                          " => " + Utility.escape(rules));
                } else {
                    errln("FAIL: " + DATA[d+1] +
                          " => " + Utility.escape(rules + ", exp " + expRules));
                }
                if (escapedRules.equals(expEscapedRules)) {
                    logln("Ok: " + DATA[d+1] +
                          " => " + escapedRules);
                } else {
                    errln("FAIL: " + DATA[d+1] +
                          " => " + escapedRules + ", exp " + expEscapedRules);
                }

            } else {
                // UnicodeSet test
                String pat = DATA[d+1];
                String expToPat = DATA[d+2];
                UnicodeSet set = new UnicodeSet(pat);

                // Adjust spacing etc. as necessary.
                String toPat;
                toPat = set.toPattern(true);
                if (expToPat.equals(toPat)) {
                    logln("Ok: " + pat +
                          " => " + toPat);
                } else {
                    errln("FAIL: " + pat +
                          " => " + Utility.escape(toPat) +
                          ", exp " + Utility.escape(pat));
                }
            }
        }
    }

    public void TestContext() {
        Transliterator.Position pos = new Transliterator.Position(0, 2, 0, 1); // cs cl s l

        expect("de > x; {d}e > y;",
               "de",
               "ye",
               pos);

        expect("ab{c} > z;",
               "xadabdabcy",
               "xadabdabzy");
    }

    static final String CharsToUnicodeString(String s) {
        return Utility.unescape(s);
    }

    public void TestSupplemental() {

        expect(CharsToUnicodeString("$a=\\U00010300; $s=[\\U00010300-\\U00010323];" +
                                    "a > $a; $s > i;"),
               CharsToUnicodeString("ab\\U0001030Fx"),
               CharsToUnicodeString("\\U00010300bix"));

        expect(CharsToUnicodeString("$a=[a-z\\U00010300-\\U00010323];" +
                                    "$b=[A-Z\\U00010400-\\U0001044D];" +
                                    "($a)($b) > $2 $1;"),
               CharsToUnicodeString("aB\\U00010300\\U00010400c\\U00010401\\U00010301D"),
               CharsToUnicodeString("Ba\\U00010400\\U00010300\\U00010401cD\\U00010301"));

        // k|ax\\U00010300xm

        // k|a\\U00010400\\U00010300xm
        // ky|\\U00010400\\U00010300xm
        // ky\\U00010400|\\U00010300xm

        // ky\\U00010400|\\U00010300\\U00010400m
        // ky\\U00010400y|\\U00010400m
        expect(CharsToUnicodeString("$a=[a\\U00010300-\\U00010323];" +
                                    "$a {x} > | @ \\U00010400;" +
                                    "{$a} [^\\u0000-\\uFFFF] > y;"),
               CharsToUnicodeString("kax\\U00010300xm"),
               CharsToUnicodeString("ky\\U00010400y\\U00010400m"));

        expect(Transliterator.getInstance("Any-Name"),
               CharsToUnicodeString("\\U00010330\\U000E0061\\u00A0"),
               "\\N{GOTHIC LETTER AHSA}\\N{TAG LATIN SMALL LETTER A}\\N{NO-BREAK SPACE}");

        expect(Transliterator.getInstance("Name-Any"),
               "\\N{GOTHIC LETTER AHSA}\\N{TAG LATIN SMALL LETTER A}\\N{NO-BREAK SPACE}",
               CharsToUnicodeString("\\U00010330\\U000E0061\\u00A0"));

        expect(Transliterator.getInstance("Any-Hex/Unicode"),
               CharsToUnicodeString("\\U00010330\\U0010FF00\\U000E0061\\u00A0"),
               "U+10330U+10FF00U+E0061U+00A0");

        expect(Transliterator.getInstance("Any-Hex/C"),
               CharsToUnicodeString("\\U00010330\\U0010FF00\\U000E0061\\u00A0"),
               "\\U00010330\\U0010FF00\\U000E0061\\u00A0");

        expect(Transliterator.getInstance("Any-Hex/Perl"),
               CharsToUnicodeString("\\U00010330\\U0010FF00\\U000E0061\\u00A0"),
               "\\x{10330}\\x{10FF00}\\x{E0061}\\x{A0}");

        expect(Transliterator.getInstance("Any-Hex/Java"),
               CharsToUnicodeString("\\U00010330\\U0010FF00\\U000E0061\\u00A0"),
               "\\uD800\\uDF30\\uDBFF\\uDF00\\uDB40\\uDC61\\u00A0");

        expect(Transliterator.getInstance("Any-Hex/XML"),
               CharsToUnicodeString("\\U00010330\\U0010FF00\\U000E0061\\u00A0"),
               "&#x10330;&#x10FF00;&#xE0061;&#xA0;");

        expect(Transliterator.getInstance("Any-Hex/XML10"),
               CharsToUnicodeString("\\U00010330\\U0010FF00\\U000E0061\\u00A0"),
               "&#66352;&#1113856;&#917601;&#160;");

        expect(Transliterator.getInstance("[\\U000E0000-\\U000E0FFF] Remove"),
               CharsToUnicodeString("\\U00010330\\U0010FF00\\U000E0061\\u00A0"),
               CharsToUnicodeString("\\U00010330\\U0010FF00\\u00A0"));
    }

    public void TestQuantifier() {

        // Make sure @ in a quantified anteContext works
        expect("a+ {b} > | @@ c; A > a; (a+ c) > '(' $1 ')';",
               "AAAAAb",
               "aaa(aac)");

        // Make sure @ in a quantified postContext works
        expect("{b} a+ > c @@ |; (a+) > '(' $1 ')';",
               "baaaaa",
               "caa(aaa)");

        // Make sure @ in a quantified postContext with seg ref works
        expect("{(b)} a+ > $1 @@ |; (a+) > '(' $1 ')';",
               "baaaaa",
               "baa(aaa)");

        // Make sure @ past ante context doesn't enter ante context
        Transliterator.Position pos = new Transliterator.Position(0, 5, 3, 5);
        expect("a+ {b} > | @@ c; x > y; (a+ c) > '(' $1 ')';",
               "xxxab",
               "xxx(ac)",
               pos);

        // Make sure @ past post context doesn't pass limit
        Transliterator.Position pos2 = new Transliterator.Position(0, 4, 0, 2);
        expect("{b} a+ > c @@ |; x > y; a > A;",
               "baxx",
               "caxx",
               pos2);

        // Make sure @ past post context doesn't enter post context
        expect("{b} a+ > c @@ |; x > y; a > A;",
               "baxx",
               "cayy");

        expect("(ab)? c > d;",
               "c abc ababc",
               "d d abd");

        // NOTE: The (ab)+ when referenced just yields a single "ab",
        // not the full sequence of them.  This accords with perl behavior.
        expect("(ab)+ {x} > '(' $1 ')';",
               "x abx ababxy",
               "x ab(ab) abab(ab)y");

        expect("b+ > x;",
               "ac abc abbc abbbc",
               "ac axc axc axc");

        expect("[abc]+ > x;",
               "qac abrc abbcs abtbbc",
               "qx xrx xs xtx");

        expect("q{(ab)+} > x;",
               "qa qab qaba qababc qaba",
               "qa qx qxa qxc qxa");

        expect("q(ab)* > x;",
               "qa qab qaba qababc",
               "xa x xa xc");

        // NOTE: The (ab)+ when referenced just yields a single "ab",
        // not the full sequence of them.  This accords with perl behavior.
        expect("q(ab)* > '(' $1 ')';",
               "qa qab qaba qababc",
               "()a (ab) (ab)a (ab)c");

        // 'foo'+ and 'foo'* -- the quantifier should apply to the entire
        // quoted string
        expect("'ab'+ > x;",
               "bb ab ababb",
               "bb x xb");

        // $foo+ and $foo* -- the quantifier should apply to the entire
        // variable reference
        expect("$var = ab; $var+ > x;",
               "bb ab ababb",
               "bb x xb");
    }

    static class TestFact implements Transliterator.Factory {
        static class NameableNullTrans extends NullTransliterator {
            public NameableNullTrans(String id) {
                setID(id);
            }
        };
        String id;
        public TestFact(String theID) {
            id = theID;
        }
        public Transliterator getInstance(String ignoredID) {
            return new NameableNullTrans(id);
        }
    };

    public void TestSTV() {
        Enumeration es = Transliterator.getAvailableSources();
        for (int i=0; es.hasMoreElements(); ++i) {
            String source = (String) es.nextElement();
            logln("" + i + ": " + source);
            if (source.length() == 0) {
                errln("FAIL: empty source");
                continue;
            }
            Enumeration et = Transliterator.getAvailableTargets(source);
            for (int j=0; et.hasMoreElements(); ++j) {
                String target = (String) et.nextElement();
                logln(" " + j + ": " + target);
                if (target.length() == 0) {
                    errln("FAIL: empty target");
                    continue;
                }
                Enumeration ev = Transliterator.getAvailableVariants(source, target);
                for (int k=0; ev.hasMoreElements(); ++k) {
                    String variant = (String) ev.nextElement();
                    if (variant.length() == 0) {
                        logln("  " + k + ": <empty>");
                    } else {
                        logln("  " + k + ": " + variant);
                    }
                }
            }
        }

        // Test registration
        String[] IDS = { "Fieruwer", "Seoridf-Sweorie", "Oewoir-Oweri/Vsie" };
        String[] FULL_IDS = { "Any-Fieruwer", "Seoridf-Sweorie", "Oewoir-Oweri/Vsie" };
        String[] SOURCES = { null, "Seoridf", "Oewoir" };
        for (int i=0; i<3; ++i) {
            Transliterator.registerFactory(IDS[i], new TestFact(IDS[i]));
            try {
                Transliterator t = Transliterator.getInstance(IDS[i]);
                if (t.getID().equals(IDS[i])) {
                    logln("Ok: Registration/creation succeeded for ID " +
                          IDS[i]);
                } else {
                    errln("FAIL: Registration of ID " +
                          IDS[i] + " creates ID " + t.getID());
                }
                Transliterator.unregister(IDS[i]);
                try {
                    t = Transliterator.getInstance(IDS[i]);
                    errln("FAIL: Unregistration failed for ID " +
                          IDS[i] + "; still receiving ID " + t.getID());
                } catch (IllegalArgumentException e2) {
                    // Good; this is what we expect
                    logln("Ok; Unregistered " + IDS[i]);
                }
            } catch (IllegalArgumentException e) {
                errln("FAIL: Registration/creation failed for ID " +
                      IDS[i]);
            } finally {
                Transliterator.unregister(IDS[i]);
            }
        }

        // Make sure getAvailable API reflects removal
        for (Enumeration e = Transliterator.getAvailableIDs();
             e.hasMoreElements(); ) {
            String id = (String) e.nextElement();
            for (int i=0; i<3; ++i) {
                if (id.equals(FULL_IDS[i])) {
                    errln("FAIL: unregister(" + id + ") failed");
                }
            }
        }
        for (Enumeration e = Transliterator.getAvailableTargets("Any");
             e.hasMoreElements(); ) {
            String t = (String) e.nextElement();
            if (t.equals(IDS[0])) {
                errln("FAIL: unregister(Any-" + t + ") failed");
            }
        }
        for (Enumeration e = Transliterator.getAvailableSources();
             e.hasMoreElements(); ) {
            String s = (String) e.nextElement();
            for (int i=0; i<3; ++i) {
                if (SOURCES[i] == null) continue;
                if (s.equals(SOURCES[i])) {
                    errln("FAIL: unregister(" + s + "-*) failed");
                }
            }
        }
    }

    /**
     * Test inverse of Greek-Latin; Title()
     */
    public void TestCompoundInverse() {
        Transliterator t = Transliterator.getInstance
            ("Greek-Latin; Title()", Transliterator.REVERSE);
        if (t == null) {
            errln("FAIL: createInstance");
            return;
        }
        String exp = "(Title);Latin-Greek";
        if (t.getID().equals(exp)) {
            logln("Ok: inverse of \"Greek-Latin; Title()\" is \"" +
                  t.getID());
        } else {
            errln("FAIL: inverse of \"Greek-Latin; Title()\" is \"" +
                  t.getID() + "\", expected \"" + exp + "\"");
        }
    }

    /**
     * Test NFD chaining with RBT
     */
    public void TestNFDChainRBT() {
        Transliterator t = Transliterator.createFromRules(
                               "TEST", "::NFD; aa > Q; a > q;",
                               Transliterator.FORWARD);
        logln(t.toRules(true));
        expect(t, "aa", "Q");
    }

    /**
     * Inverse of "Null" should be "Null". (J21)
     */
    public void TestNullInverse() {
        Transliterator t = Transliterator.getInstance("Null");
        Transliterator u = t.getInverse();
        if (!u.getID().equals("Null")) {
            errln("FAIL: Inverse of Null should be Null");
        }
    }

    /**
     * Check ID of inverse of alias. (J22)
     */
    public void TestAliasInverseID() {
        String ID = "Latin-Hangul"; // This should be any alias ID with an inverse
        Transliterator t = Transliterator.getInstance(ID);
        Transliterator u = t.getInverse();
        String exp = "Hangul-Latin";
        String got = u.getID();
        if (!got.equals(exp)) {
            errln("FAIL: Inverse of " + ID + " is " + got +
                  ", expected " + exp);
        }
    }

    /**
     * Test IDs of inverses of compound transliterators. (J20)
     */
    public void TestCompoundInverseID() {
        String ID = "Latin-Jamo;NFC(NFD)";
        Transliterator t = Transliterator.getInstance(ID);
        Transliterator u = t.getInverse();
        String exp = "NFD(NFC);Jamo-Latin";
        String got = u.getID();
        if (!got.equals(exp)) {
            errln("FAIL: Inverse of " + ID + " is " + got +
                  ", expected " + exp);
        }
    }

    /**
     * Test undefined variable.
     */
    public void TestUndefinedVariable() {
        String rule = "$initial } a <> \u1161;";
        try {
            Transliterator t = Transliterator.createFromRules("<ID>", rule,Transliterator.FORWARD);
            t = null;
        } catch (IllegalArgumentException e) {
            logln("OK: Got exception for " + rule + ", as expected: " +
                  e.getMessage());
            return;
        }
        errln("Fail: bogus rule " + rule + " compiled without error");
    }

    /**
     * Test empty context.
     */
    public void TestEmptyContext() {
        expect(" { a } > b;", "xay a ", "xby b ");
    }

    /**
     * Test compound filter ID syntax
     */
    public void TestCompoundFilterID() {
        String[] DATA = {
            // Col. 1 = ID or rule set (latter must start with #)

            // = columns > 1 are null if expect col. 1 to be illegal =

            // Col. 2 = direction, "F..." or "R..."
            // Col. 3 = source string
            // Col. 4 = exp result

            "[abc]; [abc]", null, null, null, // multiple filters
            "Latin-Greek; [abc];", null, null, null, // misplaced filter
            "[b]; Latin-Greek; Upper; ([xyz])", "F", "abc", "a\u0392c",
            "[b]; (Lower); Latin-Greek; Upper(); ([\u0392])", "R", "\u0391\u0392\u0393", "\u0391b\u0393",
            "#\n::[b]; ::Latin-Greek; ::Upper; ::([xyz]);", "F", "abc", "a\u0392c",
            "#\n::[b]; ::(Lower); ::Latin-Greek; ::Upper(); ::([\u0392]);", "R", "\u0391\u0392\u0393", "\u0391b\u0393",
        };

        for (int i=0; i<DATA.length; i+=4) {
            String id = DATA[i];
            int direction = (DATA[i+1] != null && DATA[i+1].charAt(0) == 'R') ?
                Transliterator.REVERSE : Transliterator.FORWARD;
            String source = DATA[i+2];
            String exp = DATA[i+3];
            boolean expOk = (DATA[i+1] != null);
            Transliterator t = null;
            IllegalArgumentException e = null;
            try {
                if (id.charAt(0) == '#') {
                    t = Transliterator.createFromRules("ID", id, direction);
                } else {
                    t = Transliterator.getInstance(id, direction);
                }
            } catch (IllegalArgumentException ee) {
                e = ee;
            }
            boolean ok = (t != null && e == null);
            if (ok == expOk) {
                logln("Ok: " + id + " => " + t +
                      (e != null ? (", " + e.getMessage()) : ""));
                if (source != null) {
                    expect(t, source, exp);
                }
            } else {
                errln("FAIL: " + id + " => " + t +
                      (e != null ? (", " + e.getMessage()) : ""));
            }
        }
    }

    /**
     * Test new property set syntax
     */
    public void TestPropertySet() {
        expect("a>A; \\p{Lu}>x; \\p{Any}>y;", "abcDEF", "Ayyxxx");
        expect("(.+)>'[' $1 ']';", " a stitch \n in time \r saves 9",
               "[ a stitch ]\n[ in time ]\r[ saves 9]");
    }

    /**
     * Test various failure points of the new 2.0 engine.
     */
    public void TestNewEngine() {
        Transliterator t = Transliterator.getInstance("Latin-Hiragana");
        // Katakana should be untouched
        expect(t, "a\u3042\u30A2", "\u3042\u3042\u30A2");

        if (true) {
            // This test will only work if Transliterator.ROLLBACK is
            // true.  Otherwise, this test will fail, revealing a
            // limitation of global filters in incremental mode.

            Transliterator a =
                Transliterator.createFromRules("a", "a > A;", Transliterator.FORWARD);
            Transliterator A =
                Transliterator.createFromRules("A", "A > b;", Transliterator.FORWARD);

            Transliterator array[] = new Transliterator[] {
                a,
                Transliterator.getInstance("NFD"),
                A };

            t = new CompoundTransliterator(array, new UnicodeSet("[:Ll:]"));

            expect(t, "aAaA", "bAbA");
        }

        expect("$smooth = x; $macron = q; [:^L:] { ([aeiouyAEIOUY] $macron?) } [^aeiouyAEIOUY$smooth$macron] > | $1 $smooth ;",
               "a",
               "ax");

        String gr =
            "$ddot = \u0308 ;" +
            "$lcgvowel = [\u03b1\u03b5\u03b7\u03b9\u03bf\u03c5\u03c9] ;" +
            "$rough = \u0314 ;" +
            "($lcgvowel+ $ddot?) $rough > h | $1 ;" +
            "\u03b1 <> a ;" +
            "$rough <> h ;";

        expect(gr, "\u03B1\u0314", "ha");
    }

    /**
     * Test quantified segment behavior.  We want:
     * ([abc])+ > x $1 x; applied to "cba" produces "xax"
     */
    public void TestQuantifiedSegment() {
        // The normal case
        expect("([abc]+) > x $1 x;", "cba", "xcbax");

        // The tricky case; the quantifier is around the segment
        expect("([abc])+ > x $1 x;", "cba", "xax");

        // Tricky case in reverse direction
        expect("([abc])+ { q > x $1 x;", "cbaq", "cbaxax");

        // Check post-context segment
        expect("{q} ([a-d])+ > '(' $1 ')';", "ddqcba", "dd(a)cba");

        // Test toRule/toPattern for non-quantified segment.
        // Careful with spacing here.
        String r = "([a-c]){q} > x $1 x;";
        Transliterator t = Transliterator.createFromRules("ID", r, Transliterator.FORWARD);
        String rr = t.toRules(true);
        if (!r.equals(rr)) {
            errln("FAIL: \"" + r + "\" x toRules() => \"" + rr + "\"");
        } else {
            logln("Ok: \"" + r + "\" x toRules() => \"" + rr + "\"");
        }

        // Test toRule/toPattern for quantified segment.
        // Careful with spacing here.
        r = "([a-c])+{q} > x $1 x;";
        t = Transliterator.createFromRules("ID", r, Transliterator.FORWARD);
        rr = t.toRules(true);
        if (!r.equals(rr)) {
            errln("FAIL: \"" + r + "\" x toRules() => \"" + rr + "\"");
        } else {
            logln("Ok: \"" + r + "\" x toRules() => \"" + rr + "\"");
        }
    }

    //======================================================================
    // Ram's tests
    //======================================================================
 /* this test performs  test of rules in ISO 15915 */
    public void  TestDevanagariLatinRT(){
        String[]  source = {
            "bh\u0101rata",
            "kra",
            "k\u1E63a",
            "khra",
            "gra",
            "\u1E45ra",
            "cra",
            "chra",
            "j\u00F1a",
            "jhra",
            "\u00F1ra",
            "\u1E6Dya",
            "\u1E6Dhra",
            "\u1E0Dya",
        //"r\u0323ya", // \u095c is not valid in Devanagari
            "\u1E0Dhya",
            "\u1E5Bhra",
            "\u1E47ra",
            "tta",
            "thra",
            "dda",
            "dhra",
            "nna",
            "pra",
            "phra",
            "bra",
            "bhra",
            "mra",
            "\u1E49ra",
        //"l\u0331ra",
            "yra",
            "\u1E8Fra",
        //"l-",
            "vra",
            "\u015Bra",
            "\u1E63ra",
            "sra",
            "hma",
            "\u1E6D\u1E6Da",
            "\u1E6D\u1E6Dha",
            "\u1E6Dh\u1E6Dha",
            "\u1E0D\u1E0Da",
            "\u1E0D\u1E0Dha",
            "\u1E6Dya",
            "\u1E6Dhya",
            "\u1E0Dya",
            "\u1E0Dhya",
            // Not roundtrippable --
            // \u0939\u094d\u094d\u092E  - hma
            // \u0939\u094d\u092E         - hma
            // CharsToUnicodeString("hma"),
            "hya",
            "\u015Br\u0325",
            "\u015Bca",
            "\u0115",
            "san\u0304j\u012Bb s\u0113nagupta",
            "\u0101nand vaddir\u0101ju",
        };
        String[]  expected = {
            "\u092D\u093E\u0930\u0924",    /* bha\u0304rata */
            "\u0915\u094D\u0930",          /* kra         */
            "\u0915\u094D\u0937",          /* ks\u0323a  */
            "\u0916\u094D\u0930",          /* khra        */
            "\u0917\u094D\u0930",          /* gra         */
            "\u0919\u094D\u0930",          /* n\u0307ra  */
            "\u091A\u094D\u0930",          /* cra         */
            "\u091B\u094D\u0930",          /* chra        */
            "\u091C\u094D\u091E",          /* jn\u0303a  */
            "\u091D\u094D\u0930",          /* jhra        */
            "\u091E\u094D\u0930",          /* n\u0303ra  */
            "\u091F\u094D\u092F",          /* t\u0323ya  */
            "\u0920\u094D\u0930",          /* t\u0323hra */
            "\u0921\u094D\u092F",          /* d\u0323ya  */
        //"\u095C\u094D\u092F",          /* r\u0323ya  */ // \u095c is not valid in Devanagari
            "\u0922\u094D\u092F",          /* d\u0323hya */
            "\u0922\u093C\u094D\u0930",    /* r\u0323hra */
            "\u0923\u094D\u0930",          /* n\u0323ra  */
            "\u0924\u094D\u0924",          /* tta         */
            "\u0925\u094D\u0930",          /* thra        */
            "\u0926\u094D\u0926",          /* dda         */
            "\u0927\u094D\u0930",          /* dhra        */
            "\u0928\u094D\u0928",          /* nna         */
            "\u092A\u094D\u0930",          /* pra         */
            "\u092B\u094D\u0930",          /* phra        */
            "\u092C\u094D\u0930",          /* bra         */
            "\u092D\u094D\u0930",          /* bhra        */
            "\u092E\u094D\u0930",          /* mra         */
            "\u0929\u094D\u0930",          /* n\u0331ra  */
        //"\u0934\u094D\u0930",          /* l\u0331ra  */
            "\u092F\u094D\u0930",          /* yra         */
            "\u092F\u093C\u094D\u0930",    /* y\u0307ra  */
        //"l-",
            "\u0935\u094D\u0930",          /* vra         */
            "\u0936\u094D\u0930",          /* s\u0301ra  */
            "\u0937\u094D\u0930",          /* s\u0323ra  */
            "\u0938\u094D\u0930",          /* sra         */
            "\u0939\u094d\u092E",          /* hma         */
            "\u091F\u094D\u091F",          /* t\u0323t\u0323a  */
            "\u091F\u094D\u0920",          /* t\u0323t\u0323ha */
            "\u0920\u094D\u0920",          /* t\u0323ht\u0323ha*/
            "\u0921\u094D\u0921",          /* d\u0323d\u0323a  */
            "\u0921\u094D\u0922",          /* d\u0323d\u0323ha */
            "\u091F\u094D\u092F",          /* t\u0323ya  */
            "\u0920\u094D\u092F",          /* t\u0323hya */
            "\u0921\u094D\u092F",          /* d\u0323ya  */
            "\u0922\u094D\u092F",          /* d\u0323hya */
        // "hma",                         /* hma         */
            "\u0939\u094D\u092F",          /* hya         */
            "\u0936\u0943",                /* s\u0301r\u0325a  */
            "\u0936\u094D\u091A",          /* s\u0301ca  */
            "\u090d",                      /* e\u0306    */
            "\u0938\u0902\u091C\u0940\u092C\u094D \u0938\u0947\u0928\u0917\u0941\u092A\u094D\u0924",
            "\u0906\u0928\u0902\u0926\u094D \u0935\u0926\u094D\u0926\u093F\u0930\u093E\u091C\u0941",
        };

        Transliterator latinToDev=Transliterator.getInstance("Latin-Devanagari", Transliterator.FORWARD );
        Transliterator devToLatin=Transliterator.getInstance("Devanagari-Latin", Transliterator.FORWARD);

        for(int i= 0; i<source.length; i++){
            expect(latinToDev,(source[i]),(expected[i]));
            expect(devToLatin,(expected[i]),(source[i]));
        }

    }
    public void  TestTeluguLatinRT(){
        String[]  source = {
            "raghur\u0101m vi\u015Bvan\u0101dha",                           /* Raghuram Viswanadha    */
            "\u0101nand vaddir\u0101ju",                                    /* Anand Vaddiraju        */
            "r\u0101j\u012Bv ka\u015Barab\u0101da",                         /* Rajeev Kasarabada      */
            "san\u0304j\u012Bv ka\u015Barab\u0101da",                       /* sanjeev kasarabada     */
            "san\u0304j\u012Bb sen'gupta",                                  /* sanjib sengupata       */
            "amar\u0113ndra hanum\u0101nula",                               /* Amarendra hanumanula   */
            "ravi kum\u0101r vi\u015Bvan\u0101dha",                         /* Ravi Kumar Viswanadha  */
            "\u0101ditya kandr\u0113gula",                                  /* Aditya Kandregula      */
            "\u015Br\u012Bdhar ka\u1E47\u1E6Dama\u015Be\u1E6D\u1E6Di",      /* Shridhar Kantamsetty   */
            "m\u0101dhav de\u015Be\u1E6D\u1E6Di"                            /* Madhav Desetty         */
        };

        String[]  expected = {
            "\u0c30\u0c18\u0c41\u0c30\u0c3e\u0c2e\u0c4d \u0c35\u0c3f\u0c36\u0c4d\u0c35\u0c28\u0c3e\u0c27",
            "\u0c06\u0c28\u0c02\u0c26\u0c4d \u0C35\u0C26\u0C4D\u0C26\u0C3F\u0C30\u0C3E\u0C1C\u0C41",
            "\u0c30\u0c3e\u0c1c\u0c40\u0c35\u0c4d \u0c15\u0c36\u0c30\u0c2c\u0c3e\u0c26",
            "\u0c38\u0c02\u0c1c\u0c40\u0c35\u0c4d \u0c15\u0c36\u0c30\u0c2c\u0c3e\u0c26",
            "\u0c38\u0c02\u0c1c\u0c40\u0c2c\u0c4d \u0c38\u0c46\u0c28\u0c4d\u0c17\u0c41\u0c2a\u0c4d\u0c24",
            "\u0c05\u0c2e\u0c30\u0c47\u0c02\u0c26\u0c4d\u0c30 \u0c39\u0c28\u0c41\u0c2e\u0c3e\u0c28\u0c41\u0c32",
            "\u0c30\u0c35\u0c3f \u0c15\u0c41\u0c2e\u0c3e\u0c30\u0c4d \u0c35\u0c3f\u0c36\u0c4d\u0c35\u0c28\u0c3e\u0c27",
            "\u0c06\u0c26\u0c3f\u0c24\u0c4d\u0c2f \u0C15\u0C02\u0C26\u0C4D\u0C30\u0C47\u0C17\u0C41\u0c32",
            "\u0c36\u0c4d\u0c30\u0c40\u0C27\u0C30\u0C4D \u0c15\u0c02\u0c1f\u0c2e\u0c36\u0c46\u0c1f\u0c4d\u0c1f\u0c3f",
            "\u0c2e\u0c3e\u0c27\u0c35\u0c4d \u0c26\u0c46\u0c36\u0c46\u0c1f\u0c4d\u0c1f\u0c3f",
        };


        Transliterator latinToDev=Transliterator.getInstance("Latin-Telugu", Transliterator.FORWARD);
        Transliterator devToLatin=Transliterator.getInstance("Telugu-Latin", Transliterator.FORWARD);

        for(int i= 0; i<source.length; i++){
            expect(latinToDev,(source[i]),(expected[i]));
            expect(devToLatin,(expected[i]),(source[i]));
        }
    }

    public void  TestSanskritLatinRT(){
        int MAX_LEN =15;
        String[]  source = {
            "rmk\u1E63\u0113t",
            "\u015Br\u012Bmad",
            "bhagavadg\u012Bt\u0101",
            "adhy\u0101ya",
            "arjuna",
            "vi\u1E63\u0101da",
            "y\u014Dga",
            "dhr\u0325tar\u0101\u1E63\u1E6Dra",
            "uv\u0101cr\u0325",
            "dharmak\u1E63\u0113tr\u0113",
            "kuruk\u1E63\u0113tr\u0113",
            "samav\u0113t\u0101",
            "yuyutsava\u1E25",
            "m\u0101mak\u0101\u1E25",
        // "p\u0101\u1E47\u1E0Dav\u0101\u015Bcaiva",
            "kimakurvata",
            "san\u0304java",
        };
        String[]  expected = {
            "\u0930\u094D\u092E\u094D\u0915\u094D\u0937\u0947\u0924\u094D",
            "\u0936\u094d\u0930\u0940\u092e\u0926\u094d",
            "\u092d\u0917\u0935\u0926\u094d\u0917\u0940\u0924\u093e",
            "\u0905\u0927\u094d\u092f\u093e\u092f",
            "\u0905\u0930\u094d\u091c\u0941\u0928",
            "\u0935\u093f\u0937\u093e\u0926",
            "\u092f\u094b\u0917",
            "\u0927\u0943\u0924\u0930\u093e\u0937\u094d\u091f\u094d\u0930",
            "\u0909\u0935\u093E\u091A\u0943",
            "\u0927\u0930\u094d\u092e\u0915\u094d\u0937\u0947\u0924\u094d\u0930\u0947",
            "\u0915\u0941\u0930\u0941\u0915\u094d\u0937\u0947\u0924\u094d\u0930\u0947",
            "\u0938\u092e\u0935\u0947\u0924\u093e",
            "\u092f\u0941\u092f\u0941\u0924\u094d\u0938\u0935\u0903",
            "\u092e\u093e\u092e\u0915\u093e\u0903",
        //"\u092a\u093e\u0923\u094d\u0921\u0935\u093e\u0936\u094d\u091a\u0948\u0935",
            "\u0915\u093f\u092e\u0915\u0941\u0930\u094d\u0935\u0924",
            "\u0938\u0902\u091c\u0935",
        };

        Transliterator latinToDev=Transliterator.getInstance("Latin-Devanagari", Transliterator.FORWARD);
        Transliterator devToLatin=Transliterator.getInstance("Devanagari-Latin", Transliterator.FORWARD);
        for(int i= 0; i<MAX_LEN; i++){
            expect(latinToDev,(source[i]),(expected[i]));
            expect(devToLatin,(expected[i]),(source[i]));
        }
    }

    public void  TestCompoundLatinRT(){
        int MAX_LEN =15;
        String[]  source = {
            "rmk\u1E63\u0113t",
            "\u015Br\u012Bmad",
            "bhagavadg\u012Bt\u0101",
            "adhy\u0101ya",
            "arjuna",
            "vi\u1E63\u0101da",
            "y\u014Dga",
            "dhr\u0325tar\u0101\u1E63\u1E6Dra",
            "uv\u0101cr\u0325",
            "dharmak\u1E63\u0113tr\u0113",
            "kuruk\u1E63\u0113tr\u0113",
            "samav\u0113t\u0101",
            "yuyutsava\u1E25",
            "m\u0101mak\u0101\u1E25",
        // "p\u0101\u1E47\u1E0Dav\u0101\u015Bcaiva",
            "kimakurvata",
            "san\u0304java"
        };
        String[]  expected = {
            "\u0930\u094D\u092E\u094D\u0915\u094D\u0937\u0947\u0924\u094D",
            "\u0936\u094d\u0930\u0940\u092e\u0926\u094d",
            "\u092d\u0917\u0935\u0926\u094d\u0917\u0940\u0924\u093e",
            "\u0905\u0927\u094d\u092f\u093e\u092f",
            "\u0905\u0930\u094d\u091c\u0941\u0928",
            "\u0935\u093f\u0937\u093e\u0926",
            "\u092f\u094b\u0917",
            "\u0927\u0943\u0924\u0930\u093e\u0937\u094d\u091f\u094d\u0930",
            "\u0909\u0935\u093E\u091A\u0943",
            "\u0927\u0930\u094d\u092e\u0915\u094d\u0937\u0947\u0924\u094d\u0930\u0947",
            "\u0915\u0941\u0930\u0941\u0915\u094d\u0937\u0947\u0924\u094d\u0930\u0947",
            "\u0938\u092e\u0935\u0947\u0924\u093e",
            "\u092f\u0941\u092f\u0941\u0924\u094d\u0938\u0935\u0903",
            "\u092e\u093e\u092e\u0915\u093e\u0903",
        //  "\u092a\u093e\u0923\u094d\u0921\u0935\u093e\u0936\u094d\u091a\u0948\u0935",
            "\u0915\u093f\u092e\u0915\u0941\u0930\u094d\u0935\u0924",
            "\u0938\u0902\u091c\u0935"
        };

        Transliterator latinToDevToLatin=Transliterator.getInstance("Latin-Devanagari;Devanagari-Latin", Transliterator.FORWARD);
        Transliterator devToLatinToDev=Transliterator.getInstance("Devanagari-Latin;Latin-Devanagari", Transliterator.FORWARD);
        for(int i= 0; i<MAX_LEN; i++){
            expect(latinToDevToLatin,(source[i]),(source[i]));
            expect(devToLatinToDev,(expected[i]),(expected[i]));
        }
    }
    /**
     * Test Gurmukhi-Devanagari Tippi and Bindi
     */
    public void TestGurmukhiDevanagari(){
        // the rule says:
        // (\u0902) (when preceded by vowel)      --->  (\u0A02)
        // (\u0902) (when preceded by consonant)  --->  (\u0A70)

        UnicodeSet vowel =new UnicodeSet("[\u0905-\u090A \u090F\u0910\u0913\u0914 \u093e-\u0942\u0947\u0948\u094B\u094C\u094D]");
        UnicodeSet non_vowel =new UnicodeSet("[\u0915-\u0928\u092A-\u0930]");

        UnicodeSetIterator vIter = new UnicodeSetIterator(vowel);
        UnicodeSetIterator nvIter = new UnicodeSetIterator(non_vowel);
        Transliterator trans = Transliterator.getInstance("Devanagari-Gurmukhi");
        StringBuffer src = new StringBuffer(" \u0902");
        StringBuffer expect = new StringBuffer(" \u0A02");
        while(vIter.next()){
            src.setCharAt(0,(char) vIter.codepoint);
            expect.setCharAt(0,(char) (vIter.codepoint+0x0100));
            expect(trans,src.toString(),expect.toString());
        }

        expect.setCharAt(1,'\u0A70');
        while(nvIter.next()){
            //src.setCharAt(0,(char) nvIter.codepoint);
            src.setCharAt(0,(char)nvIter.codepoint);
            expect.setCharAt(0,(char) (nvIter.codepoint+0x0100));
            expect(trans,src.toString(),expect.toString());
        }
    }
    /**
     * Test instantiation from a locale.
     */
    public void TestLocaleInstantiation() {
        Transliterator t = Transliterator.getInstance("ru_RU-Latin");
        expect(t, "\u0430", "a");

        t = Transliterator.getInstance("en-el");
        expect(t, "a", "\u03B1");
    }

    /**
     * Test title case handling of accent (should ignore accents)
     */
    public void TestTitleAccents() {
        Transliterator t = Transliterator.getInstance("Title");
        expect(t, "a\u0300b can't abe", "A\u0300b Can't Abe");
    }

    /**
     * Basic test of a locale resource based rule.
     */
    public void TestLocaleResource() {
        String DATA[] = {
            // id                    from             to
            "Latin-Greek/UNGEGN",    "b",             "\u03bc\u03c0",
            "Latin-el",              "b",             "\u03bc\u03c0",
            "Latin-Greek",           "b",             "\u03B2",
            "Greek-Latin/UNGEGN",    "\u03B2",        "v",
            "el-Latin",              "\u03B2",        "v",
            "Greek-Latin",           "\u03B2",        "b",
        };
        for (int i=0; i<DATA.length; i+=3) {
            Transliterator t = Transliterator.getInstance(DATA[i]);
            expect(t, DATA[i+1], DATA[i+2]);
        }
    }

    /**
     * Make sure parse errors reference the right line.
     */
    public void TestParseError() {
        String rule =
            "a > b;\n" +
            "# more stuff\n" +
            "d << b;";
        try {
            Transliterator t = Transliterator.createFromRules("ID", rule, Transliterator.FORWARD);
            if(t!=null){
                errln("FAIL: Did not get expected exception");
            }
        } catch (IllegalArgumentException e) {
            String err = e.getMessage();
            if (err.indexOf("d << b") >= 0) {
                logln("Ok: " + err);
            } else {
                errln("FAIL: " + err);
            }
            return;
        }
        errln("FAIL: no syntax error");
    }

    /**
     * Make sure sets on output are disallowed.
     */
    public void TestOutputSet() {
        String rule = "$set = [a-cm-n]; b > $set;";
        Transliterator t = null;
        try {
            t = Transliterator.createFromRules("ID", rule, Transliterator.FORWARD);
            if(t!=null){
                errln("FAIL: Did not get the expected exception");
            }
        } catch (IllegalArgumentException e) {
            logln("Ok: " + e.getMessage());
            return;
        }
        errln("FAIL: No syntax error");
    }

    /**
     * Test the use variable range pragma, making sure that use of
     * variable range characters is detected and flagged as an error.
     */
    public void TestVariableRange() {
        String rule = "use variable range 0x70 0x72; a > A; b > B; q > Q;";
        try {
            Transliterator t =
                Transliterator.createFromRules("ID", rule, Transliterator.FORWARD);
            if(t!=null){
                errln("FAIL: Did not get the expected exception");
            }
        } catch (IllegalArgumentException e) {
            logln("Ok: " + e.getMessage());
            return;
        }
        errln("FAIL: No syntax error");
    }

    /**
     * Test invalid post context error handling
     */
    public void TestInvalidPostContext() {
        try {
            Transliterator t =
                Transliterator.createFromRules("ID", "a}b{c>d;", Transliterator.FORWARD);
            if(t!=null){
                errln("FAIL: Did not get the expected exception");
            }
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg.indexOf("a}b{c") >= 0) {
                logln("Ok: " + msg);
            } else {
                errln("FAIL: " + msg);
            }
            return;
        }
        errln("FAIL: No syntax error");
    }

    /**
     * Test ID form variants
     */
    public void TestIDForms() {
        String DATA[] = {
            "NFC", null, "NFD",
            "nfd", null, "NFC", // make sure case is ignored
            "Any-NFKD", null, "Any-NFKC",
            "Null", null, "Null",
            "-nfkc", "nfkc", "NFKD",
            "-nfkc/", "nfkc", "NFKD",
            "Latin-Greek/UNGEGN", null, "Greek-Latin/UNGEGN",
            "Greek/UNGEGN-Latin", "Greek-Latin/UNGEGN", "Latin-Greek/UNGEGN",
            "Bengali-Devanagari/", "Bengali-Devanagari", "Devanagari-Bengali",
            "Source-", null, null,
            "Source/Variant-", null, null,
            "Source-/Variant", null, null,
            "/Variant", null, null,
            "/Variant-", null, null,
            "-/Variant", null, null,
            "-/", null, null,
            "-", null, null,
            "/", null, null,
        };

        for (int i=0; i<DATA.length; i+=3) {
            String ID = DATA[i];
            String expID = DATA[i+1];
            String expInvID = DATA[i+2];
            boolean expValid = (expInvID != null);
            if (expID == null) {
                expID = ID;
            }
            try {
                Transliterator t =
                    Transliterator.getInstance(ID);
                Transliterator u = t.getInverse();
                if (t.getID().equals(expID) &&
                    u.getID().equals(expInvID)) {
                    logln("Ok: " + ID + ".getInverse() => " + expInvID);
                } else {
                    errln("FAIL: getInstance(" + ID + ") => " +
                          t.getID() + " x getInverse() => " + u.getID() +
                          ", expected " + expInvID);
                }
            } catch (IllegalArgumentException e) {
                if (!expValid) {
                    logln("Ok: getInstance(" + ID + ") => " + e.getMessage());
                } else {
                    errln("FAIL: getInstance(" + ID + ") => " + e.getMessage());
                }
            }
        }
    }

    void checkRules(String label, Transliterator t2, String testRulesForward) {
        String rules2 = t2.toRules(true);
        //rules2 = TestUtility.replaceAll(rules2, new UnicodeSet("[' '\n\r]"), "");
        rules2 = TestUtility.replace(rules2, " ", "");
        rules2 = TestUtility.replace(rules2, "\n", "");
        rules2 = TestUtility.replace(rules2, "\r", "");
        testRulesForward = TestUtility.replace(testRulesForward, " ", "");

        if (!rules2.equals(testRulesForward)) {
            errln(label);
            logln("GENERATED RULES: " + rules2);
            logln("SHOULD BE:       " + testRulesForward);
        }
    }

    /**
     * Mark's toRules test.
     */
    public void TestToRulesMark() {

        String testRules =
            "::[[:Latin:][:Mark:]];"
            + "::NFKD (NFC);"
            + "::Lower (Lower);"
            + "a <> \\u03B1;" // alpha
            + "::NFKC (NFD);"
            + "::Upper (Lower);"
            + "::Lower ();"
            + "::([[:Greek:][:Mark:]]);"
            ;
        String testRulesForward =
            "::[[:Latin:][:Mark:]];"
            + "::NFKD(NFC);"
            + "::Lower(Lower);"
            + "a > \\u03B1;"
            + "::NFKC(NFD);"
            + "::Upper (Lower);"
            + "::Lower ();"
            ;
        String testRulesBackward =
            "::[[:Greek:][:Mark:]];"
            + "::Lower (Upper);"
            + "::NFD(NFKC);"
            + "\\u03B1 > a;"
            + "::Lower(Lower);"
            + "::NFC(NFKD);"
            ;
        String source = "\u00E1"; // a-acute
        String target = "\u03AC"; // alpha-acute

        Transliterator t2 = Transliterator.createFromRules("source-target", testRules, Transliterator.FORWARD);
        Transliterator t3 = Transliterator.createFromRules("target-source", testRules, Transliterator.REVERSE);

        expect(t2, source, target);
        expect(t3, target, source);

        checkRules("Failed toRules FORWARD", t2, testRulesForward);
        checkRules("Failed toRules BACKWARD", t3, testRulesBackward);
    }

    /**
     * Test Escape and Unescape transliterators.
     */
    public void TestEscape() {
        expect(Transliterator.getInstance("Hex-Any"),
               "\\x{40}\\U00000031&#x32;&#81;",
               "@12Q");
        expect(Transliterator.getInstance("Any-Hex/C"),
               CharsToUnicodeString("A\\U0010BEEF\\uFEED"),
               "\\u0041\\U0010BEEF\\uFEED");
        expect(Transliterator.getInstance("Any-Hex/Java"),
               CharsToUnicodeString("A\\U0010BEEF\\uFEED"),
               "\\u0041\\uDBEF\\uDEEF\\uFEED");
        expect(Transliterator.getInstance("Any-Hex/Perl"),
               CharsToUnicodeString("A\\U0010BEEF\\uFEED"),
               "\\x{41}\\x{10BEEF}\\x{FEED}");
    }

    /**
     * Make sure display names of variants look reasonable.
     */
    public void TestDisplayName() {
        String DATA[] = {
            // ID, forward name, reverse name
            // Update the text as necessary -- the important thing is
            // not the text itself, but how various cases are handled.

            // Basic test
            "Any-Hex", "Any to Hex Escape", "Hex Escape to Any",

            // Variants
            "Any-Hex/Perl", "Any to Hex Escape/Perl", "Hex Escape to Any/Perl",

            // Target-only IDs
            "NFC", "Any to NFC", "Any to NFD",
        };

        Locale US = Locale.US;

        for (int i=0; i<DATA.length; i+=3) {
            String name = Transliterator.getDisplayName(DATA[i], US);
            if (!name.equals(DATA[i+1])) {
                errln("FAIL: " + DATA[i] + ".getDisplayName() => " +
                      name + ", expected " + DATA[i+1]);
            } else {
                logln("Ok: " + DATA[i] + ".getDisplayName() => " + name);
            }
            Transliterator t = Transliterator.getInstance(DATA[i], Transliterator.REVERSE);
            name = Transliterator.getDisplayName(t.getID(), US);
            if (!name.equals(DATA[i+2])) {
                errln("FAIL: " + t.getID() + ".getDisplayName() => " +
                      name + ", expected " + DATA[i+2]);
            } else {
                logln("Ok: " + t.getID() + ".getDisplayName() => " + name);
            }
        }
    }

    /**
     * Test anchor masking
     */
    public void TestAnchorMasking() {
        String rule = "^a > Q; a > q;";
        try {
            Transliterator t = Transliterator.createFromRules("ID", rule, Transliterator.FORWARD);
            if(t==null){
                errln("FAIL: Did not get the expected exception");
            }
        } catch (IllegalArgumentException e) {
            errln("FAIL: " + rule + " => " + e);
        }
    }

    /**
     * This test is not in trnstst.cpp. This test has been moved from com/ibm/icu/dev/test/lang/TestUScript.java
     * during ICU4J modularization to remove dependency of tests on Transliterator.
     */
     public void TestScriptAllCodepoints(){
            int code;
             String oldId="";
             String oldAbbrId="";
            for( int i =0; i <= 0x10ffff; i++){
                code =UScript.INVALID_CODE;
                code = UScript.getScript(i);
                if(code==UScript.INVALID_CODE){
                    errln("UScript.getScript for codepoint 0x"+ hex(i)+" failed");
                }
                 String id =UScript.getName(code);
                 String abbr = UScript.getShortName(code);
                 String newId ="[:"+id+":];NFD";
                 String newAbbrId ="[:"+abbr+":];NFD";
                 if(!oldId.equals(newId)){
                     try{
                         Transliterator t = Transliterator.getInstance(newId);
                         if(t==null){
                              errln("Failed to create transliterator for "+hex(i)+
                              " script code: " +id);
                         }
                     }catch(Exception e){
                         errln("Failed to create transliterator for "+hex(i)
                                 +" script code: " +id
                                 + " Exception: "+e.getMessage());
                     }
                 }
                 oldId = newId;
                 if(!oldAbbrId.equals(newAbbrId)){
                     try{
                         Transliterator t = Transliterator.getInstance(newAbbrId);
                         if(t==null){
                              errln("Failed to create transliterator for "+hex(i)+
                              " script code: " +abbr);
                         }
                     }catch(Exception e){
                         errln("Failed to create transliterator for "+hex(i)
                                 +" script code: " +abbr
                                 + " Exception: "+e.getMessage());
                     }
                 }
                 oldAbbrId = newAbbrId;
            }
    }


    static final String[][] registerRules = {
        {"Any-Dev1", "x > X; y > Y;"},
        {"Any-Dev2", "XY > Z"},
        {"Greek-Latin/FAKE",
            "[^[:L:][:M:]] { \u03bc\u03c0 > b ; "+
            "\u03bc\u03c0 } [^[:L:][:M:]] > b ; "+
            "[^[:L:][:M:]] { [\u039c\u03bc][\u03a0\u03c0] > B ; "+
            "[\u039c\u03bc][\u03a0\u03c0] } [^[:L:][:M:]] > B ;"
            },
    };

    static final String DESERET_DEE = UTF16.valueOf(0x10414);
    static final String DESERET_dee = UTF16.valueOf(0x1043C);

    static final String[][] testCases = {

        // NORMALIZATION
        // should add more test cases
        {"NFD" , "a\u0300 \u00E0 \u1100\u1161 \uFF76\uFF9E\u03D3"},
        {"NFC" , "a\u0300 \u00E0 \u1100\u1161 \uFF76\uFF9E\u03D3"},
        {"NFKD", "a\u0300 \u00E0 \u1100\u1161 \uFF76\uFF9E\u03D3"},
        {"NFKC", "a\u0300 \u00E0 \u1100\u1161 \uFF76\uFF9E\u03D3"},

        // mp -> b BUG
        {"Greek-Latin/UNGEGN", "(\u03BC\u03C0)", "(b)"},
        {"Greek-Latin/FAKE", "(\u03BC\u03C0)", "(b)"},

        // check for devanagari bug
        {"nfd;Dev1;Dev2;nfc", "xy", "Z"},

        // ff, i, dotless-i, I, dotted-I, LJLjlj deseret deeDEE
        {"Title", "ab'cD ffi\u0131I\u0130 \u01C7\u01C8\u01C9 " + DESERET_dee + DESERET_DEE,
                  "Ab'cd Ffi\u0131ii\u0307 \u01C8\u01C9\u01C9 " + DESERET_DEE + DESERET_dee},
        //TODO: enable this test once Titlecase works right
        //{"Title", "\uFB00i\u0131I\u0130 \u01C7\u01C8\u01C9 " + DESERET_dee + DESERET_DEE,
        //          "Ffi\u0131ii \u01C8\u01C9\u01C9 " + DESERET_DEE + DESERET_dee},

        {"Upper", "ab'cD \uFB00i\u0131I\u0130 \u01C7\u01C8\u01C9 " + DESERET_dee + DESERET_DEE,
                  "AB'CD FFIII\u0130 \u01C7\u01C7\u01C7 " + DESERET_DEE + DESERET_DEE},
        {"Lower", "ab'cD \uFB00i\u0131I\u0130 \u01C7\u01C8\u01C9 " + DESERET_dee + DESERET_DEE,
                  "ab'cd \uFB00i\u0131ii\u0307 \u01C9\u01C9\u01C9 " + DESERET_dee + DESERET_dee},

        {"Upper", "ab'cD \uFB00i\u0131I\u0130 \u01C7\u01C8\u01C9 " + DESERET_dee + DESERET_DEE},
        {"Lower", "ab'cD \uFB00i\u0131I\u0130 \u01C7\u01C8\u01C9 " + DESERET_dee + DESERET_DEE},

         // FORMS OF S
        {"Greek-Latin/UNGEGN", "\u03C3 \u03C3\u03C2 \u03C2\u03C3", "s ss s\u0331s\u0331"},
        {"Latin-Greek/UNGEGN", "s ss s\u0331s\u0331", "\u03C3 \u03C3\u03C2 \u03C2\u03C3"},
        {"Greek-Latin", "\u03C3 \u03C3\u03C2 \u03C2\u03C3", "s ss s\u0331s\u0331"},
        {"Latin-Greek", "s ss s\u0331s\u0331", "\u03C3 \u03C3\u03C2 \u03C2\u03C3"},

        // Tatiana bug
        // Upper: TAT\u02B9\u00C2NA
        // Lower: tat\u02B9\u00E2na
        // Title: Tat\u02B9\u00E2na
        {"Upper", "tat\u02B9\u00E2na", "TAT\u02B9\u00C2NA"},
        {"Lower", "TAT\u02B9\u00C2NA", "tat\u02B9\u00E2na"},
        {"Title", "tat\u02B9\u00E2na", "Tat\u02B9\u00E2na"},
    };

    public void TestSpecialCases() {

        for (int i = 0; i < registerRules.length; ++i) {
            Transliterator t = Transliterator.createFromRules(registerRules[i][0],
                registerRules[i][1], Transliterator.FORWARD);
            DummyFactory.add(registerRules[i][0], t);
        }
        for (int i = 0; i < testCases.length; ++i) {
            String name = testCases[i][0];
            Transliterator t = Transliterator.getInstance(name);
            String id = t.getID();
            String source = testCases[i][1];
            String target = null;

            // Automatic generation of targets, to make it simpler to add test cases (and more fail-safe)

            if (testCases[i].length > 2)    target = testCases[i][2];
            else if (id.equalsIgnoreCase("NFD"))    target = com.ibm.icu.text.Normalizer.normalize(source, com.ibm.icu.text.Normalizer.NFD);
            else if (id.equalsIgnoreCase("NFC"))    target = com.ibm.icu.text.Normalizer.normalize(source, com.ibm.icu.text.Normalizer.NFC);
            else if (id.equalsIgnoreCase("NFKD"))   target = com.ibm.icu.text.Normalizer.normalize(source, com.ibm.icu.text.Normalizer.NFKD);
            else if (id.equalsIgnoreCase("NFKC"))   target = com.ibm.icu.text.Normalizer.normalize(source, com.ibm.icu.text.Normalizer.NFKC);
            else if (id.equalsIgnoreCase("Lower"))  target = UCharacter.toLowerCase(Locale.US, source);
            else if (id.equalsIgnoreCase("Upper"))  target = UCharacter.toUpperCase(Locale.US, source);

            expect(t, source, target);
        }
        for (int i = 0; i < registerRules.length; ++i) {
            Transliterator.unregister(registerRules[i][0]);
        }
    }

    // seems like there should be an easier way to just register an instance of a transliterator

    static class DummyFactory implements Transliterator.Factory {
        static DummyFactory singleton = new DummyFactory();
        static HashMap m = new HashMap();

        // Since Transliterators are immutable, we don't have to clone on set & get
        static void add(String ID, Transliterator t) {
            m.put(ID, t);
            //System.out.println("Registering: " + ID + ", " + t.toRules(true));
            Transliterator.registerFactory(ID, singleton);
        }
        public Transliterator getInstance(String ID) {
            return (Transliterator) m.get(ID);
        }
    }

    public void TestSurrogateCasing () {
        // check that casing handles surrogates
        // titlecase is currently defective
        int dee = UTF16.charAt(DESERET_dee,0);
        int DEE = UCharacter.toTitleCase(dee);
        if (!UTF16.valueOf(DEE).equals(DESERET_DEE)) {
            errln("Fails titlecase of surrogates" + Integer.toString(dee,16) + ", " + Integer.toString(DEE,16));
        }

        if (!UCharacter.toUpperCase(DESERET_dee + DESERET_DEE).equals(DESERET_DEE + DESERET_DEE)) {
            errln("Fails uppercase of surrogates");
        }

        if (!UCharacter.toLowerCase(DESERET_dee + DESERET_DEE).equals(DESERET_dee + DESERET_dee)) {
            errln("Fails lowercase of surrogates");
        }
    }

    // Check to see that incremental gets at least part way through a reasonable string.

    public void TestIncrementalProgress() {
        String latinTest = "The Quick Brown Fox.";
        String devaTest = Transliterator.getInstance("Latin-Devanagari").transliterate(latinTest);
        String kataTest = Transliterator.getInstance("Latin-Katakana").transliterate(latinTest);
        String[][] tests = {
            {"Any", latinTest},
            {"Latin", latinTest},
            {"Halfwidth", latinTest},
            {"Devanagari", devaTest},
            {"Katakana", kataTest},
        };

        Enumeration sources = Transliterator.getAvailableSources();
        while(sources.hasMoreElements()) {
            String source = (String) sources.nextElement();
            String test = findMatch(source, tests);
            if (test == null) {
                logln("Skipping " + source + "-X");
                continue;
            }
            Enumeration targets = Transliterator.getAvailableTargets(source);
            while(targets.hasMoreElements()) {
                String target = (String) targets.nextElement();
                Enumeration variants = Transliterator.getAvailableVariants(source, target);
                while(variants.hasMoreElements()) {
                    String variant = (String) variants.nextElement();
                    String id = source + "-" + target + "/" + variant;
                    logln("id: " + id);

                    String filter = getFilter();
                    if (filter != null && id.indexOf(filter) < 0) continue;

                    Transliterator t = Transliterator.getInstance(id);
                    CheckIncrementalAux(t, test);

                    String rev = t.transliterate(test);
                    Transliterator inv = t.getInverse();
                    CheckIncrementalAux(inv, rev);
                }
            }
        }
    }

    public String findMatch (String source, String[][] pairs) {
        for (int i = 0; i < pairs.length; ++i) {
            if (source.equalsIgnoreCase(pairs[i][0])) return pairs[i][1];
        }
        return null;
    }

    public void CheckIncrementalAux(Transliterator t, String input) {

        Replaceable test = new ReplaceableString(input);
        Transliterator.Position pos = new Transliterator.Position(0, test.length(), 0, test.length());
        t.transliterate(test, pos);
        boolean gotError = false;

        // we have a few special cases. Any-Remove (pos.start = 0, but also = limit) and U+XXXXX?X?

        if (pos.start == 0 && pos.limit != 0 && !t.getID().equals("Hex-Any/Unicode")) {
            errln("No Progress, " + t.getID() + ": " + UtilityExtensions.formatInput(test, pos));
            gotError = true;
        } else {
            logln("PASS Progress, " + t.getID() + ": " + UtilityExtensions.formatInput(test, pos));
        }
        t.finishTransliteration(test, pos);
        if (pos.start != pos.limit) {
            errln("Incomplete, " + t.getID() + ":  " + UtilityExtensions.formatInput(test, pos));
            gotError = true;
        }
        if(!gotError){
            //errln("FAIL: Did not get expected error");
        }
    }

    public void TestFunction() {
        // Careful with spacing and ';' here:  Phrase this exactly
        // as toRules() is going to return it.  If toRules() changes
        // with regard to spacing or ';', then adjust this string.
        String rule =
            "([:Lu:]) > $1 '(' &Lower( $1 ) '=' &Hex( &Any-Lower( $1 ) ) ')';";

        Transliterator t = Transliterator.createFromRules("Test", rule, Transliterator.FORWARD);
        if (t == null) {
            errln("FAIL: createFromRules failed");
            return;
        }

        String r = t.toRules(true);
        if (r.equals(rule)) {
            logln("OK: toRules() => " + r);
        } else {
            errln("FAIL: toRules() => " + r +
                  ", expected " + rule);
        }

        expect(t, "The Quick Brown Fox",
               "T(t=\\u0074)he Q(q=\\u0071)uick B(b=\\u0062)rown F(f=\\u0066)ox");
        rule =
            "([^\\ -\\u007F]) > &Hex/Unicode( $1 ) ' ' &Name( $1 ) ;";

        t = Transliterator.createFromRules("Test", rule, Transliterator.FORWARD);
        if (t == null) {
            errln("FAIL: createFromRules failed");
            return;
        }

        r = t.toRules(true);
        if (r.equals(rule)) {
            logln("OK: toRules() => " + r);
        } else {
            errln("FAIL: toRules() => " + r +
                  ", expected " + rule);
        }

        expect(t, "\u0301",
               "U+0301 \\N{COMBINING ACUTE ACCENT}");
    }

    public void TestInvalidBackRef() {
        String rule =  ". > $1;";
        String rule2 ="(.) <> &hex/unicode($1) &name($1); . > $1; [{}] >\u0020;";
        try {
            Transliterator t = Transliterator.createFromRules("Test", rule, Transliterator.FORWARD);
            if (t != null) {
                errln("FAIL: createFromRules should have returned NULL");
            }
            errln("FAIL: Ok: . > $1; => no error");
            Transliterator t2= Transliterator.createFromRules("Test2", rule2, Transliterator.FORWARD);
            if (t2 != null) {
                errln("FAIL: createFromRules should have returned NULL");
            }
            errln("FAIL: Ok: . > $1; => no error");
        } catch (IllegalArgumentException e) {
             logln("Ok: . > $1; => " + e.getMessage());
        }
    }

    public void TestMulticharStringSet() {
        // Basic testing
        String rule =
            "       [{aa}]       > x;" +
            "         a          > y;" +
            "       [b{bc}]      > z;" +
            "[{gd}] { e          > q;" +
            "         e } [{fg}] > r;" ;

        Transliterator t = Transliterator.createFromRules("Test", rule, Transliterator.FORWARD);
        if (t == null) {
            errln("FAIL: createFromRules failed");
            return;
        }

        expect(t, "a aa ab bc d gd de gde gdefg ddefg",
                  "y x yz z d gd de gdq gdqfg ddrfg");

        // Overlapped string test.  Make sure that when multiple
        // strings can match that the longest one is matched.
        rule =
            "    [a {ab} {abc}]    > x;" +
            "           b          > y;" +
            "           c          > z;" +
            " q [t {st} {rst}] { e > p;" ;

        t = Transliterator.createFromRules("Test", rule, Transliterator.FORWARD);
        if (t == null) {
            errln("FAIL: createFromRules failed");
            return;
        }

        expect(t, "a ab abc qte qste qrste",
                  "x x x qtp qstp qrstp");
    }

    /**
     * Test that user-registered transliterators can be used under function
     * syntax.
     */
    public void TestUserFunction() {
        Transliterator t;

        // There's no need to register inverses if we don't use them
        TestUserFunctionFactory.add("Any-gif",
            Transliterator.createFromRules("gif",
                "'\\'u(..)(..) > '<img src=\"http://www.unicode.org/gifs/24/' $1 '/U' $1$2 '.gif\">';",
                Transliterator.FORWARD));
        //TestUserFunctionFactory.add("gif-Any", Transliterator.getInstance("Any-Null"));

        TestUserFunctionFactory.add("Any-RemoveCurly",
            Transliterator.createFromRules("RemoveCurly", "[\\{\\}] > ; \\\\N > ;", Transliterator.FORWARD));
        //TestUserFunctionFactory.add("RemoveCurly-Any", Transliterator.getInstance("Any-Null"));

        logln("Trying &hex");
        t = Transliterator.createFromRules("hex2", "(.) > &hex($1);", Transliterator.FORWARD);
        logln("Registering");
        TestUserFunctionFactory.add("Any-hex2", t);
        t = Transliterator.getInstance("Any-hex2");
        expect(t, "abc", "\\u0061\\u0062\\u0063");

        logln("Trying &gif");
        t = Transliterator.createFromRules("gif2", "(.) > &Gif(&Hex2($1));", Transliterator.FORWARD);
        logln("Registering");
        TestUserFunctionFactory.add("Any-gif2", t);
        t = Transliterator.getInstance("Any-gif2");
        expect(t, "ab", "<img src=\"http://www.unicode.org/gifs/24/00/U0061.gif\">" +
               "<img src=\"http://www.unicode.org/gifs/24/00/U0062.gif\">");

        // Test that filters are allowed after &
        t = Transliterator.createFromRules("test",
                "(.) > &Hex($1) ' ' &Any-RemoveCurly(&Name($1)) ' ';", Transliterator.FORWARD);
        expect(t, "abc", "\\u0061 LATIN SMALL LETTER A \\u0062 LATIN SMALL LETTER B \\u0063 LATIN SMALL LETTER C ");

        // Unregister our test stuff
        TestUserFunctionFactory.unregister();
    }

    static class TestUserFunctionFactory implements Transliterator.Factory {
        static TestUserFunctionFactory singleton = new TestUserFunctionFactory();
        static HashMap m = new HashMap();

        static void add(String ID, Transliterator t) {
            m.put(new CaseInsensitiveString(ID), t);
            Transliterator.registerFactory(ID, singleton);
        }

        public Transliterator getInstance(String ID) {
            return (Transliterator) m.get(new CaseInsensitiveString(ID));
        }

        static void unregister() {
            Iterator ids = m.keySet().iterator();
            while (ids.hasNext()) {
                CaseInsensitiveString id = (CaseInsensitiveString) ids.next();
                Transliterator.unregister(id.getString());
                ids.remove(); // removes pair from m
            }
        }
    }

    /**
     * Test the Any-X transliterators.
     */
    public void TestAnyX() {
        Transliterator anyLatin =
            Transliterator.getInstance("Any-Latin", Transliterator.FORWARD);

        expect(anyLatin,
               "greek:\u03B1\u03B2\u03BA\u0391\u0392\u039A hiragana:\u3042\u3076\u304F cyrillic:\u0430\u0431\u0446",
               "greek:abkABK hiragana:abuku cyrillic:abc");
    }

    /**
     * Test the source and target set API.  These are only implemented
     * for RBT and CompoundTransliterator at this time.
     */
    public void TestSourceTargetSet() {
        // Rules
        String r =
            "a > b; " +
            "r [x{lu}] > q;";

        // Expected source
        UnicodeSet expSrc = new UnicodeSet("[arx{lu}]");

        // Expected target
        UnicodeSet expTrg = new UnicodeSet("[bq]");

        Transliterator t = Transliterator.createFromRules("test", r, Transliterator.FORWARD);
        UnicodeSet src = t.getSourceSet();
        UnicodeSet trg = t.getTargetSet();

        if (src.equals(expSrc) && trg.equals(expTrg)) {
            logln("Ok: " + r + " => source = " + src.toPattern(true) +
                  ", target = " + trg.toPattern(true));
        } else {
            errln("FAIL: " + r + " => source = " + src.toPattern(true) +
                  ", expected " + expSrc.toPattern(true) +
                  "; target = " + trg.toPattern(true) +
                  ", expected " + expTrg.toPattern(true));
        }
    }

    /**
     * Test handling of rule whitespace, for both RBT and UnicodeSet.
     */
    public void TestRuleWhitespace() {
        // Rules
        String r = "a > \u200E b;";

        Transliterator t = Transliterator.createFromRules("test", r, Transliterator.FORWARD);

        expect(t, "a", "b");

        // UnicodeSet
        UnicodeSet set = new UnicodeSet("[a \u200E]");

        if (set.contains(0x200E)) {
            errln("FAIL: U+200E not being ignored by UnicodeSet");
        }
    }

    public void TestAlternateSyntax() {
        // U+2206 == &
        // U+2190 == <
        // U+2192 == >
        // U+2194 == <>
        expect("a \u2192 x; b \u2190 y; c \u2194 z",
               "abc",
               "xbz");
        expect("([:^ASCII:]) \u2192 \u2206Name($1);",
               "<=\u2190; >=\u2192; <>=\u2194; &=\u2206",
               "<=\\N{LEFTWARDS ARROW}; >=\\N{RIGHTWARDS ARROW}; <>=\\N{LEFT RIGHT ARROW}; &=\\N{INCREMENT}");
    }

    public void TestPositionAPI() {
        Transliterator.Position a = new Transliterator.Position(3,5,7,11);
        Transliterator.Position b = new Transliterator.Position(a);
        Transliterator.Position c = new Transliterator.Position();
        c.set(a);
        // Call the toString() API:
        if (a.equals(b) && a.equals(c)) {
            logln("Ok: " + a + " == " + b + " == " + c);
        } else {
            errln("FAIL: " + a + " != " + b + " != " + c);
        }
    }
    
    //======================================================================
    // These tests are not mirrored (yet) in icu4c at
    // source/test/intltest/transtst.cpp
    //======================================================================

    /**
     * Improve code coverage.
     */
    public void TestCoverage() {
        // NullTransliterator
        Transliterator t = Transliterator.getInstance("Null", Transliterator.FORWARD);
        expect(t, "a", "a");

        // Source, target set
        t = Transliterator.getInstance("Latin-Greek", Transliterator.FORWARD);
        t.setFilter(new UnicodeSet("[A-Z]"));
        logln("source = " + t.getSourceSet());
        logln("target = " + t.getTargetSet());

        t = Transliterator.createFromRules("x", "(.) > &Any-Hex($1);", Transliterator.FORWARD);
        logln("source = " + t.getSourceSet());
        logln("target = " + t.getTargetSet());
    }

    //======================================================================
    // Support methods
    //======================================================================
    void expect(String rules,
                String source,
                String expectedResult,
                Transliterator.Position pos) {
        Transliterator t = Transliterator.createFromRules("<ID>", rules, Transliterator.FORWARD);
        expect(t, source, expectedResult, pos);
    }

    void expect(String rules, String source, String expectedResult) {
        expect(rules, source, expectedResult, null);
    }

    void expect(Transliterator t, String source, String expectedResult,
                Transliterator reverseTransliterator) {
        expect(t, source, expectedResult);
        if (reverseTransliterator != null) {
            expect(reverseTransliterator, expectedResult, source);
        }
    }

    void expect(Transliterator t, String source, String expectedResult) {
        expect(t, source, expectedResult, (Transliterator.Position) null);
    }

    void expect(Transliterator t, String source, String expectedResult,
                Transliterator.Position pos) {
        if (pos == null) {
            String result = t.transliterate(source);
            if (!expectAux(t.getID() + ":String", source, result, expectedResult)) return;
        }

        Transliterator.Position index = null;
        if (pos == null) {
            index = new Transliterator.Position(0, source.length(), 0, source.length());
        } else {
            index = new Transliterator.Position(pos.contextStart, pos.contextLimit,
                                                pos.start, pos.limit);
        }

        ReplaceableString rsource = new ReplaceableString(source);

        t.finishTransliteration(rsource, index);
        // Do it all at once -- below we do it incrementally

        if (index.start != index.limit) {
            expectAux(t.getID() + ":UNFINISHED", source,
                "start: " + index.start + ", limit: " + index.limit, false, expectedResult);
            return;
        }
        String result = rsource.toString();
        if (!expectAux(t.getID() + ":Replaceable", source, result, expectedResult)) return;


        if (pos == null) {
            index = new Transliterator.Position();
        } else {
            index = new Transliterator.Position(pos.contextStart, pos.contextLimit,
                                                pos.start, pos.limit);
        }

        // Test incremental transliteration -- this result
        // must be the same after we finalize (see below).
        Vector v = new Vector();
        v.add(source);
        rsource.replace(0, rsource.length(), "");
        if (pos != null) {
            rsource.replace(0, 0, source);
            v.add(UtilityExtensions.formatInput(rsource, index));
            t.transliterate(rsource, index);
            v.add(UtilityExtensions.formatInput(rsource, index));
        } else {
            for (int i=0; i<source.length(); ++i) {
                //v.add(i == 0 ? "" : " + " + source.charAt(i) + "");
                //log.append(source.charAt(i)).append(" -> "));
                t.transliterate(rsource, index, source.charAt(i));
                //v.add(UtilityExtensions.formatInput(rsource, index) + source.substring(i+1));
                v.add(UtilityExtensions.formatInput(rsource, index) +
                      ((i<source.length()-1)?(" + '" + source.charAt(i+1) + "' ->"):" =>"));
            }
        }

        // As a final step in keyboard transliteration, we must call
        // transliterate to finish off any pending partial matches that
        // were waiting for more input.
        t.finishTransliteration(rsource, index);
        result = rsource.toString();
        //log.append(" => ").append(rsource.toString());
        v.add(result);

        String[] results = new String[v.size()];
        v.copyInto(results);
        expectAux(t.getID() + ":Incremental", results,
                  result.equals(expectedResult),
                  expectedResult);
    }

    boolean expectAux(String tag, String source,
                   String result, String expectedResult) {
        return expectAux(tag, new String[] {source, result},
                  result.equals(expectedResult),
                  expectedResult);
    }

    boolean expectAux(String tag, String source,
                   String result, boolean pass,
                   String expectedResult) {
        return expectAux(tag, new String[] {source, result},
                  pass,
                  expectedResult);
    }

    boolean expectAux(String tag, String source,
                   boolean pass,
                   String expectedResult) {
        return expectAux(tag, new String[] {source},
                  pass,
                  expectedResult);
    }

    boolean expectAux(String tag, String[] results, boolean pass,
                   String expectedResult) {
        msg((pass?"(":"FAIL: (")+tag+")", pass ? LOG : ERR, true, true);

        for (int i = 0; i < results.length; ++i) {
            String label;
            if (i == 0) {
                label = "source:   ";
            } else if (i == results.length - 1) {
                label = "result:   ";
            } else {
                if (!isVerbose() && pass) continue;
                label = "interm" + i + ":  ";
            }
            msg("    " + label + results[i], pass ? LOG : ERR, false, true);
        }

        if (!pass) {
            msg(  "    expected: " + expectedResult, ERR, false, true);
        }

        return pass;
    }
}


