/**
 * Copyright (c) 2010 Chris Lonnen. All rights reserved.
 *
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.opensource.org/licenses/eclipse-1.0.php
 * 
 * Contributors:
 *     Chris Lonnen - initial API and implementation
 */
package processing.plugin.ui.processingeditor.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

/** Manages the colors used in the Processing Editor */
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
	
	protected Map fColorTable= new HashMap(12); // number of color categories

	/** Release all of the color resources held onto by the receiver. */
	public void dispose() {
		Iterator e= fColorTable.values().iterator();
		while (e.hasNext())
			 ((Color) e.next()).dispose();
	}
	
	/**
	 * Convert an RGB value to a Color using the resource table.
	 * <p>
	 * Please note that this is an SWT color, not a Processing color.
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
