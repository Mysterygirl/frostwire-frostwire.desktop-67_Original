/*
 * Copyright (c) 2003 JGoodies Karsten Lentzsch. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * o Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * o Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * o Neither the name of JGoodies Karsten Lentzsch nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.jgoodies.plaf.windows;

import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;
import javax.swing.JPasswordField;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.Element;
import javax.swing.text.PasswordView;
import javax.swing.text.View;

import com.sun.java.swing.plaf.windows.WindowsPasswordFieldUI;

/**
 * The JGoodies Windows Look&amp;Feel implementation of a password field UI
 * delegate. It differs from its superclass in that it utilizes a password 
 * view that renders a circle, not a star (&quot;*&quot;) character.
 * 
 * @author Karsten Lentzsch
 * @version $Revision: 1.1.1.1 $
 */
public final class ExtWindowsXPPasswordFieldUI extends WindowsPasswordFieldUI {

    /**
	 * Creates a UI for a {@link JPasswordField}.
	 * 
	 * @param c the password field component
	 * @return the UI
	 */
    public static ComponentUI createUI(JComponent c) {
        return new ExtWindowsXPPasswordFieldUI();
    }

    /**
	 * Creates a view (an <code>ExtWindowsXPPasswordView</code>) for an element.
	 * 
	 * @param elem the element
	 * @return the view
	 */
    public View create(Element elem) {
        return new ExtWindowsXPPasswordView(elem);
    }

    // Helper Class ***********************************************************

    private static class ExtWindowsXPPasswordView extends PasswordView {

        private ExtWindowsXPPasswordView(Element element) {
            super(element);
        }

        /*
         * Overrides the superclass behavior to paint a filled circle,
         * not the star (&quot;*&quot;) character.
         */
        protected int drawEchoCharacter(Graphics g, int x, int y, char c) {
            Container container = getContainer();
            if (!(container instanceof JPasswordField))
                return super.drawEchoCharacter(g, x, y, c);

            JPasswordField field = (JPasswordField) container;
            int charWidth = getFontMetrics().charWidth(field.getEchoChar());
            int advance   = 2; // want 3
            int diameter  = Math.max(charWidth - 2, 5); // want - 1

            Graphics2D g2 = (Graphics2D) g;
            Object oldHints = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Try to vertically align the circle with the font base line.
            g.fillOval(x, y - diameter, diameter, diameter);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldHints);

            return x + diameter + advance;
        }
        
//        public float getPreferredSpan(int axis) {
//            switch (axis) {
//            case View.X_AXIS:
//                Container c = getContainer();
//                if (c instanceof JPasswordField) {
//                    JPasswordField f = (JPasswordField) c;
//                    if (f.echoCharIsSet()) {
//                        int charWidth = getFontMetrics().charWidth(f.getEchoChar());
//                        int advance   = 3;
//                        int diameter  = Math.max(charWidth - 1, 5);
//                        return (diameter + advance) * getDocument().getLength();
//                    }
//                }
//            }
//            return super.getPreferredSpan(axis);
//        }
//        
    }

}