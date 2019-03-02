/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
 * JEditTextArea.java - jEdit's text component
 * Copyright (C) 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

package processing.app.syntax;

import processing.app.*;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;
import javax.swing.*;

import java.awt.im.InputMethodRequests;

import processing.app.syntax.im.InputMethodSupport;
import processing.core.PApplet;

/**
 * The text area component from the JEdit Syntax (syntax.jedit.org) project.
 * This is a very early version of what later was completely rewritten and
 * become jEdit (jedit.org). Over the years we've also added minor features
 * for use with Processing (notably mouse wheel support and copyAsHTML). [fry]
 * <p>
 * jEdit's text area component. It is more suited for editing program
 * source code than JEditorPane, because it drops the unnecessary features
 * (images, variable-width lines, and so on) and adds a whole bunch of
 * useful goodies such as:
 * <ul>
 * <li>More flexible key binding scheme
 * <li>Supports macro recorders
 * <li>Rectangular selection
 * <li>Bracket highlighting
 * <li>Syntax highlighting
 * <li>Command repetition
 * <li>Block caret can be enabled
 * </ul>
 * It is also faster and doesn't have as many problems. It can be used
 * in other applications; the only other part of jEdit it depends on is
 * the syntax package.
 * <p>
 * To use it in your app, treat it like any other component, for example:
 * <pre>JEditTextArea ta = new JEditTextArea();
 * ta.setTokenMarker(new JavaTokenMarker());
 * ta.setText("public class Test {\n"
 *     + "    public static void main(String[] args) {\n"
 *     + "        System.out.println(\"Hello World\");\n"
 *     + "    }\n"
 *     + "}");</pre>
 *
 * @author Slava Pestov
 */
public class JEditTextArea extends JComponent
{
  /**
   * Adding components with this name to the text area will place
   * them left of the horizontal scroll bar. In jEdit, the status
   * bar is added this way.
   */
  public static String LEFT_OF_SCROLLBAR = "los";

  /** The size of the offset between the leftmost padding and the code */
  public static final int leftHandGutter = 6;

  private InputMethodSupport inputMethodSupport;

  private TextAreaDefaults defaults;

  private Brackets bracketHelper = new Brackets();


  /**
   * Creates a new JEditTextArea with the specified settings.
   * @param defaults The default settings
   */
  public JEditTextArea(TextAreaDefaults defaults, InputHandler inputHandler) {
    this.defaults = defaults;

    // Enable the necessary events
    enableEvents(AWTEvent.KEY_EVENT_MASK);

    if (!DISABLE_CARET) {
      caretTimer = new Timer(500, new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (hasFocus()) {
            blinkCaret();
          }
        }
      });
      caretTimer.setInitialDelay(500);
      caretTimer.start();
    }

    // Initialize some misc. stuff
    painter = createPainter(defaults);
    documentHandler = new DocumentHandler();
    eventListenerList = new EventListenerList();
    caretEvent = new MutableCaretEvent();
    lineSegment = new Segment();
    bracketLine = bracketPosition = -1;
    blink = true;

    // Initialize the GUI
    setLayout(new ScrollLayout());
    add(CENTER, painter);
    add(RIGHT, vertical = new JScrollBar(Adjustable.VERTICAL));
    add(BOTTOM, horizontal = new JScrollBar(Adjustable.HORIZONTAL));

    // Add some event listeners
    vertical.addAdjustmentListener(new AdjustHandler());
    horizontal.addAdjustmentListener(new AdjustHandler());
    painter.addComponentListener(new ComponentHandler());
    painter.addMouseListener(new MouseHandler());
    painter.addMouseMotionListener(new DragHandler());
    addFocusListener(new FocusHandler());
    // send tab keys through to the text area
    // http://dev.processing.org/bugs/show_bug.cgi?id=1267
    setFocusTraversalKeysEnabled(false);

    // Load the defaults
    setInputHandler(inputHandler);
    setDocument(defaults.document);
//    editable = defaults.editable;
    caretVisible = defaults.caretVisible;
    caretBlinks = defaults.caretBlinks;
    electricScroll = defaults.electricScroll;

    // We don't seem to get the initial focus event?
//    focusedComponent = this;

    addMouseWheelListener(new MouseWheelListener() {

      @Override
      public void mouseWheelMoved(MouseWheelEvent e) {
        if (scrollBarsInitialized) {
          if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
            int scrollAmount = e.getUnitsToScroll();
//            System.out.println("rot/amt = " + e.getWheelRotation() + " " + amt);
//            int max = vertical.getMaximum();
//            System.out.println("UNIT SCROLL of " + amt + " at value " + vertical.getValue() + " and max " + max);
//            System.out.println("  get wheel rotation is " + e.getWheelRotation());
//            int ex = e.getModifiersEx();
//            String mods = InputEvent.getModifiersExText(ex);
//            if (ex != 0) {
//              System.out.println("  3 2         1         0");
//              System.out.println("  10987654321098765432109876543210");
//              System.out.println("  " + PApplet.binary(e.getModifiersEx()));
////            if (mods.length() > 0) {
//              System.out.println("  mods extext = " + mods + " " + mods.length() + " " + PApplet.hex(mods.charAt(0)));
//            }
//            System.out.println("  " + e);

            // inertia scrolling on OS X will fire several shift-wheel events
            // that are negative values.. this makes the scrolling area jump.
            boolean isHorizontal = Platform.isMacOS() && e.isShiftDown();
            if (isHorizontal) {
              horizontal.setValue(horizontal.getValue() + scrollAmount);
            }else{
              vertical.setValue(vertical.getValue() + scrollAmount);
            }
          }
        }
      }
    });
  }


  /**
   * Override this to provide your own painter for this {@link JEditTextArea}.
   * @param defaults
   * @return a newly constructed {@link TextAreaPainter}.
   */
  protected TextAreaPainter createPainter(final TextAreaDefaults defaults) {
    return new TextAreaPainter(this, defaults);
  }


  /**
   * Inline Input Method Support for Japanese.
   */
  public InputMethodRequests getInputMethodRequests() {
    if (Preferences.getBoolean("editor.input_method_support")) {
      if (inputMethodSupport == null) {
        inputMethodSupport = new InputMethodSupport(this);
      }
      return inputMethodSupport;
    }
    return null;
  }


  /**
   * Get current position of the vertical scroll bar. [fry]
   * @deprecated Use {@link #getVerticalScrollPosition()}.
   */
  public int getScrollPosition() {
    return getVerticalScrollPosition();
  }


  /**
   * Set position of the vertical scroll bar. [fry]
   * @deprecated Use {@link #setVerticalScrollPosition(int)}.
   */
  public void setScrollPosition(int what) {
    setVerticalScrollPosition(what);
  }


  /**
   * Get current position of the vertical scroll bar.
   */
  public int getVerticalScrollPosition() {
    return vertical.getValue();
  }


  /**
   * Set position of the vertical scroll bar.
   */
  public void setVerticalScrollPosition(int what) {
    vertical.setValue(what);
  }


  /**
   * Get current position of the horizontal scroll bar.
   */
  public int getHorizontalScrollPosition() {
    return horizontal.getValue();
  }


  /**
   * Set position of the horizontal scroll bar.
   */
  public void setHorizontalScrollPosition(int what) {
    horizontal.setValue(what);
  }


  /**
   * Returns the object responsible for painting this text area.
   */
  public final TextAreaPainter getPainter() {
    return painter;
  }


  public TextAreaDefaults getDefaults() {
    return defaults;
  }


  /**
   * Returns the input handler.
   */
  public final InputHandler getInputHandler() {
    return inputHandler;
  }


  /**
   * Sets the input handler.
   * @param inputHandler The new input handler
   */
  public void setInputHandler(InputHandler inputHandler) {
    this.inputHandler = inputHandler;
  }


  /**
   * Returns true if the caret is blinking, false otherwise.
   */
  public final boolean isCaretBlinkEnabled() {
    return caretBlinks;
  }


  /**
   * Toggles caret blinking.
   * @param caretBlinks True if the caret should blink, false otherwise
   */
  public void setCaretBlinkEnabled(boolean caretBlinks) {
    this.caretBlinks = caretBlinks;
    if (!caretBlinks) {
      blink = false;
    }
    painter.invalidateSelectedLines();
  }


  /**
   * Returns true if the caret is visible, false otherwise.
   */
  public final boolean isCaretVisible() {
    return (!caretBlinks || blink) && caretVisible;
  }


  /**
   * Sets if the caret should be visible.
   * @param caretVisible True if the caret should be visible, false
   * otherwise
   */
  public void setCaretVisible(boolean caretVisible) {
    this.caretVisible = caretVisible;
    blink = true;

    painter.invalidateSelectedLines();
  }


  /**
   * Blinks the caret.
   */
  public final void blinkCaret() {
    if (caretBlinks)  {
      blink = !blink;
      painter.invalidateSelectedLines();
    } else {
      blink = true;
    }
  }


  /**
   * Returns the number of lines from the top and button of the
   * text area that are always visible.
   */
  public final int getElectricScroll() {
    return electricScroll;
  }


  /**
   * Sets the number of lines from the top and bottom of the text
   * area that are always visible
   * @param electricScroll The number of lines always visible from
   * the top or bottom
   */
  public final void setElectricScroll(int electricScroll) {
    this.electricScroll = electricScroll;
  }


  /**
   * Updates the state of the scroll bars. This should be called
   * if the number of lines in the document changes, or when the
   * size of the text area changes.
   */
  public void updateScrollBars() {
    if (vertical != null && visibleLines != 0) {
      vertical.setValues(firstLine,visibleLines,0,getLineCount());
      vertical.setUnitIncrement(2);
      vertical.setBlockIncrement(visibleLines);
    }

    //if (horizontal != null && width != 0) {
    if ((horizontal != null) && (painter.getWidth() != 0)) {
      //int value = horizontal.getValue();
      //System.out.println("updateScrollBars");
      //int width = painter.getWidth();
      int lineCount = getLineCount();
      int maxLineLength = 0;
      for (int i = 0; i < lineCount; i++) {
        int lineLength = getLineLength(i);
        if (lineLength > maxLineLength) {
          maxLineLength = lineLength;
        }
      }
      int charWidth = painter.getFontMetrics().charWidth('w');
      int width = maxLineLength * charWidth;
      int painterWidth = painter.getScrollWidth();

      // Update to how horizontal scrolling is handled
      // http://code.google.com/p/processing/issues/detail?id=280
      // http://code.google.com/p/processing/issues/detail?id=316
      //setValues(int newValue, int newExtent, int newMin, int newMax)
      if (horizontalOffset < 0) {
        horizontal.setValues(-horizontalOffset, painterWidth, -leftHandGutter, width);
      } else {
        horizontal.setValues(-leftHandGutter, painterWidth, -leftHandGutter, width);
      }

      //horizontal.setUnitIncrement(painter.getFontMetrics().charWidth('w'));
      horizontal.setUnitIncrement(charWidth);
      horizontal.setBlockIncrement(width / 2);
    }
  }


  /**
   * Returns the line displayed at the text area's origin.
   */
  public final int getFirstLine() {
    return firstLine;
  }


  /**
   * Sets the line displayed at the text area's origin without
   * updating the scroll bars.
   */
  public void setFirstLine(int firstLine) {
    if(firstLine < 0 || firstLine > getLineCount()) {
      throw new IllegalArgumentException("First line out of range: "
        + firstLine + " [0, " + getLineCount() + "]");
    }

    if (firstLine == this.firstLine) return;

    this.firstLine = firstLine;
    if (firstLine != vertical.getValue()) {
      updateScrollBars();
    }
    painter.repaint();
  }


  /**
   * Convenience for checking what's on-screen. [fry]
   */
  public final int getLastLine() {
    return getFirstLine() + getVisibleLines();
  }


  /**
   * Returns the number of lines visible in this text area.
   */
  public final int getVisibleLines() {
    return visibleLines;
  }


  /**
   * Recalculates the number of visible lines. This should not
   * be called directly.
   */
  public final void recalculateVisibleLines() {
    if (painter == null) return;

    int height = painter.getHeight();
    int lineHeight = painter.getFontMetrics().getHeight();
    visibleLines = height / lineHeight;
    updateScrollBars();
  }


  /**
   * Returns the horizontal offset of drawn lines.
   */
  public final int getHorizontalOffset() {
    return horizontalOffset;
  }


  /**
   * Sets the horizontal offset of drawn lines. This can be used to
   * implement horizontal scrolling.
   * @param horizontalOffset offset The new horizontal offset
   */
  public void setHorizontalOffset(int horizontalOffset) {
    if (horizontalOffset == this.horizontalOffset) {
      return;
    }
    this.horizontalOffset = horizontalOffset;
    if (horizontalOffset != horizontal.getValue()) {
      updateScrollBars();
    }
    painter.repaint();
  }


  /**
   * A fast way of changing both the first line and horizontal
   * offset.
   * @param firstLine The new first line
   * @param horizontalOffset The new horizontal offset
   * @return True if any of the values were changed, false otherwise
   */
  public boolean setOrigin(int firstLine, int horizontalOffset) {
    boolean changed = false;

    if (horizontalOffset != this.horizontalOffset) {
      this.horizontalOffset = horizontalOffset;
      changed = true;
    }

    if (firstLine != this.firstLine) {
      this.firstLine = firstLine;
      changed = true;
    }

    if (changed) {
      updateScrollBars();
      painter.repaint();
    }
    return changed;
  }


  /**
   * Ensures that the caret is visible by scrolling the text area if
   * necessary.
   * @return True if scrolling was actually performed, false if the
   * caret was already visible
   */
  public boolean scrollToCaret() {
    int line = getCaretLine();
    int lineStart = getLineStartOffset(line);
    int offset = Math.max(0,Math.min(getLineLength(line) - 1,
        getCaretPosition() - lineStart));

    return scrollTo(line,offset);
  }


  /**
   * Ensures that the specified line and offset is visible by scrolling
   * the text area if necessary.
   * @param line The line to scroll to
   * @param offset The offset in the line to scroll to
   * @return True if scrolling was actually performed, false if the
   * line and offset was already visible
   */
  public boolean scrollTo(int line, int offset) {
    // visibleLines == 0 before the component is realized
    // we can't do any proper scrolling then, so we have
    // this hack...
    if (visibleLines == 0) {
      setFirstLine(Math.max(0,line - electricScroll));
      return true;
    }

    int newFirstLine = firstLine;
    int newHorizontalOffset = horizontalOffset;

    if(line < firstLine + electricScroll) {
      newFirstLine = Math.max(0,line - electricScroll);

    } else if(line + electricScroll >= firstLine + visibleLines) {
      newFirstLine = (line - visibleLines) + electricScroll + 1;
      if(newFirstLine + visibleLines >= getLineCount())
        newFirstLine = getLineCount() - visibleLines;
      if(newFirstLine < 0)
        newFirstLine = 0;
    }

    int x = _offsetToX(line,offset);
    int width = painter.getFontMetrics().charWidth('w');

    if(x < 0) {
      newHorizontalOffset = Math.max(0,horizontalOffset - x + width + 5);
    } else if(x + width >= painter.getWidth()) {
      newHorizontalOffset = horizontalOffset +
      (painter.getWidth() - x) - width - 5;
    }

    return setOrigin(newFirstLine,newHorizontalOffset);
  }


  /**
   * Converts a line index to a y co-ordinate.
   * @param line The line
   */
  public int lineToY(int line) {
    FontMetrics fm = painter.getFontMetrics();
    return (line - firstLine) * fm.getHeight()
    - (fm.getLeading() + fm.getMaxDescent());
  }


  /**
   * Converts a y co-ordinate to a line index.
   * @param y The y co-ordinate
   */
  public int yToLine(int y) {
    FontMetrics fm = painter.getFontMetrics();
    int height = fm.getHeight();
    return Math.max(0, Math.min(getLineCount() - 1, y / height + firstLine));
  }


  /**
   * Converts an offset in a line into an x co-ordinate. This is a
   * slow version that can be used any time.
   * @param line The line
   * @param offset The offset, from the start of the line
   */
  public final int offsetToX(int line, int offset) {
    // don't use cached tokens
    painter.currentLineTokens = null;
    return _offsetToX(line,offset);
  }


  /**
   * Converts an offset in a line into an x coordinate. This is a
   * fast version that should only be used if no changes were made
   * to the text since the last repaint.
   * @param line The line
   * @param offset The offset, from the start of the line
   */
  public int _offsetToX(int line, int offset) {
    TokenMarkerState tokenMarker = getTokenMarker();

    // Use painter's cached info for speed
    FontMetrics fm = painter.getFontMetrics();

    getLineText(line, lineSegment);

    int segmentOffset = lineSegment.offset;
    int x = horizontalOffset;

    // If syntax coloring is disabled, do simple translation
    if (tokenMarker == null) {
      lineSegment.count = offset;
      return x + Utilities.getTabbedTextWidth(lineSegment, fm, x, painter, 0);

    } else {
      // If syntax coloring is enabled, we have to do this
      // because tokens can vary in width
      Token tokens;
      if (painter.currentLineIndex == line && painter.currentLineTokens != null) {
        tokens = painter.currentLineTokens;
      } else {
        painter.currentLineIndex = line;
        tokens = painter.currentLineTokens = tokenMarker.markTokens(lineSegment, line);
      }

//      Font defaultFont = painter.getFont();
      SyntaxStyle[] styles = painter.getStyles();

      for (;;) {
        byte id = tokens.id;
        if (id == Token.END) {
          return x;
        }

        if (id == Token.NULL) {
          fm = painter.getFontMetrics();
        } else {
          //fm = styles[id].getFontMetrics(defaultFont, this);
          fm = painter.getFontMetrics(styles[id]);
        }

        int length = tokens.length;
        if (offset + segmentOffset < lineSegment.offset + length) {
          lineSegment.count = offset - (lineSegment.offset - segmentOffset);
          return x + Utilities.getTabbedTextWidth(lineSegment, fm, x, painter, 0);
        } else {
          lineSegment.count = length;
          x += Utilities.getTabbedTextWidth(lineSegment, fm, x, painter, 0);
          lineSegment.offset += length;
        }
        tokens = tokens.next;
      }
    }
  }

  /**
   * Converts an x co-ordinate to an offset within a line.
   * @param line The line
   * @param x The x co-ordinate
   */
  public int xToOffset(int line, int x) {
    TokenMarkerState tokenMarker = getTokenMarker();

    /* Use painter's cached info for speed */
    FontMetrics fm = painter.getFontMetrics();
//    System.out.println("metrics: " + fm);

    getLineText(line,lineSegment);

    char[] segmentArray = lineSegment.array;
    int segmentOffset = lineSegment.offset;
    int segmentCount = lineSegment.count;

    int width = horizontalOffset;

    if(tokenMarker == null)
    {
      for(int i = 0; i < segmentCount; i++)
      {
        char c = segmentArray[i + segmentOffset];
        int charWidth;
        if(c == '\t')
          charWidth = (int)painter.nextTabStop(width,i)
          - width;
        else
          charWidth = fm.charWidth(c);

        if(painter.isBlockCaretEnabled())
        {
          if(x - charWidth <= width)
            return i;
        }
        else
        {
          if(x - charWidth / 2 <= width)
            return i;
        }

        width += charWidth;
      }
      return segmentCount;

    } else {
      Token tokens;
      if (painter.currentLineIndex == line &&
          painter.currentLineTokens != null) {
        tokens = painter.currentLineTokens;
      } else {
        painter.currentLineIndex = line;
        tokens = painter.currentLineTokens = tokenMarker.markTokens(lineSegment,line);
      }

      int offset = 0;
//      Font defaultFont = painter.getFont();
      SyntaxStyle[] styles = painter.getStyles();
//      System.out.println("painter is " + painter + ", doc is " + document);

      for (;;) {
        byte id = tokens.id;
        if(id == Token.END)
          return offset;

        if (id == Token.NULL) {
          fm = painter.getFontMetrics();
        } else {
          //fm = styles[id].getFontMetrics(defaultFont, this);
          fm = painter.getFontMetrics(styles[id]);
        }

        int length = tokens.length;

        for (int i = 0; i < length; i++) {
//          System.out.println("segmentOffset = " + segmentOffset +
//                             ", offset = " + offset +
//                             ", i = " + i +
//                             ", length = " + length +
//                             ", array len = " + segmentArray.length);
          if (segmentOffset + offset + i >= segmentArray.length) {
            return segmentArray.length - segmentOffset - 1;
          }
          char c = segmentArray[segmentOffset + offset + i];
          int charWidth;
          if (c == '\t') {
            charWidth = (int)painter.nextTabStop(width,offset + i) - width;
          } else {
            charWidth = fm.charWidth(c);
          }

          if (painter.isBlockCaretEnabled()) {
            if (x - charWidth <= width) {
              return offset + i;
            }
          } else {
            if (x - charWidth / 2 <= width) {
              return offset + i;
            }
          }

          width += charWidth;
        }

        offset += length;
        tokens = tokens.next;
      }
    }
  }


  /**
   * Converts a point to an offset, from the start of the text.
   * @param x The x co-ordinate of the point
   * @param y The y co-ordinate of the point
   */
  public int xyToOffset(int x, int y) {
    int line = yToLine(y);
    int start = getLineStartOffset(line);
    return start + xToOffset(line,x);
  }


  /**
   * Returns the document this text area is editing.
   */
  public final SyntaxDocument getDocument() {
    return document;
  }


  /**
   * Sets the document this text area is editing.
   * @param document The document
   */
  public void setDocument(SyntaxDocument document) {
    if (this.document == document)
      return;
    if (this.document != null)
      this.document.removeDocumentListener(documentHandler);
    this.document = document;

    document.addDocumentListener(documentHandler);

    bracketHelper.invalidate();
    select(0, 0);
    updateScrollBars();
    painter.repaint();
  }


  /**
   * Set document with a twist, includes the old caret
   * and scroll positions, added for p5. [fry]
   */
  public void setDocument(SyntaxDocument document,
                          int start, int stop, int scroll) {
    if (this.document == document)
      return;
    if (this.document != null)
      this.document.removeDocumentListener(documentHandler);
    this.document = document;

    document.addDocumentListener(documentHandler);

    bracketHelper.invalidate();
    select(start, stop);
    updateScrollBars();
    setVerticalScrollPosition(scroll);
    painter.repaint();
  }


  /**
   * Returns the document's token marker. Equivalent to calling
   * <code>getDocument().getTokenMarker()</code>.
   */
  public final TokenMarkerState getTokenMarker() {
    return document.getTokenMarker();
  }


  /**
   * Sets the document's token marker. Equivalent to caling
   * <code>getDocument().setTokenMarker()</code>.
   * @param tokenMarker The token marker
   */
  public final void setTokenMarker(TokenMarker tokenMarker) {
    document.setTokenMarker(tokenMarker);
  }


  /**
   * Returns the length of the document. Equivalent to calling
   * <code>getDocument().getLength()</code>.
   */
  public final int getDocumentLength() {
    return document.getLength();
  }


  /**
   * Returns the number of lines in the document.
   */
  public final int getLineCount() {
    return document.getDefaultRootElement().getElementCount();
  }


  /**
   * Returns the line containing the specified offset.
   * @param offset The offset
   */
  public final int getLineOfOffset(int offset) {
    return document.getDefaultRootElement().getElementIndex(offset);
  }


  /**
   * Returns the start offset of the specified line.
   * @param line The line
   * @return The start offset of the specified line, or -1 if the line is
   * invalid
   */
  public int getLineStartOffset(int line) {
    Element lineElement = document.getDefaultRootElement().getElement(line);
    return (lineElement == null) ? -1 : lineElement.getStartOffset();
  }


  public int getLineStartNonWhiteSpaceOffset(int line) {
    int offset = getLineStartOffset(line);
    int length = getLineLength(line);
    String str = getText(offset, length);

    for(int i = 0; i < str.length(); i++) {
      if(!Character.isWhitespace(str.charAt(i))) {
        return offset + i;
      }
    }
    return offset + length;
  }


  /**
   * Returns the end offset of the specified line.
   * @param line The line
   * @return The end offset of the specified line, or -1 if the line is
   * invalid.
   */
  public int getLineStopOffset(int line) {
    Element lineElement = document.getDefaultRootElement().getElement(line);
    return (lineElement == null) ? -1 : lineElement.getEndOffset();
  }


  public int getLineStopNonWhiteSpaceOffset(int line) {
    int offset = getLineStopOffset(line);
    int length = getLineLength(line);
    String str = getText(offset - length - 1, length);

    for (int i = 0; i < length; i++) {
      if(!Character.isWhitespace(str.charAt(length - i - 1))) {
        return offset - i;
      }
    }
    return offset - length;
  }


  /**
   * Returns the start offset of the line after this line, or the end of
   * this line if there is no next line.
   * @param line The line
   * @return The end offset of the specified line, or -1 if the line is
   * invalid.
   */
  public int getLineSelectionStopOffset(int line) {
    Element lineElement = document.getDefaultRootElement().getElement(line);
    return (lineElement == null) ? -1 :
      Math.min(lineElement.getEndOffset(), getDocumentLength());
  }


  /**
   * Returns the length of the specified line.
   * @param line The line
   */
  public int getLineLength(int line) {
    Element lineElement = document.getDefaultRootElement().getElement(line);
    return (lineElement == null) ? -1 :
      lineElement.getEndOffset() - lineElement.getStartOffset() - 1;
  }


  /**
   * Returns the entire text of this text area.
   */
  public String getText() {
    try {
      return document.getText(0,document.getLength());

    } catch(BadLocationException bl) {
      bl.printStackTrace();
      return null;
    }
  }


  /**
   * Sets the entire text of this text area.
   */
  public void setText(String text) {
    try {
      document.beginCompoundEdit();
      document.remove(0,document.getLength());
      document.insertString(0,text,null);

    } catch (BadLocationException bl) {
      bl.printStackTrace();

    } finally {
      document.endCompoundEdit();
    }
  }


  /**
   * Returns the specified substring of the document.
   * @param start The start offset
   * @param len The length of the substring
   * @return The substring, or null if the offsets are invalid
   */
  public final String getText(int start, int len) {
    try {
      return document.getText(start,len);

    } catch(BadLocationException bl) {
      bl.printStackTrace();
      return null;
    }
  }


  /**
   * Copies the specified substring of the document into a segment.
   * If the offsets are invalid, the segment will contain a null string.
   * @param start The start offset
   * @param len The length of the substring
   * @param segment The segment
   */
  public final void getText(int start, int len, Segment segment) {
    try {
      document.getText(start,len,segment);

    } catch (BadLocationException bl) {
      bl.printStackTrace();
      System.err.format("Bad Location: %d for start %d and length %d",
                        bl.offsetRequested(), start, len);
      segment.offset = segment.count = 0;
    }
  }


  /**
   * Returns the text on the specified line.
   * @param lineIndex The line
   * @return The text, or null if the line is invalid
   */
  public final String getLineText(int lineIndex) {
    int start = getLineStartOffset(lineIndex);
    return getText(start,getLineStopOffset(lineIndex) - start - 1);
  }


  /**
   * Copies the text on the specified line into a segment. If the line
   * is invalid, the segment will contain a null string.
   * @param lineIndex The line
   */
  public final void getLineText(int lineIndex, Segment segment) {
    int start = getLineStartOffset(lineIndex);
    getText(start,getLineStopOffset(lineIndex) - start - 1,segment);
  }


  /**
   * Returns the selection start offset.
   */
  public final int getSelectionStart() {
    return selectionStart;
  }


  /**
   * Returns the offset where the selection starts on the specified
   * line.
   */
  public int getSelectionStart(int line)
  {
    if (line == selectionStartLine)
      return selectionStart;
    else
      return getLineStartOffset(line);
  }

  /**
   * Returns the selection start line.
   */
  public final int getSelectionStartLine()
  {
    return selectionStartLine;
  }

  /**
   * Sets the selection start. The new selection will be the new
   * selection start and the old selection end.
   * @param selectionStart The selection start
   * @see #select(int,int)
   */
  public final void setSelectionStart(int selectionStart)
  {
    select(selectionStart,selectionEnd);
  }

  /**
   * Returns the selection end offset.
   */
  public final int getSelectionStop()
  {
    return selectionEnd;
  }

  /**
   * Returns the offset where the selection ends on the specified
   * line.
   */
  public int getSelectionStop(int line)
  {
    if (line == selectionEndLine)
      return selectionEnd;
    else
      return getLineStopOffset(line) - 1;
  }

  /**
   * Returns the selection end line.
   */
  public final int getSelectionStopLine()
  {
    return selectionEndLine;
  }

  /**
   * Sets the selection end. The new selection will be the old
   * selection start and the bew selection end.
   * @param selectionEnd The selection end
   * @see #select(int,int)
   */
  public final void setSelectionEnd(int selectionEnd)
  {
    select(selectionStart,selectionEnd);
  }


  public final boolean isSelectionActive()
  {
    return(selectionStart != selectionEnd);
  }

  /**
   * Returns the caret position. This will either be the selection
   * start or the selection end, depending on which direction the
   * selection was made in.
   */
  public final int getCaretPosition()
  {
    return (biasLeft ? selectionStart : selectionEnd);
  }

  /**
   * Returns the caret line.
   */
  public final int getCaretLine()
  {
    return (biasLeft ? selectionStartLine : selectionEndLine);
  }

  /**
   * Returns the mark position. This will be the opposite selection
   * bound to the caret position.
   * @see #getCaretPosition()
   */
  public final int getMarkPosition()
  {
    return (biasLeft ? selectionEnd : selectionStart);
  }

  /**
   * Returns the mark line.
   */
  public final int getMarkLine()
  {
    return (biasLeft ? selectionEndLine : selectionStartLine);
  }

  /**
   * Sets the caret position. The new selection will consist of the
   * caret position only (hence no text will be selected)
   * @param caret The caret position
   * @see #select(int,int)
   */
  public final void setCaretPosition(int caret)
  {
    select(caret,caret);
  }

  /**
   * Selects all text in the document.
   */
  public final void selectAll()
  {
    select(0,getDocumentLength());
  }

  /**
   * Moves the mark to the caret position.
   */
  public final void selectNone()
  {
    select(getCaretPosition(),getCaretPosition());
  }

  /**
   * Selects from the start offset to the end offset. This is the
   * general selection method used by all other selecting methods.
   * The caret position will be start if start &lt; end, and end
   * if end &gt; start.
   * @param start The start offset
   * @param end The end offset
   */
  public void select(int start, int end)
  {
    int newStart, newEnd;
    boolean newBias;
    if(start <= end)
    {
      newStart = start;
      newEnd = end;
      newBias = false;
    }
    else
    {
      newStart = end;
      newEnd = start;
      newBias = true;
    }

    if((newStart < 0 || newEnd > getDocumentLength()) && start != end)
    {
      throw new IllegalArgumentException("Bounds out of"
          + " range: " + newStart + "," +
          newEnd + " [" + getDocumentLength() + "]");
    }

    // If the new position is the same as the old, we don't
    // do all this crap, however we still do the stuff at
    // the end (clearing magic position, scrolling)
    if(newStart != selectionStart || newEnd != selectionEnd
        || newBias != biasLeft)
    {
      int newStartLine = getLineOfOffset(newStart);
      int newEndLine = getLineOfOffset(newEnd);

      if(painter.isBracketHighlightEnabled())
      {
        if(bracketLine != -1)
          painter.invalidateLine(bracketLine);
        updateBracketHighlight(end);
        if(bracketLine != -1)
          painter.invalidateLine(bracketLine);
      }

      painter.invalidateLineRange(selectionStartLine,selectionEndLine);
      painter.invalidateLineRange(newStartLine,newEndLine);

      document.addUndoableEdit(new CaretUndo(selectionStart,selectionEnd));

      selectionStart = newStart;
      selectionEnd = newEnd;
      selectionStartLine = newStartLine;
      selectionEndLine = newEndLine;
      biasLeft = newBias;

      fireCaretEvent();
    }

    // When the user is typing, etc, we don't want the caret to blink
    blink = true;
    if (!DISABLE_CARET) {
      caretTimer.restart();
    }

    // Clear the `magic' caret position used by up/down
    magicCaret = -1;

    scrollToCaret();

//    // notify the line number feller
//    if (editorLineStatus != null) {
//      editorLineStatus.set(selectionStartLine, selectionEndLine);
//      //System.out.println("why " + selectionStartLine + " " + selectionEndLine);
//      //System.out.println(getLineOfOffset(start) + " " +
//      //                 getLineOfOffset(end));
//    }
  }

  private enum CharacterKinds {
      Word,
      Whitespace,
      Other
  }

  private CharacterKinds CharacterKind( char ch, String noWordSep )
  {
    if ( Character.isLetterOrDigit(ch) || ch=='_' || noWordSep.indexOf(ch) != -1 )
      return CharacterKinds.Word;
    else if ( Character.isWhitespace(ch) )
      return CharacterKinds.Whitespace;
    else
      return CharacterKinds.Other;
  }

  protected void setNewSelectionWord( int line, int offset )
  {
    if (getLineLength(line) == 0) {
      newSelectionStart = getLineStartOffset(line);
      newSelectionEnd = newSelectionStart;
      return;
    }

    String noWordSep = (String)document.getProperty("noWordSep");
    if(noWordSep == null)
      noWordSep = "";

    String lineText = getLineText(line);

    int wordStart = 0;
    int wordEnd = lineText.length();

    int charPos = PApplet.constrain(offset - 1, 0, lineText.length() - 1);
    char ch = lineText.charAt(charPos);

    CharacterKinds thisWord = CharacterKind(ch,noWordSep);

    for(int i = offset - 1; i >= 0; i--) {
      ch = lineText.charAt(i);
      if(CharacterKind(ch,noWordSep) != thisWord) {
        wordStart = i + 1;
        break;
      }
    }

    for(int i = offset; i < lineText.length(); i++) {
      ch = lineText.charAt(i);
      if(CharacterKind(ch,noWordSep) != thisWord) {
        wordEnd = i;
        break;
      }
    }
    int lineStart = getLineStartOffset(line);

    newSelectionStart = lineStart + wordStart;
    newSelectionEnd = lineStart + wordEnd;
  }


  /**
   * Returns the selected text, or null if no selection is active.
   */
  public final String getSelectedText()
  {
    if (selectionStart == selectionEnd) {
      return null;
    } else {
      return getText(selectionStart, selectionEnd - selectionStart);
    }
  }

  /**
   * Replaces the selection with the specified text.
   * @param selectedText The replacement text for the selection
   */
  public void setSelectedText(String selectedText) {
    setSelectedText(selectedText, false);
  }


  /**
   * Replaces the selection with the specified text.
   * @param selectedText The replacement text for the selection
   * @param recordCompoundEdit Whether the replacement should be
   * recorded as a compound edit
   */
  public void setSelectedText(String selectedText, boolean recordCompoundEdit) {
    if (!editable) {
      throw new InternalError("Text component read only");
    }

    if (recordCompoundEdit) {
      document.beginCompoundEdit();
    }

    try {
      document.remove(selectionStart, selectionEnd - selectionStart);
      if (selectedText != null) {
        document.insertString(selectionStart, selectedText,null);
      }
    } catch (BadLocationException bl) {
      bl.printStackTrace();
      throw new InternalError("Cannot replace selection");

    } finally {
      // No matter what happens... stops us from leaving document in a bad state
      // (provided this has to be recorded as a compound edit, of course...)
      if (recordCompoundEdit) {
        document.endCompoundEdit();
      }
    }
    setCaretPosition(selectionEnd);
  }


  /**
   * Returns true if this text area is editable, false otherwise.
   */
  public final boolean isEditable() {
    return editable;
  }


  /**
   * Sets if this component is editable.
   * @param editable True if this text area should be editable,
   * false otherwise
   */
  public final void setEditable(boolean editable) {
    this.editable = editable;
  }


  /**
   * Returns the right click popup menu.
   */
  public final JPopupMenu getRightClickPopup() {
    return popup;
  }


  /**
   * Sets the right click popup menu.
   * @param popup The popup
   */
  public final void setRightClickPopup(JPopupMenu popup) {
    this.popup = popup;
  }


  /**
   * Returns the 'magic' caret position. This can be used to preserve
   * the column position when moving up and down lines.
   */
  public final int getMagicCaretPosition() {
    return magicCaret;
  }


  /**
   * Sets the 'magic' caret position. This can be used to preserve
   * the column position when moving up and down lines.
   * @param magicCaret The magic caret position
   */
  public final void setMagicCaretPosition(int magicCaret) {
    this.magicCaret = magicCaret;
  }


  /**
   * Similar to <code>setSelectedText()</code>, but overstrikes the
   * appropriate number of characters if overwrite mode is enabled.
   * @param str The string
   * @see #setSelectedText(String)
   * @see #isOverwriteEnabled()
   */
  public void overwriteSetSelectedText(String str)
  {
    // Don't overstrike if there is a selection
    if(!overwrite || selectionStart != selectionEnd)
    {
      // record the whole operation as a compound edit if
      // selected text is being replaced
      boolean isSelectAndReplaceOp = (selectionStart != selectionEnd);
      setSelectedText(str, isSelectAndReplaceOp);
      return;
    }

    // Don't overstrike if we're on the end of
    // the line
    int caret = getCaretPosition();
    int caretLineEnd = getLineStopOffset(getCaretLine());
    if(caretLineEnd - caret <= str.length())
    {
      setSelectedText(str, false);
      return;
    }

    try
    {
      document.remove(caret,str.length());
      document.insertString(caret,str,null);
    }
    catch(BadLocationException bl)
    {
      bl.printStackTrace();
    }
  }

  /**
   * Returns true if overwrite mode is enabled, false otherwise.
   */
  public final boolean isOverwriteEnabled()
  {
    return overwrite;
  }

  /**
   * Sets if overwrite mode should be enabled.
   * @param overwrite True if overwrite mode should be enabled,
   * false otherwise.
   */
  public final void setOverwriteEnabled(boolean overwrite)
  {
    this.overwrite = overwrite;
    painter.invalidateSelectedLines();
  }


  /**
   * Returns the position of the highlighted bracket (the bracket
   * matching the one before the caret)
   */
  public final int getBracketPosition()
  {
    return bracketPosition;
  }

  /**
   * Returns the line of the highlighted bracket (the bracket
   * matching the one before the caret)
   */
  public final int getBracketLine()
  {
    return bracketLine;
  }

  /**
   * Adds a caret change listener to this text area.
   * @param listener The listener
   */
  public final void addCaretListener(CaretListener listener)
  {
    eventListenerList.add(CaretListener.class,listener);
  }

  /**
   * Removes a caret change listener from this text area.
   * @param listener The listener
   */
  public final void removeCaretListener(CaretListener listener)
  {
    eventListenerList.remove(CaretListener.class,listener);
  }


  /**
   * Deletes the selected text from the text area and places it
   * into the clipboard.
   */
  public void cut() {
    if (editable) {
      copy();
      setSelectedText("");
    }
  }


  /**
   * Places the selected text into the clipboard.
   */
  public void copy() {
    if (selectionStart != selectionEnd) {
      Clipboard clipboard = getToolkit().getSystemClipboard();

      String selection = getSelectedText();
      if (selection != null) {
        int repeatCount = inputHandler.getRepeatCount();
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < repeatCount; i++)
          sb.append(selection);

        clipboard.setContents(new StringSelection(sb.toString()), null);
      }
    }
  }


  /**
   * Copy the current selection as HTML, formerly "Format for Discourse".
   * <p/>
   * Original code by <A HREF="http://usuarios.iponet.es/imoreta">owd</A>.
   * <p/>
   * Revised and updated for revision 0108 by Ben Fry (10 March 2006).
   * <p/>
   * Updated for 0122 to simply copy the code directly to the clipboard,
   * rather than opening a new window.
   * <p/>
   * Updated for 0144 to only format the selected lines.
   * <p/>
   * Updated for 0185 to incorporate the HTML changes from the Arduino project,
   * and set the formatter to always use HTML (disabling, but not removing the
   * YaBB version of the code) and also fixing it for the Tools API.
   * <p/>
   * Updated for 0190 to simply be part of JEditTextArea, removed YaBB code.
   * Simplest and most sensible to have it live here, since it's no longer
   * specific to any language or version of the PDE.
   */
  public void copyAsHTML() {
    HtmlSelection formatted = new HtmlSelection("<html><body><pre>\n"
        + getTextAsHtml(null) + "\n</pre></body></html>");

    Clipboard clipboard = processing.app.ui.Toolkit.getSystemClipboard();
    clipboard.setContents(formatted, new ClipboardOwner() {
      public void lostOwnership(Clipboard clipboard, Transferable contents) {
        // I don't care about ownership
      }
    });
  }


  /**
   * Guts of copyAsHTML, minus the pre, body, and html blocks surrounding.
   * @param doc If null, read only the selection if any, and use the active
   *            document. Otherwise, the whole of doc is used.
   */
  public String getTextAsHtml(SyntaxDocument doc) {
    StringBuilder cf = new StringBuilder();

    int selStart = getSelectionStart();
    int selStop = getSelectionStop();

    int startLine = getSelectionStartLine();
    int stopLine = getSelectionStopLine();

    if (doc != null) {
      startLine = 0;
      stopLine = doc.getDefaultRootElement().getElementCount() - 1;
    }
    // If no selection, convert all the lines
    else if (selStart == selStop) {
      startLine = 0;
      stopLine = getLineCount() - 1;
    } else {
      // Make sure the selection doesn't end at the beginning of the last line
      if (getLineStartOffset(stopLine) == selStop) {
        stopLine--;
      }
    }
    if (doc == null) {
      doc = getDocument();
    }

    // Read the code line by line
    for (int i = startLine; i <= stopLine; i++) {
      emitAsHTML(cf, i, doc);
    }

    return cf.toString();
  }


  private void emitAsHTML(StringBuilder cf, int line, SyntaxDocument doc) {
    // Almost static; only needs the painter for a color scheme.
    Segment segment = new Segment();
    try {
      Element element = doc.getDefaultRootElement().getElement(line);
      int start = element.getStartOffset();
      int stop  = element.getEndOffset();
      doc.getText(start, stop - start - 1, segment);
    } catch (BadLocationException e) { return; }

    char[] segmentArray = segment.array;
    int limit = segment.getEndIndex();
    int segmentOffset = segment.offset;
    int segmentCount = segment.count;

    TokenMarkerState tokenMarker = doc.getTokenMarker();
    // If syntax coloring is disabled, do simple translation
    if (tokenMarker == null) {
      for (int j = 0; j < segmentCount; j++) {
        char c = segmentArray[j + segmentOffset];
        appendAsHTML(cf, c);
      }
    } else {
      // If syntax coloring is enabled, we have to do this
      // because tokens can vary in width
      Token tokens = tokenMarker.markTokens(segment, line);

      int offset = 0;
      SyntaxStyle[] styles = painter.getStyles();

      for (;;) {
        byte id = tokens.id;
        if (id == Token.END) {
          if (segmentOffset + offset < limit) {
            appendAsHTML(cf, segmentArray[segmentOffset + offset]);
          } else {
            cf.append('\n');
          }
          return; // cf.toString();
        }
        if (id != Token.NULL) {
          cf.append("<span style=\"color: #");
          cf.append(PApplet.hex(styles[id].getColor().getRGB() & 0xFFFFFF, 6));
          cf.append(";\">");

          if (styles[id].isBold())
            cf.append("<b>");
        }
        int length = tokens.length;

        for (int j = 0; j < length; j++) {
          char c = segmentArray[segmentOffset + offset + j];
          if (offset == 0 && c == ' ') {
            // Force spaces at the beginning of the line
            cf.append("&nbsp;");
          } else {
            appendAsHTML(cf, c);
          }
          // Place close tags [/]
          if (j == (length - 1) && id != Token.NULL && styles[id].isBold())
            cf.append("</b>");
          if (j == (length - 1) && id != Token.NULL)
            cf.append("</span>");
        }
        offset += length;
        tokens = tokens.next;
      }
    }
  }


  /**
   * Handle encoding HTML entities for lt, gt, and anything non-ASCII.
   */
  private void appendAsHTML(StringBuilder buffer, char c) {
    if (c == '<') {
      buffer.append("&lt;");
    } else if (c == '>') {
      buffer.append("&gt;");
    } else if (c == '&') {
      buffer.append("&amp;");
    } else if (c == '\'') {
      buffer.append("&apos;");
    } else if (c == '"') {
      buffer.append("&quot;");
    } else if (c > 127) {
      buffer.append("&#" + ((int) c) + ";");  // use unicode entity
    } else {
      buffer.append(c);  // normal character
    }
  }


  /**
   * Inserts the clipboard contents into the text.
   */
  public void paste() {
//    System.out.println("focus owner is: " + isFocusOwner());
    if (editable) {
      Clipboard clipboard = getToolkit().getSystemClipboard();
      try {
        String selection =
          ((String) clipboard.getContents(this).getTransferData(DataFlavor.stringFlavor));

        if (selection.contains("\r\n")) {
          selection = selection.replaceAll("\r\n", "\n");

        } else if (selection.contains("\r")) {
          // The Mac OS MRJ doesn't convert \r to \n, so do it here
          selection = selection.replace('\r','\n');
        }

        // Remove tabs and replace with spaces
        // http://code.google.com/p/processing/issues/detail?id=69
        if (selection.contains("\t")) {
          int tabSize = Preferences.getInteger("editor.tabs.size");
          char[] c = new char[tabSize];
          Arrays.fill(c, ' ');
          String tabString = new String(c);
          selection = selection.replaceAll("\t", tabString);
        }

        // Replace unicode x00A0 (non-breaking space) with just a plain space.
        // Seen often on Mac OS X when pasting from Safari. [fry 030929]
        selection = selection.replace('\u00A0', ' ');

        // Remove ASCII NUL characters. Reported when pasting from
        // Acrobat Reader and PDF documents. [fry 130719]
        // https://github.com/processing/processing/issues/1973
        if (selection.indexOf('\0') != -1) {
          //System.out.println("found NUL charaacters");
          //int before = selection.length();
          selection = selection.replaceAll("\0", "");
          //int after = selection.length();
          //System.out.println(before + " " + after);
        }

        int repeatCount = inputHandler.getRepeatCount();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < repeatCount; i++) {
          sb.append(selection);
        }
        selection = sb.toString();
        setSelectedText(selection);

      } catch (Exception e) {
        getToolkit().beep();
        System.err.println("Clipboard does not contain a string");
        DataFlavor[] flavors = clipboard.getAvailableDataFlavors();
        for (DataFlavor f : flavors) {
          try {
            Object o = clipboard.getContents(this).getTransferData(f);
            System.out.println(f + " = " + o);
          } catch (Exception ex) {
            ex.printStackTrace();
          }
        }

      }
    }
  }

  /**
   * Called by the AWT when this component is removed from it's parent.
   * This stops clears the currently focused component.
   */
  public void removeNotify() {
    super.removeNotify();
//    if(focusedComponent == this)
//      focusedComponent = null;
    if (!DISABLE_CARET) {
      caretTimer.stop();
    }
  }

//  /**
//   * The component that tracks the current line number.
//   */
//  public EditorLineStatus editorLineStatus;


  /*
  public void processKeyEvent(KeyEvent evt) {
    // this had to be added in Processing 007X, because the menu key
    // events weren't making it up to the frame.
    super.processKeyEvent(evt);

    //System.out.println("jedittextarea: " + evt);
    //System.out.println();
    if (inputHandler == null) return;

    switch(evt.getID()) {
    case KeyEvent.KEY_TYPED:
      if ((editorListener == null) || !editorListener.keyTyped(evt)) {
        inputHandler.keyTyped(evt);
      }
      break;
    case KeyEvent.KEY_PRESSED:
      if ((editorListener == null) || !editorListener.keyPressed(evt)) {
        inputHandler.keyPressed(evt);
      }
      break;
    case KeyEvent.KEY_RELEASED:
      inputHandler.keyReleased(evt);
      break;
    }
  }
   */


  public void processKeyEvent(KeyEvent event) {
    // this had to be added in Processing 007X, because the menu key
    // events weren't making it up to the frame.
    super.processKeyEvent(event);

    if (inputHandler != null) {
      switch (event.getID()) {
      case KeyEvent.KEY_TYPED:
        inputHandler.keyTyped(event);
        break;
      case KeyEvent.KEY_PRESSED:
        inputHandler.keyPressed(event);
        break;
      case KeyEvent.KEY_RELEASED:
        inputHandler.keyReleased(event);
        break;
      }
    }
  }


  // protected members
  protected static String CENTER = "center";
  protected static String RIGHT = "right";
  protected static String BOTTOM = "bottom";

  protected Timer caretTimer;
  static private final boolean DISABLE_CARET = false;

  protected TextAreaPainter painter;

  protected JPopupMenu popup;

  protected EventListenerList eventListenerList;
  protected MutableCaretEvent caretEvent;

  protected boolean caretBlinks;
  protected boolean caretVisible;
  protected boolean blink;

  protected boolean editable = true;

  protected int firstLine;
  protected int visibleLines;
  protected int electricScroll;

  protected int horizontalOffset;

  protected JScrollBar vertical;
  protected JScrollBar horizontal;
  protected boolean scrollBarsInitialized;

  protected InputHandler inputHandler;
  protected SyntaxDocument document;
  protected DocumentHandler documentHandler;

  protected Segment lineSegment;

  protected int selectionStart;
  protected int selectionStartLine;
  protected int selectionEnd;
  protected int selectionEndLine;
  protected boolean biasLeft;

  protected int newSelectionStart; // hack to get around lack of multiple returns in Java
  protected int newSelectionEnd;

  protected boolean selectWord;
  protected boolean selectLine;
  protected int selectionAncorStart;
  protected int selectionAncorEnd;

  protected int bracketPosition;
  protected int bracketLine;

  protected int magicCaret;
  protected boolean overwrite;


  protected void fireCaretEvent()
  {
    Object[] listeners = eventListenerList.getListenerList();
    for(int i = listeners.length - 2; i >= 0; i--)
    {
      if(listeners[i] == CaretListener.class)
      {
        ((CaretListener)listeners[i+1]).caretUpdate(caretEvent);
      }
    }
  }

  protected void updateBracketHighlight(int newCaretPosition)
  {
    if(newCaretPosition == 0)
    {
      bracketPosition = bracketLine = -1;
      return;
    }

    try
    {
      int offset = bracketHelper.findMatchingBracket(document.getText(0,
          document.getLength()), newCaretPosition - 1);
      if(offset != -1)
      {
        bracketLine = getLineOfOffset(offset);
        bracketPosition = offset - getLineStartOffset(bracketLine);
        return;
      }
    }
    catch(BadLocationException bl)
    {
      bl.printStackTrace();
    }

    bracketLine = bracketPosition = -1;
  }

  protected void documentChanged(DocumentEvent evt)
  {
    bracketHelper.invalidate();

    DocumentEvent.ElementChange ch =
      evt.getChange(document.getDefaultRootElement());

    int count;
    if(ch == null)
      count = 0;
    else
      count = ch.getChildrenAdded().length -
      ch.getChildrenRemoved().length;

    int line = getLineOfOffset(evt.getOffset());
    if(count == 0)
    {
      painter.invalidateLine(line);
    }
    // do magic stuff
    else if(line < firstLine)
    {
      setFirstLine(line);
    }
    // end of magic stuff
    else
    {
      painter.invalidateLineRange(line,firstLine + visibleLines);
      updateScrollBars();
    }
  }

  class ScrollLayout implements LayoutManager
  {
    //final int LEFT_EXTRA = 5;

    public void addLayoutComponent(String name, Component comp)
    {
      if(name.equals(CENTER))
        center = comp;
      else if(name.equals(RIGHT))
        right = comp;
      else if(name.equals(BOTTOM))
        bottom = comp;
      else if(name.equals(LEFT_OF_SCROLLBAR))
        leftOfScrollBar.addElement(comp);
    }

    public void removeLayoutComponent(Component comp)
    {
      if(center == comp)
        center = null;
      if(right == comp)
        right = null;
      if(bottom == comp)
        bottom = null;
      else
        leftOfScrollBar.removeElement(comp);
    }

    public Dimension preferredLayoutSize(Container parent)
    {
      Dimension dim = new Dimension();
      Insets insets = getInsets();
      dim.width = insets.left + insets.right;
      dim.height = insets.top + insets.bottom;

      Dimension centerPref = center.getPreferredSize();
      dim.width += centerPref.width;
      dim.height += centerPref.height;
      Dimension rightPref = right.getPreferredSize();
      dim.width += rightPref.width;
      Dimension bottomPref = bottom.getPreferredSize();
      dim.height += bottomPref.height;

      return dim;
    }

    public Dimension minimumLayoutSize(Container parent)
    {
      Dimension dim = new Dimension();
      Insets insets = getInsets();
      dim.width = insets.left + insets.right;
      dim.height = insets.top + insets.bottom;

      Dimension centerPref = center.getMinimumSize();
      dim.width += centerPref.width;
      dim.height += centerPref.height;
      Dimension rightPref = right.getMinimumSize();
      dim.width += rightPref.width;
      Dimension bottomPref = bottom.getMinimumSize();
      dim.height += bottomPref.height;

      dim.height += 5;

      return dim;
    }

    public void layoutContainer(Container parent)
    {
      Dimension size = parent.getSize();
      Insets insets = parent.getInsets();
      int itop = insets.top;
      int ileft = insets.left;
      int ibottom = insets.bottom;
      int iright = insets.right;

      int rightWidth = right.getPreferredSize().width;
      int bottomHeight = bottom.getPreferredSize().height;
      int centerWidth = size.width - rightWidth - ileft - iright;
      int centerHeight = size.height - bottomHeight - itop - ibottom;

      center.setBounds(ileft, // + LEFT_EXTRA,
          itop,
          centerWidth, // - LEFT_EXTRA,
          centerHeight);

      right.setBounds(ileft + centerWidth,
          itop,
          rightWidth,
          centerHeight);

      // Lay out all status components, in order
      Enumeration status = leftOfScrollBar.elements();
      while (status.hasMoreElements()) {
        Component comp = (Component)status.nextElement();
        Dimension dim = comp.getPreferredSize();
        comp.setBounds(ileft,
            itop + centerHeight,
            dim.width,
            bottomHeight);
        ileft += dim.width;
      }

      bottom.setBounds(ileft,
          itop + centerHeight,
          size.width - rightWidth - ileft - iright,
          bottomHeight);
    }

    // private members
    private Component center;
    private Component right;
    private Component bottom;
    private Vector leftOfScrollBar = new Vector();
  }

//  static class CaretBlinker implements ActionListener
//  {
//    public void actionPerformed(ActionEvent evt)
//    {
//      if(focusedComponent != null
//          && focusedComponent.hasFocus())
//        focusedComponent.blinkCaret();
//    }
//  }

  class MutableCaretEvent extends CaretEvent
  {
    MutableCaretEvent()
    {
      super(JEditTextArea.this);
    }

    public int getDot()
    {
      return getCaretPosition();
    }

    public int getMark()
    {
      return getMarkPosition();
    }
  }

  class AdjustHandler implements AdjustmentListener
  {
    public void adjustmentValueChanged(final AdjustmentEvent evt)
    {
      if(!scrollBarsInitialized)
        return;

      // If this is not done, mousePressed events accumulate
      // and the result is that scrolling doesn't stop after
      // the mouse is released
      SwingUtilities.invokeLater(new Runnable() {
        public void run()
        {
          if (evt.getAdjustable() == vertical) {
            setFirstLine(vertical.getValue());
          } else {
            setHorizontalOffset(-horizontal.getValue());
          }
        }
      });
    }
  }

  class ComponentHandler extends ComponentAdapter
  {
    public void componentResized(ComponentEvent evt)
    {
      recalculateVisibleLines();
      scrollBarsInitialized = true;
    }
  }

  class DocumentHandler implements DocumentListener
  {
    public void insertUpdate(DocumentEvent evt)
    {
      documentChanged(evt);

      int offset = evt.getOffset();
      int length = evt.getLength();

      int newStart;
      int newEnd;

      if (selectionStart > offset ||
          (selectionStart == selectionEnd && selectionStart == offset))
        newStart = selectionStart + length;
      else
        newStart = selectionStart;

      if(selectionEnd >= offset)
        newEnd = selectionEnd + length;
      else
        newEnd = selectionEnd;

      select(newStart,newEnd);
    }

    public void removeUpdate(DocumentEvent evt)
    {
      documentChanged(evt);

      int offset = evt.getOffset();
      int length = evt.getLength();

      int newStart;
      int newEnd;

      if(selectionStart > offset)
      {
        if(selectionStart > offset + length)
          newStart = selectionStart - length;
        else
          newStart = offset;
      }
      else
        newStart = selectionStart;

      if(selectionEnd > offset)
      {
        if(selectionEnd > offset + length)
          newEnd = selectionEnd - length;
        else
          newEnd = offset;
      }
      else
        newEnd = selectionEnd;

      select(newStart,newEnd);
    }

    public void changedUpdate(DocumentEvent evt)
    {
    }
  }


  class DragHandler implements MouseMotionListener
  {
    public void mouseDragged(MouseEvent evt) {
      if (popup != null && popup.isVisible()) return;

      if (!selectWord && !selectLine) {
        try {
          select(getMarkPosition(), xyToOffset(evt.getX(), evt.getY()));
        } catch (ArrayIndexOutOfBoundsException e) {
          Messages.loge("xToOffset problem", e);
        }
      } else {
        int line = yToLine(evt.getY());
        if ( selectWord ) {
          setNewSelectionWord( line, xToOffset(line,evt.getX()) );
        } else {
          newSelectionStart = getLineStartOffset(line);
          newSelectionEnd = getLineSelectionStopOffset(line);
        }
        if ( newSelectionStart < selectionAncorStart ) {
          select(newSelectionStart,selectionAncorEnd);
        } else if ( newSelectionEnd > selectionAncorEnd ) {
          select(selectionAncorStart,newSelectionEnd);
        } else {
          select(newSelectionStart,newSelectionEnd);
        }
      }
    }

    public void mouseMoved(MouseEvent evt) {}
  }


  class FocusHandler implements FocusListener {

    public void focusGained(FocusEvent evt) {
      setCaretVisible(true);
    }

    public void focusLost(FocusEvent evt) {
      setCaretVisible(false);
    }
  }


  class MouseHandler extends MouseAdapter {

    public void mousePressed(MouseEvent event) {
//      try {
//      requestFocus();
//      // Focus events not fired sometimes?
//      setCaretVisible(true);
//      focusedComponent = JEditTextArea.this;
        // Here be dragons: for release 0195, this fixes a problem where the
        // line segment data from the previous window was being used for
        // selections, causing an exception when the window you're clicking to
        // was not full of text. Simply ignoring clicks when not focused fixes
        // the problem, though it's not clear why the wrong Document data was
        // being using regardless of the focusedComponent.
//        if (focusedComponent != JEditTextArea.this) return;
      if (!hasFocus()) {
//          System.out.println("requesting focus in window");
        // The following condition check fixes #3649 [manindra, 08/20/15]
        if(!requestFocusInWindow()) {
          return;
        }
      }

      // isPopupTrigger() is handled differently across platforms,
      // so it may fire during release, or during the press.
      // http://docs.oracle.com/javase/7/docs/api/java/awt/event/MouseEvent.html#isPopupTrigger()
      // However, we have to exit out of this method if it's a right-click
      // anyway, because otherwise it'll de-select the current word.
      // As a result, better to just check for BUTTON3 now, indicating that
      // isPopupTrigger() is going to fire on the release anyway.
      boolean windowsRightClick =
        Platform.isWindows() && (event.getButton() == MouseEvent.BUTTON3);
      if ((event.isPopupTrigger() || windowsRightClick) && (popup != null)) {
//      // Windows fires the popup trigger on release (see mouseReleased() below)(
//      if (!Base.isWindows()) {
//        if (event.isPopupTrigger() && (popup != null)) {

        // If user right-clicked inside the selection, preserve it;
        // move caret to click offset otherwise
        int offset = xyToOffset(event.getX(), event.getY());
        int selectionStart = getSelectionStart();
        int selectionStop = getSelectionStop();
        if (offset < selectionStart || offset >= selectionStop) {
          select(offset, offset);
        }

        popup.show(painter, event.getX(), event.getY());
        return;
//        }
      }

      int line = yToLine(event.getY());
      int offset = xToOffset(line, event.getX());
      int dot = getLineStartOffset(line) + offset;

      selectLine = false;
      selectWord = false;

      switch (event.getClickCount()) {

      case 1:
        doSingleClick(event,line,offset,dot);
        break;

      case 2:
        // It uses the bracket matching stuff, so it can throw a BLE
        try {
          doDoubleClick(event, line, offset, dot);
        } catch (BadLocationException bl) {
          bl.printStackTrace();
        }
        break;

      case 3:
        doTripleClick(event,line,offset,dot);
        break;
      }
//      } catch (ArrayIndexOutOfBoundsException aioobe) {
//        aioobe.printStackTrace();
//        int line = yToLine(evt.getY());
//        System.out.println("line is " + line + ", line count is " + getLineCount());
//      }
    }


    /*
    // Because isPopupTrigger() is handled differently across platforms,
    // it may fire during release, or during the press.
    // http://docs.oracle.com/javase/7/docs/api/java/awt/event/MouseEvent.html#isPopupTrigger()
    public void mouseReleased(MouseEvent event) {
      if (event.isPopupTrigger() && (popup != null)) {
        popup.show(painter, event.getX(), event.getY());
      }
    }
    */


    private void doSingleClick(MouseEvent evt, int line, int offset, int dot) {
      if ((evt.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
        select(getMarkPosition(),dot);
      } else {
        setCaretPosition(dot);
      }
    }


    private void doDoubleClick(MouseEvent evt, int line, int offset,
                               int dot) throws BadLocationException {
      // Ignore empty lines
      if (getLineLength(line) != 0) {
        try {
          String text = document.getText(0, document.getLength());
          int bracket = bracketHelper.findMatchingBracket(text, Math.max(0, dot - 1));
          if (bracket != -1) {
            int mark = getMarkPosition();
            // Hack
            if (bracket > mark) {
              bracket++;
              mark--;
            }
            select(mark,bracket);
            return;
          }
        } catch(BadLocationException bl) {
          bl.printStackTrace();
        }

        setNewSelectionWord( line, offset );
        select(newSelectionStart,newSelectionEnd);
        selectWord = true;
        selectionAncorStart = selectionStart;
        selectionAncorEnd = selectionEnd;

      /*
        String lineText = getLineText(line);
        String noWordSep = (String)document.getProperty("noWordSep");
        int wordStart = TextUtilities.findWordStart(lineText,offset,noWordSep);
        int wordEnd = TextUtilities.findWordEnd(lineText,offset,noWordSep);

        int lineStart = getLineStartOffset(line);
        select(lineStart + wordStart,lineStart + wordEnd);
       */
      }
    }


    private void doTripleClick(MouseEvent evt, int line, int offset, int dot) {
      selectLine = true;
      select(getLineStartOffset(line),getLineSelectionStopOffset(line));
      selectionAncorStart = selectionStart;
      selectionAncorEnd = selectionEnd;
    }
  }


  class CaretUndo extends AbstractUndoableEdit {
    private int start;
    private int end;

    CaretUndo(int start, int end) {
      this.start = start;
      this.end = end;
    }

    public boolean isSignificant() {
      return false;
    }

    public String getPresentationName() {
      return "caret move";
    }

    public void undo() throws CannotUndoException {
      super.undo();
      select(start,end);
    }

    public void redo() throws CannotRedoException {
      super.redo();
      select(start,end);
    }

    public boolean addEdit(UndoableEdit edit) {
      if (edit instanceof CaretUndo) {
        CaretUndo cedit = (CaretUndo)edit;
        start = cedit.start;
        end = cedit.end;
        cedit.die();
        return true;
      }
      return false;
    }
  }
}
