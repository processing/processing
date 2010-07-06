package org.processing.actions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.StringWriter;
import java.util.Hashtable;

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
import processing.app.Preferences; // test test 1, 2

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

		// Get program as a string -- editorContents contains the program as a string
		spoof_preferences();// fake the preferences object.
		
		// Fire up that preprocessor
		try{
			PdePreprocessor preproc = new PdePreprocessor("test"); // PdePreprocessor("sketch name", tabWidth), hard coded for now
			
			StringWriter feedback = new StringWriter();
			PreprocessResult result = preproc.write(feedback, editorContents);
			ProcessingLog.logInfo(feedback.toString());
		}catch(antlr.RecognitionException re){ // adapted from Sketch.java preprocess method
			String msg = re.getMessage();
			
			// skipped some stuff to find where the error appeared.
			// at some point it should be put in here.
			// lonnen july 5 2010
			
			// changed these helpers to change what is logged
			// instead of throwing an exception
			if (msg.equals("expecting RCURLY, found 'null'")) {
		        // This can be a problem since the error is sometimes listed as a line
		        // that's actually past the number of lines. For instance, it might
		        // report "line 15" of a 14 line program. Added code to highlightLine()
		        // inside Editor to deal with this situation (since that code is also
		        // useful for other similar situations).
//		        throw new RunnerException("Found one too many { characters " +
//		                                  "without a } to match it.",
//		                                  errorFile, errorLine, re.getColumn());
		        msg = "Found one too many { characters without a } to match it.";
		      }

		      if (msg.indexOf("expecting RBRACK") != -1) {
		        System.err.println(msg);
//		        throw new RunnerException("Syntax error, " +
//		                                  "maybe a missing ] character?",
//		                                  errorFile, errorLine, re.getColumn());
		        msg = "Syntax error, maybe a missing right ] character?";
		      }

		      if (msg.indexOf("expecting SEMI") != -1) {
		        System.err.println(msg);
//		        throw new RunnerException("Syntax error, " +
//		                                  "maybe a missing semicolon?",
//		                                  errorFile, errorLine, re.getColumn());
		        msg = "Syntax error, maybe a missing semicolon?";
		      }

		      if (msg.indexOf("expecting RPAREN") != -1) {
		        System.err.println(msg);
//		        throw new RunnerException("Syntax error, " +
//		                                  "maybe a missing right parenthesis?",
//		                                  errorFile, errorLine, re.getColumn());
		        msg = "Syntax error, maybe a missing right parenthesis?";
		      }

		      if (msg.indexOf("preproc.web_colors") != -1) {
//		        throw new RunnerException("A web color (such as #ffcc00) " +
//		                                  "must be six digits.",
//		                                  errorFile, errorLine, re.getColumn(), false);
		        msg = "A web color (such as #ffcc00) must be six digits.";
		      }

		      //System.out.println("msg is " + msg);
		      // throw new RunnerException(msg, errorFile, errorLine, re.getColumn());

		      // if there is no friendly translation, just report what you can
  			  ProcessingLog.logError(msg, re);
			
		} catch (Exception e){
			ProcessingLog.logError(e);
		}
//		 do something with the results
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
	
	/**
	 * Sets up the Static processing.app.Preferences class. 
	 */
	private void spoof_preferences(){
		Preferences.set("editor.tabs.size", "4");
		Preferences.set("preproc.substitute_floats","true");
		Preferences.set("preproc.web_colors", "true");
		Preferences.set("preproc.color_datatype", "true");
		Preferences.set("preproc.enhanced_casting", "true");
		Preferences.set("preproc.substitute.unicode", "true");
		Preferences.set("preproc.output_parse.tree", "false");
		Preferences.set("export.application.fullscreen", "false");
		Preferences.set("run.present.bgcolor", "#666666");
		Preferences.set("export.application.stop", "true");
		Preferences.set("run.present.stop.color", "#cccccc");
		Preferences.set("run.window.bgcolor", "#ECE9D8");
		Preferences.set("preproc.imports.list", "java.applet.*,java.awt.Dimension,java.awt.Frame,java.awt.event.MouseEvent,java.awt.event.KeyEvent,java.awt.event.FocusEvent,java.awt.Image,java.io.*,java.net.*,java.text.*,java.util.*,java.util.zip.*,java.util.regex.*");
	}
	
}
