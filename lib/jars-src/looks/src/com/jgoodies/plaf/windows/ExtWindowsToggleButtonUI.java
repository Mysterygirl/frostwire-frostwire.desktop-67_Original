/*
 * Copyright (c) 2003 JGoodies Karsten Lentzsch. All Rights Reserved.
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

package com.jgoodies.plaf.windows;

import java.beans.PropertyChangeListener;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

import com.sun.java.swing.plaf.windows.WindowsToggleButtonUI;

import com.jgoodies.plaf.LookUtils;
import com.jgoodies.plaf.Options;
import com.jgoodies.plaf.common.ButtonMarginListener;

/**
 * Allows to use an optional narrow button margin.
 *
 * @author Karsten Lentzsch
 */
public final class ExtWindowsToggleButtonUI extends WindowsToggleButtonUI {
	
	
	private static final ExtWindowsToggleButtonUI INSTANCE = new ExtWindowsToggleButtonUI();
	
	
	public static ComponentUI createUI(JComponent b) { 
		return INSTANCE; 
	}
	
	
	/**
	 * Installs defaults and honors the client property <code>isNarrow</code>.
	 */	
	protected void installDefaults(AbstractButton b) {
		super.installDefaults(b);
		LookUtils.installNarrowMargin(b, getPropertyPrefix());
	}
	
	
	/**
	 * Installs an extra listener for a change of the isNarrow property.
	 */
	public void installListeners(AbstractButton b) {
		super.installListeners(b);
		PropertyChangeListener listener = new ButtonMarginListener(getPropertyPrefix());
		b.putClientProperty(ButtonMarginListener.CLIENT_KEY, listener);
		b.addPropertyChangeListener(Options.IS_NARROW_KEY, listener);
	}


	/**
	 * Uninstalls the extra listener for a change of the isNarrow property.
	 */
	public void uninstallListeners(AbstractButton b) {
		super.uninstallListeners(b);
		PropertyChangeListener listener = (PropertyChangeListener) b.getClientProperty(
												ButtonMarginListener.CLIENT_KEY);
		b.removePropertyChangeListener(listener);
	}
	
	
	
}