package processing.plugin.ui.processingeditor;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import processing.plugin.ui.ProcessingPlugin;
import processing.plugin.ui.processingeditor.ProcessingPartitionScanner;

public class ProcessingDocumentSetupParticipant implements IDocumentSetupParticipant {

	/** Empty Constructor */
	public ProcessingDocumentSetupParticipant(){}
	
	/** Set up the document*/
	public void setup(IDocument document) {
		if (document instanceof IDocumentExtension3) {
			IDocumentExtension3 extension3= (IDocumentExtension3) document;
			IDocumentPartitioner partitioner= new FastPartitioner(ProcessingPlugin.getDefault().getProcessingPartitionScanner(), ProcessingPartitionScanner.PARTITION_TYPES);
			extension3.setDocumentPartitioner(ProcessingPlugin.PROCESSING_PARTITIONING, partitioner);
			partitioner.connect(document);
		}		
	}

}
