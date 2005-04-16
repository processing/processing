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
    styles[Token.COMMENT1] = Preferences.getStyle("comment1");
    styles[Token.COMMENT2] = Preferences.getStyle("comment2");

    // abstract, final, private
    styles[Token.KEYWORD1] = Preferences.getStyle("keyword1");

    // beginShape, point, line
    styles[Token.KEYWORD2] = Preferences.getStyle("keyword2");

    // byte, char, short, color
    styles[Token.KEYWORD3] = Preferences.getStyle("keyword3");

    // constants: null, true, this, RGB, TWO_PI
    styles[Token.LITERAL1] = Preferences.getStyle("literal1");

    // p5 built in variables: mouseX, width, pixels
    styles[Token.LITERAL2] = Preferences.getStyle("literal2");

    // ??
    styles[Token.LABEL] = Preferences.getStyle("label");

    // + - = /
    styles[Token.OPERATOR] = Preferences.getStyle("operator");

    // area that's not in use by the text (replaced with tildes)
    styles[Token.INVALID] = Preferences.getStyle("invalid");


    // moved from TextAreaPainter

    font = Preferences.getFont("editor.font");

    fgcolor = Preferences.getColor("editor.fgcolor");
    bgcolor = Preferences.getColor("editor.bgcolor");

    caretVisible = true;
    caretBlinks = Preferences.getBoolean("editor.caret.blink");
    caretColor = Preferences.getColor("editor.caret.color");

    selectionColor = Preferences.getColor("editor.selection.color");

    lineHighlight =
      Preferences.getBoolean("editor.linehighlight");
    lineHighlightColor =
      Preferences.getColor("editor.linehighlight.color");

    bracketHighlight =
      Preferences.getBoolean("editor.brackethighlight");
    bracketHighlightColor =
      Preferences.getColor("editor.brackethighlight.color");

    eolMarkers = Preferences.getBoolean("editor.eolmarkers");
    eolMarkerColor = Preferences.getColor("editor.eolmarkers.color");

    paintInvalid = Preferences.getBoolean("editor.invalid");
  }
}
