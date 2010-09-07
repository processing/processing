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
package processing.plugin.ui.processingeditor.language;

import java.text.MessageFormat;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationPresenter;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

/**
 * Naive auto completion provider.
 * <p>
 * None of this is particularly useful, but may be extended in the future.
 * 
 * 
 * @author lonnen
 */
public class ProcessingCompletionProcessor implements IContentAssistProcessor {

	/** Utility class with methods to determine if the context tip window is still valid */
	protected static class Validator implements IContextInformationValidator, IContextInformationPresenter {

		protected int fInstallOffset;

		/* @see IContextInformationValidator#isContextInformationValid(int) */
		public boolean isContextInformationValid(int offset) { return Math.abs(fInstallOffset - offset) < 5; }

		/* @see IContextInformationValidator#install(IContextInformation, ITextViewer, int) */
		public void install(IContextInformation info, ITextViewer viewer, int offset) { fInstallOffset= offset; }
		
		/* @see org.eclipse.jface.text.contentassist.IContextInformationPresenter#updatePresentation(int, TextPresentation) */
		public boolean updatePresentation(int documentPosition, TextPresentation presentation) { return false; }
	}
	
	/** Strings to populate the context assistance pop up box. */
	protected final static String[] fgProposals = {};
		
	/** Validates the context tip window.  */
	protected IContextInformationValidator fValidator= new Validator();

	/* Method declared on IContentAssistProcessor */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {
		ICompletionProposal[] result= new ICompletionProposal[fgProposals.length];
		return result; // not useful
	}
	
	/* Method declared on IContentAssistProcessor */
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int documentOffset) {
		IContextInformation[] result= new IContextInformation[5];
		for (int i= 0; i < result.length; i++)
			result[i]= new ContextInformation(
				MessageFormat.format("proposal {0} at position {1}", new Object[] { new Integer(i), new Integer(documentOffset) }),
				MessageFormat.format("proposal {0} valid from {1} to {2}", new Object[] { new Integer(i), new Integer(documentOffset - 5), new Integer(documentOffset + 5)}));
		return result;	}
	
	/* Method declared on IContentAssistProcessor */
	public char[] getCompletionProposalAutoActivationCharacters() {
		return new char[] { '.', '(' };
	}
	
	/* Method declared on IContentAssistProcessor */
	public char[] getContextInformationAutoActivationCharacters() {
		return new char[] { '#' };
	}
	
	/* Method declared on IContentAssistProcessor */
	public IContextInformationValidator getContextInformationValidator() {
		return fValidator;
	}
	
	/* Method declared on IContentAssistProcessor */
	public String getErrorMessage() {
		return null;
	}

}