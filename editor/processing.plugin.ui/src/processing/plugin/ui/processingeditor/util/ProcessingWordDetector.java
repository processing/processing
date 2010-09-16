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
package processing.plugin.ui.processingeditor.util;

import org.eclipse.jface.text.rules.IWordDetector;

/** 
 * Detects Processing words
 * <p>
 * Processing Words are Java words, so this class wraps Character 
 * methods to detect Java words. The interface prevents these
 * from being made static.
 */
public class ProcessingWordDetector implements IWordDetector {

	public boolean isWordPart(char character) {
		return Character.isJavaIdentifierPart(character);
	}
	
	public boolean isWordStart(char character) {
		return Character.isJavaIdentifierStart(character);
	}
}
