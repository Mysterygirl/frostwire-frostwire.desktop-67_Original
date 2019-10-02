package com.frostwire.gnutella.gui.tabs;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;

import com.frostwire.gnutella.gui.chat.ChatMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.tabs.AbstractTab;
import com.limegroup.gnutella.settings.ApplicationSettings;
/**
 * This class contains access to the chat tab properties.
 */
public final class ChatTab extends AbstractTab {

	/**
	 * Constant for the <tt>Component</tt> instance containing the 
	 * elements of this tab.
	 */
	private static JComponent COMPONENT;
	private static JPanel PANEL = new JPanel(new BorderLayout());
	
	//old style 
	private static ChatMediator CHAT_MEDIATOR;

	/**
	 * Construcs the connections tab.
	 *
	 * @param CHAT_MEDIATOR the <tt>ChatMediator</tt> instance
	 */
	public ChatTab(final ChatMediator CHAT_MEDIATOR) {
	//public ChatTab(final ChatMediator cm) {
		super(I18n.tr("Community Chat"),
		        I18n.tr("Show our community chat"), "chat_tab");
		//lime style
		COMPONENT = CHAT_MEDIATOR.getComponent();
		PANEL.add(COMPONENT);
		//CHAT_MEDIATOR = cm;		
		//PANEL.add(cm.getComponent());
	}

	public void storeState(boolean visible) {
        	ApplicationSettings.CHAT_VIEW_ENABLED.setValue(visible);
	}

	public JComponent getComponent() {
		return PANEL;
	}
}
