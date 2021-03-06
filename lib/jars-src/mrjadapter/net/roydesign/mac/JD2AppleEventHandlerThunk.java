/*******************************************************************************

	File:		JD2AppleEventHandlerThunk.java
	Author:		Steve Roy
	Copyright:	Copyright (c) 2003-2007 Steve Roy <sroy@mac.com>
				
	Part of MRJ Adapter, a unified API for easy integration of Mac OS specific
	functionality within your cross-platform Java application.
	
	This library is open source and can be modified and/or distributed under
	the terms of the Artistic License.
	<http://homepage.mac.com/sroy/mrjadapter/license.html>
	
	Change History:
	08/31/03	Created this file - Steve

*******************************************************************************/

package net.roydesign.mac;

import com.apple.mrj.jdirect.MethodClosureUPP;

/**
 * This class is a necessary wrapper used internally for event handling with
 * MRJ 2.x. It creates an object that can be used as a callback in the context
 * of JDirect 2.
 * 
 * @version MRJ Adapter 1.1
 */
class JD2AppleEventHandlerThunk extends MethodClosureUPP
{
	/**
	 * Construct an Apple event handler thunk.
	 * @param handle the Apple event handler to be wrapped
	 */
	public JD2AppleEventHandlerThunk(AppleEventHandler handler)
	{
		super(handler, "handleEvent", "(III)S", 0x00000FE0);
	}
}
