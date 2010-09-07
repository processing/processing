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
package processing.plugin.ui.processingeditor;

import org.eclipse.jface.text.DefaultIndentLineAutoEditStrategy;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
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
import org.eclipse.swt.graphics.RGB;

import processing.plugin.ui.ProcessingPlugin;

import processing.plugin.ui.processingeditor.language.ProcessingAutoIndentStrategy;
import processing.plugin.ui.processingeditor.language.ProcessingCompletionProcessor;
import processing.plugin.ui.processingeditor.language.ProcessingDoubleClickSelector;
import processing.plugin.ui.processingeditor.util.ProcessingColorProvider;

public class ProcessingSourceViewerConfiguration extends SourceViewerConfiguration {
	
	/** token scanner. */
	static class SingleTokenScanner extends BufferedRuleBasedScanner {
		public SingleTokenScanner(TextAttribute attribute) {
			setDefaultReturnToken(new Token(attribute));
		}
	}	

	/** a boring default constructor */
	public ProcessingSourceViewerConfiguration() {	}
	
	/* Method declared on SourceViewerConfiguration */
	public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) { return new ProcessingAnnotationHover(); }
		
	/* @see org.eclipse.jface.text.source.SourceViewerConfiguration#getAutoEditStrategies(org.eclipse.jface.text.source.ISourceViewer, java.lang.String) */
	public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
		IAutoEditStrategy strategy= (IDocument.DEFAULT_CONTENT_TYPE.equals(contentType) ? new ProcessingAutoIndentStrategy() : new DefaultIndentLineAutoEditStrategy());
		return new IAutoEditStrategy[] { strategy };
	}
	
	/* @see org.eclipse.jface.text.source.SourceViewerConfiguration#getConfiguredDocumentPartitioning(org.eclipse.jface.text.source.ISourceViewer) */
	public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
		return ProcessingPlugin.PROCESSING_PARTITIONING;
	}
	
	/* Method declared on SourceViewerConfiguration */
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] { IDocument.DEFAULT_CONTENT_TYPE, ProcessingPartitionScanner.MULTILINE_COMMENT };
	}
	
	/* Method declared on SourceViewerConfiguration */
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {

		ContentAssistant assistant= new ContentAssistant();
		assistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
		assistant.setContentAssistProcessor(new ProcessingCompletionProcessor(), IDocument.DEFAULT_CONTENT_TYPE);
		
		assistant.enableAutoActivation(true);
		assistant.setAutoActivationDelay(500);
		assistant.setProposalPopupOrientation(IContentAssistant.PROPOSAL_OVERLAY);
		assistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
		assistant.setContextInformationPopupBackground(ProcessingPlugin.getDefault().getProcessingColorProvider().getColor(new RGB(150, 150, 0)));

		return assistant;
	}
	
	/* Method declared on SourceViewerConfiguration */
	public String getDefaultPrefix(ISourceViewer sourceViewer, String contentType) {
		return (IDocument.DEFAULT_CONTENT_TYPE.equals(contentType) ? "//" : null); //$NON-NLS-1$
	}
	
	/* Method declared on SourceViewerConfiguration */
	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
		return new ProcessingDoubleClickSelector();
	}
	
	/* Method declared on SourceViewerConfiguration */
	public String[] getIndentPrefixes(ISourceViewer sourceViewer, String contentType) {
		return new String[] { "\t", "    " }; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/* Method declared on SourceViewerConfiguration */
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		
		ProcessingColorProvider provider= ProcessingPlugin.getDefault().getProcessingColorProvider();
		
		PresentationReconciler reconciler= new PresentationReconciler();
		reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
		
		DefaultDamagerRepairer dr= new DefaultDamagerRepairer(ProcessingPlugin.getDefault().getProcessingCodeScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		dr= new DefaultDamagerRepairer(new SingleTokenScanner(new TextAttribute(provider.getColor(ProcessingColorProvider.COMMENT1))));
		reconciler.setDamager(dr, ProcessingPartitionScanner.MULTILINE_COMMENT);
		reconciler.setRepairer(dr, ProcessingPartitionScanner.MULTILINE_COMMENT);

		return reconciler;
	}
	
	/* Method declared on SourceViewerConfiguration */
	public int getTabWidth(ISourceViewer sourceViewer) { return 4; }
	
}
