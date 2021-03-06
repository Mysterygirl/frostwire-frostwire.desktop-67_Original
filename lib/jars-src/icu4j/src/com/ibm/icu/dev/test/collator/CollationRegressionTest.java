/*
 *******************************************************************************
 * Copyright (C) 2002, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 * $Source: 
 * $Date: 
 * $Revision: 
 *
 *****************************************************************************************
 */

/** 
 * Port From:   ICU4C v2.1 : collate/CollationRegressionTest
 * Source File: $ICU4CRoot/source/test/intltest/regcoll.cpp
 **/
 
package com.ibm.icu.dev.test.collator;

import com.ibm.icu.dev.test.*;
import com.ibm.icu.text.*;
import java.util.Locale;

public class CollationRegressionTest extends TestFmwk {
    public static void main(String[] args) throws Exception{
        new CollationRegressionTest().run(args);
    }
    
    // @bug 4048446
    //
    // CollationElementIterator.reset() doesn't work
    //
    public void Test4048446() {
        final String test1 = "XFILE What subset of all possible test cases has the highest probability of detecting the most errors?";
        //final String test2 = "Xf_ile What subset of all possible test cases has the lowest probability of detecting the least errors?";
        RuleBasedCollator en_us = (RuleBasedCollator) Collator.getInstance(Locale.US);
        CollationElementIterator i1 = en_us.getCollationElementIterator(test1);
        CollationElementIterator i2 = en_us.getCollationElementIterator(test1);
        
        if (i1 == null || i2 == null) {
            errln("Could not create CollationElementIterator's");
            return;
        }
    
        while (i1.next() != CollationElementIterator.NULLORDER) {
            //
        }
    
        i1.reset();
        assertEqual(i1, i2);
    }
    
    void assertEqual(CollationElementIterator i1, CollationElementIterator i2) {
        int c1, c2, count = 0;
    
        do {
            c1 = i1.next();
            c2 = i2.next();
    
            if (c1 != c2) {
                String msg = "";
                String msg1 = "    ";
                
                msg += msg1 + count;
                msg += ": strength(0x" + Integer.toHexString(c1);
                msg += ") != strength(0x" + Integer.toHexString(c2);
                msg += ")";
                errln(msg);
                break;
            }
            count += 1;
        } while (c1 != CollationElementIterator.NULLORDER);
    }
    
    // @bug 4051866
    //
    // Collator -> rules -> Collator round-trip broken for expanding characters
    //
    public void Test4051866() {
       String rules = "< o & oe ,o\u3080& oe ,\u1530 ,O& OE ,O\u3080& OE ,\u1520< p ,P";

        // Build a collator containing expanding characters
        RuleBasedCollator c1 = null;
        
        try {
            c1 = new RuleBasedCollator(rules);
        } catch (Exception e) {
            errln("Fail to create RuleBasedCollator with rules:" + rules);
            return;
        }
    
        // Build another using the rules from  the first
        RuleBasedCollator c2 = null;
        try {
            c2 = new RuleBasedCollator(c1.getRules());
        } catch (Exception e) {
            errln("Fail to create RuleBasedCollator with rules:" + rules);
            return;
        }
    
        // Make sure they're the same
        if (!(c1.getRules().equals(c2.getRules())))
        {
            errln("Rules are not equal");
        }
    }
    
    // @bug 4053636
    //
    // Collator thinks "black-bird" == "black"
    //
    public void Test4053636() {
        RuleBasedCollator en_us = (RuleBasedCollator) Collator.getInstance(Locale.US);
        if (en_us.equals("black_bird", "black")) {
            errln("black-bird == black");
        }
    }
    
    // @bug 4054238
    //
    // CollationElementIterator will not work correctly if the associated
    // Collator object's mode is changed
    //
    public void Test4054238(/* char* par */) {
        final char[] chars3 = {0x61, 0x00FC, 0x62, 0x65, 0x63, 0x6b, 0x20, 0x47, 0x72, 0x00F6, 0x00DF, 0x65, 0x20, 0x4c, 0x00FC, 0x62, 0x63, 0x6b, 0};
        final String test3 = new String(chars3);
        RuleBasedCollator c = (RuleBasedCollator) Collator.getInstance(Locale.US);
    
        // NOTE: The Java code uses en_us to create the CollationElementIterators
        // but I'm pretty sure that's wrong, so I've changed this to use c.
        c.setDecomposition(Collator.NO_DECOMPOSITION);
        CollationElementIterator i1 = c.getCollationElementIterator(test3);
        logln("Offset:" + i1.getOffset());
    }
    
    // @bug 4054734
    //
    // Collator::IDENTICAL documented but not implemented
    //
    public void Test4054734(/* char* par */) {
        
            //Here's the original Java:
    
            String[] decomp = {
                "\u0001",   "<",    "\u0002",
                "\u0001",   "=",    "\u0001",
                "A\u0001",  ">",    "~\u0002",      // Ensure A and ~ are not compared bitwise
                "\u00C0",   "=",    "A\u0300",      // Decomp should make these equal
            };
    
        RuleBasedCollator c = (RuleBasedCollator) Collator.getInstance(Locale.US);
        c.setStrength(Collator.IDENTICAL);
        c.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        compareArray(c, decomp);
    }
    
    void compareArray(Collator c, String[] tests) {
        
        int expectedResult = 0;
    
        for (int i = 0; i < tests.length; i += 3) {
            String source = tests[i];
            String comparison = tests[i + 1];
            String target = tests[i + 2];
    
            if (comparison.equals("<")) {
                expectedResult = -1;
            } else if (comparison.equals(">")) {
                expectedResult = 1;
            } else if (comparison.equals("=")) {
                expectedResult = 0;
            } else {
                errln("Bogus comparison string \"" + comparison + "\"");
            }
            
            int compareResult = 0;
            
            logln("i = " + i);
            logln(source);
            logln(target);
            try {
                compareResult = c.compare(source, target);
            } catch (Exception e) {
                errln(e.toString());
            }
    
            CollationKey sourceKey = null, targetKey = null;
            try {
                sourceKey = c.getCollationKey(source);
            } catch (Exception e) {
                errln("Couldn't get collationKey for source");
                continue;
            }
    
            try {
                targetKey = c.getCollationKey(target);
            } catch (Exception e) {
                errln("Couldn't get collationKey for target");
                continue;
            }
    
            int keyResult = sourceKey.compareTo(targetKey);
            reportCResult( source, target, sourceKey, targetKey, compareResult, keyResult, compareResult, expectedResult );
        }
    }
    
    void reportCResult( String source, String target, CollationKey sourceKey, CollationKey targetKey,
                                int compareResult, int keyResult, int incResult, int expectedResult ){
        if (expectedResult < -1 || expectedResult > 1)
        {
            errln("***** invalid call to reportCResult ****");
            return;
        }

        boolean ok1 = (compareResult == expectedResult);
        boolean ok2 = (keyResult == expectedResult);
        boolean ok3 = (incResult == expectedResult);

        if (ok1 && ok2 && ok3 && !isVerbose()){
            return;    
        }else{
            String msg1 = ok1? "Ok: compare(\"" : "FAIL: compare(\"";
            String msg2 = "\", \"";
            String msg3 = "\") returned ";
            String msg4 = "; expected ";
            
            String sExpect = new String("");
            String sResult = new String("");
            sResult = appendCompareResult(compareResult, sResult);
            sExpect = appendCompareResult(expectedResult, sExpect);
            if (ok1) {
                logln(msg1 + source + msg2 + target + msg3 + sResult);
            } else {
                errln(msg1 + source + msg2 + target + msg3 + sResult + msg4 + sExpect);
            }
            
            msg1 = ok2 ? "Ok: key(\"" : "FAIL: key(\"";
            msg2 = "\").compareTo(key(\"";
            msg3 = "\")) returned ";
            sResult = appendCompareResult(keyResult, sResult);
            if (ok2) {
                logln(msg1 + source + msg2 + target + msg3 + sResult);
            } else {
                errln(msg1 + source + msg2 + target + msg3 + sResult + msg4 + sExpect);
                msg1 = "  ";
                msg2 = " vs. ";
                errln(msg1 + prettify(sourceKey) + msg2 + prettify(targetKey));
            }
            
            msg1 = ok3 ? "Ok: incCompare(\"" : "FAIL: incCompare(\"";
            msg2 = "\", \"";
            msg3 = "\") returned ";

            sResult = appendCompareResult(incResult, sResult);

            if (ok3) {
                logln(msg1 + source + msg2 + target + msg3 + sResult);
            } else {
                errln(msg1 + source + msg2 + target + msg3 + sResult + msg4 + sExpect);
            }                
        }
    }
    
    String appendCompareResult(int result, String target) {
        if (result == -1) {  //LESS
            target += "LESS";
        } else if (result == 0) {  //EQUAL
            target += "EQUAL";
        } else if (result == 1) {  //GREATER
            target += "GREATER";
        } else {
            String huh = "?";
            target += huh + result;
        }
        return target;
    }
    
    String prettify(CollationKey sourceKey) {
        int i;
        byte[] bytes= sourceKey.toByteArray();
        String target = "[";
    
        for (i = 0; i < bytes.length; i++) {
            target += Integer.toHexString(bytes[i]);
            target += " ";
        }
        target += "]";
        return target;
    }
    
    // @bug 4054736
    //
    // Full Decomposition mode not implemented
    //
    public void Test4054736(/* char* par */) {
        RuleBasedCollator c = (RuleBasedCollator) Collator.getInstance(Locale.US);
    
        c.setStrength(Collator.SECONDARY);
        c.setDecomposition(Collator.NO_DECOMPOSITION);
    
        final String[] tests = { "\uFB4F", "\u003d", "\u05D0\u05DC" };  // Alef-Lamed vs. Alef, Lamed
        compareArray(c, tests);
    }
    
    // @bug 4058613
    //
    // Collator::createInstance() causes an ArrayIndexOutofBoundsException for Korean  
    //
    public void Test4058613(/* char* par */) {
        // Creating a default collator doesn't work when Korean is the default
        // locale
        
        Locale oldDefault = Locale.getDefault();
        Locale.setDefault(new Locale("ko", ""));
    
        Collator c = null;
        
        c = Collator.getInstance(new Locale("en", "US"));
    
        if (c == null) {
            errln("Could not create a Korean collator");
            Locale.setDefault(oldDefault);
            return;
        }
        
        // Since the fix to this bug was to turn off decomposition for Korean collators,
        // ensure that's what we got
        if (c.getDecomposition() != Collator.NO_DECOMPOSITION) {
          errln("Decomposition is not set to NO_DECOMPOSITION for Korean collator");
        }
    
        Locale.setDefault(oldDefault);
    }
    
    // @bug 4059820
    //
    // RuleBasedCollator.getRules does not return the exact pattern as input
    // for expanding character sequences
    //
    public void Test4059820(/* char* par */) {
        RuleBasedCollator c = null;
        String rules = "< a < b , c/a < d < z";
        try {
            c = new RuleBasedCollator(rules);
        } catch (Exception e) {
            errln("Failure building a collator.");
            return;
        }
    
        if ( c.getRules().indexOf("c/a") == -1)
        {
            errln("returned rules do not contain 'c/a'");
        }
    }
    
    // @bug 4060154
    //
    // MergeCollation::fixEntry broken for "& H < \u0131, \u0130, i, I"
    //
    public void Test4060154(/* char* par */) {
        String rules ="< g, G < h, H < i, I < j, J & H < \u0131, \u0130, i, I";
    
        RuleBasedCollator c = null;
        try {
            c = new RuleBasedCollator(rules);
        } catch (Exception e) {
            //System.out.println(e);
            errln("failure building collator.");
            return;
        }
    
        c.setDecomposition(Collator.NO_DECOMPOSITION);
    
        String[] tertiary = {
            "A",        "<",    "B",
            "H",        "<",    "\u0131",
            "H",        "<",    "I",
            "\u0131",   "<",    "\u0130",
            "\u0130",   "<",    "i",
            "\u0130",   ">",    "H",
        };
    
        c.setStrength(Collator.TERTIARY);
        compareArray(c, tertiary);
    
        String[] secondary = {
            "H",        "<",    "I",
            "\u0131",   "=",    "\u0130",
        };
    
        c.setStrength(Collator.PRIMARY);
        compareArray(c, secondary);
    };
    
    // @bug 4062418
    //
    // Secondary/Tertiary comparison incorrect in French Secondary
    //
    public void Test4062418(/* char* par */) {
        RuleBasedCollator c = null;
        try {
            c = (RuleBasedCollator) Collator.getInstance(Locale.FRANCE);
        } catch (Exception e) {
            errln("Failed to create collator for Locale::FRANCE()");
            return;
        }
        c.setStrength(Collator.SECONDARY);
    
        String[] tests = {
                "p\u00eache",    "<",    "p\u00e9ch\u00e9",    // Comparing accents from end, p\u00e9ch\u00e9 is greater
        };
    
        compareArray(c, tests);
    }
    
    // @bug 4065540
    //
    // Collator::compare() method broken if either string contains spaces
    //
    public void Test4065540(/* char* par */) {
        RuleBasedCollator en_us = (RuleBasedCollator) Collator.getInstance(Locale.US);
        if (en_us.compare("abcd e", "abcd f") == 0) {
            errln("'abcd e' == 'abcd f'");
        }
    }
    
    // @bug 4066189
    //
    // Unicode characters need to be recursively decomposed to get the
    // correct result. For example,
    // u1EB1 -> \u0103 + \u0300 -> a + \u0306 + \u0300.
    //
    public void Test4066189(/* char* par */) {
        final  String test1 = "\u1EB1";
        final  String test2 = "\u0061\u0306\u0300";
    
        // NOTE: The java code used en_us to create the
        // CollationElementIterator's. I'm pretty sure that
        // was wrong, so I've change the code to use c1 and c2
        RuleBasedCollator c1 = (RuleBasedCollator) Collator.getInstance(Locale.US);
        c1.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        CollationElementIterator i1 = c1.getCollationElementIterator(test1);
    
        RuleBasedCollator c2 = (RuleBasedCollator) Collator.getInstance(Locale.US);
        c2.setDecomposition(Collator.NO_DECOMPOSITION);
        CollationElementIterator i2 = c2.getCollationElementIterator(test2);
    
        assertEqual(i1, i2);
    }
    
    // @bug 4066696
    //
    // French secondary collation checking at the end of compare iteration fails
    //
    public void Test4066696(/* char* par */) {
        RuleBasedCollator c = null;
        try {
            c = (RuleBasedCollator)Collator.getInstance(Locale.FRANCE);
        } catch(Exception e) {
            errln("Failure creating collator for Locale::getFrance()");
            return;
        }
        c.setStrength(Collator.SECONDARY);
    
        String[] tests = {
            "\u00e0",   ">",     "\u01fa",       // a-grave <  A-ring-acute
        };    
        compareArray(c, tests);
    }
    
    // @bug 4076676
    //
    // Bad canonicalization of same-class combining characters
    //
    public void Test4076676(/* char* par */) {
        // These combining characters are all in the same class, so they should not
        // be reordered, and they should compare as unequal.
        final String s1 = "\u0041\u0301\u0302\u0300";
        final String s2 = "\u0041\u0302\u0300\u0301";
    
        RuleBasedCollator c = (RuleBasedCollator) Collator.getInstance(Locale.US);
        c.setStrength(Collator.TERTIARY);
    
        if (c.compare(s1,s2) == 0) {
            errln("Same-class combining chars were reordered");
        }
    }

    // @bug 4078588
    //
    // RuleBasedCollator breaks on "< a < bb" rule
    //
    public void Test4078588(/* char *par */) {
        RuleBasedCollator rbc = null;
        try {
            rbc = new RuleBasedCollator("< a < bb");
        } catch (Exception e) {
            errln("Failed to create RuleBasedCollator.");
            return;
        }
    
        int result = rbc.compare("a","bb");
    
        if (result >= 0) {
            errln("Compare(a,bb) returned " + result + "; expected -1");
        }
    }
    
    // @bug 4079231
    //
    // RuleBasedCollator::operator==(NULL) throws NullPointerException
    //
    public void Test4079231(/* char* par */) {    
        RuleBasedCollator en_us = (RuleBasedCollator) Collator.getInstance(Locale.US);
        try {
            if (en_us.equals(null)) {
                errln("en_us.equals(null) returned true");
            }
        } catch (Exception e) {
            errln("en_us.equals(null) threw " + e.toString());
        }
    }
    
    // @bug 4081866
    //
    // Combining characters in different classes not reordered properly.
    //
    public void Test4081866(/* char* par */) {
        // These combining characters are all in different classes,
        // so they should be reordered and the strings should compare as equal.
        String s1 = "\u0041\u0300\u0316\u0327\u0315";
        String s2 = "\u0041\u0327\u0316\u0315\u0300";
    
        RuleBasedCollator c = (RuleBasedCollator) Collator.getInstance(Locale.US);
        c.setStrength(Collator.TERTIARY);
        
        // Now that the default collators are set to NO_DECOMPOSITION
        // (as a result of fixing bug 4114077), we must set it explicitly
        // when we're testing reordering behavior.  -- lwerner, 5/5/98
        c.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        if (c.compare(s1,s2) != 0) {
            errln("Combining chars were not reordered");
        }
    }
    
    // @bug 4087241
    //
    // string comparison errors in Scandinavian collators
    //
    public void Test4087241(/* char* par */) {
        Locale da_DK = new Locale("da", "DK");
        RuleBasedCollator c = null;
        try {
            c = (RuleBasedCollator) Collator.getInstance(da_DK);
        } catch (Exception e) {
            errln("Failed to create collator for da_DK locale");
            return;
        }
        c.setStrength(Collator.SECONDARY);
        String tests[] = {
            "\u007a",       "\u003c", "\u00E6",            // z        < ae
            "\u0061\u0308", "\u003c", "\u0061\u030A",      // a-unlaut < a-ring
            "\u0059",       "\u003c", "\u0075\u0308",      // Y        < u-umlaut
        };
        compareArray(c, tests);
    }
    
    // @bug 4087243
    //
    // CollationKey takes ignorable strings into account when it shouldn't
    //
    public void Test4087243(/* char* par */) {
        RuleBasedCollator c = (RuleBasedCollator) Collator.getInstance(Locale.US);
        c.setStrength(Collator.TERTIARY);
        String tests[] = {
            "\u0031\u0032\u0033", "\u003d", "\u0031\u0032\u0033\u0001"    // 1 2 3  =  1 2 3 ctrl-A
        };
        compareArray(c, tests);
    }
    
    // @bug 4092260
    //
    // Mu/micro conflict
    // Micro symbol and greek lowercase letter Mu should sort identically
    //
    public void Test4092260(/* char* par */) {
        Locale el = new Locale("el", "");
        Collator c = null;
        try {
            c = Collator.getInstance(el);
        } catch (Exception e) {
            errln("Failed to create collator for el locale.");
            return;
        }
        // These now have tertiary differences in UCA
        c.setStrength(Collator.SECONDARY);
        String tests[] = {
            "\u00B5", "\u003d", "\u03BC",
        };
        compareArray(c, tests);
    }
    
    // @bug 4095316
    //
    public void Test4095316(/* char* par */) {
        Locale el_GR = new Locale("el", "GR");
        Collator c = null;
        try {
            c = Collator.getInstance(el_GR);
        } catch (Exception e) {
            errln("Failed to create collator for el_GR locale");
            return;
        }
        // These now have tertiary differences in UCA
        //c->setStrength(Collator::TERTIARY);
        //c->setAttribute(UCOL_STRENGTH, UCOL_SECONDARY, status);
        c.setStrength(Collator.SECONDARY);
        String tests[] = {
            "\u03D4", "\u003d", "\u03AB",
        };
        compareArray(c, tests);
    }
    
    // @bug 4101940
    //
    public void Test4101940(/* char* par */) {
        RuleBasedCollator c = null;
        String rules = "< a < b";
        String nothing = "";
        try {
            c = new RuleBasedCollator(rules);
        } catch (Exception e) {
            errln("Failed to create RuleBasedCollator");
            return;
        }
        CollationElementIterator i = c.getCollationElementIterator(nothing);
        i.reset();
        if (i.next() != CollationElementIterator.NULLORDER) {
            errln("next did not return NULLORDER");
        }
    }
    
    // @bug 4103436
    //
    // Collator::compare not handling spaces properly
    //
    public void Test4103436(/* char* par */) {
        RuleBasedCollator c = (RuleBasedCollator) Collator.getInstance(Locale.US);
        c.setStrength(Collator.TERTIARY);
        String[] tests = {
            "\u0066\u0069\u006c\u0065", "\u003c", "\u0066\u0069\u006c\u0065\u0020\u0061\u0063\u0063\u0065\u0073\u0073",
            "\u0066\u0069\u006c\u0065", "\u003c", "\u0066\u0069\u006c\u0065\u0061\u0063\u0063\u0065\u0073\u0073",
        };
        compareArray(c, tests);
    }
    
    // @bug 4114076
    //
    // Collation not Unicode conformant with Hangul syllables
    //
    public void Test4114076(/* char* par */) {
        RuleBasedCollator c = (RuleBasedCollator) Collator.getInstance(Locale.US);
        c.setStrength(Collator.TERTIARY);
    
        //
        // With Canonical decomposition, Hangul syllables should get decomposed
        // into Jamo, but Jamo characters should not be decomposed into
        // conjoining Jamo
        //
        String test1[] = {
            "\ud4db", "\u003d", "\u1111\u1171\u11b6"
        };
    
        c.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        compareArray(c, test1);
    
        // From UTR #15:
        // *In earlier versions of Unicode, jamo characters like ksf
        //  had compatibility mappings to kf + sf. These mappings were 
        //  removed in Unicode 2.1.9 to ensure that Hangul syllables are maintained.)
        // That is, the following test is obsolete as of 2.1.9
    
    //obsolete-    // With Full decomposition, it should go all the way down to
    //obsolete-    // conjoining Jamo characters.
    //obsolete-    //
    //obsolete-    static const UChar test2[][CollationRegressionTest::MAX_TOKEN_LEN] =
    //obsolete-    {
    //obsolete-        {0xd4db, 0}, {0x3d, 0}, {0x1111, 0x116e, 0x1175, 0x11af, 0x11c2, 0}
    //obsolete-    };
    //obsolete-
    //obsolete-    c->setDecomposition(Normalizer::DECOMP_COMPAT);
    //obsolete-    compareArray(*c, test2, ARRAY_LENGTH(test2));
    }

    // @bug 4114077
    //
    // Collation with decomposition off doesn't work for Europe 
    //
    public void Test4114077(/* char* par */) {
        // Ensure that we get the same results with decomposition off
        // as we do with it on....
        RuleBasedCollator c = (RuleBasedCollator) Collator.getInstance(Locale.US);
        c.setStrength(Collator.TERTIARY);
        String test1[] = {
            "\u00C0",                         "\u003d", "\u0041\u0300",            // Should be equivalent
            "\u0070\u00ea\u0063\u0068\u0065", "\u003e", "\u0070\u00e9\u0063\u0068\u00e9",
            "\u0204",                         "\u003d", "\u0045\u030F",
            "\u01fa",                         "\u003d", "\u0041\u030a\u0301",    // a-ring-acute -> a-ring, acute
                                                    //   -> a, ring, acute
            "\u0041\u0300\u0316",             "\u003c", "\u0041\u0316\u0300"        // No reordering --> unequal
        };
    
        c.setDecomposition(Collator.NO_DECOMPOSITION);
        compareArray(c, test1);
    
        String test2[] = {
            "\u0041\u0300\u0316", "\u003d", "\u0041\u0316\u0300"      // Reordering --> equal
        };
    
        c.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        compareArray(c, test2);
    }
    
    // @bug 4124632
    //
    // Collator::getCollationKey was hanging on certain character sequences
    //
    public void Test4124632(/* char* par */) {
        Collator coll = null;
        try {
            coll = Collator.getInstance(Locale.JAPAN);
        } catch (Exception e) {
            errln("Failed to create collator for Locale::JAPAN");
            return;
        }
        String test = "\u0041\u0308\u0062\u0063";
        CollationKey key;
        try {
            key = coll.getCollationKey(test);
            logln(key.getSourceString());
        } catch (Exception e) {
            errln("CollationKey creation failed.");
        }
    }
    
    // @bug 4132736
    //
    // sort order of french words with multiple accents has errors
    //
    public void Test4132736(/* char* par */) {
        Collator c = null;
        try {
            c = Collator.getInstance(Locale.FRANCE);
            c.setStrength(Collator.TERTIARY);
        } catch (Exception e) {
            errln("Failed to create a collator for Locale::getFrance()");
        }
    
        String test1[] = {
            "\u0065\u0300\u0065\u0301", "\u003c", "\u0065\u0301\u0065\u0300",
            "\u0065\u0300\u0301",       "\u003c", "\u0065\u0301\u0300",
        };
        compareArray(c, test1);
    }
    
    // @bug 4133509
    //
    // The sorting using java.text.CollationKey is not in the exact order
    //
    public void Test4133509(/* char* par */) {
        RuleBasedCollator en_us = (RuleBasedCollator) Collator.getInstance(Locale.US);
        String test1[] = {
            "\u0045\u0078\u0063\u0065\u0070\u0074\u0069\u006f\u006e", "\u003c", "\u0045\u0078\u0063\u0065\u0070\u0074\u0069\u006f\u006e\u0049\u006e\u0049\u006e\u0069\u0074\u0069\u0061\u006c\u0069\u007a\u0065\u0072\u0045\u0072\u0072\u006f\u0072",
            "\u0047\u0072\u0061\u0070\u0068\u0069\u0063\u0073",       "\u003c", "\u0047\u0072\u0061\u0070\u0068\u0069\u0063\u0073\u0045\u006e\u0076\u0069\u0072\u006f\u006e\u006d\u0065\u006e\u0074",
            "\u0053\u0074\u0072\u0069\u006e\u0067",                   "\u003c", "\u0053\u0074\u0072\u0069\u006e\u0067\u0042\u0075\u0066\u0066\u0065\u0072",
        };
    
        compareArray(en_us, test1);
    }
    
    // @bug 4139572
    //
    // getCollationKey throws exception for spanish text 
    // Cannot reproduce this bug on 1.2, however it DOES fail on 1.1.6
    //
    public void Test4139572(/* char* par */) {
        //
        // Code pasted straight from the bug report
        // (and then translated to C++ ;-)
        //
        // create spanish locale and collator
        Locale l = new Locale("es", "es");
        Collator col = null;
        try {
            col = Collator.getInstance(l);
        } catch (Exception e) {
            errln("Failed to create a collator for es_es locale.");
            return;
        }
        CollationKey key = null;
        // this spanish phrase kills it!
        try {
            key = col.getCollationKey("Nombre De Objeto");
            logln("source:" + key.getSourceString());
        } catch (Exception e) {
            errln("Error creating CollationKey for \"Nombre De Ojbeto\"");
        }
    }
    
    // @bug 4141640
    //
    // Support for Swedish gone in 1.1.6 (Can't create Swedish collator) 
    //
    public void Test4141640(/* char* par */) {
        //
        // Rather than just creating a Swedish collator, we might as well
        // try to instantiate one for every locale available on the system
        // in order to prevent this sort of bug from cropping up in the future
        //
        Locale locales[] = Collator.getAvailableLocales();
        
        for (int i = 0; i < locales.length; i += 1)
        {
            Collator c = null;
            try {
                c = Collator.getInstance(locales[i]);
                logln("source: " + c.getStrength());
            } catch (Exception e) {
                String msg = "";
                msg += "Could not create collator for locale ";
                msg += locales[i].getDisplayName();
                errln(msg);
            }
        }
    }
    
    /* RuleBasedCollator not subclassable
     * @bug 4146160
    //
    // RuleBasedCollator doesn't use createCollationElementIterator internally
    //
    public void Test4146160() {
        //
        // Use a custom collator class whose createCollationElementIterator
        // methods increment a count....
        //     
        RuleBasedCollator en_us = (RuleBasedCollator) Collator.getInstance(Locale.US);
        My4146160Collator.count = 0;
        My4146160Collator mc = null;
        try {
            mc = new My4146160Collator(en_us);
        } catch (Exception e) {
            errln("Failed to create a My4146160Collator.");
            return;
        }
    
        CollationKey key = null;
        try {
            key = mc.getCollationKey("1");
        } catch (Exception e) {
            errln("Failure to get a CollationKey from a My4146160Collator.");
            return;
        }
    
        if (My4146160Collator.count < 1) {
            errln("My4146160Collator.getCollationElementIterator not called for getCollationKey");
        }
    
        My4146160Collator.count = 0;
        mc.compare("1", "2");
    
        if (My4146160Collator.count < 1) {
            errln("My4146160Collator.getCollationElementIterator not called for compare");
        }
    }*/
}

/* RuleBasedCollator not subclassable
 * class My4146160Collator extends RuleBasedCollator {
    static int count = 0;

    public My4146160Collator(RuleBasedCollator rbc) throws Exception {
        super(rbc.getRules());
    }

    public CollationElementIterator getCollationElementIterator(String text) {
        count += 1;
        return super.getCollationElementIterator(text);
    }
    
    public CollationElementIterator getCollationElementIterator(java.text.CharacterIterator text) {
        count += 1;
        return super.getCollationElementIterator(text);
    }
}
*/