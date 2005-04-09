/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeTextAreaDefaults - grabs font/color settings for the editor
  Part of the Processing project - http://Proce55ing.net

  Except where noted, code is written by Ben Fry
  Copyright (c) 2001-03 Massachusetts Institute of Technology

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app.syntax;

import processing.app.*;


public class PdeTextAreaDefaults extends TextAreaDefaults {

  public PdeTextAreaDefaults() {

    inputHandler = new DefaultInputHandler();
    inputHandler.addDefaultKeyBindings();
    document = new SyntaxDocument();
    editable = true;
    electricScroll = 3;

    cols = 80;
    rows = 15;


    // moved from SyntaxUtilities
    //DEFAULTS.styles = SyntaxUtilities.getDefaultSyntaxStyles();

    styles = new SyntaxStyle[Token.ID_COUNT];

    // comments
    styles[Token.COMMENT1] = PdePreferences.getStyle("comment1");
    styles[Token.COMMENT2] = PdePreferences.getStyle("comment2");

    // abstract, final, private
    styles[Token.KEYWORD1] = PdePreferences.getStyle("keyword1");

    // beginShape, point, line
    styles[Token.KEYWORD2] = PdePreferences.getStyle("keyword2");

    // byte, char, short, color
    styles[Token.KEYWORD3] = PdePreferences.getStyle("keyword3");

    // constants: null, true, this, RGB, TWO_PI
    styles[Token.LITERAL1] = PdePreferences.getStyle("literal1");

    // p5 built in variables: mouseX, width, pixels
    styles[Token.LITERAL2] = PdePreferences.getStyle("literal2");

    // ??
    styles[Token.LABEL] = PdePreferences.getStyle("label");

    // + - = /
    styles[Token.OPERATOR] = PdePreferences.getStyle("operator");

    // area that's not in use by the text (replaced with tildes)
    styles[Token.INVALID] = PdePreferences.getStyle("invalid");


    // moved from TextAreaPainter

    font = PdePreferences.getFont("editor.font");

    fgcolor = PdePreferences.getColor("editor.fgcolor");
    bgcolor = PdePreferences.getColor("editor.bgcolor");

    caretVisible = true;
    caretBlinks = PdePreferences.getBoolean("editor.caret.blink");
    caretColor = PdePreferences.getColor("editor.caret.color");

    selectionColor = PdePreferences.getColor("editor.selection.color");

    lineHighlight =
      PdePreferences.getBoolean("editor.linehighlight");
    lineHighlightColor =
      PdePreferences.getColor("editor.linehighlight.color");

    bracketHighlight =
      PdePreferences.getBoolean("editor.brackethighlight");
    bracketHighlightColor =
      PdePreferences.getColor("editor.brackethighlight.color");

    eolMarkers = PdePreferences.getBoolean("editor.eolmarkers");
    eolMarkerColor = PdePreferences.getColor("editor.eolmarkers.color");

    paintInvalid = PdePreferences.getBoolean("editor.invalid");
  }
}
