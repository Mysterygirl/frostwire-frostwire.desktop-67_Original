/*
 *  Copyright (C) 2005 Sun Microsystems, Inc. All rights reserved. Use is
 *  subject to license terms.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License as
 *  published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 */
package org.jdesktop.jdic.misc;

import java.awt.Frame;

/**
 *  <p>This class sends alerts to the user when the application is not in
 *  the foreground. On Mac OS X this will bounce the dock icon until
 *  the user selects the application. On Windows, Gnome and KDE this will flash the 
 *  taskbar button/icon until the user selects the application. There is no support 
 *  for other platforms.
 *  If you would like to support another platform then please join the 
 *  JDIC mailing list at
 *  <a href="http://jdic.dev.java.net/">http://jdic.dev.java.net/</a></p>
 
 <p><b>Example:</b> To alert the user (only if the application is not in the 
 foreground) create an Alerter using the factory method and call alert:</p> 
 <pre><code> 
	Alerter alerter = Alerter.newInstance();
	alerter.alert(frame);
 </code></pre>
 *
 * @author     Joshua Marinacci <a href="mailto:joshua@marinacci.org">joshua@marinacci.org</a>
 * @created    April 8, 2005
 */
public class Alerter {
	
	/**
	 *  Implementation detail. Use factory method instead.
	 */
	protected Alerter() { }


	private static Alerter _alerter;

	/**
	 *  Factory Method: Returns a new Alerter object.
	 *  This will create the appropriate implementation for the currently
	 *  running platform. If an implementation is not available (the
	 *  current platform is unsupported) then this method will return a
	 *  dummy object that does nothing. This lets the developer safely
	 *  call the methods without worrying about which platform the
	 *  application is on.
	 *  The Alerter class is not thread-safe.
	 *
	 * @return Alerter for the current platform, or dummy.
	 */
	public static Alerter newInstance() throws IllegalAccessException,
		InstantiationException, ClassNotFoundException {
		if (_alerter == null) {
			String os_name  = System.getProperty("os.name");
			if (os_name.toLowerCase().startsWith("mac os x")) {
				loadMac();
			} else if (os_name.toLowerCase().startsWith("windows")) {
				loadWin();
			} else if (os_name.toLowerCase().startsWith("linux")) {
				loadLinux();
			} else {
				_alerter = new Alerter();
			}
		}
		return _alerter;
	}


	/**
	 *  Loads the Mac OS X implementation via reflection.
	 */
	private static void loadMac() throws IllegalAccessException, 
			InstantiationException, ClassNotFoundException{
		_alerter = (Alerter) Class.forName(
				"org.jdesktop.jdic.misc.impl.MacOSXAlerter")
				.newInstance();
	}
	
	/** loads the Windows implementation 
	*/
	private static void loadWin() throws IllegalAccessException, 
			InstantiationException, ClassNotFoundException{
		_alerter = (Alerter) Class.forName(
				"org.jdesktop.jdic.misc.impl.WinAlerter")
				.newInstance();
    }
	
	/** loads the Linux implementation 
	*/
	private static void loadLinux() throws IllegalAccessException, 
			InstantiationException, ClassNotFoundException{
		_alerter = (Alerter) Class.forName(
				"org.jdesktop.jdic.misc.impl.LinuxAlerter")
				.newInstance();
	}

	/**
	* Alert the user. On supported platforms, it will bounce/flash
	* the TaskBar button/Dock icon at a regular time to request user's attention.
	* On unsupported platforms it will do nothing. 
	* @param frame Frame to use when alerting the user, if the platform supports
	the notion of a per frame alert. Currently this parameter is required for
	Windows, Gnome and KDE.
	*/
	public void alert(Frame frame) {
	}
	
	/** Returns true if the current platform supports alerts. */
	public boolean isAlertSupported() {
		return false;
	}
}

