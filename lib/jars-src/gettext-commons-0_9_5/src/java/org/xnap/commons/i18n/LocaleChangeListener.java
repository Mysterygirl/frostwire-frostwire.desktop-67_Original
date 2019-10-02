/*
 *  Gettext Commons
 *
 *  Copyright (C) 2005  Steffen Pingel
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.xnap.commons.i18n;

import java.util.Locale;

/**
 * Defines the requirements for classes that get notified when the locale
 * is changed. 
 * 
 * <p>A component that implements this interface may dynamically change the 
 * language of all user visible strings.  
 * 
 * @see org.xnap.commons.i18n.I18nManager#setDefaultLocale(Locale)
 * @author Steffen Pingel
 * @since 0.9
 */
public interface LocaleChangeListener {
	
	/**
	 * Invoked when the local has changed.
	 * 
	 * @param event provides information about the new locale
	 * @since 0.9
	 */
	void localeChanged(LocaleChangeEvent event);
	
}
