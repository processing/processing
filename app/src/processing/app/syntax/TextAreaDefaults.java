/*
 * TextAreaDefaults.java - Encapsulates default values for various settings
 * Copyright (C) 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

package processing.app.syntax;

import java.awt.*;

/**
 * Encapsulates default settings for a text area. This can be passed
 * to the constructor once the necessary fields have been filled out.
 * The advantage of doing this over calling lots of set() methods after
 * creating the text area is that this method is faster.
 */
public class TextAreaDefaults {
  public InputHandler inputHandler;
  public SyntaxDocument document;
  public boolean editable;

  public boolean caretVisible;
  public boolean caretBlinks;
  public boolean blockCaret;
  public int electricScroll;

  public int cols;
  public int rows;
  public SyntaxStyle[] styles;
  public Color caretColor;
  public Color selectionColor;
  public Color lineHighlightColor;
  public boolean lineHighlight;
  public Color bracketHighlightColor;
  public boolean bracketHighlight;
  public Color eolMarkerColor;
  public boolean eolMarkers;
  public boolean paintInvalid;

  // moved from TextAreaPainter [fry]
  public Font font;
  public Color fgcolor;
  public Color bgcolor;
  public boolean antialias;
}
