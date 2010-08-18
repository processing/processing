package processing.plugin.ui.processingeditor;

import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.editors.text.TextEditor;

public class ProcessingEditor extends TextEditor {

	/** Projection Support handles annotations, etc.*/
	private ProjectionSupport fProjectionSupport;

	//TODO content assistance
	//TODO outline
	//TODO code folding
	
	/** Default constructor */
	public ProcessingEditor() { super(); }
	
	/**
	 *  Shut it down. 
	 *  <p>
	 *  This should include any objects the editor privately maintains.
	 *  In the future this may involve content assist, outline, etc. 
	 */
	public void dispose() {
		super.dispose();
	}
		
	public Object getAdapter(Class required){
		if (fProjectionSupport != null) {
			Object adapter= fProjectionSupport.getAdapter(getSourceViewer(), required);
			if (adapter != null)
				return adapter;
		}
		
		return super.getAdapter(required);
	}
	
	/**
	 * Initializes this editor and provides a <code>SourceViewerConfiguration</code>
	 * 
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration
	 */
	protected void initializeEditor() {
		super.initializeEditor();
		setSourceViewerConfiguration(new ProcessingSourceViewerConfiguration());
	}
	
	protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
		
		fAnnotationAccess= createAnnotationAccess();
		fOverviewRuler= createOverviewRuler(getSharedColors());
		
		ISourceViewer viewer= new ProjectionViewer(parent, ruler, getOverviewRuler(), isOverviewRulerVisible(), styles);
		
		// ensure decoration support has been created and configured.
		// preferred over SourceViewerDecorationSupport due to impending API changes [lonnen] june 11, 2010
		fSourceViewerDecorationSupport = getSourceViewerDecorationSupport(viewer);
		
		return viewer;
	}
	
	/* @see org.eclipse.ui.texteditor.ExtendedTextEditor#createPartControl(org.eclipse.swt.widgets.Composite) */
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		ProjectionViewer viewer= (ProjectionViewer) getSourceViewer();
		fProjectionSupport= new ProjectionSupport(viewer, getAnnotationAccess(), getSharedColors());
		fProjectionSupport.addSummarizableAnnotationType("org.eclipse.ui.workbench.texteditor.error"); //$NON-NLS-1$
		fProjectionSupport.addSummarizableAnnotationType("org.eclipse.ui.workbench.texteditor.warning"); //$NON-NLS-1$
		fProjectionSupport.install();
		viewer.doOperation(ProjectionViewer.TOGGLE);
	}
	
	/* @see org.eclipse.ui.texteditor.AbstractTextEditor#adjustHighlightRange(int, int) */
	protected void adjustHighlightRange(int offset, int length) {
		ISourceViewer viewer= getSourceViewer();
		if (viewer instanceof ITextViewerExtension5) {
			ITextViewerExtension5 extension= (ITextViewerExtension5) viewer;
			extension.exposeModelRange(new Region(offset, length));
		}
	}
	
}
