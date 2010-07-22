package org.processing.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import org.processing.editor.ProcessingLog;

/**
 * Right now this just does some hello world test stuff.
 * Soon it will run the preprocessor only.
 * Eventually it will function like the run button in the PDE.
 * 
 * @author lonnen
 *
 */
public class RunButton implements IEditorActionDelegate {
	String editorContents = null;
	
	/** Main logic for the button */
	public void run(IAction action) {
		ProcessingLog.logInfo("Someone hit the toolbar button!");		
	}

	/** 
	 *  Notifies this action delegate that the selection in the workbench has changed.
	 *  
	 *  We're required to implement this, but right now it does nothing.
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		// ignore. we don't care about the selection, but we have to implement this 
	}

	/**
	 * Retrieved the editor contents when a new editor is set.
	 * This is messy but this is how we get the editor contents from
	 * the IEditorActionDelegate. When the preprocessor is implemented 
	 * as a proper builder it will be able to retrieve an up-to-date 
	 * copy of the editor contents.
	 */
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		if (targetEditor instanceof ITextEditor) {
			IDocumentProvider provider= ((ITextEditor) targetEditor).getDocumentProvider();
			IEditorInput input= targetEditor.getEditorInput();
			IDocument document= provider.getDocument(input);
			editorContents = document.get();
		}
	}
	
}
