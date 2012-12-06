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

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.text.BadLocationException;
import javax.swing.text.Segment;
import javax.swing.text.Utilities;

import processing.app.syntax.TextAreaDefaults;
import processing.app.syntax.TokenMarker;

/**
 * Customized line painter. Adds support for background colors, left hand gutter
 * area with background color and text.
 *
 * @author Martin Leopold <m@martinleopold.com>
 */
public class TextAreaPainter extends processing.app.syntax.TextAreaPainter {

    protected TextArea ta; // we need the subclassed textarea
    protected ErrorCheckerService errorCheckerService;
    
    /**
     * Error line underline color
     */
    public Color errorColor = new Color(0xED2630);

    /**
     * Warning line underline color
     */

    public Color warningColor = new Color(0xFFC30E);

    /**
     * Color of Error Marker
     */
    public Color errorMarkerColor = new Color(0xED2630);

    /**
     * Color of Warning Marker
     */
    public Color warningMarkerColor = new Color(0xFFC30E);
    
    public TextAreaPainter(TextArea textArea, TextAreaDefaults defaults) {
        super(textArea, defaults);
        ta = textArea;
    }
    
    private void loadTheme(ExperimentalMode mode){
      errorColor = mode.getThemeColor("editor.errorcolor", errorColor);
      warningColor = mode.getThemeColor("editor.warningcolor",
          warningColor);
      errorMarkerColor = mode.getThemeColor("editor.errormarkercolor",
          errorMarkerColor);
      warningMarkerColor = mode.getThemeColor(
          "editor.warningmarkercolor", warningMarkerColor);
    }

    /**
     * Paint a line. Paints the gutter (with background color and text) then the
     * line (background color and text).
     *
     * @param gfx the graphics context
     * @param tokenMarker
     * @param line 0-based line number
     * @param x horizontal position
     */
    @Override
    protected void paintLine(Graphics gfx, TokenMarker tokenMarker,
            int line, int x) {

        // paint gutter
        paintGutterBg(gfx, line, x);

        paintLineBgColor(gfx, line, x + ta.getGutterWidth());

        paintGutterLine(gfx, line, x);

        // paint gutter symbol
        paintGutterText(gfx, line, x);
        
        paintErrorLine(gfx, line, x);
        
        super.paintLine(gfx, tokenMarker, line, x + ta.getGutterWidth());
    }

    /**
     * Paint the gutter background (solid color).
     *
     * @param gfx the graphics context
     * @param line 0-based line number
     * @param x horizontal position
     */
    protected void paintGutterBg(Graphics gfx, int line, int x) {
        gfx.setColor(ta.gutterBgColor);
        int y = ta.lineToY(line) + fm.getLeading() + fm.getMaxDescent();
        gfx.fillRect(0, y, ta.getGutterWidth(), fm.getHeight());
    }

    /**
     * Paint the vertical gutter separator line.
     *
     * @param gfx the graphics context
     * @param line 0-based line number
     * @param x horizontal position
     */
    protected void paintGutterLine(Graphics gfx, int line, int x) {
        int y = ta.lineToY(line) + fm.getLeading() + fm.getMaxDescent();
        gfx.setColor(ta.gutterLineColor);
        gfx.drawLine(ta.getGutterWidth(), y, ta.getGutterWidth(), y + fm.getHeight());
    }

    /**
     * Paint the gutter text.
     *
     * @param gfx the graphics context
     * @param line 0-based line number
     * @param x horizontal position
     */
    protected void paintGutterText(Graphics gfx, int line, int x) {
        String text = ta.getGutterText(line);
        if (text == null) {
            return;
        }

        gfx.setFont(getFont());
        Color textColor = ta.getGutterTextColor(line);
        if (textColor == null) {
            gfx.setColor(getForeground());
        } else {
            gfx.setColor(textColor);
        }
        int y = ta.lineToY(line) + fm.getHeight();

        // draw 4 times to make it appear bold, displaced 1px to the right, to the bottom and bottom right.
        //int len = text.length() > ta.gutterChars ? ta.gutterChars : text.length();
        Utilities.drawTabbedText(new Segment(text.toCharArray(), 0, text.length()), ta.getGutterMargins(), y, gfx, this, 0);
        Utilities.drawTabbedText(new Segment(text.toCharArray(), 0, text.length()), ta.getGutterMargins() + 1, y, gfx, this, 0);
        Utilities.drawTabbedText(new Segment(text.toCharArray(), 0, text.length()), ta.getGutterMargins(), y + 1, gfx, this, 0);
        Utilities.drawTabbedText(new Segment(text.toCharArray(), 0, text.length()), ta.getGutterMargins() + 1, y + 1, gfx, this, 0);
    }

    /**
     * Paint the background color of a line.
     *
     * @param gfx the graphics context
     * @param line 0-based line number
     * @param x
     */
    protected void paintLineBgColor(Graphics gfx, int line, int x) {
        int y = ta.lineToY(line);
        y += fm.getLeading() + fm.getMaxDescent();
        int height = fm.getHeight();

        // get the color
        Color col = ta.getLineBgColor(line);
        //System.out.print("bg line " + line + ": ");
        // no need to paint anything
        if (col == null) {
            //System.out.println("none");
            return;
        }
        // paint line background
        gfx.setColor(col);
        gfx.fillRect(0, y, getWidth(), height);
    }
    
    /**
     * Paints the underline for an error/warning line
     * 
     * @param gfx
     *            the graphics context
     * @param tokenMarker
     * @param line
     *            0-based line number: NOTE
     * @param x
     */
    private void paintErrorLine(Graphics gfx, int line, int x) {
      
      if (errorCheckerService == null) {
        return;
      }
      
      if (errorCheckerService.problemsList== null) {
        return;
      }
      
      boolean notFound = true;
      boolean isWarning = false;

      // Check if current line contains an error. If it does, find if it's an
      // error or warning
      for (ErrorMarker emarker : errorCheckerService.getEditor().errorBar.errorPoints) {
        if (emarker.problem.lineNumber == line + 1) {
          notFound = false;
          if (emarker.type == ErrorMarker.Warning) {
            isWarning = true;
          }
          break;
        }
      }

      if (notFound) {
        return;
      }
      
      // Determine co-ordinates
      // System.out.println("Hoff " + ta.getHorizontalOffset() + ", " +
      // horizontalAdjustment);
      int y = ta.lineToY(line);
      y += fm.getLeading() + fm.getMaxDescent();
      int height = fm.getHeight();
      int start = ta.getLineStartOffset(line);

      try {
        String linetext = null;

        try {
          linetext = ta.getDocument().getText(start,
              ta.getLineStopOffset(line) - start - 1);
        } catch (BadLocationException bl) {
          // Error in the import statements or end of code.
          // System.out.print("BL caught. " + ta.getLineCount() + " ,"
          // + line + " ,");
          // System.out.println((ta.getLineStopOffset(line) - start - 1));
          return;
        }

        // Take care of offsets
        int aw = fm.stringWidth(trimRight(linetext))
            + ta.getHorizontalOffset(); // apparent width. Whitespaces
                          // to the left of line + text
                          // width
        int rw = fm.stringWidth(linetext.trim()); // real width
        int x1 = 0 + (aw - rw), y1 = y + fm.getHeight() - 2, x2 = x1 + rw;
        // Adding offsets for the gutter
        x1 += 20;
        x2 += 20;
        
        // gfx.fillRect(x1, y, rw, height);

        // Let the painting begin!
        gfx.setColor(errorMarkerColor);
        if (isWarning) {
          gfx.setColor(warningMarkerColor);
        }
        gfx.fillRect(1, y + 2, 3, height - 2);
        
        gfx.setColor(errorColor);
        if (isWarning) {
          gfx.setColor(warningColor);
        }
        int xx = x1;

        // Draw the jagged lines
        while (xx < x2) {
          gfx.drawLine(xx, y1, xx + 2, y1 + 1);
          xx += 2;
          gfx.drawLine(xx, y1 + 1, xx + 2, y1);
          xx += 2;
        }
      } catch (Exception e) {
        System.out
            .println("Looks like I messed up! XQTextAreaPainter.paintLine() : "
                + e);
        //e.printStackTrace();
      }

      // Won't highlight the line. Select the text instead.
      // gfx.setColor(Color.RED);
      // gfx.fillRect(2, y, 3, height);
    }
    
    /**
     * Trims out trailing whitespaces (to the right)
     * 
     * @param string
     * @return - String
     */
    private String trimRight(String string) {
      String newString = "";
      for (int i = 0; i < string.length(); i++) {
        if (string.charAt(i) != ' ') {
          newString = string.substring(0, i) + string.trim();
          break;
        }
      }
      return newString;
    }
    
    public void setECSandTheme(ErrorCheckerService ecs, ExperimentalMode mode){
      this.errorCheckerService = ecs;
      loadTheme(mode);
    }
}
