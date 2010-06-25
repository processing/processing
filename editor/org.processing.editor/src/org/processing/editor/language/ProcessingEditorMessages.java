/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.processing.editor.language;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Handles localization stuff using the ProcessingEditorMessages.preferences 
 * file. This is never instantiated, and all of its variables and methods are 
 * static.
 * 
 * @author lonnen
 */
public class ProcessingEditorMessages {

	/** location of the resource bundle */
	private static final String RESOURCE_BUNDLE= "org.processing.editor.ProcessingEditorMessages";//$NON-NLS-1$

	/**  the resource bundle object itself*/
	static ResourceBundle fgResourceBundle= ResourceBundle.getBundle(RESOURCE_BUNDLE);

	private ProcessingEditorMessages() {
	}

	/**
	 * Gets a string for the given key from this resource bundle or one of its parents.
	 * Calling this method is equivalent to calling <code> (String) getObject(key) </code> 
	 * 
	 * @param key the key for the desired string
	 * @return the string for the given key
	 */
	public static String getString(String key) {
		try {
			return fgResourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return "!" + key + "!";
		}
	}
	
}
