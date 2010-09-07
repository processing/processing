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

import org.eclipse.jface.text.rules.IWhitespaceDetector;

public class ProcessingWhitespaceDetector implements IWhitespaceDetector {

		public boolean isWhitespace(char character){
			return Character.isWhitespace(character);
		}
	
}
