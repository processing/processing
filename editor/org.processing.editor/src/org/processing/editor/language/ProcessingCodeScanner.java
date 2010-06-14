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
package org.processing.editor.language;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.*;
import org.processing.editor.util.*;

/**
 * A Processing code scanner.
 */
public class ProcessingCodeScanner extends RuleBasedScanner {

	private static String[] fgKeywords1= { "abstract", "break", "case", "catch", "class", "continue", "default", "do", "else", "extends", "final", "finally", "for", "if", "implements", "import", "instanceof", "interface", "native", "new", "package", "private", "protected", "public", "return", "static", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while" }; //$NON-NLS-36$ //$NON-NLS-35$ //$NON-NLS-34$ //$NON-NLS-33$ //$NON-NLS-32$ //$NON-NLS-31$ //$NON-NLS-30$ //$NON-NLS-29$ //$NON-NLS-28$ //$NON-NLS-27$ //$NON-NLS-26$ //$NON-NLS-25$ //$NON-NLS-24$ //$NON-NLS-23$ //$NON-NLS-22$ //$NON-NLS-21$ //$NON-NLS-20$ //$NON-NLS-19$ //$NON-NLS-18$ //$NON-NLS-17$ //$NON-NLS-16$ //$NON-NLS-15$ //$NON-NLS-14$ //$NON-NLS-13$ //$NON-NLS-12$ //$NON-NLS-11$ //$NON-NLS-10$ //$NON-NLS-9$ //$NON-NLS-8$ //$NON-NLS-7$ //$NON-NLS-6$ //$NON-NLS-5$ //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
	//TODO populate rule based scanner [lonnen] June 8 2010
	// at the moment we'll use a few examples, the whole thing will need to be
	// loaded with a helper class from keywords.txt
	private static String[] fgKeywords2= {"setup","random","size","for"}; // methods
	private static String[] fgKeywords3= {"byte","short","color","char","int","float"}; // byte short color char
	private static String[] fgLiterals1= {"null", "true", "this", "false", "P2D"}; // 
	private static String[] fgLiterals2= {"mouseX","width","pixels","frameRate","height"}; 
	private static String[] fgLabels = {}; //unused?
	private static String[] fgOperators = {"+", "-", "=", "/", "*"};
//	private static String[] fgInvalids = {}; // ?? commented out until I figure out what this does
	// Deleted the java 'types' and 'constants'
		
	/**
	 * Creates a Processing code scanner with the given color provider.
	 * 
	 * @param provider the color provider
	 */
	public ProcessingCodeScanner(ProcessingColorProvider provider) {

		IToken keyword1= new Token(new TextAttribute(provider.getColor(ProcessingColorProvider.KEYWORD1)));
		IToken keyword2= new Token(new TextAttribute(provider.getColor(ProcessingColorProvider.KEYWORD2)));
		IToken keyword3= new Token(new TextAttribute(provider.getColor(ProcessingColorProvider.KEYWORD3)));
		IToken literal1= new Token(new TextAttribute(provider.getColor(ProcessingColorProvider.LITERAL1)));
		IToken literal2= new Token(new TextAttribute(provider.getColor(ProcessingColorProvider.LITERAL2)));
		IToken label= new Token(new TextAttribute(provider.getColor(ProcessingColorProvider.LABEL)));
		IToken operator= new Token(new TextAttribute(provider.getColor(ProcessingColorProvider.OPERATOR)));
//		IToken invalid= new Token(new TextAttribute(provider.getColor(ProcessingColorProvider.INVALID)));
		// leave the rest for now
		IToken string= new Token(new TextAttribute(provider.getColor(ProcessingColorProvider.STRING)));
		IToken comment= new Token(new TextAttribute(provider.getColor(ProcessingColorProvider.COMMENT2)));
		IToken other= new Token(new TextAttribute(provider.getColor(ProcessingColorProvider.DEFAULT)));

		List rules= new ArrayList();

		// Add rule for single line comments.
		rules.add(new EndOfLineRule("//", comment)); //$NON-NLS-1$

		// Add rule for strings and character constants.
		rules.add(new SingleLineRule("\"", "\"", string, '\\')); //$NON-NLS-2$ //$NON-NLS-1$
		rules.add(new SingleLineRule("'", "'", string, '\\')); //$NON-NLS-2$ //$NON-NLS-1$

		// Add generic whitespace rule.
		rules.add(new WhitespaceRule(new ProcessingWhitespaceDetector()));

		// Add word rule for keywords
		WordRule wordRule= new WordRule(new ProcessingWordDetector(), other);
		for (int i= 0; i < fgKeywords1.length; i++)
			wordRule.addWord(fgKeywords1[i], keyword1);
		for (int i= 0; i < fgKeywords2.length; i++)
			wordRule.addWord(fgKeywords2[i], keyword2);
		for (int i= 0; i < fgKeywords3.length; i++)
			wordRule.addWord(fgKeywords3[i], keyword3);
		// literals
		for (int i= 0; i < fgLiterals1.length; i++)
			wordRule.addWord(fgLiterals1[i], literal1);
		for (int i= 0; i < fgLiterals2.length; i++)
			wordRule.addWord(fgLiterals2[i], literal2);
		// label
		for (int i= 0; i < fgLabels.length; i++)
			wordRule.addWord(fgLabels[i], label);		
		// operator
		for (int i= 0; i < fgOperators.length; i++)
			wordRule.addWord(fgOperators[i], operator);

		rules.add(wordRule);

		IRule[] result= new IRule[rules.size()];
		rules.toArray(result);
		setRules(result);
	}
}
