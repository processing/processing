/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
Part of the Processing project - http://processing.org
Copyright (c) 2012-16 The Processing Foundation

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License version 2
as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software Foundation, Inc.
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/

package processing.app.syntax;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import processing.app.Mode;
import processing.app.ui.Editor;


public class PdeTextAreaPainter extends TextAreaPainter {
  public Color errorUnderlineColor;
  public Color warningUnderlineColor;

  protected Font gutterTextFont;
  protected Color gutterTextColor;
  protected Color gutterPastColor;
  protected Color gutterLineHighlightColor;


  public PdeTextAreaPainter(JEditTextArea textArea, TextAreaDefaults defaults) {
    super(textArea, defaults);

    // Handle mouse clicks to toggle breakpoints
    addMouseListener(new MouseAdapter() {
      long lastTime;  // OS X seems to be firing multiple mouse events

      public void mousePressed(MouseEvent event) {
        // Don't toggle breakpoints when the debugger isn't enabled
        // https://github.com/processing/processing/issues/3306
        if (getEditor().isDebuggerEnabled()) {
          long thisTime = event.getWhen();
          if (thisTime - lastTime > 100) {
            if (event.getX() < Editor.LEFT_GUTTER) {
              int offset = textArea.xyToOffset(event.getX(), event.getY());
              if (offset >= 0) {
                int lineIndex = textArea.getLineOfOffset(offset);
                getEditor().toggleBreakpoint(lineIndex);
              }
            }
            lastTime = thisTime;
          }
        }
      }
    });
  }


  /**
   * Loads theme for TextAreaPainter
   */
  public void setMode(Mode mode) {
    errorUnderlineColor = mode.getColor("editor.error.underline.color");
    warningUnderlineColor = mode.getColor("editor.warning.underline.color");

    gutterTextFont = mode.getFont("editor.gutter.text.font");
    gutterTextColor = mode.getColor("editor.gutter.text.color");
    gutterPastColor = new Color(gutterTextColor.getRed(),
                                gutterTextColor.getGreen(),
                                gutterTextColor.getBlue(),
                                96);
    gutterLineHighlightColor = mode.getColor("editor.gutter.linehighlight.color");
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  @Override
  public int getScrollWidth() {
    // https://github.com/processing/processing/issues/3591
    return super.getWidth() - Editor.LEFT_GUTTER;
  }


  public Editor getEditor() {
    return ((PdeTextArea) textArea).editor;
  }
}