/*
 *  Copyright (C) 2004 Sun Microsystems, Inc. All rights reserved. Use is
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
package org.jdesktop.jdic.misc.test;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import java.io.*;

import org.jdesktop.jdic.misc.*;

/**
 *  Description of the Class
 *
 * @author     joshua@marinacci.org
 * @created    April 8, 2005
 */
public class WallpaperTest {

	/**
	 *  The main program for the AlerterTest class
	 *
	 * @param  args  The command line arguments
	 */
	public static void main(String[] args) throws Exception {
        Wallpaper wp = WallpaperFactory.createWallpaper();
        System.out.println("wp = " + wp);
        File file = new File(args[0]);
        System.out.println("file = " + file.toURL());
        wp.setBackground(file.getCanonicalPath(),Wallpaper.STRETCH);
	}
}

