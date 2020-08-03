/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
 * TextAreaPainter.java - Paints the text area
 * Copyright (C) 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */
package processing.app.syntax;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;

import javax.swing.ToolTipManager;
import javax.swing.text.*;
import javax.swing.JComponent;

import processing.app.Preferences;
import processing.app.syntax.im.CompositionTextPainter;
import processing.app.ui.Toolkit;


/**
 * The text area repaint manager. It performs double buffering and paints
 * lines of text.
 * @author Slava Pestov
 */
public class TextAreaPainter extends JComponent implements TabExpander {
  /** A specific painter composed by the InputMethod.*/
  protected CompositionTextPainter compositionTextPainter;

  protected JEditTextArea textArea;
  protected TextAreaDefaults defaults;

//  protected boolean blockCaret;
//  protected SyntaxStyle[] styles;
//  protected Color caretColor;
//  protected Color selectionColor;
//  protected Color lineHighlightColor;
//  protected boolean lineHighlight;
//  protected Color bracketHighlightColor;
//  protected boolean bracketHighlight;
//  protected Color eolMarkerColor;
//  protected boolean eolMarkers;

//  protected int cols;
//  protected int rows;

  // moved from TextAreaDefaults
  private Font plainFont;
  private Font boldFont;
  private boolean antialias;
//  private Color fgcolor;
//  private Color bgcolor;

  protected int tabSize;
  protected FontMetrics fm;

  protected Highlight highlights;

  int currentLineIndex;
  Token currentLineTokens;
  Segment currentLine;


  /**
   * Creates a new repaint manager. This should be not be called directly.
   */
  public TextAreaPainter(JEditTextArea textArea, TextAreaDefaults defaults) {
    this.textArea = textArea;
    this.defaults = defaults;

    setAutoscrolls(true);
//    setDoubleBuffered(true);
    setOpaque(true);

    ToolTipManager.sharedInstance().registerComponent(this);

    currentLine = new Segment();
    currentLineIndex = -1;

    setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));

//    // unfortunately probably can't just do setDefaults() since things aren't quite set up
//    setFont(defaults.plainFont);
////    System.out.println("defaults font is " + defaults.font);
//    setForeground(defaults.fgcolor);
//    setBackground(defaults.bgcolor);
    updateAppearance();

//    blockCaret = defaults.blockCaret;
//    styles = defaults.styles;
//    caretColor = defaults.caretColor;
//    selectionColor = defaults.selectionColor;
//    lineHighlightColor = defaults.lineHighlightColor;
//    lineHighlight = defaults.lineHighlight;
//    bracketHighlightColor = defaults.bracketHighlightColor;
//    bracketHighlight = defaults.bracketHighlight;
//    eolMarkerColor = defaults.eolMarkerColor;
//    eolMarkers = defaults.eolMarkers;
//    antialias = defaults.antialias;

//    cols = defaults.cols;
//    rows = defaults.rows;
  }


  public void updateAppearance() {
    setForeground(defaults.fgcolor);
    setBackground(defaults.bgcolor);

    // Ensure that our monospaced font is loaded
    // https://github.com/processing/processing/pull/4639
    Toolkit.getMonoFontName();

    String fontFamily = Preferences.get("editor.font.family");
    final int fontSize = Toolkit.zoom(Preferences.getInteger("editor.font.size"));
    plainFont = new Font(fontFamily, Font.PLAIN, fontSize);
    if (!fontFamily.equals(plainFont.getFamily())) {
      System.err.println(fontFamily + " not available, resetting to monospaced");
      fontFamily = "Monospaced";
      Preferences.set("editor.font.family", fontFamily);
      plainFont = new Font(fontFamily, Font.PLAIN, fontSize);
    }
    boldFont = new Font(fontFamily, Font.BOLD, fontSize);
    antialias = Preferences.getBoolean("editor.smooth");

    // moved from setFont() override (never quite comfortable w/ that override)
    fm = super.getFontMetrics(plainFont);
    tabSize = fm.charWidth(' ') * Preferences.getInteger("editor.tabs.size");
    textArea.recalculateVisibleLines();
  }


  /*
  public void setDefaults(TextAreaDefaults defaults) {
    setFont(defaults.font);
    setForeground(defaults.fgcolor);
    setBackground(defaults.bgcolor);

    setBlockCaretEnabled(defaults.blockCaret);
    setStyles(defaults.styles);
    setCaretColor(defaults.caretColor);
    setSelectionColor(defaults.selectionColor);
    setLineHighlightColor(defaults.lineHighlightColor);
    setLineHighlightEnabled(defaults.lineHighlight);
    setBracketHighlightColor(defaults.bracketHighlightColor);
    setBracketHighlightEnabled(defaults.bracketHighlight);
    setEOLMarkerColor(defaults.eolMarkerColor);
    setEOLMarkersPainted(defaults.eolMarkers);
    setAntialias(defaults.antialias);

    // only used for getPreferredSize()
    cols = defaults.cols;
    rows = defaults.rows;
  }
  */


  /**
   * Get CompositionTextPainter, creating one if it doesn't exist.
   */
   public CompositionTextPainter getCompositionTextpainter() {
     if (compositionTextPainter == null){
       compositionTextPainter = new CompositionTextPainter(textArea);
     }
     return compositionTextPainter;
   }


  /**
   * Returns the syntax styles used to paint colorized text. Entry <i>n</i>
   * will be used to paint tokens with id = <i>n</i>.
   * @see processing.app.syntax.Token
   */
  public final SyntaxStyle[] getStyles() {
    return defaults.styles;
  }


//  /**
//   * Sets the syntax styles used to paint colorized text. Entry <i>n</i>
//   * will be used to paint tokens with id = <i>n</i>.
//   * @param styles The syntax styles
//   * @see processing.app.syntax.Token
//   */
//  public final void setStyles(SyntaxStyle[] styles) {
//    this.styles = styles;
//    repaint();
//  }


//  /**
//   * Returns the caret color.
//   */
//  public final Color getCaretColor() {
//    return caretColor;
//  }


//  /**
//   * Sets the caret color.
//   * @param caretColor The caret color
//   */
//  public final void setCaretColor(Color caretColor) {
//    this.caretColor = caretColor;
//    invalidateSelectedLines();
//  }


//  /**
//   * Returns the selection color.
//   */
//  public final Color getSelectionColor() {
//    return selectionColor;
//  }


//  /**
//   * Sets the selection color.
//   * @param selectionColor The selection color
//   */
//  public final void setSelectionColor(Color selectionColor) {
//    this.selectionColor = selectionColor;
//    invalidateSelectedLines();
//  }


//  /**
//   * Returns the line highlight color.
//   */
//  public final Color getLineHighlightColor() {
//    return lineHighlightColor;
//  }


//  /**
//   * Sets the line highlight color.
//   * @param lineHighlightColor The line highlight color
//   */
//  public final void setLineHighlightColor(Color lineHighlightColor) {
//    this.lineHighlightColor = lineHighlightColor;
//    invalidateSelectedLines();
//  }


//  /**
//   * Returns true if line highlight is enabled, false otherwise.
//   */
//  public final boolean isLineHighlightEnabled() {
//    return lineHighlight;
//  }


  /**
   * Enables or disables current line highlighting.
   * @param lineHighlight True if current line highlight
   * should be enabled, false otherwise
   */
  public final void setLineHighlightEnabled(boolean lineHighlight) {
//    this.lineHighlight = lineHighlight;
    defaults.lineHighlight = lineHighlight;
    invalidateSelectedLines();
  }


//  /**
//   * Returns the bracket highlight color.
//   */
//  public final Color getBracketHighlightColor() {
//    return bracketHighlightColor;
//  }


//  /**
//   * Sets the bracket highlight color.
//   * @param bracketHighlightColor The bracket highlight color
//   */
//  public final void setBracketHighlightColor(Color bracketHighlightColor) {
//    this.bracketHighlightColor = bracketHighlightColor;
//    invalidateLine(textArea.getBracketLine());
//  }


  /**
   * Returns true if bracket highlighting is enabled, false otherwise.
   * When bracket highlighting is enabled, the bracket matching the
   * one before the caret (if any) is highlighted.
   */
  public final boolean isBracketHighlightEnabled() {
//    return bracketHighlight;
    return defaults.bracketHighlight;
  }


//  /**
//   * Enables or disables bracket highlighting.
//   * When bracket highlighting is enabled, the bracket matching the
//   * one before the caret (if any) is highlighted.
//   * @param bracketHighlight True if bracket highlighting should be
//   * enabled, false otherwise
//   */
//  public final void setBracketHighlightEnabled(boolean bracketHighlight) {
//    this.bracketHighlight = bracketHighlight;
//    invalidateLine(textArea.getBracketLine());
//  }


  /**
   * Returns true if the caret should be drawn as a block, false otherwise.
   */
  public final boolean isBlockCaretEnabled() {
    return defaults.blockCaret;
  }


//  /**
//   * Sets if the caret should be drawn as a block, false otherwise.
//   * @param blockCaret True if the caret should be drawn as a block,
//   * false otherwise.
//   */
//  public final void setBlockCaretEnabled(boolean blockCaret) {
//    this.blockCaret = blockCaret;
//    invalidateSelectedLines();
//  }


//  /**
//   * Returns the EOL marker color.
//   */
//  public final Color getEOLMarkerColor() {
//    return eolMarkerColor;
//  }


//  /**
//   * Sets the EOL marker color.
//   * @param eolMarkerColor The EOL marker color
//   */
//  public final void setEOLMarkerColor(Color eolMarkerColor) {
//    this.eolMarkerColor = eolMarkerColor;
//    repaint();
//  }


//  /**
//   * Returns true if EOL markers are drawn, false otherwise.
//   */
//  public final boolean getEOLMarkersPainted() {
//    return eolMarkers;
//  }


//  /**
//   * Sets if EOL markers are to be drawn.
//   * @param eolMarkers True if EOL markers should be drawn, false otherwise
//   */
//  public final void setEOLMarkersPainted(boolean eolMarkers) {
//    this.eolMarkers = eolMarkers;
//    repaint();
//  }


//  public final void setAntialias(boolean antialias) {
//    this.antialias = antialias;
//  }


//  /**
//   * Adds a custom highlight painter.
//   * @param highlight The highlight
//   */
//  public void addCustomHighlight(Highlight highlight) {
//    highlight.init(textArea,highlights);
//    highlights = highlight;
//  }


  /**
   * Highlight interface.
   */
  public interface Highlight {
    /**
     * Called after the highlight painter has been added.
     * @param textArea The text area
     * @param next The painter this one should delegate to
     */
    void init(JEditTextArea textArea, Highlight next);

    /**
     * This should paint the highlight and delgate to the
     * next highlight painter.
     * @param gfx The graphics context
     * @param line The line number
     * @param y The y co-ordinate of the line
     */
    void paintHighlight(Graphics gfx, int line, int y);

    /**
     * Returns the tool tip to display at the specified
     * location. If this highlighter doesn't know what to
     * display, it should delegate to the next highlight
     * painter.
     * @param evt The mouse event
     */
    String getToolTipText(MouseEvent evt);
  }


//  /**
//   * Returns the tool tip to display at the specified location.
//   * @param evt The mouse event
//   */
//  public String getToolTipText(MouseEvent evt) {
//    return (highlights == null) ? null : highlights.getToolTipText(evt);
//  }


  /** Returns the font metrics used by this component. */
  public FontMetrics getFontMetrics() {
    return fm;
  }


  public FontMetrics getFontMetrics(SyntaxStyle style) {
//    return getFontMetrics(style.isBold() ?
//                          defaults.boldFont : defaults.plainFont);
    return getFontMetrics(style.isBold() ? boldFont : plainFont);
  }


  // fry [160806 for 3.2]
  public int getLineHeight() {
    return fm.getHeight() + fm.getDescent();
  }


//  /**
//   * Sets the font for this component. This is overridden to update the
//   * cached font metrics and to recalculate which lines are visible.
//   * @param font The font
//   */
//  public void setFont(Font font) {
////    new Exception().printStackTrace(System.out);
//    super.setFont(font);
//    fm = super.getFontMetrics(font);
//    textArea.recalculateVisibleLines();
//  }


  /**
   * Repaints the text.
   * @param gfx The graphics context
   */
  public void paint(Graphics gfx) {
    Graphics2D g2 = (Graphics2D) gfx;
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        antialias ?
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON :
                        RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

    // no effect, one way or the other
//    g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
//                        RenderingHints.VALUE_FRACTIONALMETRICS_ON);

    Rectangle clipRect = gfx.getClipBounds();

    gfx.setColor(getBackground());
    gfx.fillRect(clipRect.x, clipRect.y, clipRect.width, clipRect.height);

    // We don't use yToLine() here because that method doesn't
    // return lines past the end of the document
    int height = fm.getHeight();
    int firstLine = textArea.getFirstLine();
    int firstInvalid = firstLine + clipRect.y / height;
    // Because the clipRect's height is usually an even multiple
    // of the font height, we subtract 1 from it, otherwise one
    // too many lines will always be painted.
    int lastInvalid = firstLine + (clipRect.y + clipRect.height - 1) / height;

    try {
      TokenMarkerState tokenMarker = textArea.getDocument().getTokenMarker();
      int x = textArea.getHorizontalOffset();

      for (int line = firstInvalid; line <= lastInvalid; line++) {
        paintLine(gfx, line, x, tokenMarker);
      }

      if (tokenMarker != null && tokenMarker.isNextLineRequested()) {
        int h = clipRect.y + clipRect.height;
        repaint(0, h, getWidth(), getHeight() - h);
      }
    } catch (Exception e) {
      System.err.println("Error repainting line" +
                         " range {" + firstInvalid + "," + lastInvalid + "}:");
      e.printStackTrace();
    }
  }


  /**
   * Marks a line as needing a repaint.
   * @param line The line to invalidate
   */
  final public void invalidateLine(int line) {
    repaint(0, textArea.lineToY(line) + fm.getMaxDescent() + fm.getLeading(),
            getWidth(), fm.getHeight());
  }


  /**
   * Marks a range of lines as needing a repaint.
   * @param firstLine The first line to invalidate
   * @param lastLine The last line to invalidate
   */
  final void invalidateLineRange(int firstLine, int lastLine) {
    repaint(0,textArea.lineToY(firstLine) +
            fm.getMaxDescent() + fm.getLeading(),
            getWidth(),(lastLine - firstLine + 1) * fm.getHeight());
  }


  /** Repaints the lines containing the selection. */
  final void invalidateSelectedLines() {
    invalidateLineRange(textArea.getSelectionStartLine(),
                        textArea.getSelectionStopLine());
  }


  /** Returns next tab stop after a specified point. */
//  TabExpander tabExpander = new TabExpander() {
  @Override
  public float nextTabStop(float x, int tabOffset) {
    int offset = textArea.getHorizontalOffset();
    int ntabs = ((int)x - offset) / tabSize;
    return (ntabs + 1) * tabSize + offset;
  }
//  };


  // do we go here? do will kill tabs?
//  public float nextTabStop(float x, int tabOffset) {
//    return x;
//  }


  public Dimension getPreferredSize() {
    return new Dimension(fm.charWidth('w') * defaults.cols,
                         fm.getHeight() * defaults.rows);
  }


  public Dimension getMinimumSize() {
    return getPreferredSize();
  }


  /**
   * Accessor used by tools that want to hook in and grab the formatting.
   */
  public int getCurrentLineIndex() {
    return currentLineIndex;
  }


  /**
   * Accessor used by tools that want to hook in and grab the formatting.
   */
  public void setCurrentLineIndex(int what) {
    currentLineIndex = what;
  }


  /**
   * Accessor used by tools that want to hook in and grab the formatting.
   */
  public Token getCurrentLineTokens() {
    return currentLineTokens;
  }


  /**
   * Accessor used by tools that want to hook in and grab the formatting.
   */
  public void setCurrentLineTokens(Token tokens) {
    currentLineTokens = tokens;
  }


  /**
   * Accessor used by tools that want to hook in and grab the formatting.
   */
  public Segment getCurrentLine() {
    return currentLine;
  }


  protected void paintLine(Graphics gfx, int line, int x,
                           TokenMarkerState tokenMarker) {
    currentLineIndex = line;
    int y = textArea.lineToY(line);

    if (tokenMarker == null) {
      //paintPlainLine(gfx, line, defaultFont, defaultColor, x, y);
      paintPlainLine(gfx, line, x, y);
    } else if (line >= 0 && line < textArea.getLineCount()) {
      //paintSyntaxLine(gfx, tokenMarker, line, defaultFont, defaultColor, x, y);
      paintSyntaxLine(gfx, line, x, y, tokenMarker);
    }
  }


  protected void paintPlainLine(Graphics gfx, int line, int x, int y) {
    paintHighlight(gfx, line, y);

    // don't try to draw lines past where they exist in the document
    // https://github.com/processing/processing/issues/5628
    if (line < textArea.getLineCount()) {
      textArea.getLineText(line, currentLine);

      int x0 = x - textArea.getHorizontalOffset();
      // prevent the blinking from drawing with last color used
      // https://github.com/processing/processing/issues/5628
      gfx.setColor(defaults.fgcolor);
      gfx.setFont(plainFont);

      y += fm.getHeight();
      // doesn't respect fixed width like it should
//    x = Utilities.drawTabbedText(currentLine, x, y, gfx, this, 0);
//    int w = fm.charWidth(' ');
      for (int i = 0; i < currentLine.count; i++) {
        gfx.drawChars(currentLine.array, currentLine.offset+i, 1, x, y);
        x = currentLine.array[currentLine.offset + i] == '\t' ?
          x0 + (int)nextTabStop(x - x0, i) :
          x + fm.charWidth(currentLine.array[currentLine.offset+i]);
        //textArea.offsetToX(line, currentLine.offset + i);
      }

      // Draw characters via input method.
      if (compositionTextPainter != null &&
        compositionTextPainter.hasComposedTextLayout()) {
        compositionTextPainter.draw(gfx, defaults.lineHighlightColor);
      }
    }
    if (defaults.eolMarkers) {
      gfx.setColor(defaults.eolMarkerColor);
      gfx.drawString(".", x, y);
    }
  }


  protected void paintSyntaxLine(Graphics gfx, int line, int x, int y,
                                 TokenMarkerState tokenMarker) {
    textArea.getLineText(currentLineIndex, currentLine);
    currentLineTokens = tokenMarker.markTokens(currentLine, currentLineIndex);

//    gfx.setFont(plainFont);
    paintHighlight(gfx, line, y);

//    gfx.setFont(defaultFont);
//    gfx.setColor(defaultColor);
    y += fm.getHeight();
//    x = paintSyntaxLine(currentLine,
//                        currentLineTokens,
//                        defaults.styles, this, gfx, x, y);
    x = paintSyntaxLine(gfx, currentLine, x, y,
                        currentLineTokens,
                        defaults.styles);
    // Draw characters via input method.
    if (compositionTextPainter != null &&
        compositionTextPainter.hasComposedTextLayout()) {
      compositionTextPainter.draw(gfx, defaults.lineHighlightColor);
    }
    if (defaults.eolMarkers) {
      gfx.setColor(defaults.eolMarkerColor);
      gfx.drawString(".", x, y);
    }
  }


  /**
   * Paints the specified line onto the graphics context. Note that this
   * method munges the offset and count values of the segment.
   * @param line The line segment
   * @param tokens The token list for the line
   * @param styles The syntax style list
   * @param expander The tab expander used to determine tab stops. May
   * be null
   * @param gfx The graphics context
   * @param x The x co-ordinate
   * @param y The y co-ordinate
   * @return The x co-ordinate, plus the width of the painted string
   */
//  public int paintSyntaxLine(Segment line, Token tokens, SyntaxStyle[] styles,
//                             TabExpander expander, Graphics gfx,
//                             int x, int y) {
  protected int paintSyntaxLine(Graphics gfx, Segment line, int x, int y,
                                Token tokens, SyntaxStyle[] styles) {
//    Font defaultFont = gfx.getFont();
//    Color defaultColor = gfx.getColor();

    int x0 = x - textArea.getHorizontalOffset();

//    for (byte id = tokens.id; id != Token.END; tokens = tokens.next) {
    for (;;) {
      byte id = tokens.id;
      if (id == Token.END)
        break;

      int length = tokens.length;
      if (id == Token.NULL) {
//        if(!defaultColor.equals(gfx.getColor()))
//          gfx.setColor(defaultColor);
//        if(!defaultFont.equals(gfx.getFont()))
//          gfx.setFont(defaultFont);
        gfx.setColor(defaults.fgcolor);
        gfx.setFont(plainFont);
      } else {
        //styles[id].setGraphicsFlags(gfx,defaultFont);
        SyntaxStyle ss = styles[id];
        gfx.setColor(ss.getColor());
        gfx.setFont(ss.isBold() ? boldFont : plainFont);
      }
      line.count = length;  // huh? suspicious
      // doesn't respect mono metrics, insists on spacing w/ fractional or something
//      x = Utilities.drawTabbedText(line, x, y, gfx, this, 0);
//      gfx.drawChars(line.array, line.offset, line.count, x, y);
//      int w = fm.charWidth(' ');
      for (int i = 0; i < line.count; i++) {
        gfx.drawChars(line.array, line.offset+i, 1, x, y);
        x = line.array[line.offset + i] == '\t' ?
            x0 + (int)nextTabStop(x - x0, i) :
            x + fm.charWidth(line.array[line.offset+i]);
      }
      //x += fm.charsWidth(line.array, line.offset, line.count);
      //x += fm.charWidth(' ') * line.count;
      line.offset += length;

      tokens = tokens.next;
    }

    return x;
  }


  protected void paintHighlight(Graphics gfx, int line, int y) {
    if (line >= textArea.getSelectionStartLine() &&
        line <= textArea.getSelectionStopLine()) {
      paintLineHighlight(gfx, line, y);
    }

    if (highlights != null) {
      highlights.paintHighlight(gfx, line, y);
    }

    if (defaults.bracketHighlight && line == textArea.getBracketLine()) {
      paintBracketHighlight(gfx, line, y);
    }

    if (line == textArea.getCaretLine()) {
      paintCaret(gfx, line, y);
    }
  }


  protected void paintLineHighlight(Graphics gfx, int line, int y) {
    int height = fm.getHeight();
    y += fm.getLeading() + fm.getMaxDescent();

    int selectionStart = textArea.getSelectionStart();
    int selectionEnd = textArea.getSelectionStop();

    if (selectionStart == selectionEnd) {
      if (defaults.lineHighlight) {
        gfx.setColor(defaults.lineHighlightColor);
        gfx.fillRect(0, y, getWidth(), height);
      }
    } else {
      gfx.setColor(defaults.selectionColor);

      int selectionStartLine = textArea.getSelectionStartLine();
      int selectionEndLine = textArea.getSelectionStopLine();
      int lineStart = textArea.getLineStartOffset(line);

      int x1, x2;
      if (selectionStartLine == selectionEndLine) {
        x1 = textArea._offsetToX(line, selectionStart - lineStart);
        x2 = textArea._offsetToX(line, selectionEnd - lineStart);
      } else if(line == selectionStartLine) {
        x1 = textArea._offsetToX(line, selectionStart - lineStart);
        x2 = getWidth();
      } else if(line == selectionEndLine) {
        //x1 = 0;
        // hack from stendahl to avoid doing weird side selection thing
        x1 = textArea._offsetToX(line, 0);
        // attempt at getting the gutter too, but doesn't seem to work
        //x1 = textArea._offsetToX(line, -textArea.getHorizontalOffset());
        x2 = textArea._offsetToX(line, selectionEnd - lineStart);
      } else {
        //x1 = 0;
        // hack from stendahl to avoid doing weird side selection thing
        x1 = textArea._offsetToX(line, 0);
        // attempt at getting the gutter too, but doesn't seem to work
        //x1 = textArea._offsetToX(line, -textArea.getHorizontalOffset());
        x2 = getWidth();
      }

      // "inlined" min/max()
      gfx.fillRect(x1 > x2 ? x2 : x1,y,x1 > x2 ?
                   (x1 - x2) : (x2 - x1),height);
    }
  }


  protected void paintBracketHighlight(Graphics gfx, int line, int y) {
    int position = textArea.getBracketPosition();
    if (position != -1) {
      y += fm.getLeading() + fm.getMaxDescent();
      int x = textArea._offsetToX(line, position);
      gfx.setColor(defaults.bracketHighlightColor);
      // Hack!!! Since there is no fast way to get the character
      // from the bracket matching routine, we use ( since all
      // brackets probably have the same width anyway
      gfx.drawRect(x,y,fm.charWidth('(') - 1, fm.getHeight() - 1);
    }
  }


  protected void paintCaret(Graphics gfx, int line, int y) {
    //System.out.println("painting caret " + line + " " + y);
    if (textArea.isCaretVisible()) {
      //System.out.println("caret is visible");
      int offset =
        textArea.getCaretPosition() - textArea.getLineStartOffset(line);
      int caretX = textArea._offsetToX(line, offset);
      int caretWidth = ((defaults.blockCaret ||
                         textArea.isOverwriteEnabled()) ?
                        fm.charWidth('w') : 1);
      y += fm.getLeading() + fm.getMaxDescent();
      int height = fm.getHeight();

      //System.out.println("caretX, width = " + caretX + " " + caretWidth);

      gfx.setColor(defaults.caretColor);

      if (textArea.isOverwriteEnabled()) {
        gfx.fillRect(caretX, y + height - 1, caretWidth,1);

      } else {
        // some machines don't like the drawRect for the single
        // pixel caret.. this caused a lot of hell because on that
        // minority of machines, the caret wouldn't show up past
        // the first column. the fix is to use drawLine() in
        // those cases, as a workaround.
        if (caretWidth == 1) {
          gfx.drawLine(caretX, y, caretX, y + height - 1);
        } else {
          gfx.drawRect(caretX, y, caretWidth - 1, height - 1);
        }
        //gfx.drawRect(caretX, y, caretWidth, height - 1);
      }
    }
  }


  public int getScrollWidth() {
    // https://github.com/processing/processing/issues/3591
    return super.getWidth();
  }
}
