/*
 * Copyright (C) 2012 Martin Leopold <m@martinleopold.com>
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package processing.mode.experimental;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Position;

/**
 * Describes an ID for a code line. Comprised of a file name and a (0-based)
 * line number. Can track changes to the line number due to text editing by
 * attaching a {@link Document}. Registered {@link LineListener}s are notified
 * of changes to the line number.
 *
 * @author Martin Leopold <m@martinleopold.com>
 */
public class LineID implements DocumentListener {

    protected String fileName; // the filename
    protected int lineIdx; // the line number, 0-based
    protected Document doc; // the Document to use for line number tracking
    protected Position pos; // the Position acquired during line number tracking
    protected Set<LineListener> listeners = new HashSet(); // listeners for line number changes

    public LineID(String fileName, int lineIdx) {
        this.fileName = fileName;
        this.lineIdx = lineIdx;
    }

    /**
     * Get the file name of this line.
     *
     * @return the file name
     */
    public String fileName() {
        return fileName;
    }

    /**
     * Get the (0-based) line number of this line.
     *
     * @return the line index (i.e. line number, starting at 0)
     */
    public synchronized int lineIdx() {
        return lineIdx;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * Test whether this {@link LineID} is equal to another object. Two
     * {@link LineID}'s are equal when both their fileName and lineNo are equal.
     *
     * @param obj the object to test for equality
     * @return {@code true} if equal
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final LineID other = (LineID) obj;
        if ((this.fileName == null) ? (other.fileName != null) : !this.fileName.equals(other.fileName)) {
            return false;
        }
        if (this.lineIdx != other.lineIdx) {
            return false;
        }
        return true;
    }

    /**
     * Output a string representation in the form fileName:lineIdx+1. Note this
     * uses a 1-based line number as is customary for human-readable line
     * numbers.
     *
     * @return the string representation of this line ID
     */
    @Override
    public String toString() {
        return fileName + ":" + (lineIdx + 1);
    }

//    /**
//     * Retrieve a copy of this line ID.
//     *
//     * @return the copy
//     */
//    @Override
//    public LineID clone() {
//        return new LineID(fileName, lineIdx);
//    }

    /**
     * Attach a {@link Document} to enable line number tracking when editing.
     * The position to track is before the first non-whitespace character on the
     * line. Edits happening before that position will cause the line number to
     * update accordingly. Multiple {@link #startTracking} calls will replace
     * the tracked document. Whoever wants a tracked line should track it and
     * add itself as listener if necessary.
     * ({@link LineHighlight}, {@link LineBreakpoint})
     *
     * @param doc the {@link Document} to use for line number tracking
     */
    public synchronized void startTracking(Document doc) {
        //System.out.println("tracking: " + this);
        if (doc == null) {
            return; // null arg
        }
        if (doc == this.doc) {
            return; // already tracking that doc
        }
        try {
            Element line = doc.getDefaultRootElement().getElement(lineIdx);
            if (line == null) {
                return; // line doesn't exist
            }
            String lineText = doc.getText(line.getStartOffset(), line.getEndOffset() - line.getStartOffset());
            // set tracking position at (=before) first non-white space character on line
            pos = doc.createPosition(line.getStartOffset() + nonWhiteSpaceOffset(lineText));
            this.doc = doc;
            doc.addDocumentListener(this);
        } catch (BadLocationException ex) {
            Logger.getLogger(LineID.class.getName()).log(Level.SEVERE, null, ex);
            pos = null;
            this.doc = null;
        }
    }

    /**
     * Notify this {@link LineID} that it is no longer in use. Will stop
     * position tracking. Call this when this {@link LineID} is no longer
     * needed.
     */
    public synchronized void stopTracking() {
        if (doc != null) {
            doc.removeDocumentListener(this);
            doc = null;
        }
    }

    /**
     * Update the tracked position. Will notify listeners if line number has
     * changed.
     */
    protected synchronized void updatePosition() {
        if (doc != null && pos != null) {
            // track position
            int offset = pos.getOffset();
            int oldLineIdx = lineIdx;
            lineIdx = doc.getDefaultRootElement().getElementIndex(offset); // offset to lineNo
            if (lineIdx != oldLineIdx) {
                for (LineListener l : listeners) {
                    if (l != null) {
                        l.lineChanged(this, oldLineIdx, lineIdx);
                    } else {
                        listeners.remove(l); // remove null listener
                    }
                }
            }
        }
    }

    /**
     * Add listener to be notified when the line number changes.
     *
     * @param l the listener to add
     */
    public void addListener(LineListener l) {
        listeners.add(l);
    }

    /**
     * Remove a listener for line number changes.
     *
     * @param l the listener to remove
     */
    public void removeListener(LineListener l) {
        listeners.remove(l);
    }

    /**
     * Calculate the offset of the first non-whitespace character in a string.
     *
     * @param str the string to examine
     * @return offset of first non-whitespace character in str
     */
    protected static int nonWhiteSpaceOffset(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return i;
            }
        }
        return str.length();
    }

    /**
     * Called when the {@link Document} registered using {@link #startTracking}
     * is edited. This happens when text is inserted or removed.
     *
     * @param de
     */
    protected void editEvent(DocumentEvent de) {
        //System.out.println("document edit @ " + de.getOffset());
        if (de.getOffset() <= pos.getOffset()) {
            updatePosition();
            //System.out.println("updating, new line no: " + lineNo);
        }
    }

    /**
     * {@link DocumentListener} callback. Called when text is inserted.
     *
     * @param de
     */
    @Override
    public void insertUpdate(DocumentEvent de) {
        editEvent(de);
    }

    /**
     * {@link DocumentListener} callback. Called when text is removed.
     *
     * @param de
     */
    @Override
    public void removeUpdate(DocumentEvent de) {
        editEvent(de);
    }

    /**
     * {@link DocumentListener} callback. Called when attributes are changed.
     * Not used.
     *
     * @param de
     */
    @Override
    public void changedUpdate(DocumentEvent de) {
        // not needed.
    }
}
