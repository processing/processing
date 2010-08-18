package processing.plugin.ui.processingeditor.util;

import org.eclipse.jface.text.rules.IWordDetector;

/** 
 * Processing Words are Java words, so this class wraps Character methods to detect 
 * Java words. JavaDoc for the methods can be found in the IWordDetector interface.
 * */
public class ProcessingWordDetector implements IWordDetector {

	public boolean isWordPart(char character) {
		return Character.isJavaIdentifierPart(character);
	}
	
	public boolean isWordStart(char character) {
		return Character.isJavaIdentifierStart(character);
	}
}
