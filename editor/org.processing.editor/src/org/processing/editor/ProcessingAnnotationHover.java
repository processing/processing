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


import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;

/** 
 * Provides the information thats displayed in the presentation area of hover popup windows.
 * Right now this doesn't provide anything useful. It just hands back the contents of the line
 * the mouse is hovering on.
 * 
 * @see org.eclipse.jface.text.source.IAnnotationHover
 */
 
public class ProcessingAnnotationHover implements IAnnotationHover {

	/**
	 * Provides info for a given mouse hover position to the tooltip.
	 * Currently this means retrieving the text under the mouse.
	 */
	public String getHoverInfo(ISourceViewer sourceViewer, int lineNumber) {
		IDocument document= sourceViewer.getDocument();

		try {
			IRegion info= document.getLineInformation(lineNumber);
			return document.get(info.getOffset(), info.getLength());
		} catch (BadLocationException x) {
		}

		return null;
	}
	
}
