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


import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;

/**
 * A toolbar action which toggles the presentation model of the
 * connected text editor. The editor shows either the highlight range
 * only or always the whole document.
 */
public class PresentationAction extends TextEditorAction {

	/**
	 * Constructs and updates the action.
	 */
	public PresentationAction() {
		super(ProcessingEditorMessages.getResourceBundle(), "TogglePresentation.", null);
		update();
	}
	
	/**
	 * Toggles between showing the highlight range only and showing the whole
	 * document. This isn't currently enabled in the Processing editor, and
	 * was generated as stub code. I'm not sure how useful it is. I will do
	 * something better with this later.
	 * 
	 * [lonnen] june 21 2010
	 */
	public void run() {

		ITextEditor editor= getTextEditor();

		editor.resetHighlightRange();
		boolean show= editor.showsHighlightRangeOnly();
		setChecked(!show);
		editor.showHighlightRangeOnly(!show);
	}
	
	/**
	 * Changes the PresentationAction in light of changes to the editor.
	 */
	public void update() {
		setChecked(getTextEditor() != null && getTextEditor().showsHighlightRangeOnly());
		setEnabled(true);
	}
}
