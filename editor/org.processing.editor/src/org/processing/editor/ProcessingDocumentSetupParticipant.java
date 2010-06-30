package org.processing.editor;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;

/**
 * Participates in the setup of a text file buffer document.
 * <p>
 * Requires the "org.eclipse.core.filebuffers.documentSetup" extension point.
 * 
 * @see org.eclipse.core.filebuffers.IDocumentSetupParticipant
 */
public class ProcessingDocumentSetupParticipant implements IDocumentSetupParticipant {
	
	/**
	 */
	public ProcessingDocumentSetupParticipant() {
	}

	/*
	 * @see org.eclipse.core.filebuffers.IDocumentSetupParticipant#setup(org.eclipse.jface.text.IDocument)
	 */
	public void setup(IDocument document) {
		if (document instanceof IDocumentExtension3) {
			IDocumentExtension3 extension3= (IDocumentExtension3) document;
			IDocumentPartitioner partitioner= new FastPartitioner(ProcessingEditorPlugin.getDefault().getProcessingPartitionScanner(), ProcessingPartitionScanner.JAVA_PARTITION_TYPES);
			extension3.setDocumentPartitioner(ProcessingEditorPlugin.PROCESSING_PARTITIONING, partitioner);
			partitioner.connect(document);
		}
	}
}
