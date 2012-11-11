/*
 * Copyright (C) 2012 Martin Leopold <m@martinleopold.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package processing.mode.java2;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

/**
 * Model/Controller for a highlighted source code line. Implements a custom
 * background color and a text based marker placed in the left-hand gutter area.
 *
 * @author Martin Leopold <m@martinleopold.com>
 */
public class LineHighlight implements LineListener {

    protected DebugEditor editor; // the view, used for highlighting lines by setting a background color
    protected Color bgColor; // the background color for highlighting lines
    protected LineID lineID; // the id of the line
    protected String marker; //
    protected Color markerColor;
    protected int priority = 0;
    protected static Set<LineHighlight> allHighlights = new HashSet();

    protected static boolean isHighestPriority(LineHighlight hl) {
        for (LineHighlight check : allHighlights) {
            if (check.lineID().equals(hl.lineID()) && check.priority() > hl.priority()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Create a {@link LineHighlight}.
     *
     * @param lineID the line id to highlight
     * @param bgColor the background color used for highlighting
     * @param editor the {@link DebugEditor}
     */
    public LineHighlight(LineID lineID, Color bgColor, DebugEditor editor) {
        this.lineID = lineID;
        this.bgColor = bgColor;
        this.editor = editor;
        lineID.addListener(this);
        lineID.startTracking(editor.getTab(lineID.fileName()).getDocument()); // TODO: overwrite a previous doc?
        paint(); // already checks if on current tab
        allHighlights.add(this);
    }

    public void setPriority(int p) {
        this.priority = p;
    }

    public int priority() {
        return priority;
    }

    /**
     * Create a {@link LineHighlight} on the current tab.
     *
     * @param lineIdx the line index on the current tab to highlight
     * @param bgColor the background color used for highlighting
     * @param editor the {@link DebugEditor}
     */
    // TODO: Remove and replace by {@link #LineHighlight(LineID lineID, Color bgColor, DebugEditor editor)}
    public LineHighlight(int lineIdx, Color bgColor, DebugEditor editor) {
        this(editor.getLineIDInCurrentTab(lineIdx), bgColor, editor);
    }

    /**
     * Set a text based marker displayed in the left hand gutter area of this
     * highlighted line.
     *
     * @param marker the marker text
     */
    public void setMarker(String marker) {
        this.marker = marker;
        paint();
    }

    /**
     * Set a text based marker displayed in the left hand gutter area of this
     * highlighted line. Also use a custom text color.
     *
     * @param marker the marker text
     * @param markerColor the text color
     */
    public void setMarker(String marker, Color markerColor) {
        this.markerColor = markerColor;
        setMarker(marker);
    }

    /**
     * Retrieve the line id of this {@link LineHighlight}.
     *
     * @return the line id
     */
    public LineID lineID() {
        return lineID;
    }

    /**
     * Retrieve the color for highlighting this line.
     *
     * @return the highlight color.
     */
    public Color getColor() {
        return bgColor;
    }

    /**
     * Test if this highlight is on a certain line.
     *
     * @param testLine the line to test
     * @return true if this highlight is on the given line
     */
    public boolean isOnLine(LineID testLine) {
        return lineID.equals(testLine);
    }

    /**
     * Event handler for line number changes (due to editing). Will remove the
     * highlight from the old line number and repaint it at the new location.
     *
     * @param line the line that has changed
     * @param oldLineIdx the old line index (0-based)
     * @param newLineIdx the new line index (0-based)
     */
    @Override
    public void lineChanged(LineID line, int oldLineIdx, int newLineIdx) {
        // clear old line
        if (editor.isInCurrentTab(new LineID(line.fileName(), oldLineIdx))) {
            editor.textArea().clearLineBgColor(oldLineIdx);
            editor.textArea().clearGutterText(oldLineIdx);
        }

        // paint new line
        // but only if it's on top -> fixes current line being hidden by breakpoint moving it down.
        // lineChanged events seem to come in inverse order of startTracking the LineID. (and bp is created first...)
        if (LineHighlight.isHighestPriority(this)) {
            paint();
        }
    }

    /**
     * Notify this line highlight that it is no longer used. Call this for
     * cleanup before the {@link LineHighlight} is discarded.
     */
    public void dispose() {
        lineID.removeListener(this);
        lineID.stopTracking();
        allHighlights.remove(this);
    }

    /**
     * (Re-)paint this line highlight.
     */
    public void paint() {
        if (editor.isInCurrentTab(lineID)) {
            editor.textArea().setLineBgColor(lineID.lineIdx(), bgColor);
            if (marker != null) {
                if (markerColor != null) {
                    editor.textArea().setGutterText(lineID.lineIdx(), marker, markerColor);
                } else {
                    editor.textArea().setGutterText(lineID.lineIdx(), marker);
                }
            }
        }
    }

    /**
     * Clear this line highlight.
     */
    public void clear() {
        if (editor.isInCurrentTab(lineID)) {
            editor.textArea().clearLineBgColor(lineID.lineIdx());
            editor.textArea().clearGutterText(lineID.lineIdx());
        }
    }
}
