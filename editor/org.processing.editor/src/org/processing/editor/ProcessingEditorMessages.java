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
package org.processing.editor;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Processing Editor Messages object  handles localization stuff
 * using the ProcessingEditorMessages.preferences file. This class
 * is never instantiated, and all of its variables and methods are 
 * static.
 * 
 * @author lonnen
 */
public class ProcessingEditorMessages {

	private static final String RESOURCE_BUNDLE= "org.processing.editor.ProcessingEditorMessages";//$NON-NLS-1$

	private static ResourceBundle fgResourceBundle= ResourceBundle.getBundle(RESOURCE_BUNDLE);

	private ProcessingEditorMessages() {
	}

	public static String getString(String key) {
		try {
			return fgResourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return "!" + key + "!";//$NON-NLS-2$ //$NON-NLS-1$
		}
	}
	
	public static ResourceBundle getResourceBundle() {
		return fgResourceBundle;
	}
}
