package com.limegroup.gnutella.gui.options;

import java.awt.event.ActionEvent;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.actions.AbstractAction;
import com.limegroup.gnutella.gui.actions.LimeAction;

public class ConfigureOptionsAction extends AbstractAction {

	/**
	 * Resource key to go to in the options window
	 */
	private String paneTitle;
    
    public ConfigureOptionsAction(String pane) {
        paneTitle = pane;
    }
	
	public ConfigureOptionsAction(String pane, String name, String tooltip) {
        this(pane);
        putValue(NAME, name);
        putValue(SHORT_DESCRIPTION, tooltip);
        putValue(LimeAction.ICON_NAME,"LIBRARY_SHARING_OPTIONS");
    }

	/**
	 * Launches LimeWire's options with the given options pane selected.
	 */
	public void actionPerformed(ActionEvent e) {
		GUIMediator.instance().setOptionsVisible(true, paneTitle);
	}
}
