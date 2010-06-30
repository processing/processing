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
package org.processing.editor.util;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

/**
 * Manager for colors used in the Java editor
 */
public class ProcessingColorProvider {

	public static final RGB COMMENT1= new RGB(126, 126, 126); //comment 1
	public static final RGB COMMENT2= new RGB(126, 126, 126); //comment 2
	public static final RGB KEYWORD1= new RGB(204, 102, 0);
	public static final RGB KEYWORD2= new RGB(204, 102, 0);
	public static final RGB KEYWORD3= new RGB(204, 102, 0);
	public static final RGB LITERAL1= new RGB(0, 102, 153); // currently unused [lonnen] june 16, 2010
	public static final RGB LITERAL2= new RGB(0, 102, 153);
	public static final RGB LABEL= new RGB(0, 0, 128); // use listed as '?' in p5 doc
	public static final RGB OPERATOR= new RGB(0, 0, 0);
	public static final RGB INVALID= new RGB(126, 126, 126); // never used [lonnen] june 16, 2010
	public static final RGB STRING= new RGB(0, 102, 153);   
	public static final RGB DEFAULT= new RGB(0, 0, 0);
	
	// used to color JavaDoc by org.processing.editor.javadoc.JavaDocScanner
	// currently ignored, as JavaDoc is treated like normal multiline comments
	// left available to avoid compiler errors from the JavaDocScanner.java class
	// which is never instantiated but wants access to them anyway because the
	// compiler doesn't realize these things. [lonnen] june 16, 2010
	public static final RGB JAVADOC_KEYWORD= new RGB(126, 126, 126);
	public static final RGB JAVADOC_TAG= new RGB(126, 126, 126);
	public static final RGB JAVADOC_LINK= new RGB(126, 126, 126);
	public static final RGB JAVADOC_DEFAULT= new RGB(126, 126, 126);
	
	protected Map fColorTable= new HashMap(17);

	/**
	 * Release all of the color resources held onto by the receiver.
	 */	
	public void dispose() {
		Iterator e= fColorTable.values().iterator();
		while (e.hasNext())
			 ((Color) e.next()).dispose();
	}
	
	/**
	 * Return the color that is stored in the color table under the given RGB
	 * value.
	 * 
	 * @param rgb the RGB value 
	 * @return the color stored in the color table for the given RGB value
	 */
	public Color getColor(RGB rgb) {
		Color color= (Color) fColorTable.get(rgb);
		if (color == null) {
			color= new Color(Display.getCurrent(), rgb);
			fColorTable.put(rgb, color);
		}
		return color;
	}
}
