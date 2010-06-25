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


import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextViewer;

/**
 * Double click strategy aware of Java identifier syntax rules. These should work
 * for Processing without modification.
 */
public class ProcessingDoubleClickSelector implements ITextDoubleClickStrategy {

	protected ITextViewer fText;
	protected int fPos;
	protected int fStartPos;
	protected int fEndPos;

	protected static char[] fgBrackets= { '{', '}', '(', ')', '[', ']', '"', '"' };

	/* 
	 * Create a JavaDoubleClickSelector.
	 */
	 public ProcessingDoubleClickSelector() {
		super();
	}
	
	/* (non-Javadoc)
	 * Method declared on ITextDoubleClickStrategy
	 */
	public void doubleClicked(ITextViewer text) {

		fPos= text.getSelectedRange().x;

		if (fPos < 0)
			return;

		fText= text;

		if (!selectBracketBlock())
			selectWord();
	}
	
	/**
	 * Match the brackets at the current selection. Return <code>true</code> if successful,
	 * <code>false</code> otherwise.
	 * 
	 * @return <code>true</code> if brackets match, <code>false</code> otherwise
	 */
	 protected boolean matchBracketsAt() {

		char prevChar, nextChar;

		int i;
		int bracketIndex1= fgBrackets.length;
		int bracketIndex2= fgBrackets.length;

		fStartPos= -1;
		fEndPos= -1;

		// get the chars preceding and following the start position
		try {

			IDocument doc= fText.getDocument();

			prevChar= doc.getChar(fPos - 1);
			nextChar= doc.getChar(fPos);

			// is the char either an open or close bracket?
			for (i= 0; i < fgBrackets.length; i= i + 2) {
				if (prevChar == fgBrackets[i]) {
					fStartPos= fPos - 1;
					bracketIndex1= i;
				}
			}
			for (i= 1; i < fgBrackets.length; i= i + 2) {
				if (nextChar == fgBrackets[i]) {
					fEndPos= fPos;
					bracketIndex2= i;
				}
			}

			if (fStartPos > -1 && bracketIndex1 < bracketIndex2) {
				fEndPos= searchForClosingBracket(fStartPos, prevChar, fgBrackets[bracketIndex1 + 1], doc);
				if (fEndPos > -1)
					return true;
				fStartPos= -1;
			} else if (fEndPos > -1) {
				fStartPos= searchForOpenBracket(fEndPos, fgBrackets[bracketIndex2 - 1], nextChar, doc);
				if (fStartPos > -1)
					return true;
				fEndPos= -1;
			}

		} catch (BadLocationException x) {
		}

		return false;
	}
	
	/**
	 * Select the word at the current selection location. Return <code>true</code> if successful,
	 * <code>false</code> otherwise.
	 * 
	 * @return <code>true</code> if a word can be found at the current selection location, <code>false</code> otherwise
	 */
	 protected boolean matchWord() {

		IDocument doc= fText.getDocument();

		try {

			int pos= fPos;
			char c;

			while (pos >= 0) {
				c= doc.getChar(pos);
				if (!Character.isJavaIdentifierPart(c))
					break;
				--pos;
			}

			fStartPos= pos;

			pos= fPos;
			int length= doc.getLength();

			while (pos < length) {
				c= doc.getChar(pos);
				if (!Character.isJavaIdentifierPart(c))
					break;
				++pos;
			}

			fEndPos= pos;

			return true;

		} catch (BadLocationException x) {
		}

		return false;
	}
	
	/**
	 * Returns the position of the closing bracket after <code>startPosition</code>.
	 * 
	 * @param startPosition - the beginning position
	 * @param openBracket - the character that represents the open bracket
	 * @param closeBracket - the character that represents the close bracket
	 * @param document - the document being searched
	 * @return the location of the closing bracket.
	 * @throws BadLocationException in case <code>startPosition</code> is invalid in the document
	 */
	 protected int searchForClosingBracket(int startPosition, char openBracket, char closeBracket, IDocument document) throws BadLocationException {
		int stack= 1;
		int closePosition= startPosition + 1;
		int length= document.getLength();
		char nextChar;

		while (closePosition < length && stack > 0) {
			nextChar= document.getChar(closePosition);
			if (nextChar == openBracket && nextChar != closeBracket)
				stack++;
			else if (nextChar == closeBracket)
				stack--;
			closePosition++;
		}

		if (stack == 0)
			return closePosition - 1;
		return -1;

	}
	
	/**
	 * Returns the position of the open bracket before <code>startPosition</code>.
	 * 
	 * @param startPosition - the beginning position
	 * @param openBracket - the character that represents the open bracket
	 * @param closeBracket - the character that represents the close bracket
	 * @param document - the document being searched
	 * @return the location of the starting bracket.
	 * @throws BadLocationException in case <code>startPosition</code> is invalid in the document
	 */
	 protected int searchForOpenBracket(int startPosition, char openBracket, char closeBracket, IDocument document) throws BadLocationException {
		int stack= 1;
		int openPos= startPosition - 1;
		char nextChar;

		while (openPos >= 0 && stack > 0) {
			nextChar= document.getChar(openPos);
			if (nextChar == closeBracket && nextChar != openBracket)
				stack++;
			else if (nextChar == openBracket)
				stack--;
			openPos--;
		}

		if (stack == 0)
			return openPos + 1;
		return -1;
	}
	
	/**
	 * Select the area between the selected bracket and the closing bracket.
	 * 
	 * @return <code>true</code> if selection was successful, <code>false</code> otherwise
	 */
	 protected boolean selectBracketBlock() {
		if (matchBracketsAt()) {

			if (fStartPos == fEndPos)
				fText.setSelectedRange(fStartPos, 0);
			else
				fText.setSelectedRange(fStartPos + 1, fEndPos - fStartPos - 1);

			return true;
		}
		return false;
	}
	
	/**
	 * Select the word at the current selection. 
	 */
	 protected void selectWord() {
		if (matchWord()) {

			if (fStartPos == fEndPos)
				fText.setSelectedRange(fStartPos, 0);
			else
				fText.setSelectedRange(fStartPos + 1, fEndPos - fStartPos - 1);
		}
	}
}
