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


import java.text.MessageFormat;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.contentassist.*;

/**
 * Example Java completion processor. Should work unmodified for Processing.
 */
public class ProcessingCompletionProcessor implements IContentAssistProcessor {

	/**
	 * Simple content assist tip closer. The tip is valid in a range
	 * of 5 characters around its popup location.
	 */
	protected static class Validator implements IContextInformationValidator, IContextInformationPresenter {

		protected int fInstallOffset;

		/**
		 * {@inheritDoc}
		 */
		public boolean isContextInformationValid(int offset) {
			return Math.abs(fInstallOffset - offset) < 5;
		}

		/**
		 * {@inheritDoc}
		 */
		public void install(IContextInformation info, ITextViewer viewer, int offset) {
			fInstallOffset= offset;
		}
		
		/**
		 * {@inheritDoc}
		 */
		public boolean updatePresentation(int documentPosition, TextPresentation presentation) {
			return false;
		}
	}

	/**
	 * The list of proposals that populates the content assistance dialog box.
	 * 
	 * Dialog assistance is not context sensitive right now, so it will always provide the same suggestions
	 * anytime is provides suggestions. I find it more annoying than useful, so until it is done right I'm
	 * turning off the pop up box by leaving this empty.
	 * 
	 * Auto-completion by hotkey will still work, but it will pick the closest keyword or variable that is
	 * already present in the sketch. This is actually useful, so I'm leaving that intact for now.
	 * 
	 * [lonnen] June 17, 2010
	 * 
	 * @see http://help.eclipse.org/help33/index.jsp?topic=/org.eclipse.platform.doc.isv/guide/jface.htm
	 */
	protected final static String[] fgProposals= //ProcessingCodeScanner.getKeywords(); // naive solution?
  		//generated automatically for Java. Not useful, ignoring them for now.
//		  { "abstract", "boolean", "break", "byte", "case", "catch", "char", "class", "continue", 
//		    "default", "do", "double", "else", "extends", "false", "final", "finally", "float", "for", 
//		    "if", "implements", "import", "instanceof", "int", "interface", "long", "native", "new", 
//		    "null", "package", "private", "protected", "public", "return", "short", "static", "super", 
//		    "switch", "synchronized", "this", "throw", "throws", "transient", "true", "try", "void", 
//		    "volatile", "while" }; 		
		  {}; // turns off the content assist dialog box
		
	protected IContextInformationValidator fValidator= new Validator();

	/* (non-Javadoc)
	 * Method declared on IContentAssistProcessor
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {
		ICompletionProposal[] result= new ICompletionProposal[fgProposals.length];
		for (int i= 0; i < fgProposals.length; i++) {
			IContextInformation info= new ContextInformation(fgProposals[i], MessageFormat.format(ProcessingEditorMessages.getString("CompletionProcessor.Proposal.ContextInfo.pattern"), new Object[] { fgProposals[i] })); //$NON-NLS-1$
			result[i]= new CompletionProposal(fgProposals[i], documentOffset, 0, fgProposals[i].length(), null, fgProposals[i], info, MessageFormat.format(ProcessingEditorMessages.getString("CompletionProcessor.Proposal.hoverinfo.pattern"), new Object[] { fgProposals[i]})); //$NON-NLS-1$
		}
		return result;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int documentOffset) {
		IContextInformation[] result= new IContextInformation[5];
		for (int i= 0; i < result.length; i++)
			result[i]= new ContextInformation(
				MessageFormat.format(ProcessingEditorMessages.getString("CompletionProcessor.ContextInfo.display.pattern"), new Object[] { new Integer(i), new Integer(documentOffset) }),  //$NON-NLS-1$
				MessageFormat.format(ProcessingEditorMessages.getString("CompletionProcessor.ContextInfo.value.pattern"), new Object[] { new Integer(i), new Integer(documentOffset - 5), new Integer(documentOffset + 5)})); //$NON-NLS-1$
		return result;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public char[] getCompletionProposalAutoActivationCharacters() {
		return new char[] { '.', '(' };
	}
	
	/**
	 * {@inheritDoc}
	 */
	public char[] getContextInformationAutoActivationCharacters() {
		return new char[] { '#' };
	}
	
	/**
	 * {@inheritDoc}
	 */
	public IContextInformationValidator getContextInformationValidator() {
		return fValidator;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getErrorMessage() {
		return null;
	}
}
