package org.processing.actions;

import java.io.StringWriter;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.processing.editor.ProcessingLog;

import processing.app.preproc.PdePreprocessor; // get the preprocessor stuff
import processing.app.preproc.PreprocessResult;

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
		// Get the program as a string
			// editorContents contains the program as a string
		// Generate a Preferences object, or enough of one to fake it.
		// Fire up that preprocessor
		try{ // we expect exceptions until the Preferences object is generated
			StringWriter feedback = new StringWriter();
			PdePreprocessor preproc = new PdePreprocessor("test", 4); // PdePreprocessor("sketch name", tabWidth), hard coded for now
			@SuppressWarnings("unused")
			PreprocessResult result = preproc.write(feedback, editorContents);
			ProcessingLog.logInfo(feedback.toString());
		} catch (Exception e){
			ProcessingLog.logError("Expected problem with the preprocessor.", e);
		}
		// do something with the results
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
