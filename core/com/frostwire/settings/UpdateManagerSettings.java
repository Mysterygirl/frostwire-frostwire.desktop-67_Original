package com.frostwire.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.StringSetting;

import com.limegroup.gnutella.settings.LimeProps;

public class UpdateManagerSettings extends LimeProps {
	private UpdateManagerSettings() {

	}
	
	/** Wether or not to show promotion overlays */
	public static BooleanSetting SHOW_PROMOTION_OVERLAYS = (BooleanSetting) FACTORY.createBooleanSetting("SHOW_PROMOTION_OVERLAYS", true).setAlwaysSave(true);
	
	/** URL to feed the Slideshow with the promotional frostclick overlays */
	public static StringSetting OVERLAY_SLIDESHOW_JSON_URL = FACTORY.createStringSetting("OVERLAY_SLIDESHOW_JSON_URL", "http://update.frostwire.com/o.php");
}
