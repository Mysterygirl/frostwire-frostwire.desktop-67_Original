/*    */ package org.tritonus.share.sampled;
/*    */ 
/*    */ public class TVolumeUtils
/*    */ {
/* 29 */   private static final double FACTOR1 = 20.0D / Math.log(10.0D);
/*    */   private static final double FACTOR2 = 0.05D;
/*    */ 
/*    */   public static double lin2log(double dLinear)
/*    */   {
/* 36 */     return FACTOR1 * Math.log(dLinear);
/*    */   }
/*    */ 
/*    */   public static double log2lin(double dLogarithmic)
/*    */   {
/* 43 */     return Math.pow(10.0D, dLogarithmic * 0.05D);
/*    */   }
/*    */ }

