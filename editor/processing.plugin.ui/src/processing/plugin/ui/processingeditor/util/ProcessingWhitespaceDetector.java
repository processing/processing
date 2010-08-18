package processing.plugin.ui.processingeditor.util;

import org.eclipse.jface.text.rules.IWhitespaceDetector;

public class ProcessingWhitespaceDetector implements IWhitespaceDetector {

		public boolean isWhitespace(char character){
			return Character.isWhitespace(character);
		}
	
}
