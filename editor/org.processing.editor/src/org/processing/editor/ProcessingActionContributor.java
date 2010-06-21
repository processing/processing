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


import org.eclipse.jface.action.*;
import org.eclipse.ui.*;
import org.eclipse.ui.editors.text.TextEditorActionContributor;
import org.eclipse.ui.texteditor.*;

/**
 * Contributes content assistance actions to the desktop's Edit menu and the toolbar.
 */
public class ProcessingActionContributor extends TextEditorActionContributor {

	protected RetargetTextEditorAction fContentAssistProposal;
	protected RetargetTextEditorAction fContentAssistTip;
	protected TextEditorAction fTogglePresentation;

	/**
	 * Default constructor.
	 */
	public ProcessingActionContributor() {
		super();
		fContentAssistProposal= new RetargetTextEditorAction(ProcessingEditorMessages.getResourceBundle(), "ContentAssistProposal.");
		fContentAssistProposal.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS); 
		fContentAssistTip= new RetargetTextEditorAction(ProcessingEditorMessages.getResourceBundle(), "ContentAssistTip.");
		fContentAssistTip.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_CONTEXT_INFORMATION);
		fTogglePresentation= new PresentationAction();
	}
	
	/**
	 * Initializes the edit menu actions invoking content assistance and the assitance tooltip
	 * These are functional items, but of (extremely?) limited usefulness.
	 */
	public void init(IActionBars bars) {
		super.init(bars);
		
		IMenuManager menuManager= bars.getMenuManager();
		IMenuManager editMenu= menuManager.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
		if (editMenu != null) {
			editMenu.add(new Separator());
			editMenu.add(fContentAssistProposal);
			editMenu.add(fContentAssistTip);
		}	
		
		IToolBarManager toolBarManager= bars.getToolBarManager();
		if (toolBarManager != null) {
			toolBarManager.add(new Separator());
			toolBarManager.add(fTogglePresentation);
		}
	}
	
	/**
	 * Adds content assistance and tooltip actions to the Processing Editor
	 */
	private void doSetActiveEditor(IEditorPart part) {
		super.setActiveEditor(part);

		ITextEditor editor= null;
		if (part instanceof ITextEditor)
			editor= (ITextEditor) part;

		fContentAssistProposal.setAction(getAction(editor, "ContentAssistProposal")); //$NON-NLS-1$
		fContentAssistTip.setAction(getAction(editor, "ContentAssistTip")); //$NON-NLS-1$

		fTogglePresentation.setEditor(editor);
		fTogglePresentation.update();
	}
	
	/**
	 * @see doSetActiveEditor
	 */
	public void setActiveEditor(IEditorPart part) {
		super.setActiveEditor(part);
		doSetActiveEditor(part);
	}
	
	/**
	 * Shut it down
	 */
	public void dispose() {
		doSetActiveEditor(null);
		super.dispose();
	}
}
