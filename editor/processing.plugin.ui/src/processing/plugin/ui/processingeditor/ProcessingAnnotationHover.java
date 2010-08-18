package processing.plugin.ui.processingeditor;

//import org.eclipse.jface.text.BadLocationException;
//import org.eclipse.jface.text.IDocument;
//import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;

/**
 * Provides the content in the presentation area of a mouse hover popup window.
 * 
 * @author lonnen
 * @see org.eclipse.jface.text.source.IAnnotationHover
 */
public class ProcessingAnnotationHover implements IAnnotationHover {

	/* */
	public String getHoverInfo(ISourceViewer sourceViewer, int lineNumber) {
		// TODO write useful annotation hover, will require AST
//		IDocument document= sourceViewer.getDocument();
//
//		try {
//			IRegion info= document.getLineInformation(lineNumber);
//			return document.get(info.getOffset(), info.getLength());
//		} catch (BadLocationException x) { }
		
		return null;
	}

}
