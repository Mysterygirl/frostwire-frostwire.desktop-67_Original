/*
 * Copyright (c) 2001-2004 JGoodies Karsten Lentzsch. All Rights Reserved.
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicBorders;
import javax.swing.plaf.metal.MetalBorders;
import javax.swing.text.JTextComponent;

import com.jgoodies.plaf.LookUtils;
import com.jgoodies.plaf.common.ShadowPopupBorder;


/**
 * This class consists of a set of <code>Border</code>s used 
 * by the JGoodies Plastic Look and Feel UI delegates.
 *
 * @author Karsten Lentzsch
 * @version $Revision: 1.1.1.1 $
 */

final class PlasticBorders {


    // Accessing and Creating Borders ***************************************

    private static Border buttonBorder;
    private static Border comboBoxEditorBorder;
    private static Border comboBoxArrowButtonBorder;
    private static Border etchedBorder;
    private static Border flush3DBorder;
    private static Border menuBarHeaderBorder;
    private static Border menuBorder;
    private static Border menuItemBorder;
    private static Border popupMenuBorder;
    private static Border dropShadowPopupMenuBorder;
    private static Border rolloverButtonBorder;
    private static Border scrollPaneBorder;
    private static Border separatorBorder;
    private static Border textFieldBorder;
    private static Border thinLoweredBorder;
    private static Border thinRaisedBorder;
    private static Border toggleButtonBorder;
    private static Border toolBarHeaderBorder;


    /**
     * Returns a border instance for a <code>JButton</code>.
     * 
     * @return the lazily created button border
     */
    static Border getButtonBorder() {
        if (buttonBorder == null) {
            buttonBorder = new BorderUIResource.CompoundBorderUIResource(
                                    new ButtonBorder(),
                                    new BasicBorders.MarginBorder());
        }
        return buttonBorder;
    }

    /**
     * Returns a border for a <code>JComboBox</code>'s button.
     * 
     * @return the lazily created combo box arrow button border
     */
    static Border getComboBoxArrowButtonBorder() {
        if (comboBoxArrowButtonBorder == null) { 
            comboBoxArrowButtonBorder = new CompoundBorder(  // No UIResource
                    new ComboBoxArrowButtonBorder(),
                    new BasicBorders.MarginBorder());
        }
        return comboBoxArrowButtonBorder;
    }

    /**
     * Returns a border for a <code>JComboBox</code>'s editor.
     * 
     * @return the lazily created combo box editor border
     */
    static Border getComboBoxEditorBorder() {
        if (comboBoxEditorBorder == null) { 
            comboBoxEditorBorder = new CompoundBorder( // No UIResource
                            new ComboBoxEditorBorder(),
                            new BasicBorders.MarginBorder());
        }
        return comboBoxEditorBorder;
    }

    /**
     * Returns an etched border instance for <code>JMenuBar</code> or
     * <code>JToolBar</code>.
     * 
     * @return the lazily created etched border
     */
    static Border getEtchedBorder() {
        if (etchedBorder == null) {
            etchedBorder = new BorderUIResource.CompoundBorderUIResource(
                                    new EtchedBorder(),
                                    new BasicBorders.MarginBorder());
        }
        return etchedBorder;
    }

    /**
     * Returns a flushed 3D border.
     * 
     * @return the lazily created flushed 3D border
     */
    static Border getFlush3DBorder() {
        if (flush3DBorder == null) {
            flush3DBorder = new Flush3DBorder();
        }
        return flush3DBorder;
    }

    /**
     * Returns a border for a <code>JInternalFrame</code>.
     * 
     * @return an internal frame border
     */
    static Border getInternalFrameBorder() {
        return new InternalFrameBorder();
    }

    /**
     * Returns a special border for a <code>JMenuBar</code> that 
     * is used in a header just above a <code>JToolBar</code>.
     * 
     * @return the lazily created menu bar header border
     */
    static Border getMenuBarHeaderBorder() {
        if (menuBarHeaderBorder == null) {
            menuBarHeaderBorder = new BorderUIResource.CompoundBorderUIResource(
                                    new MenuBarHeaderBorder(),
                                    new BasicBorders.MarginBorder());
        }
        return menuBarHeaderBorder;
    }

    /**
     * Returns a border instance for a <code>JMenu</code>.
     * 
     * @return the lazily created menu border
     */
    static Border getMenuBorder() {
        if (menuBorder == null) {
            menuBorder = new BorderUIResource.CompoundBorderUIResource(
                            new MenuBorder(),
                            new BasicBorders.MarginBorder());
        }
        return menuBorder;
    }

    /**
     * Returns a border instance for a <code>JMenuItem</code>.
     * 
     * @return the lazily created menu item border
     */
    static Border getMenuItemBorder() {
        if (menuItemBorder == null) {
            menuItemBorder =
                new BorderUIResource(new BasicBorders.MarginBorder());
        }
        return menuItemBorder;
    }

    /**
     * Returns a border instance for a <code>JPopupMenu</code>.
     * 
     * @return the lazily created popup menu border
     */
    static Border getPopupMenuBorder() {
        if (popupMenuBorder == null) {
            popupMenuBorder = new PopupMenuBorder();
        }
        return popupMenuBorder;
    }

    /**
     * Returns a border instance with drop shadow for a <code>JPopupMenu</code>.
     * 
     * @return the lazily created popup menu border
     */
    static Border getDropShadowPopupMenuBorder() {
        if (dropShadowPopupMenuBorder == null) {
            dropShadowPopupMenuBorder = new BorderUIResource.CompoundBorderUIResource(
                    ShadowPopupBorder.getInstance(),
                    new PopupMenuBorder());
        }
        return dropShadowPopupMenuBorder;
    }

    /**
     * Returns a border for a <code>JInternalFrame</code>'s palette.
     * 
     * @return a border for an internal frame in palette mode
     */
    static Border getPaletteBorder() {
        return new PaletteBorder();
    }

    /**
     * Returns a rollover border for buttons in a <code>JToolBar</code>.
     * 
     * @return the lazily created rollover button border
     */
    static Border getRolloverButtonBorder() {
        if (rolloverButtonBorder == null) { 
            rolloverButtonBorder = new BorderUIResource.CompoundBorderUIResource(
                                    new RolloverButtonBorder(),
                                    new RolloverMarginBorder());
        }
        return rolloverButtonBorder;
    }

    /**
     * Returns a separator border instance for <code>JScrollPane</code>.
     * 
     * @return the lazily created scroll pane border
     */
    static Border getScrollPaneBorder() {
        if (scrollPaneBorder == null) {
            scrollPaneBorder = new ScrollPaneBorder();
        }
        return scrollPaneBorder;
    }

    /**
     * Returns a separator border instance for <code>JMenuBar</code> or
     * <code>JToolBar</code>.
     * 
     * @return the lazily created separator border
     */
    static Border getSeparatorBorder() {
        if (separatorBorder == null) {
            separatorBorder = new BorderUIResource.CompoundBorderUIResource(
                                    new SeparatorBorder(),
                                    new BasicBorders.MarginBorder());
        }
        return separatorBorder;
    }

    /**
     * Returns a border instance for a JTextField.
     * 
     * @return the lazily created text field border
     */
    static Border getTextFieldBorder() {
        if (textFieldBorder == null) {
            textFieldBorder = new BorderUIResource.CompoundBorderUIResource(
                                    new TextFieldBorder(),
                                    new BasicBorders.MarginBorder());
        }
        return textFieldBorder;
    }

    /**
     * Returns a thin lowered border.
     * 
     * @return the lazily created thin lowered border
     */
    static Border getThinLoweredBorder() {
        if (thinLoweredBorder == null) {
            thinLoweredBorder = new ThinLoweredBorder();
        }
        return thinLoweredBorder;
    }

    /**
     * Returns a thin raised border.
     * 
     * @return the lazily created thin raised border
     */
    static Border getThinRaisedBorder() {
        if (thinRaisedBorder == null) {
            thinRaisedBorder = new ThinRaisedBorder();
        }
        return thinRaisedBorder;
    }

    /**
     * Returns a border instance for a JToggleButton.
     * 
     * @return the lazily created toggle button border
     */
    static Border getToggleButtonBorder() {
        if (toggleButtonBorder == null) {
            toggleButtonBorder = new BorderUIResource.CompoundBorderUIResource(
                                    new ToggleButtonBorder(),
                                    new BasicBorders.MarginBorder());
        }
        return toggleButtonBorder;
    }

    /**
     * Returns a special border for a <code>JToolBar</code> that 
     * is used in a header just below a <code>JMenuBar</code>.
     * 
     * @return the lazily created toolbar header border
     */
    static Border getToolBarHeaderBorder() {
        if (toolBarHeaderBorder == null) { 
            toolBarHeaderBorder = new BorderUIResource.CompoundBorderUIResource(
                                    new ToolBarHeaderBorder(),
                                    new BasicBorders.MarginBorder());
        }
        return toolBarHeaderBorder;
    }

	private static class Flush3DBorder extends AbstractBorder implements UIResource {

		private static final Insets INSETS = new Insets(2, 2, 2, 2);

		public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
			if (c.isEnabled())
				PlasticUtils.drawFlush3DBorder(g, x, y, w, h);
			else
				PlasticUtils.drawDisabledBorder(g, x, y, w, h);
		}
		
		public Insets getBorderInsets(Component c) { return INSETS; }
		
		public Insets getBorderInsets(Component c, Insets newInsets) {
			newInsets.top	 = INSETS.top;
			newInsets.left	 = INSETS.left;
			newInsets.bottom = INSETS.bottom;
			newInsets.right	 = INSETS.right;
			return newInsets;
		}
	}
	
	
	private static class ButtonBorder extends AbstractBorder implements UIResource {

		protected static final Insets INSETS = LookUtils.IS_LOW_RESOLUTION
            ? new Insets(2, 3, 3, 3)
            : new Insets(1, 3, 1, 3);

		public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
			AbstractButton button = (AbstractButton) c;
			ButtonModel model = button.getModel();

			if (model.isEnabled()) {
				boolean isPressed = model.isPressed() && model.isArmed();
				boolean isDefault = button instanceof JButton 
									 && ((JButton) button).isDefaultButton();

				if (isPressed && isDefault) 
					PlasticUtils.drawDefaultButtonPressedBorder(g, x, y, w, h);
				else if (isPressed)
					PlasticUtils.drawPressed3DBorder(g, x, y, w, h);
				else if (isDefault)
					PlasticUtils.drawDefaultButtonBorder(g, x, y, w, h, false);
				else
					PlasticUtils.drawButtonBorder(g, x, y, w, h, false);
			} else { // disabled state
				PlasticUtils.drawDisabledBorder(g, x, y, w - 1, h - 1);
			}
		}

		public Insets getBorderInsets(Component c) { return INSETS; }
		
		public Insets getBorderInsets(Component c, Insets newInsets) {
			newInsets.top	 = INSETS.top;
			newInsets.left	 = INSETS.left;
			newInsets.bottom = INSETS.bottom;
			newInsets.right  = INSETS.right;
			return newInsets;
		}
	}	
	
	
	private static class ComboBoxArrowButtonBorder extends AbstractBorder implements UIResource {

	    protected static final Insets INSETS = new Insets(1, 1, 1, 1);

	    public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
	        AbstractButton button = (AbstractButton) c;
	        ButtonModel model = button.getModel();

	        if (model.isEnabled()) {
	            boolean isPressed = model.isPressed() && model.isArmed();

	            if (isPressed)
	                PlasticUtils.drawPressed3DBorder(g, x, y, w, h);
	            else
	                PlasticUtils.drawButtonBorder(g, x, y, w, h, false);
	        } else {
	            PlasticUtils.drawDisabledBorder(g, x, y, w - 1, h - 1);
	        }
	    }

	    public Insets getBorderInsets(Component c) { return INSETS; }
	}	

	
	private static class ComboBoxEditorBorder extends AbstractBorder {

        private static final Insets INSETS  = new Insets(2, 2, 2, 0);

        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            if (c.isEnabled())
                PlasticUtils.drawFlush3DBorder(g, x, y, w + 2, h);
            else {
                PlasticUtils.drawDisabledBorder(g, x, y, w + 2, h-1);
                g.setColor(UIManager.getColor("control"));
                g.drawLine(x, y + h-1, x + w, y + h-1);
            }
        }

        public Insets getBorderInsets(Component c) { return INSETS; }
    }


	/**
	 * A border used for <code>JInternalFrame</code>s.
	 */
    private static class InternalFrameBorder extends AbstractBorder implements UIResource {

        private static final Insets NORMAL_INSETS	= new Insets(2, 2, 3, 3);
        private static final Insets MAXIMIZED_INSETS	= new Insets(2, 2, 2, 2);
				 static final int   ALPHA1			= 150;
				 static final int   ALPHA2			=  50;


		public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
			JInternalFrame frame = (JInternalFrame) c;
			if (frame.isMaximum())
				PlasticUtils.drawFlush3DBorder(g, x, y, w, h);
			else
				paintShadowedBorder(g, x, y, w, h);
		}
		
		private void paintShadowedBorder(Graphics g, int x, int y, int w, int h) {
			Color background	= UIManager.getColor("desktop");
			Color highlight		= UIManager.getColor("controlLtHighlight");
			Color darkShadow    = UIManager.getColor("controlDkShadow");
			Color lightShadow   = new Color(darkShadow.getRed(), 
											darkShadow.getGreen(), 
											darkShadow.getBlue(), 
											ALPHA1);
			Color lighterShadow = new Color(darkShadow.getRed(), 
											darkShadow.getGreen(), 
											darkShadow.getBlue(), 
											ALPHA2);
			g.translate(x, y);
			// Dark border 
			g.setColor(darkShadow);
			g.drawRect(0,   0, w-3, h-3);
			// Highlight top and left
			g.setColor(highlight);
			g.drawLine(1, 1, w - 4, 1);
			g.drawLine(1, 1, 1, h - 4);
			// Paint background before painting the shadow
			g.setColor(background);
			g.fillRect(w - 2, 0, 2, h);
			g.fillRect(0, h-2, w, 2);
			// Shadow line 1
			g.setColor(lightShadow);
			g.drawLine(w - 2, 1, w - 2, h - 2);
			g.drawLine(1, h - 2, w - 3, h - 2);
			// Shadow line2
			g.setColor(lighterShadow);
			g.drawLine(w - 1, 2, w - 1, h - 2);
			g.drawLine(2, h - 1, w - 2, h - 1);
			g.translate(-x, -y);
		}

	    public Insets getBorderInsets(Component c) { 
	    	return ((JInternalFrame) c).isMaximum() ? MAXIMIZED_INSETS : NORMAL_INSETS;
	    }
    }
    
    
	/**
	 * A border used for the palette of <code>JInternalFrame</code>s.
	 */
    private static class PaletteBorder extends AbstractBorder implements UIResource {
    	
        private static final Insets INSETS = new Insets(1, 1, 1, 1);

        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h ) {  
		    g.translate(x,y);  
		    g.setColor(PlasticLookAndFeel.getControlDarkShadow());
		    g.drawRect(0, 0, w-1, h-1);
		    g.translate(-x,-y);
		}

        public Insets getBorderInsets(Component c) { return INSETS; }
    }
    

	/**
	 * A border that looks like a separator line; used for menu bars 
     * and tool bars.
	 */	
	private static class SeparatorBorder extends AbstractBorder implements UIResource {

		private static final Insets INSETS = new Insets(0, 0, 2, 1);

		public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
			g.translate(x, y);
	  		g.setColor( UIManager.getColor("Separator.foreground"));
	  		g.drawLine( 0, h - 2, w - 1, h - 2 );

	  		g.setColor( UIManager.getColor("Separator.background"));
	  		g.drawLine( 0, h - 1, w - 1, h - 1 );
			g.translate(-x, -y);
		}
		public Insets getBorderInsets(Component c) { return INSETS; }
	}


	private static class ThinRaisedBorder extends AbstractBorder implements UIResource {
		private static final Insets INSETS = new Insets(2, 2, 2, 2);

		public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
			PlasticUtils.drawThinFlush3DBorder(g, x, y, w, h);
		}

		public Insets getBorderInsets(Component c) { return INSETS; }
	}
	

	private static class ThinLoweredBorder extends AbstractBorder implements UIResource {
		private static final Insets INSETS = new Insets(2, 2, 2, 2);

		public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
			PlasticUtils.drawThinPressed3DBorder(g, x, y, w, h);
		}

		public Insets getBorderInsets(Component c) { return INSETS; }
	}
	

	/**
	 * A border used for menu bars and tool bars in 
     * <code>HeaderStyle.SINGLE</code>. The bar is wrapped by an inner thin
     * raised border, which in turn is wrapped by an outer thin lowered
     * border.
	 */	
	private static class EtchedBorder extends AbstractBorder implements UIResource {

		private static final Insets INSETS = new Insets(2, 2, 2, 2);

		public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
			PlasticUtils.drawThinPressed3DBorder(g, x, y, w, h);
			PlasticUtils.drawThinFlush3DBorder  (g, x + 1, y + 1, w - 2, h - 2);
		}

		public Insets getBorderInsets(Component c) { return INSETS; }
	}
	
	
	/**
	 * A border used for menu bars in <code>HeaderStyle.BOTH</code>.
	 * The menu bar and tool bar are wrapped by a thin raised border,
	 * both together are wrapped by a thin lowered border.
	 */
	private static class MenuBarHeaderBorder extends AbstractBorder implements UIResource {

		private static final Insets INSETS = new Insets(2, 2, 1, 2);

		public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
			PlasticUtils.drawThinPressed3DBorder(g, x, y, w, h + 1);
			PlasticUtils.drawThinFlush3DBorder  (g, x + 1, y + 1, w - 2, h - 1);
		}

		public Insets getBorderInsets(Component c) { return INSETS; }
	}
	
	
	/**
	 * A border used for tool bars in <code>HeaderStyle.BOTH</code>.
	 * The menu bar and tool bar are wrapped by a thin raised border,
	 * both together are wrapped by a thin lowered border.
	 */
	private static class ToolBarHeaderBorder extends AbstractBorder implements UIResource {

		private static final Insets INSETS = new Insets(1, 2, 2, 2);

		public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
			PlasticUtils.drawThinPressed3DBorder(g, x, y - 1, w, h + 1);
			PlasticUtils.drawThinFlush3DBorder  (g, x + 1, y, w - 2, h - 1);
		}

		public Insets getBorderInsets(Component c) { return INSETS; }
	}
	
	
	private static class MenuBorder extends AbstractBorder implements UIResource {
        private static final Insets INSETS = new Insets( 2, 2, 2, 2 );

		public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
			JMenuItem b = (JMenuItem) c;
			ButtonModel model = b.getModel();

			if (model.isArmed() || model.isSelected()) {
	            g.setColor(PlasticLookAndFeel.getControlDarkShadow());
	            g.drawLine(0, 0, w - 2, 0 );
	            g.drawLine(0, 0, 0, h - 1 );
	            //g.drawLine(w - 2, 2, w - 2, h - 1 );

	            g.setColor(PlasticLookAndFeel.getPrimaryControlHighlight());
	            g.drawLine(w - 1, 0, w - 1, h - 1 );
			} else if (model.isRollover()) {
				g.translate(x, y);
				PlasticUtils.drawFlush3DBorder(g, x, y, w, h);
				g.translate(-x, -y);
			}
		}
		
        public Insets getBorderInsets(Component c) { return INSETS; }

        public Insets getBorderInsets(Component c, Insets newInsets) {
	    	newInsets.top	 = INSETS.top;
	    	newInsets.left	 = INSETS.left;
	    	newInsets.bottom = INSETS.bottom;
	    	newInsets.right	 = INSETS.right;
	    	return newInsets;
		}
	}


	private static class PopupMenuBorder extends AbstractBorder implements UIResource {
		private static final Insets INSETS = new Insets(3, 3, 3, 3);

		public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
			g.translate(x, y);
			g.setColor(PlasticLookAndFeel.getControlDarkShadow());
			g.drawRect(0, 0, w-1, h-1);
            g.setColor(PlasticLookAndFeel.getMenuItemBackground());
			g.drawRect(1, 1, w-3, h-3);
			g.drawRect(2, 2, w-5, h-5);
			g.translate(-x, -y);
		}

		public Insets getBorderInsets(Component c) { return INSETS; }
	}
	

	private static class RolloverButtonBorder extends ButtonBorder {
		private static final Insets INSETS_3 = new Insets(3, 3, 3, 3);

		public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
			AbstractButton b = (AbstractButton) c;
			ButtonModel model = b.getModel();

			if (!model.isEnabled())
				return;

			if (!(c instanceof JToggleButton)) {
            	if ( model.isRollover() && !( model.isPressed() && !model.isArmed() ) ) {
	        		super.paintBorder( c, g, x, y, w, h );
            	}
				return;
			}

			//if ( model.isRollover() && !( model.isPressed() && !model.isArmed() ) ) {
			//super.paintBorder( c, g, x, y, w, h );
			//}

			if (model.isRollover()) {
				if (model.isPressed() && model.isArmed()) {
					PlasticUtils.drawPressed3DBorder(g, x, y, w, h);
				} else {
					PlasticUtils.drawFlush3DBorder(g, x, y, w, h);
				}
			} else if (model.isSelected())
				PlasticUtils.drawDark3DBorder(g, x, y, w, h);
		}
		public Insets getBorderInsets(Component c) { return INSETS_3; }
	}
	
	
    /**
     * A border which is like a Margin border but it will only honor the margin
     * if the margin has been explicitly set by the developer.
     */
	private static class RolloverMarginBorder extends EmptyBorder {

        private RolloverMarginBorder() {
            super(1, 1, 1, 1); 
        }


        public Insets getBorderInsets(Component c) {
            return getBorderInsets(c, new Insets(0, 0, 0, 0));
        }


        public Insets getBorderInsets(Component c, Insets insets) {
            Insets margin = null;

            if (c instanceof AbstractButton) {
                margin = ((AbstractButton) c).getMargin();
            }
            if (margin == null || margin instanceof UIResource) {
                // default margin so replace
                insets.left = left;
                insets.top = top;
                insets.right = right;
                insets.bottom = bottom;
            } else {
                // Margin which has been explicitly set by the user.
                insets.left = margin.left;
                insets.top = margin.top;
                insets.right = margin.right;
                insets.bottom = margin.bottom;
            }
            return insets;
        }
    }

	/**
	 * Unlike Metal we don't paint the (misplaced) control color edges.
	 * Being a subclass of MetalBorders.ScrollPaneBorders ensures that
	 * the ScrollPaneUI will update the ScrollbarsFreeStanding property.
	 */
	private static class ScrollPaneBorder extends MetalBorders.ScrollPaneBorder {
		
		public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
			g.translate(x, y);

			g.setColor(PlasticLookAndFeel.getControlDarkShadow());
			g.drawRect(0, 0, w - 2, h - 2);
			g.setColor(PlasticLookAndFeel.getControlHighlight());
			g.drawLine(w - 1, 0, w - 1, h - 1);
			g.drawLine(0, h - 1, w - 1, h - 1);

			g.translate(-x, -y);
		}
	}
	
	
    private static class TextFieldBorder extends Flush3DBorder {
		public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
		
			if (!(c instanceof JTextComponent)) {
				// special case for non-text components (bug ID 4144840)
				if (c.isEnabled()) {
					PlasticUtils.drawFlush3DBorder(g, x, y, w, h);
				} else {
					PlasticUtils.drawDisabledBorder(g, x, y, w, h);
				}
				return;
			}
		
			if (c.isEnabled() && ((JTextComponent) c).isEditable())
				PlasticUtils.drawFlush3DBorder(g, x, y, w, h);
			else
				PlasticUtils.drawDisabledBorder(g, x, y, w, h);
		}    
	}


	private static class ToggleButtonBorder extends ButtonBorder {
		public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
			if (!c.isEnabled()) {
				PlasticUtils.drawDisabledBorder(g, x, y, w - 1, h - 1);
			} else {
				AbstractButton button = (AbstractButton) c;
				ButtonModel    model  = button.getModel();
				if (model.isPressed() && model.isArmed())
					PlasticUtils.drawPressed3DBorder(g, x, y, w, h);
				else if (model.isSelected())
					PlasticUtils.drawDark3DBorder(g, x, y, w, h);
				else
					PlasticUtils.drawFlush3DBorder(g, x, y, w, h);
			}
		}
	}	
	

}