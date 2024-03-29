/*
******************************************************************************
* Copyright (C) 1996-2003, International Business Machines Corporation and   *
* others. All Rights Reserved.                                               *
******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/util/ValueIterator.java,v $
* $Date: 2003/06/03 18:49:36 $
* $Revision: 1.11 $
*
******************************************************************************
*/

package com.ibm.icu.util;

/**
 * <p>Interface for enabling iteration over sets of <int, Object>, where
 * int is the sorted integer index in ascending order and Object, its 
 * associated value.</p>
 * <p>The ValueIterator allows iterations over integer indexes in the range 
 * of Integer.MIN_VALUE to Integer.MAX_VALUE inclusive. Implementations of 
 * ValueIterator should specify their own maximum subrange within the above 
 * range that is meaningful to its applications.</p>
 * <p>Most implementations will be created by factory methods, such as the
 * character name iterator in UCharacter.getNameIterator. See example below.
 * </p>
 * Example of use:<br>
 * <pre>
 * ValueIterator iterator = UCharacter.getNameIterator();
 * ValueIterator.Element result = new ValueIterator.Element();
 * iterator.setRange(UCharacter.MIN_VALUE, UCharacter.MAX_VALUE);
 * while (iterator.next(result)) {
 *     System.out.println("Codepoint \\u" + 
 *                        Integer.toHexString(result.integer) + 
 *                        " has the character name " + (String)result.value);
 * }
 * </pre>
 * @author synwee
 * @stable ICU 2.6
 */
public interface ValueIterator
{
    // public inner class ---------------------------------------------
    
    /**
    * <p>The return result container of each iteration. Stores the next 
    * integer index and its associated value Object.</p> 
    * @stable ICU 2.6
    */
    public static final class Element
    {
        // public data members ----------------------------------------
        
        /**
        * Integer index of the current iteration
        * @stable ICU 2.6
        */
        public int integer;
        /**
        * Gets the Object value associated with the integer index.
        * @stable ICU 2.6
        */ 
        public Object value;
        
        // public constructor ------------------------------------------
        
        /**
         * Empty default constructor to make javadoc happy
         * @draft ICU 2.4
         */
        public Element()
        {
        }
    }
    
    // public methods -------------------------------------------------
    
    /**
    * <p>Gets the next result for this iteration and returns 
    * true if we are not at the end of the iteration, false otherwise.</p>
    * <p>If the return boolean is a false, the contents of elements will not
    * be updated.</p>
    * @param element for storing the result index and value
    * @return true if we are not at the end of the iteration, false otherwise.
    * @see Element
    * @stable ICU 2.6
    */
    public boolean next(Element element);
    
    /**
    * <p>Resets the iterator to start iterating from the integer index 
    * Integer.MIN_VALUE or X if a setRange(X, Y) has been called previously.
    * </p>
    * @stable ICU 2.6
    */
    public void reset();
    
    /**
     * <p>Restricts the range of integers to iterate and resets the iteration 
     * to begin at the index argument start.</p>
     * <p>If setRange(start, end) is not performed before next(element) is 
     * called, the iteration will start from the integer index 
     * Integer.MIN_VALUE and end at Integer.MAX_VALUE.</p>
     * <p>
     * If this range is set outside the meaningful range specified by the 
     * implementation, next(element) will always return false.
     * </p>
     * @param start first integer in range to iterate
     * @param limit 1 integer after the last integer in range 
     * @exception IllegalArgumentException thrown when attempting to set an 
     *            illegal range. E.g limit <= start
     * @stable ICU 2.6
     */
    public void setRange(int start, int end);
}