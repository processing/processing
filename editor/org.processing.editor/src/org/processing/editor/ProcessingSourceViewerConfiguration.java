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

import org.eclipse.swt.graphics.RGB;

import org.eclipse.jface.text.DefaultIndentLineAutoEditStrategy;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.BufferedRuleBasedScanner;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

//TODO implement completion properly for Processing 
//Removing JavaDoc support from sketches, but leave in code
//import org.processing.editor.javadoc.JavaDocCompletionProcessor;
import org.processing.editor.language.ProcessingAutoIndentStrategy;
import org.processing.editor.language.ProcessingCompletionProcessor;
import org.processing.editor.language.ProcessingDoubleClickSelector;
import org.processing.editor.util.ProcessingColorProvider;

/**
 * Configuration for the ProcessingSourceViewer.
 */
public class ProcessingSourceViewerConfiguration extends SourceViewerConfiguration {
	
	/**
	 * Single token scanner.
	 */
	static class SingleTokenScanner extends BufferedRuleBasedScanner {
		public SingleTokenScanner(TextAttribute attribute) {
			setDefaultReturnToken(new Token(attribute));
		}
	}	

	/**
	 * Default constructor.
	 */
	public ProcessingSourceViewerConfiguration() {
	}
	
	/**
	 * Starts up the mouse hover tooltips
	 */
	public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
		return new ProcessingAnnotationHover();
	}
		
	/**
	 * Returns the auto-indent strategy used when manipulating text in the editor. To maintain backwards
	 * compatibility with the Eclipse RCP, this always returns an array containing the auto indent strategy.
	 * 
	 * @param sourceViewer the source veiwer to be configured
	 * @param contentType the content type for which the strategies are applicable
	 * @return an auto indent strategy, or null
	 * @see org.processing.editor.language.ProcessingAutoIndentStrategy
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getAutoEditStrategies(org.eclipse.jface.text.source.ISourceViewer, java.lang.String)
	 */
	public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
		IAutoEditStrategy strategy= (IDocument.DEFAULT_CONTENT_TYPE.equals(contentType) ? new ProcessingAutoIndentStrategy() : new DefaultIndentLineAutoEditStrategy());
		return new IAutoEditStrategy[] { strategy };
	}
	
	/**
	 * Returns the Processing partitioning, because this editor only acts on .pde files
	 * 
	 * @param sourceViewer the source viewer to be configured
	 * @return a Processing partitioning
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getConfiguredDocumentPartitioning(org.eclipse.jface.text.source.ISourceViewer)
	 */
	public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
		return ProcessingEditorPlugin.PROCESSING_PARTITIONING;
	}
	
	/**
	 * Returns the partitioning types to the source viewer so it can distinguish between comments and everything else.
	 * 
	 * @param sourceViewer the source viewer to be configured
	 * @return a string array of the content types
	 */
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		//Removing JavaDoc support from sketches, but leave in code
		//return new String[] { IDocument.DEFAULT_CONTENT_TYPE, ProcessingPartitionScanner.JAVA_DOC, ProcessingPartitionScanner.JAVA_MULTILINE_COMMENT };
		return new String[] { IDocument.DEFAULT_CONTENT_TYPE, ProcessingPartitionScanner.JAVA_MULTILINE_COMMENT };
	}
	
	/**
	* Returns content assistance 
	* 
	* @param the source viewer the be configured
	* @return a content assistant of dubious utility
	*/
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {

		ContentAssistant assistant= new ContentAssistant();
		assistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
		assistant.setContentAssistProcessor(new ProcessingCompletionProcessor(), IDocument.DEFAULT_CONTENT_TYPE);
		// Disabling JavaDoc support [lonnen] june 12 2010
		//assistant.setContentAssistProcessor(new JavaDocCompletionProcessor(), ProcessingPartitionScanner.JAVA_DOC);

		assistant.enableAutoActivation(true);
		assistant.setAutoActivationDelay(500); // delay before content assist pops up
		assistant.setProposalPopupOrientation(IContentAssistant.PROPOSAL_OVERLAY);
		assistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
		assistant.setContextInformationPopupBackground(ProcessingEditorPlugin.getDefault().getProcessingColorProvider().getColor(new RGB(150, 150, 0)));

		return assistant;
	}
	
	/**
	 * Returns the default prefix to be used by the line-prefix operation in the editor.
	 * This is used for auto-commenting and uncommenting.
	 */
	public String getDefaultPrefix(ISourceViewer sourceViewer, String contentType) {
		return (IDocument.DEFAULT_CONTENT_TYPE.equals(contentType) ? "//" : null); //$NON-NLS-1$
	}
	
	/**
	 * {@inheritDoc}
	 */
	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
		return new ProcessingDoubleClickSelector();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String[] getIndentPrefixes(ISourceViewer sourceViewer, String contentType) {
		return new String[] { "\t", "    " };
	}
	
	/**
	 * {@inheritDoc}
	 */
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		
		ProcessingColorProvider provider= ProcessingEditorPlugin.getDefault().getProcessingColorProvider();
		
		PresentationReconciler reconciler= new PresentationReconciler();
		reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
		
		DefaultDamagerRepairer dr= new DefaultDamagerRepairer(ProcessingEditorPlugin.getDefault().getProcessingCodeScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
		
		//Removing JavaDoc support from sketches, but leave in code
//		dr= new DefaultDamagerRepairer(ProcessingEditorPlugin.getDefault().getProcessingDocScanner());
//		reconciler.setDamager(dr, ProcessingPartitionScanner.JAVA_DOC);
//		reconciler.setRepairer(dr, ProcessingPartitionScanner.JAVA_DOC);

		dr= new DefaultDamagerRepairer(new SingleTokenScanner(new TextAttribute(provider.getColor(ProcessingColorProvider.COMMENT1))));
		reconciler.setDamager(dr, ProcessingPartitionScanner.JAVA_MULTILINE_COMMENT);
		reconciler.setRepairer(dr, ProcessingPartitionScanner.JAVA_MULTILINE_COMMENT);

		return reconciler;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int getTabWidth(ISourceViewer sourceViewer) {
		return 4;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
		return new ProcessingTextHover();
	}
}
