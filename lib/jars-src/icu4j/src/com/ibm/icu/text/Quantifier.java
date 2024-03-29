/*
 *******************************************************************************
 * Copyright (C) 2001, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/Quantifier.java,v $ 
 * $Date: 2002/06/28 19:15:52 $ 
 * $Revision: 1.8 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.text;
import com.ibm.icu.impl.Utility;

class Quantifier implements UnicodeMatcher {

    private UnicodeMatcher matcher;

    private int minCount;

    private int maxCount;

    /**
     * Maximum count a quantifier can have.
     */
    public static final int MAX = Integer.MAX_VALUE;

    public Quantifier(UnicodeMatcher theMatcher,
                      int theMinCount, int theMaxCount) {
        if (theMatcher == null || minCount < 0 || maxCount < 0 || minCount > maxCount) {
            throw new IllegalArgumentException();
        }
        matcher = theMatcher;
        minCount = theMinCount;
        maxCount = theMaxCount;
    }

    /**
     * Implement UnicodeMatcher API.
     */
    public int matches(Replaceable text,
                       int[] offset,
                       int limit,
                       boolean incremental) {
        int start = offset[0];
        int count = 0;
        while (count < maxCount) {
            int pos = offset[0];
            int m = matcher.matches(text, offset, limit, incremental);
            if (m == U_MATCH) {
                ++count;
                if (pos == offset[0]) {
                    // If offset has not moved we have a zero-width match.
                    // Don't keep matching it infinitely.
                    break;
                }
            } else if (incremental && m == U_PARTIAL_MATCH) {
                return U_PARTIAL_MATCH;
            } else {
                break;
            }
        }
        if (incremental && offset[0] == limit) {
            return U_PARTIAL_MATCH;
        }
        if (count >= minCount) {
            return U_MATCH;
        }
        offset[0] = start;
        return U_MISMATCH;
    }

    /**
     * Implement UnicodeMatcher API
     */
    public String toPattern(boolean escapeUnprintable) {
        StringBuffer result = new StringBuffer();
        result.append(matcher.toPattern(escapeUnprintable));
        if (minCount == 0) {
            if (maxCount == 1) {
                return result.append('?').toString();
            } else if (maxCount == MAX) {
                return result.append('*').toString();
            }
            // else fall through
        } else if (minCount == 1 && maxCount == MAX) {
            return result.append('+').toString();
        }
        result.append('{');
        Utility.appendNumber(result, minCount);
        result.append(',');
        if (maxCount != MAX) {
            Utility.appendNumber(result, maxCount);
        }
        result.append('}');
        return result.toString();
    }

    /**
     * Implement UnicodeMatcher API
     */
    public boolean matchesIndexValue(int v) {
        return (minCount == 0) || matcher.matchesIndexValue(v);
    }

    /**
     * Implementation of UnicodeMatcher API.  Union the set of all
     * characters that may be matched by this object into the given
     * set.
     * @param toUnionTo the set into which to union the source characters
     * @return a reference to toUnionTo
     */
    public void addMatchSetTo(UnicodeSet toUnionTo) {
        if (maxCount > 0) {
            matcher.addMatchSetTo(toUnionTo);
        }
    }
}

//eof
