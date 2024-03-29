/*
 * Copyright (c) 2004 JGoodies Karsten Lentzsch. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *  o Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer. 
 *     
 *  o Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution. 
 *     
 *  o Neither the name of JGoodies Karsten Lentzsch nor the names of 
 *    its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *     
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */

package com.jgoodies.plaf.plastic;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.LookAndFeel;
import javax.swing.Popup;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPopupMenuUI;

import com.jgoodies.plaf.Options;
import com.jgoodies.plaf.common.ShadowPopupMenuUtils;

/**
 * The JGoodies Plastic l&amp;f implementation of <code>PopupMenuUI</code>. 
 * Adds support for a drop shadow.
 * 
 * @author Karsten Lentzsch
 * @version $Revision: 1.1.1.1 $
 * 
 * @see com.jgoodies.plaf.common.ShadowPopupBorder
 * @see com.jgoodies.plaf.common.ShadowPopupMenuUtils
 */
public final class PlasticPopupMenuUI extends BasicPopupMenuUI {
    
    /**
     * Describes whether drop shadows are active or inactive.
     * Set during the UI creation.
     */
    private static boolean dropShadowActive; 
        

	/**
	 * Creates an instance of the ui delegate for the specified component. 
	 */
	public static ComponentUI createUI(JComponent x) {
        dropShadowActive = Options.isPopupDropShadowActive();
		return new PlasticPopupMenuUI();
	}
	
    /**
     * Unlike the superclass, we install a plain border or drop shadow border
     * depending on the current setting of the <code>UIManager</code> setting
     * &quot;<code>PopupMenu.dropShadowEnabled</code>&quot; and other conditions;
     * for example, the feature is always inactive on the Mac OS X.
     */
    public void installDefaults() {
        super.installDefaults();
        String borderKey = dropShadowActive
            ? "PopupMenu.dropShadowBorder"
            : "PopupMenu.border";
        LookAndFeel.installBorder(popupMenu, borderKey);
    }
    
    /**
     * Returns the Popup that will be responsible for displaying the JPopupMenu.
     * Overwritten to make the popup component transparent (light-weight),
     * or make a background snapshot to simulate the shadows (heavy-weight).<p>
     * 
     * The snapshot will be used by class 
     * {@link com.jgoodies.plaf.common.ShadowPopupBorder}.
     */
	public Popup getPopup(JPopupMenu aPopupMenu, int x, int y) {
        Popup popup = super.getPopup(aPopupMenu, x, y);
        ShadowPopupMenuUtils.setTransparent(aPopupMenu, popup);
        return popup;
    }
    
    
}
