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
package processing.mode.java2;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.HashMap;
import java.util.Map;

import processing.app.syntax.JEditTextArea;
import processing.app.syntax.TextAreaDefaults;

/**
 * Customized text area. Adds support for line background colors.
 *
 * @author Martin Leopold <m@martinleopold.com>
 */
public class TextArea extends JEditTextArea {

    protected MouseListener[] mouseListeners; // cached mouselisteners, these are wrapped by MouseHandler
    protected DebugEditor editor; // the editor
    // line properties
    protected Map<Integer, Color> lineColors = new HashMap(); // contains line background colors
    // left-hand gutter properties
    protected int gutterPadding = 3; // [px] space added to the left and right of gutter chars
    protected Color gutterBgColor = new Color(252, 252, 252); // gutter background color
    protected Color gutterLineColor = new Color(233, 233, 233); // color of vertical separation line
    protected String breakpointMarker = "<>"; // the text marker for highlighting breakpoints in the gutter
    protected String currentLineMarker = "->"; // the text marker for highlighting the current line in the gutter
    protected Map<Integer, String> gutterText = new HashMap(); // maps line index to gutter text
    protected Map<Integer, Color> gutterTextColors = new HashMap(); // maps line index to gutter text color
    protected TextAreaPainter customPainter;
    
    public TextArea(TextAreaDefaults defaults, DebugEditor editor) {
        super(defaults);
        this.editor = editor;

        // replace the painter:
        // first save listeners, these are package-private in JEditTextArea, so not accessible
        ComponentListener[] componentListeners = painter.getComponentListeners();
        mouseListeners = painter.getMouseListeners();
        MouseMotionListener[] mouseMotionListeners = painter.getMouseMotionListeners();

        remove(painter);

        // set new painter
        customPainter = new TextAreaPainter(this, defaults);        
        painter = customPainter;
        
        // set listeners
        for (ComponentListener cl : componentListeners) {
            painter.addComponentListener(cl);
        }

        for (MouseMotionListener mml : mouseMotionListeners) {
            painter.addMouseMotionListener(mml);
        }

        // use a custom mouse handler instead of directly using mouseListeners
        MouseHandler mouseHandler = new MouseHandler();
        painter.addMouseListener(mouseHandler);
        painter.addMouseMotionListener(mouseHandler);

        add(CENTER, painter);

        // load settings from theme.txt
        DebugMode theme = (DebugMode) editor.getMode();
        gutterBgColor = theme.getThemeColor("gutter.bgcolor", gutterBgColor);
        gutterLineColor = theme.getThemeColor("gutter.linecolor", gutterLineColor);
        gutterPadding = theme.getInteger("gutter.padding");
        breakpointMarker = theme.loadThemeString("breakpoint.marker", breakpointMarker);
        currentLineMarker = theme.loadThemeString("currentline.marker", currentLineMarker);
    }
    
    public void setECSandThemeforTextArea(ErrorCheckerService ecs, DebugMode mode)
    {
      customPainter.setECSandTheme(ecs, mode);
    }

    /**
     * Retrieve the total width of the gutter area.
     *
     * @return gutter width in pixels
     */
    protected int getGutterWidth() {
        FontMetrics fm = painter.getFontMetrics();
//        System.out.println("fm: " + (fm == null));
//        System.out.println("editor: " + (editor == null));
        //System.out.println("BPBPBPBPB: " + (editor.breakpointMarker == null));

        int textWidth = Math.max(fm.stringWidth(breakpointMarker), fm.stringWidth(currentLineMarker));
        return textWidth + 2 * gutterPadding;
    }

    /**
     * Retrieve the width of margins applied to the left and right of the gutter
     * text.
     *
     * @return margins in pixels
     */
    protected int getGutterMargins() {
        return gutterPadding;
    }

    /**
     * Set the gutter text of a specific line.
     *
     * @param lineIdx the line index (0-based)
     * @param text the text
     */
    public void setGutterText(int lineIdx, String text) {
        gutterText.put(lineIdx, text);
        painter.invalidateLine(lineIdx);
    }

    /**
     * Set the gutter text and color of a specific line.
     *
     * @param lineIdx the line index (0-based)
     * @param text the text
     * @param textColor the text color
     */
    public void setGutterText(int lineIdx, String text, Color textColor) {
        gutterTextColors.put(lineIdx, textColor);
        setGutterText(lineIdx, text);
    }

    /**
     * Clear the gutter text of a specific line.
     *
     * @param lineIdx the line index (0-based)
     */
    public void clearGutterText(int lineIdx) {
        gutterText.remove(lineIdx);
        painter.invalidateLine(lineIdx);
    }

    /**
     * Clear all gutter text.
     */
    public void clearGutterText() {
        for (int lineIdx : gutterText.keySet()) {
            painter.invalidateLine(lineIdx);
        }
        gutterText.clear();
    }

    /**
     * Retrieve the gutter text of a specific line.
     *
     * @param lineIdx the line index (0-based)
     * @return the gutter text
     */
    public String getGutterText(int lineIdx) {
        return gutterText.get(lineIdx);
    }

    /**
     * Retrieve the gutter text color for a specific line.
     *
     * @param lineIdx the line index
     * @return the gutter text color
     */
    public Color getGutterTextColor(int lineIdx) {
        return gutterTextColors.get(lineIdx);
    }

    /**
     * Set the background color of a line.
     *
     * @param lineIdx 0-based line number
     * @param col the background color to set
     */
    public void setLineBgColor(int lineIdx, Color col) {
        lineColors.put(lineIdx, col);
        painter.invalidateLine(lineIdx);
    }

    /**
     * Clear the background color of a line.
     *
     * @param lineIdx 0-based line number
     */
    public void clearLineBgColor(int lineIdx) {
        lineColors.remove(lineIdx);
        painter.invalidateLine(lineIdx);
    }

    /**
     * Clear all line background colors.
     */
    public void clearLineBgColors() {
        for (int lineIdx : lineColors.keySet()) {
            painter.invalidateLine(lineIdx);
        }
        lineColors.clear();
    }

    /**
     * Get a lines background color.
     *
     * @param lineIdx 0-based line number
     * @return the color or null if no color was set for the specified line
     */
    public Color getLineBgColor(int lineIdx) {
        return lineColors.get(lineIdx);
    }

    /**
     * Convert a character offset to a horizontal pixel position inside the text
     * area. Overridden to take gutter width into account.
     *
     * @param line the 0-based line number
     * @param offset the character offset (0 is the first character on a line)
     * @return the horizontal position
     */
    @Override
    public int _offsetToX(int line, int offset) {
        return super._offsetToX(line, offset) + getGutterWidth();
    }

    /**
     * Convert a horizontal pixel position to a character offset. Overridden to
     * take gutter width into account.
     *
     * @param line the 0-based line number
     * @param x the horizontal pixel position
     * @return he character offset (0 is the first character on a line)
     */
    @Override
    public int xToOffset(int line, int x) {
        return super.xToOffset(line, x - getGutterWidth());
    }

    /**
     * Custom mouse handler. Implements double clicking in the gutter area to
     * toggle breakpoints, sets default cursor (instead of text cursor) in the
     * gutter area.
     */
    protected class MouseHandler implements MouseListener, MouseMotionListener {

        protected int lastX; // previous horizontal positon of the mouse cursor

        @Override
        public void mouseClicked(MouseEvent me) {
            // forward to standard listeners
            for (MouseListener ml : mouseListeners) {
                ml.mouseClicked(me);
            }
        }

        @Override
        public void mousePressed(MouseEvent me) {
            // check if this happened in the gutter area
            if (me.getX() < getGutterWidth()) {
                if (me.getButton() == MouseEvent.BUTTON1 && me.getClickCount() == 2) {
                    int line = me.getY() / painter.getFontMetrics().getHeight() + firstLine;
                    if (line >= 0 && line <= getLineCount() - 1) {
                        editor.gutterDblClicked(line);
                    }
                }
            } else {
                // forward to standard listeners
                for (MouseListener ml : mouseListeners) {
                    ml.mousePressed(me);
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent me) {
            // forward to standard listeners
            for (MouseListener ml : mouseListeners) {
                ml.mouseReleased(me);
            }
        }

        @Override
        public void mouseEntered(MouseEvent me) {
            // forward to standard listeners
            for (MouseListener ml : mouseListeners) {
                ml.mouseEntered(me);
            }
        }

        @Override
        public void mouseExited(MouseEvent me) {
            // forward to standard listeners
            for (MouseListener ml : mouseListeners) {
                ml.mouseExited(me);
            }
        }

        @Override
        public void mouseDragged(MouseEvent me) {
            // No need to forward since the standard MouseMotionListeners are called anyway
            // nop
        }

        @Override
        public void mouseMoved(MouseEvent me) {
            // No need to forward since the standard MouseMotionListeners are called anyway
            if (me.getX() < getGutterWidth()) {
                if (lastX >= getGutterWidth()) {
                    painter.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
            } else {
                if (lastX < getGutterWidth()) {
                    painter.setCursor(new Cursor(Cursor.TEXT_CURSOR));
                }
            }
            lastX = me.getX();
        }
    }
}
