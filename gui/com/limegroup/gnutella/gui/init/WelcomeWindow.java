package com.limegroup.gnutella.gui.init;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.URLLabel;

/**
 * this class displays information welcoming the user to the
 * setup wizard.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
final class WelcomeWindow extends SetupWindow {
    
    private static final String TEXT1 = I18n.tr("FrostWire is a Peer to Peer File Sharing Application that enables you to share the files of your choosing with other users connected to the Gnutella and BitTorrent Networks.");
    private static final String TEXT2 = I18n.tr("FrostWire is a peer-to-peer program for sharing authorized files only.  Installing and using the program does not consitute a license for obtaining or distributing unauthorized content.");
    
	/**
	 * Creates the window and its components
	 */
	WelcomeWindow(SetupManager manager, boolean partial) {
		super(manager, I18nMarker.marktr("Welcome"), partial ?
		    I18nMarker
                    .marktr("Welcome to the FrostWire setup wizard. FrostWire has recently added new features that require your configuration. FrostWire will guide you through a series of steps to configure these new features.") : I18nMarker
                    .marktr("Welcome to the FrostWire setup wizard. FrostWire will guide you through a series of steps to configure FrostWire for optimum performance."));
	}
	
	public Icon getIcon() {
		return GUIMediator.getThemeImage("logo");
	}
	
	@Override
	protected void createWindow() {
	    super.createWindow();
	    
	    JPanel panel = new JPanel(new GridBagLayout());
	    
	    GridBagConstraints c;
	    
	    JComponent label1 = createPanel(TEXT1, TEXT2);
	    c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(0, 0, 100, 0);
        panel.add(label1, c);
        
	    setSetupComponent(panel);
	}
	
	private JComponent createPanel(String text1, String text2) {
	    
	    JPanel panel = new JPanel();
	    panel.setBackground(GUIUtils.hexToColor("F7F7F7"));
	    panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GUIUtils.hexToColor("C8C8C8"), 1),
                BorderFactory.createLineBorder(GUIUtils.hexToColor("FBFBFB"), 3)));
	    panel.setLayout(new GridBagLayout());

	    GridBagConstraints c = new GridBagConstraints();
	    
	    com.limegroup.gnutella.gui.MultiLineLabel label1 = new com.limegroup.gnutella.gui.MultiLineLabel(text1, 500);
	    label1.setFont(label1.getFont().deriveFont(16f));
        label1.setForeground(GUIUtils.hexToColor("333333"));
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        c.insets = new Insets(10, 10, 10, 10);
        panel.add(label1, c);
        
        com.limegroup.gnutella.gui.MultiLineLabel label2 = new com.limegroup.gnutella.gui.MultiLineLabel(text2, 500);
        label2.setFont(label2.getFont().deriveFont(16f));
        label2.setForeground(GUIUtils.hexToColor("333333"));
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        c.insets = new Insets(10, 10, 10, 10);
        panel.add(label2, c);
        
        com.limegroup.gnutella.gui.MultiLineLabel label3 = new com.limegroup.gnutella.gui.MultiLineLabel(I18n.tr("FrostWire is free software, "), 500);
        label3.setFont(label3.getFont().deriveFont(16f));
        label3.setForeground(GUIUtils.hexToColor("333333"));
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.RELATIVE;
        c.insets = new Insets(10, 10, 10, 0);
        panel.add(label3, c);
        
        URLLabel findMore = new URLLabel("http://frostwire.wordpress.com/2007/03/19/dont-get-scammed-frostwire-is-free/", I18n.tr("Do not pay for FrostWire."));
        findMore.setFont(findMore.getFont().deriveFont(16f));
        findMore.setForeground(GUIUtils.hexToColor("333333"));
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(10, 0, 10, 300);
        panel.add(findMore, c);        
	    
	    return panel;
	}
}
