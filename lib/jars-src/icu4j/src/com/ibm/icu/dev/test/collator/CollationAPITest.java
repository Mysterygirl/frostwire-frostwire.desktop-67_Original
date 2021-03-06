/*
 *******************************************************************************
 * Copyright (C) 2002-2003, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 * $Source: 
 * $Date: 
 * $Revision: 
 *
 *****************************************************************************************
 */

/** 
 * Port From:   ICU4C v2.1 : collate/CollationAPITest
 * Source File: $ICU4CRoot/source/test/intltest/apicoll.cpp
 **/
 
package com.ibm.icu.dev.test.collator;

import java.util.Locale;
import java.util.Arrays;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import com.ibm.icu.dev.test.*;
import com.ibm.icu.text.*;

public class CollationAPITest extends TestFmwk {
    public static void main(String[] args) throws Exception {
        new CollationAPITest().run(args);
        //new CollationAPITest().TestGetTailoredSet();
    }
    
    /**
     * This tests the collation key related APIs.
     * - constructor/destructor
     * - Collator.getCollationKey
     * - == and != operators
     * - comparison between collation keys
     * - creating collation key with a byte array and vice versa
     */
    public void TestCollationKey() {
        logln("testing CollationKey begins...");
        Collator col = null;
        try {
            col = Collator.getInstance();
        } catch (Exception e) {
            
        }
        col.setStrength(Collator.TERTIARY);
    
        String test1 = "Abcda";
        String test2 = "abcda";
    
        logln("Testing weird arguments");
        CollationKey sortk1 = col.getCollationKey("");
        // key gets reset here
        byte[] bytes = sortk1.toByteArray();
        doAssert(bytes.length == 3 && bytes[0] == 1 && bytes[1] == 1 
                 && bytes[2] == 0, 
                 "Empty string should return an empty collation key");
        // bogus key returned here
        sortk1 = col.getCollationKey(null);
        doAssert(sortk1 == null, "Error code should return bogus collation key");

        logln("Use tertiary comparison level testing ....");
        sortk1 = col.getCollationKey(test1);
        CollationKey sortk2 = col.getCollationKey(test2);
        Object sortk3 = sortk2;
        doAssert((sortk1.compareTo(sortk2)) > 0 
                 && (sortk1.compareTo(sortk3)) > 0, "Result should be \"Abcda\" >>> \"abcda\"");
    
        CollationKey sortkNew;
        sortkNew = sortk1;
        doAssert(!(sortk1.equals(sortk2)) && !(sortk1.equals(sortk3)), 
                 "The sort keys should be different");
        doAssert((sortk1.hashCode() != sortk2.hashCode()), "sort key hashCode() failed");
        doAssert((sortk1.equals(sortkNew)), "The sort keys assignment failed");
        doAssert((sortk1.hashCode() == sortkNew.hashCode()), "sort key hashCode() failed");

        // check invaliad comparisons
        Object fake = "fake";
        try {
            sortk1.compareTo(fake);
            errln("Non-CollationKey comparison");
        }
        catch (Exception e) {
        }
        if (sortk1.equals(fake)) {
            errln("Non-CollationKey comparison");
        }
        
        // port from apicoll
        try {
            col = Collator.getInstance();
        } catch (Exception e) {
    
        }
        if (col.getStrength() != Collator.TERTIARY){
            errln("Default collation did not have tertiary strength");
        }

        // Need to use identical strength
        col.setStrength(Collator.IDENTICAL);

        byte key2compat[] = { // 2.6.1 key
            (byte)0x26, (byte)0x28, (byte)0x2A, (byte)0x2C, (byte)0x26, 
            (byte)0x01, 
            (byte)0x09, 
            (byte)0x01, 
            (byte)0x09, 
            (byte)0x01, 
            (byte)0x25, 
            (byte)0x01, 
            (byte)0x92, (byte)0x93, (byte)0x94, (byte)0x95, (byte)0x92, 
            (byte)0x00 

            // 2.2 key
            /*
            0x1D, 0x1F, 0x21, 0x23, 0x1D, 0x01,
            0x09, 0x01, 0x09, 0x01, 0x1C, 0x01,
            0x92, 0x93, 0x94, 0x95, 0x92, 0x00
            */
            // 2.0 key
            /*
            0x19, 0x1B, 0x1D, 0x1F, 0x19,
            0x01, 0x09, 0x01, 0x09, 0x01,
            0x18, 0x01,
            0x92, 0x93, 0x94, 0x95, 0x92,
            0x00
            */
            // 1.8.1 key.
            /*
            0x19, 0x1B, 0x1D, 0x1F, 0x19,
            0x01, 0x0A, 0x01, 0x0A, 0x01,
            0x92, 0x93, 0x94, 0x95, 0x92,
            0x00 
            */
        };

        CollationKey key1 = col.getCollationKey(test1);
        CollationKey key2 = col.getCollationKey(test2);
        CollationKey key3 = col.getCollationKey(test2);
    
        doAssert(key1.compareTo(key2) > 0, 
                 "Result should be \"Abcda\" > \"abcda\"");
        doAssert(key2.compareTo(key1) < 0,
                "Result should be \"abcda\" < \"Abcda\"");
        doAssert(key2.compareTo(key3) == 0,
                "Result should be \"abcda\" ==  \"abcda\"");
     
        doAssert(Arrays.equals(key2.toByteArray(), key2compat),
                 "Binary format for 'abcda' sortkey different for identical strength!");
    
        logln("Use secondary comparision level testing ...\n");
        col.setStrength(Collator.SECONDARY);
    
        key1 = col.getCollationKey(test1);
        key2 = col.getCollationKey(test2);
        key3 = col.getCollationKey(test2);
    
        doAssert(key1.compareTo(key2) == 0, 
                "Result should be \"Abcda\" == \"abcda\"");
        doAssert(key2.compareTo(key3) == 0,
                "Result should be \"abcda\" ==  \"abcda\"");
    
        byte tempkey[] = key2.toByteArray();
        byte subkey2compat[] = new byte[tempkey.length];
        System.arraycopy(key2compat, 0, subkey2compat, 0, tempkey.length);
        subkey2compat[subkey2compat.length - 1] = 0;
        doAssert(Arrays.equals(tempkey, subkey2compat),
                 "Binary format for 'abcda' sortkey different for secondary strength!");
    
        logln("testing sortkey ends...");
    }
    
    void doAssert(boolean conditions, String message) {
        if (!conditions) {
            errln("Error: " + message);
        }
    }
    
    /**
     * This tests the comparison convenience methods of a collator object.
     * - greater than
     * - greater than or equal to
     * - equal to
     */
    public void TestCompare() {
        logln("The compare tests begin : ");
        Collator col = null;
        try {
            col = Collator.getInstance(Locale.ENGLISH);
        } catch (Exception e) {
            errln("Default collation creation failed.");
            return;
        }
        
        String test1 = "Abcda";
        String test2 = "abcda";
        logln("Use tertiary comparison level testing ....");
        
        Object obj1 = test1;
        Object obj2 = test2;
        doAssert((col.compare(obj1, obj2) > 0), "Result should be \"Abcda\" != \"abcda\"");
        doAssert((!col.equals(test1, test2) ), "Result should be \"Abcda\" != \"abcda\"");
        doAssert((col.compare(test1, test2) > 0 ), "Result should be \"Abcda\" >>> \"abcda\"");
    
        col.setStrength(Collator.SECONDARY);
        logln("Use secondary comparison level testing ....");
                    
        doAssert((col.compare(obj1, obj2) == 0), "Result should be \"Abcda\" == \"abcda\"");
        doAssert((col.equals(test1, test2) ), "Result should be \"Abcda\" == \"abcda\"");
        doAssert((col.compare(test1, test2) == 0), "Result should be \"Abcda\" == \"abcda\"");
    
        col.setStrength(Collator.PRIMARY);
        logln("Use primary comparison level testing ....");
        
        doAssert((col.compare(obj1, obj2) == 0 ), "Result should be \"Abcda\" == \"abcda\"");
        doAssert((col.equals(test1, test2) ), "Result should be \"Abcda\" == \"abcda\"");
        doAssert((col.compare(test1, test2) == 0 ), "Result should be \"Abcda\" == \"abcda\"");
        logln("The compare tests end.");
        
        Integer die = new Integer(1);
        try {
            col.compare(die, test1);
            errln("Non-Strings should fail col.compare(Object, Object)");
        } catch (Exception e) {
        }
    }
    
    /**
    * Tests decomposition setting
    */
    public void TestDecomposition() {
        Collator en_US = null, el_GR = null, vi_VN = null;
        try {
            en_US = Collator.getInstance(new Locale("en", "US"));
            el_GR = Collator.getInstance(new Locale("el", "GR"));
            vi_VN = Collator.getInstance(new Locale("vi", "VN"));
        } catch (Exception e) {
            errln("ERROR: collation creation failed.\n");
            return;
        }
        
        // there is no reason to have canonical decomposition in en_US OR default locale */
        if (vi_VN.getDecomposition() != Collator.CANONICAL_DECOMPOSITION)
        {
            errln("ERROR: vi_VN collation did not have cannonical decomposition for normalization!\n");
        }

        if (el_GR.getDecomposition() != Collator.CANONICAL_DECOMPOSITION)
        {
            errln("ERROR: el_GR collation did not have cannonical decomposition for normalization!\n");
        }

        if (en_US.getDecomposition() != Collator.NO_DECOMPOSITION)
        {
            errln("ERROR: en_US collation had cannonical decomposition for normalization!\n");
        }
    }
    
    /**
     * This tests the duplication of a collator object.
     */
    public void TestDuplicate() {
        //Clone does not be implemented 
        Collator col1 = null;
        try {
            col1 = Collator.getInstance(Locale.ENGLISH);
        } catch (Exception e) {
            errln("Failure creating english collator");
            return;
        }
        
        // Collator col2 = (Collator)col1.clone();
        // doAssert(col1.equals(col2), "Cloned object is not equal to the orginal");
        String ruleset = "< a, A < b, B < c, C < d, D, e, E";
        RuleBasedCollator col3 = null;
        try {
            col3 = new RuleBasedCollator(ruleset);
        } catch (Exception e) {
            errln("Failure creating RuleBasedCollator with rule:" + ruleset);
            return;
        }
        doAssert(!col1.equals(col3), "Cloned object is equal to some dummy");
        col3 = (RuleBasedCollator)col1;
        doAssert(col1.equals(col3), "Copied object is not equal to the orginal");
        
    }
    
    /**
     * This tests the CollationElementIterator related APIs.
     * - creation of a CollationElementIterator object
     * - == and != operators
     * - iterating forward
     * - reseting the iterator index
     * - requesting the order properties(primary, secondary or tertiary)
     */
    public void TestElemIter() {
        // logln("testing sortkey begins...");
        Collator col = null;
        try {
            col = Collator.getInstance(Locale.ENGLISH);
        } catch (Exception e) {
            errln("Default collation creation failed.");
            return;
        }
           
        String testString1 = "XFILE What subset of all possible test cases has the highest probability of detecting the most errors?";
        String testString2 = "Xf_ile What subset of all possible test cases has the lowest probability of detecting the least errors?";
        // logln("Constructors and comparison testing....");
        CollationElementIterator iterator1 = ((RuleBasedCollator)col).getCollationElementIterator(testString1);
        
        CharacterIterator chariter=new StringCharacterIterator(testString1);
        // copy ctor
        CollationElementIterator iterator2 = ((RuleBasedCollator)col).getCollationElementIterator(chariter);
        CollationElementIterator iterator3 = ((RuleBasedCollator)col).getCollationElementIterator(testString2);
    
        int offset = 0;
        offset = iterator1.getOffset();
        if (offset != 0) {
            errln("Error in getOffset for collation element iterator\n");
            return;
        }
        iterator1.setOffset(6);
        iterator1.setOffset(0);
        int order1, order2, order3;
        
        order1 = iterator1.next();
        doAssert(!(iterator1.equals(iterator2)), "The first iterator advance failed");
        order2 = iterator2.next();
        
        doAssert((iterator1.equals(iterator2)), "The second iterator advance failed"); 
        doAssert((order1 == order2), "The order result should be the same");
        order3 = iterator3.next();
        
        doAssert((CollationElementIterator.primaryOrder(order1) == 
            CollationElementIterator.primaryOrder(order3)), "The primary orders should be the same");
        doAssert((CollationElementIterator.secondaryOrder(order1) == 
            CollationElementIterator.secondaryOrder(order3)), "The secondary orders should be the same");
        doAssert((CollationElementIterator.tertiaryOrder(order1) == 
            CollationElementIterator.tertiaryOrder(order3)), "The tertiary orders should be the same");
    
        order1 = iterator1.next(); 
        order3 = iterator3.next();
        
        doAssert((CollationElementIterator.primaryOrder(order1) == 
            CollationElementIterator.primaryOrder(order3)), "The primary orders should be identical");
        doAssert((CollationElementIterator.tertiaryOrder(order1) != 
            CollationElementIterator.tertiaryOrder(order3)), "The tertiary orders should be different");
    
        order1 = iterator1.next(); 
        order3 = iterator3.next();
        // invalid test wrong in UCA
        // doAssert((CollationElementIterator.secondaryOrder(order1) != 
        //    CollationElementIterator.secondaryOrder(order3)), "The secondary orders should not be the same");
            
        doAssert((order1 != CollationElementIterator.NULLORDER), "Unexpected end of iterator reached");
    
        iterator1.reset(); 
        iterator2.reset(); 
        iterator3.reset();
        order1 = iterator1.next();
        
        doAssert(!(iterator1.equals(iterator2)), "The first iterator advance failed");
        
        order2 = iterator2.next();
        
        doAssert((iterator1.equals(iterator2)), "The second iterator advance failed");
        doAssert((order1 == order2), "The order result should be the same");
    
        order3 = iterator3.next();
        
        doAssert((CollationElementIterator.primaryOrder(order1) == 
            CollationElementIterator.primaryOrder(order3)), "The primary orders should be the same");
        doAssert((CollationElementIterator.secondaryOrder(order1) == 
            CollationElementIterator.secondaryOrder(order3)), "The secondary orders should be the same");
        doAssert((CollationElementIterator.tertiaryOrder(order1) == 
            CollationElementIterator.tertiaryOrder(order3)), "The tertiary orders should be the same");
    
        order1 = iterator1.next(); 
        order2 = iterator2.next(); 
        order3 = iterator3.next();
        
        doAssert((CollationElementIterator.primaryOrder(order1) == 
            CollationElementIterator.primaryOrder(order3)), "The primary orders should be identical");
        doAssert((CollationElementIterator.tertiaryOrder(order1) != 
            CollationElementIterator.tertiaryOrder(order3)), "The tertiary orders should be different");
    
        order1 = iterator1.next(); 
        order3 = iterator3.next();
        
        // obsolete invalid test, removed
        // doAssert((CollationElementIterator.secondaryOrder(order1) != 
        //    CollationElementIterator.secondaryOrder(order3)), "The secondary orders should not be the same");
        doAssert((order1 != CollationElementIterator.NULLORDER), "Unexpected end of iterator reached");
        doAssert(!(iterator2.equals(iterator3)), "The iterators should be different");
        logln("testing CollationElementIterator ends...");
    }
    
    /**
     * This tests the hashCode method of a collator object.
     */
    public void TestHashCode() {
        logln("hashCode tests begin.");
        Collator col1 = null;
        try {
            col1 = Collator.getInstance(Locale.ENGLISH);
        } catch (Exception e) {
            errln("Default collation creation failed.");
            return;
        }
    
        Collator col2 = null;
        Locale dk = new Locale("da", "DK", "");
        try {
            col2 = Collator.getInstance(dk);
        } catch (Exception e) {
            errln("Danish collation creation failed.");
            return;
        }
    
        Collator col3 = null;
        try {
            col3 = Collator.getInstance(Locale.ENGLISH);
        } catch (Exception e) {
            errln("2nd default collation creation failed.");
            return;
        }
    
        logln("Collator.hashCode() testing ...");
        
        doAssert(col1.hashCode() != col2.hashCode(), "Hash test1 result incorrect" );                 
        doAssert(!(col1.hashCode() == col2.hashCode()), "Hash test2 result incorrect" );              
        doAssert(col1.hashCode() == col3.hashCode(), "Hash result not equal" );               
    
        logln("hashCode tests end.");
        
        String test1 = "Abcda";
        String test2 = "abcda";
        
        CollationKey sortk1, sortk2, sortk3;
                    
        sortk1 = col3.getCollationKey(test1);
        sortk2 = col3.getCollationKey(test2); 
        sortk3 = col3.getCollationKey(test2); 
        
        doAssert(sortk1.hashCode() != sortk2.hashCode(), "Hash test1 result incorrect");               
        doAssert(sortk2.hashCode() == sortk3.hashCode(), "Hash result not equal" );
    }
    
    /**
     * This tests the properties of a collator object.
     * - constructor
     * - factory method getInstance
     * - compare and getCollationKey
     * - get/set decomposition mode and comparison level
     */
    public void TestProperty() {
        Collator col = null;
        /* 
          All the collations have the same version in an ICU
          version.
          ICU 2.0 currVersionArray = {0x18, 0xC0, 0x02, 0x02};
          ICU 2.1 currVersionArray = {0x19, 0x00, 0x03, 0x03};
        */    
        logln("The property tests begin : ");
        logln("Test ctors : ");
        try {
            col = Collator.getInstance(Locale.ENGLISH);
        } catch (Exception e) {
            errln("Default Collator creation failed.");
            return;
        }
    
        doAssert((col.compare("ab", "abc") < 0), "ab < abc comparison failed");
        doAssert((col.compare("ab", "AB") < 0), "ab < AB comparison failed");
        doAssert((col.compare("blackbird", "black-bird") > 0), "black-bird > blackbird comparison failed");
        doAssert((col.compare("black bird", "black-bird") < 0), "black bird > black-bird comparison failed");
        doAssert((col.compare("Hello", "hello") > 0), "Hello > hello comparison failed");
    
        logln("Test ctors ends.");
        
        logln("testing Collator.getStrength() method ...");
        doAssert((col.getStrength() == Collator.TERTIARY), "collation object has the wrong strength");
        doAssert((col.getStrength() != Collator.PRIMARY), "collation object's strength is primary difference");
            
        logln("testing Collator.setStrength() method ...");
        col.setStrength(Collator.SECONDARY);
        doAssert((col.getStrength() != Collator.TERTIARY), "collation object's strength is secondary difference");
        doAssert((col.getStrength() != Collator.PRIMARY), "collation object's strength is primary difference");
        doAssert((col.getStrength() == Collator.SECONDARY), "collation object has the wrong strength");
    
        logln("testing Collator.setDecomposition() method ...");
        col.setDecomposition(Collator.NO_DECOMPOSITION);
        doAssert((col.getDecomposition() != Collator.CANONICAL_DECOMPOSITION), "Decomposition mode != Collator.CANONICAL_DECOMPOSITION");
        doAssert((col.getDecomposition() == Collator.NO_DECOMPOSITION), "Decomposition mode = Collator.NO_DECOMPOSITION");
        
        
        RuleBasedCollator rcol = (RuleBasedCollator)Collator.getInstance(new Locale("da", "DK"));
        doAssert(rcol.getRules().length() != 0, "da_DK rules does not have length 0");
        
        try {
            col = Collator.getInstance(Locale.FRENCH);
        } catch (Exception e) {
            errln("Creating French collation failed.");
            return;
        }
    
        col.setStrength(Collator.PRIMARY);
        logln("testing Collator.getStrength() method again ...");
        doAssert((col.getStrength() != Collator.TERTIARY), "collation object has the wrong strength");
        doAssert((col.getStrength() == Collator.PRIMARY), "collation object's strength is not primary difference");
            
        logln("testing French Collator.setStrength() method ...");
        col.setStrength(Collator.TERTIARY);
        doAssert((col.getStrength() == Collator.TERTIARY), "collation object's strength is not tertiary difference");
        doAssert((col.getStrength() != Collator.PRIMARY), "collation object's strength is primary difference");
        doAssert((col.getStrength() != Collator.SECONDARY), "collation object's strength is secondary difference");
    
        logln("Create junk collation: ");
        Locale abcd = new Locale("ab", "CD", "");
        
        Collator junk = null;
        try {
            junk = Collator.getInstance(abcd);
        } catch (Exception e) {
            errln("Junk collation creation failed, should at least return default.");
            return;
        }
    
        try {
            col = Collator.getInstance();
        } catch (Exception e) {
            errln("Creating default collator failed.");
            return;
        }
    
        String colrules = ((RuleBasedCollator)col).getRules();
        String junkrules = ((RuleBasedCollator)junk).getRules();
        doAssert(colrules == junkrules || colrules.equals(junkrules), 
                   "The default collation should be returned.");
        Collator frCol = null;
        try {
            frCol = Collator.getInstance(Locale.FRANCE);
        } catch (Exception e) {
            errln("Creating French collator failed.");
            return;
        }
    
        doAssert(!(frCol.equals(junk)), "The junk is the same as the French collator.");
        logln("Collator property test ended.");
    }
    
    /**
    * This tests the RuleBasedCollator
    * - constructor/destructor
    * - getRules
    */
    public void TestRuleBasedColl() {
        RuleBasedCollator col1 = null, col2 = null, col3 = null, col4 = null;
    
        String ruleset1 = "&9 < a, A < b, B < c, C; ch, cH, Ch, CH < d, D, e, E"; 
        String ruleset2 = "&9 < a, A < b, B < c, C < d, D, e, E";
        String ruleset3 = "&";
        
        try {
            col1 = new RuleBasedCollator(ruleset1);
        } catch (Exception e) {
            errln("RuleBased Collator creation failed.\n");
            return;
        }
    
        try {
            col2 = new RuleBasedCollator(ruleset2);
        } catch (Exception e) {
            errln("RuleBased Collator creation failed.\n");
            return;
        }
    
        try {
            // empty rules fail
            col3 = new RuleBasedCollator(ruleset3);
            errln("Failure: Empty rules for the collator should fail");
            return;
        } catch (Exception e) {
        }
        
        Locale locale = new Locale("aa", "AA");
        try {
            col3 = (RuleBasedCollator)Collator.getInstance(locale);
        } catch (Exception e) {
            errln("Fallback Collator creation failed.: %s\n");
            return;
        }
    
        try {
            col3 = (RuleBasedCollator)Collator.getInstance();
        } catch (Exception e) {
            errln("Default Collator creation failed.: %s\n");
            return;
        }
    
        String rule1 = col1.getRules(); 
        String rule2 = col2.getRules();
        String rule3 = col3.getRules();
    
        doAssert(!rule1.equals(rule2), "Default collator getRules failed");
        doAssert(!rule2.equals(rule3), "Default collator getRules failed");
        doAssert(!rule1.equals(rule3), "Default collator getRules failed");
        
        try {
            col4 = new RuleBasedCollator(rule2);
        } catch (Exception e) {
            errln("RuleBased Collator creation failed.\n");
            return;
        }
    
        String rule4 = col4.getRules();
        doAssert(rule2.equals(rule4), "Default collator getRules failed");
        // tests that modifier ! is always ignored
        String exclamationrules = "!&a<b";
        // java does not allow ! to be the start of the rule
        String thaistr = "\u0e40\u0e01\u0e2d";
        try {
            RuleBasedCollator col5 = new RuleBasedCollator(exclamationrules);
            RuleBasedCollator encol = (RuleBasedCollator)
                                        Collator.getInstance(Locale.ENGLISH);
            CollationElementIterator col5iter 
                                   = col5.getCollationElementIterator(thaistr);
            CollationElementIterator encoliter 
                                   = encol.getCollationElementIterator(
                                                                      thaistr);
            while (true) {
                // testing with en since thai has its own tailoring
                int ce = col5iter.next();
                int ce2 = encoliter.next();
                if (ce2 != ce) {
                    errln("! modifier test failed");
                }
                if (ce == CollationElementIterator.NULLORDER) {
                    break;
                }
            }
        } catch (Exception e) {
            errln("RuleBased Collator creation failed for ! modifier.\n");
            return;
        }
    }
    
    /**
    * This tests the RuleBasedCollator
    * - getRules
    */
    public void TestRules() {
        RuleBasedCollator coll;
        try {
            coll = (RuleBasedCollator)Collator.getInstance(Locale.ENGLISH); 
            // logln("PASS: RuleBased Collator creation passed\n");
        } catch (Exception e) {
            errln("English Collator creation failed.\n");
            return;
        }
    
        String rules = coll.getRules();
        if (rules != null && rules.length() != 0) {
            errln("English tailored rules failed");
        }
    }
    
    public void TestSafeClone() {
        String test1 = "abCda";
        String test2 = "abcda";
        
        // one default collator & two complex ones 
        RuleBasedCollator someCollators[] = {
            (RuleBasedCollator)Collator.getInstance(Locale.ENGLISH),
            (RuleBasedCollator)Collator.getInstance(Locale.KOREA),
            (RuleBasedCollator)Collator.getInstance(Locale.JAPAN)
        };
        RuleBasedCollator someClonedCollators[] = new RuleBasedCollator[3];
        
        // change orig & clone & make sure they are independent 
    
        for (int index = 0; index < someCollators.length; index ++)
        {
            try {
                someClonedCollators[index] 
                            = (RuleBasedCollator)someCollators[index].clone();
            } catch (CloneNotSupportedException e) {
                errln("Error cloning collator");
            }
    
            someClonedCollators[index].setStrength(Collator.TERTIARY);
            someCollators[index].setStrength(Collator.PRIMARY);
            someClonedCollators[index].setCaseLevel(false);
            someCollators[index].setCaseLevel(false);
            
            doAssert(someClonedCollators[index].compare(test1, test2) > 0, 
                     "Result should be \"abCda\" >>> \"abcda\" ");
            doAssert(someCollators[index].compare(test1, test2) == 0, 
                     "Result should be \"abCda\" == \"abcda\" ");
        }
    }

    public void TestGetTailoredSet() 
    {
        logln("testing getTailoredSet...");
        String rules[] = {
            "&a < \u212b",             
            "& S < \u0161 <<< \u0160",
        };
        String data[][] = {
            { "\u212b", "A\u030a", "\u00c5" },        
            { "\u0161", "s\u030C", "\u0160", "S\u030C" }            
        };
    
        int i = 0, j = 0;
        
        RuleBasedCollator coll;
        UnicodeSet set;
        
        for(i = 0; i < rules.length; i++) {
            try {
                logln("Instantiating a collator from "+rules[i]);
                coll = new RuleBasedCollator(rules[i]);
                set = coll.getTailoredSet();
                logln("Got set: "+set.toPattern(true));
                if(set.size() != data[i].length) {
                    errln("Tailored set size different ("+set.size()+") than expected ("+data[i].length+")");
                }
                for(j = 0; j < data[i].length; j++) {
                    logln("Checking to see whether "+data[i][j]+" is in set");
                    if(!set.contains(data[i][j])) {
                        errln("Tailored set doesn't contain "+data[i][j]+"... It should");
                    }
                }
            } catch (Exception e) {
                errln("Couldn't open collator with rules "+ rules[i]);
            }
        }
    }

    /** 
     * Simple test to see if Collator is subclassable
     */
    public void TestSubClass() 
    {
        class TestCollator extends Collator
        {
            public boolean equals(Object that) {
                return this == that;
            }
    
            public int hashCode() {
                return 0;
            }
            
            public int compare(String source, String target) {
                return source.compareTo(target);
            }
            
            public CollationKey getCollationKey(String source)
            {   byte temp1[] = source.getBytes();
                byte temp2[] = new byte[temp1.length + 1];
                System.arraycopy(temp1, 0, temp2, 0, temp1.length);
                temp2[temp1.length] = 0;
                return new CollationKey(source, temp2);
            }
            
            public void setVariableTop(int ce)
            {
            }
            
            public int setVariableTop(String str) 
            {
                return 0;
            }
            
            public int getVariableTop()
            {
                return 0;
            }
        }
 
        Collator col1 = new TestCollator();
        Collator col2 = new TestCollator();
        if (col1.equals(col2)) {
            errln("2 different instance of TestCollator should fail");
        }
        if (col1.hashCode() != col2.hashCode()) {
            errln("Every TestCollator has the same hashcode");
        }
        String abc = "abc";
        String bcd = "bcd";
        if (col1.compare(abc, bcd) != abc.compareTo(bcd)) {
            errln("TestCollator compare should be the same as the default " +
                  "string comparison");
        }
        CollationKey key = col1.getCollationKey(abc);
        byte temp1[] = abc.getBytes();
        byte temp2[] = new byte[temp1.length + 1];
        System.arraycopy(temp1, 0, temp2, 0, temp1.length);
        temp2[temp1.length] = 0;
        if (!java.util.Arrays.equals(key.toByteArray(), temp2) 
            || !key.getSourceString().equals(abc)) {
            errln("TestCollator collationkey API is returning wrong values");
        }
        UnicodeSet set = col1.getTailoredSet();
        if (!set.equals(new UnicodeSet(0, 0x10FFFF))) {
            errln("Error getting default tailored set");
        }
    }
    
     /** 
     * Simple test the collator setter and getters
     */
    public void TestSetGet() 
    {
        RuleBasedCollator collator = (RuleBasedCollator)Collator.getInstance();
        int decomp = collator.getDecomposition();
        int strength = collator.getStrength();
        boolean alt = collator.isAlternateHandlingShifted();
        boolean caselevel = collator.isCaseLevel();
        boolean french = collator.isFrenchCollation();
        boolean hquart = collator.isHiraganaQuaternary();
        boolean lowercase = collator.isLowerCaseFirst();
        boolean uppercase = collator.isUpperCaseFirst();
        
        collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        if (collator.getDecomposition() != Collator.CANONICAL_DECOMPOSITION) {
            errln("Setting decomposition failed");
        }
        collator.setStrength(Collator.QUATERNARY);
        if (collator.getStrength() != Collator.QUATERNARY) {
            errln("Setting strength failed");
        }
        collator.setAlternateHandlingShifted(!alt);
        if (collator.isAlternateHandlingShifted() == alt) {
            errln("Setting alternate handling failed");
        }
        collator.setCaseLevel(!caselevel);
        if (collator.isCaseLevel() == caselevel) {
            errln("Setting case level failed");
        }
        collator.setFrenchCollation(!french);
        if (collator.isFrenchCollation() == french) {
            errln("Setting french collation failed");
        }
        collator.setHiraganaQuaternary(!hquart);
        if (collator.isHiraganaQuaternary() == hquart) {
            errln("Setting hiragana quartenary failed");
        }
        collator.setLowerCaseFirst(!lowercase);
        if (collator.isLowerCaseFirst() == lowercase) {
            errln("Setting lower case first failed");
        }
        collator.setUpperCaseFirst(!uppercase);
        if (collator.isUpperCaseFirst() == uppercase) {
            errln("Setting upper case first failed");
        }   
        collator.setDecompositionDefault();
        if (collator.getDecomposition() != decomp) {
            errln("Setting decomposition default failed");
        }
        collator.setStrengthDefault();
        if (collator.getStrength() != strength) {
            errln("Setting strength default failed");
        }
        collator.setAlternateHandlingDefault();
        if (collator.isAlternateHandlingShifted() != alt) {
            errln("Setting alternate handling default failed");
        }
        collator.setCaseLevelDefault();
        if (collator.isCaseLevel() != caselevel) {
            errln("Setting case level default failed");
        }
        collator.setFrenchCollationDefault();
        if (collator.isFrenchCollation() != french) {
            errln("Setting french handling default failed");
        }
        collator.setHiraganaQuaternaryDefault();
        if (collator.isHiraganaQuaternary() != hquart) {
            errln("Setting Hiragana Quartenary default failed");
        }
        collator.setCaseFirstDefault();
        if (collator.isLowerCaseFirst() != lowercase 
            || collator.isUpperCaseFirst() != uppercase) {
            errln("Setting case first handling default failed");
        }
    }
    
    public void TestBounds() 
    {
        Collator coll = Collator.getInstance(new Locale("sh", ""));
          
        String test[] = { "John Smith", "JOHN SMITH", 
                          "john SMITH", "j\u00F6hn sm\u00EFth",
                          "J\u00F6hn Sm\u00EFth", "J\u00D6HN SM\u00CFTH",
                          "john smithsonian", "John Smithsonian",
        };
      
        String testStr[] = {
                          "\u010CAKI MIHALJ", 
                          "\u010CAKI MIHALJ",
                          "\u010CAKI PIRO\u0160KA", 
                          "\u010CABAI ANDRIJA",
                          "\u010CABAI LAJO\u0160", 
                          "\u010CABAI MARIJA",
                          "\u010CABAI STEVAN", 
                          "\u010CABAI STEVAN",
                          "\u010CABARKAPA BRANKO", 
                          "\u010CABARKAPA MILENKO",
                          "\u010CABARKAPA MIROSLAV", 
                          "\u010CABARKAPA SIMO",
                          "\u010CABARKAPA STANKO", 
                          "\u010CABARKAPA TAMARA",
                          "\u010CABARKAPA TOMA\u0160", 
                          "\u010CABDARI\u0106 NIKOLA",
                          "\u010CABDARI\u0106 ZORICA",
                          "\u010CABI NANDOR",
                          "\u010CABOVI\u0106 MILAN",
                          "\u010CABRADI AGNEZIJA",
                          "\u010CABRADI IVAN",
                          "\u010CABRADI JELENA",
                          "\u010CABRADI LJUBICA",
                          "\u010CABRADI STEVAN",
                          "\u010CABRDA MARTIN",
                          "\u010CABRILO BOGDAN",
                          "\u010CABRILO BRANISLAV",
                          "\u010CABRILO LAZAR",
                          "\u010CABRILO LJUBICA",
                          "\u010CABRILO SPASOJA",
                          "\u010CADE\u0160 ZDENKA",
                          "\u010CADESKI BLAGOJE",
                          "\u010CADOVSKI VLADIMIR",
                          "\u010CAGLJEVI\u0106 TOMA",
                          "\u010CAGOROVI\u0106 VLADIMIR",
                          "\u010CAJA VANKA",
                          "\u010CAJI\u0106 BOGOLJUB",
                          "\u010CAJI\u0106 BORISLAV",
                          "\u010CAJI\u0106 RADOSLAV",
                          "\u010CAK\u0160IRAN MILADIN",
                          "\u010CAKAN EUGEN",
                          "\u010CAKAN EVGENIJE",
                          "\u010CAKAN IVAN",
                          "\u010CAKAN JULIJAN",
                          "\u010CAKAN MIHAJLO",
                          "\u010CAKAN STEVAN",
                          "\u010CAKAN VLADIMIR",
                          "\u010CAKAN VLADIMIR",
                          "\u010CAKAN VLADIMIR",
                          "\u010CAKARA ANA",
                          "\u010CAKAREVI\u0106 MOMIR",
                          "\u010CAKAREVI\u0106 NEDELJKO",
                          "\u010CAKI \u0160ANDOR",
                          "\u010CAKI AMALIJA",
                          "\u010CAKI ANDRA\u0160",
                          "\u010CAKI LADISLAV",
                          "\u010CAKI LAJO\u0160",
                          "\u010CAKI LASLO" }; 
        
        CollationKey testKey[] = new CollationKey[testStr.length];
        for (int i = 0; i < testStr.length; i ++) {
            testKey[i] = coll.getCollationKey(testStr[i]);
        }
        
        Arrays.sort(testKey);
        for(int i = 0; i < testKey.length - 1; i ++) {
            CollationKey lower 
                           = testKey[i].getBound(CollationKey.BoundMode.LOWER,
                                                 Collator.SECONDARY);
            for (int j = i + 1; j < testKey.length; j ++) {
                CollationKey upper 
                           = testKey[j].getBound(CollationKey.BoundMode.UPPER,
                                                 Collator.SECONDARY);
                for (int k = i; k <= j; k ++) {
                    if (lower.compareTo(testKey[k]) > 0) {
                        errln("Problem with lower bound at i = " + i + " j = "
                              + j + " k = " + k);
                    }
                    if (upper.compareTo(testKey[k]) <= 0) {
                        errln("Problem with upper bound at i = " + i + " j = "
                              + j + " k = " + k);
                    }
                }
            }
        }
        
        for (int i = 0; i < test.length; i ++) 
        {
            CollationKey key = coll.getCollationKey(test[i]);
            CollationKey lower = key.getBound(CollationKey.BoundMode.LOWER,
                                              Collator.SECONDARY);
            CollationKey upper = key.getBound(CollationKey.BoundMode.UPPER_LONG,
                                              Collator.SECONDARY);
            for (int j = i + 1; j < test.length; j ++) {
                key = coll.getCollationKey(test[j]);
                if (lower.compareTo(key) > 0) {
                    errln("Problem with lower bound i = " + i + " j = " + j);
                }
                if (upper.compareTo(key) <= 0) {
                    errln("Problem with upper bound i = " + i + " j = " + j);
                }
            }
        }
    }    
}
