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
import static processing.mode.experimental.ExperimentalMode.log;
import static processing.mode.experimental.ExperimentalMode.log2;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

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

  protected ErrorCheckerService errorCheckerService;

  public TextArea(TextAreaDefaults defaults, DebugEditor editor) {
    super(defaults);
    this.editor = editor;

    // replace the painter:
    // first save listeners, these are package-private in JEditTextArea, so not accessible
    ComponentListener[] componentListeners = painter.getComponentListeners();
    mouseListeners = painter.getMouseListeners();
    MouseMotionListener[] mouseMotionListeners = painter
        .getMouseMotionListeners();

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
    //addCompletionPopupListner();
    add(CENTER, painter);

    // load settings from theme.txt
    ExperimentalMode theme = (ExperimentalMode) editor.getMode();
    gutterBgColor = theme.getThemeColor("gutter.bgcolor", gutterBgColor);
    gutterLineColor = theme.getThemeColor("gutter.linecolor", gutterLineColor);
    gutterPadding = theme.getInteger("gutter.padding");
    breakpointMarker = theme.loadThemeString("breakpoint.marker",
                                             breakpointMarker);
    currentLineMarker = theme.loadThemeString("currentline.marker",
                                              currentLineMarker);
  }

  /**
   * Sets ErrorCheckerService and loads theme for TextArea(XQMode)
   * 
   * @param ecs
   * @param mode
   */
  public void setECSandThemeforTextArea(ErrorCheckerService ecs,
                                        ExperimentalMode mode) {
    errorCheckerService = ecs;
    customPainter.setECSandTheme(ecs, mode);
  }

  public void processKeyEvent(KeyEvent evt) {
    
    if(evt.getKeyCode() == KeyEvent.VK_ESCAPE){
      if(suggestion != null){
        if(suggestion.isVisible()){
          log("esc key");
          hideSuggestion();
          evt.consume();
          return;
        }
      }
    }
    if(evt.getKeyCode() == KeyEvent.VK_ENTER){
      if (suggestion != null) {
        if (suggestion.isVisible()) {
          if (suggestion.insertSelection()) {
            hideSuggestion(); // Kill it!  
            evt.consume();
            return;
          }
        }
      }
    }
    
    
    if (evt.getID() == KeyEvent.KEY_PRESSED) {
      switch (evt.getKeyCode()) {
      case KeyEvent.VK_DOWN:
        if (suggestion != null)
          if (suggestion.isVisible()) {
            //log("KeyDown");
            suggestion.moveDown();
            return;
          }
        break;
      case KeyEvent.VK_UP:
        if (suggestion != null)
          if (suggestion.isVisible()) {
            //log("KeyUp");
            suggestion.moveUp();
            return;
          }
        break;
      case KeyEvent.VK_BACK_SPACE:
        log("BK Key");
        break;
      case KeyEvent.VK_SPACE:
        if (suggestion != null)
          if (suggestion.isVisible()) {
            log("Space bar, hide completion list");
            suggestion.hide();
          }
        break;
      default:
        break;
      }
    }
    super.processKeyEvent(evt);
      
    if (evt.getID() == KeyEvent.KEY_TYPED) {
      
      char keyChar = evt.getKeyChar();
      if (keyChar == KeyEvent.VK_ENTER || keyChar == KeyEvent.VK_ESCAPE) {
        return;
      } else if (keyChar == KeyEvent.VK_SPACE || keyChar == KeyEvent.VK_TAB
          || keyChar == KeyEvent.CHAR_UNDEFINED) {
        return;
      }
      if(evt.isAltDown() || evt.isControlDown() || evt.isMetaDown()){
        return;
      }
      final KeyEvent evt2 = evt;      
      SwingWorker worker = new SwingWorker() {
        protected Object doInBackground() throws Exception {
          errorCheckerService.runManualErrorCheck();
          // Provide completions only if it's enabled
          if(ExperimentalMode.codeCompletionsEnabled)
            log(" Typing: " + fetchPhrase(evt2) + " "
                + (evt2.getKeyChar() == KeyEvent.VK_ENTER));
          return null;
        }
      };
      worker.execute();
    }

    
  }
 
  
  private String fetchPhrase(MouseEvent evt) {
    log("--handle Mouse Right Click--");
    int off = xyToOffset(evt.getX(), evt.getY());
    if (off < 0)
      return null;
    int line = getLineOfOffset(off);
    if (line < 0)
      return null;
    String s = getLineText(line);
    if (s == null)
      return null;
    else if (s.length() == 0)
      return null;
    else {
      int x = xToOffset(line, evt.getX()), x2 = x + 1, x1 = x - 1;
      int xLS = off - getLineStartNonWhiteSpaceOffset(line);
      log("x=" + x);
      if (x < 0 || x >= s.length())
        return null;
      String word = s.charAt(x) + "";
      if (s.charAt(x) == ' ')
        return null;
      if (!(Character.isLetterOrDigit(s.charAt(x)) || s.charAt(x) == '_' || s
          .charAt(x) == '$'))
        return null;
      int i = 0;
      while (true) {
        i++;
        if (x1 >= 0 && x1 < s.length()) {
          if (Character.isLetter(s.charAt(x1)) || s.charAt(x1) == '_') {
            word = s.charAt(x1--) + word;
          } else
            x1 = -1;
        } else
          x1 = -1;

        if (x2 >= 0 && x2 < s.length()) {
          if (Character.isLetterOrDigit(s.charAt(x2)) || s.charAt(x2) == '_'
              || s.charAt(x2) == '$')
            word = word + s.charAt(x2++);
          else
            x2 = -1;
        } else
          x2 = -1;

        if (x1 < 0 && x2 < 0)
          break;
        if (i > 200) {
          // time out!
          break;
        }
      }
      if (Character.isDigit(word.charAt(0)))
        return null;
      log("Mouse click, word: " + word.trim());
      errorCheckerService.getASTGenerator().setLastClickedWord(line
          + errorCheckerService.mainClassOffset, word, xLS);
      return word.trim();
    }
  }
  private String fetchPhrase(KeyEvent evt) {
   
    int off = getCaretPosition();
    log2("off " + off);
    if (off < 0)
      return null;
    int line = getCaretLine();
    if (line < 0)
      return null;
    String s = getLineText(line);
    log2("lin " + line);
    /*
     * if (s == null) return null; else if (s.length() == 0) return null;
     */
//    else {
    //log2(s + " len " + s.length());

    int x = getCaretPosition() - getLineStartOffset(line) - 1, x2 = x + 1, x1 = x - 1;
    if(x >= s.length() || x < 0)
      return null; //TODO: Does this check cause problems? Verify.
    log2(" x char: " + s.charAt(x));
    //int xLS = off - getLineStartNonWhiteSpaceOffset(line);    

    String word = (x < s.length() ? s.charAt(x) : "") + "";
    if (s.trim().length() == 1) {
//      word = ""
//          + (keyChar == KeyEvent.CHAR_UNDEFINED ? s.charAt(x - 1) : keyChar);
      //word = (x < s.length()?s.charAt(x):"") + "";
      word = word.trim();
      if (word.endsWith("."))
        word = word.substring(0, word.length() - 1);
      
      errorCheckerService.getASTGenerator().preparePredictions(word, line
          + errorCheckerService.mainClassOffset,0);
      return word;
    }
//    if (keyChar == KeyEvent.VK_BACK_SPACE || keyChar == KeyEvent.VK_DELETE)
//      ; // accepted these keys
//    else if (!(Character.isLetterOrDigit(keyChar) || keyChar == '_' || keyChar == '$'))
//      return null;
    int i = 0;
    int closeB = 0;

    while (true) {
      i++;
      //TODO: currently works on single line only. "a. <new line> b()" won't be detected
      if (x1 >= 0) {
//        if (s.charAt(x1) != ';' && s.charAt(x1) != ',' && s.charAt(x1) != '(')
        if (Character.isLetterOrDigit(s.charAt(x1)) || s.charAt(x1) == '_'
            || s.charAt(x1) == '.' || s.charAt(x1) == ')' || s.charAt(x1) == ']') {

          if (s.charAt(x1) == ')') {
            word = s.charAt(x1--) + word;
            closeB++;
            while (x1 >= 0 && closeB > 0) {
              word = s.charAt(x1) + word;
              if (s.charAt(x1) == '(')
                closeB--;
              if (s.charAt(x1) == ')')
                closeB++;
              x1--;
            }
          }
          else if (s.charAt(x1) == ']') {
            word = s.charAt(x1--) + word;
            closeB++;
            while (x1 >= 0 && closeB > 0) {
              word = s.charAt(x1) + word;
              if (s.charAt(x1) == '[')
                closeB--;
              if (s.charAt(x1) == ']')
                closeB++;
              x1--;
            }
          }
          else {
            word = s.charAt(x1--) + word;
          }
        } else {
          break;
        }
      } else {
        break;
      }

//        if (x2 >= 0 && x2 < s.length()) {
//          if (Character.isLetterOrDigit(s.charAt(x2)) || s.charAt(x2) == '_'
//              || s.charAt(x2) == '$')
//            word = word + s.charAt(x2++);
//          else
//            x2 = -1;
//        } else
//          x2 = -1;

//        if (x1 < 0  )//&& x2 < 0
//          break;
      if (i > 200) {
        // time out!
        break;
      }
    }
//    if (keyChar != KeyEvent.CHAR_UNDEFINED)

    if (Character.isDigit(word.charAt(0)))
      return null;
    word = word.trim();
//    if (word.endsWith("."))
//      word = word.substring(0, word.length() - 1);
    int lineStartNonWSOffset = 0;
    if(word.length() > 1)
    errorCheckerService.getASTGenerator().preparePredictions(word, line
        + errorCheckerService.mainClassOffset,lineStartNonWSOffset);
    //showSuggestionLater();
    return word;

    //}
  }

  /**
   * Retrieve the total width of the gutter area.
   * 
   * @return gutter width in pixels
   */
  protected int getGutterWidth() {
    if(editor.debugToolbarEnabled == null || !editor.debugToolbarEnabled.get()){
      return 0;
    }
    FontMetrics fm = painter.getFontMetrics();
//        log("fm: " + (fm == null));
//        log("editor: " + (editor == null));
    //log("BPBPBPBPB: " + (editor.breakpointMarker == null));

    int textWidth = Math.max(fm.stringWidth(breakpointMarker),
                             fm.stringWidth(currentLineMarker));
    return textWidth + 2 * gutterPadding;
  }

  /**
   * Retrieve the width of margins applied to the left and right of the gutter
   * text.
   * 
   * @return margins in pixels
   */
  protected int getGutterMargins() {
    if(editor.debugToolbarEnabled == null || !editor.debugToolbarEnabled.get()){
      return 0;
    }
    return gutterPadding;
  }

  /**
   * Set the gutter text of a specific line.
   * 
   * @param lineIdx
   *          the line index (0-based)
   * @param text
   *          the text
   */
  public void setGutterText(int lineIdx, String text) {
    gutterText.put(lineIdx, text);
    painter.invalidateLine(lineIdx);
  }

  /**
   * Set the gutter text and color of a specific line.
   * 
   * @param lineIdx
   *          the line index (0-based)
   * @param text
   *          the text
   * @param textColor
   *          the text color
   */
  public void setGutterText(int lineIdx, String text, Color textColor) {
    gutterTextColors.put(lineIdx, textColor);
    setGutterText(lineIdx, text);
  }

  /**
   * Clear the gutter text of a specific line.
   * 
   * @param lineIdx
   *          the line index (0-based)
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
   * @param lineIdx
   *          the line index (0-based)
   * @return the gutter text
   */
  public String getGutterText(int lineIdx) {
    return gutterText.get(lineIdx);
  }

  /**
   * Retrieve the gutter text color for a specific line.
   * 
   * @param lineIdx
   *          the line index
   * @return the gutter text color
   */
  public Color getGutterTextColor(int lineIdx) {
    return gutterTextColors.get(lineIdx);
  }

  /**
   * Set the background color of a line.
   * 
   * @param lineIdx
   *          0-based line number
   * @param col
   *          the background color to set
   */
  public void setLineBgColor(int lineIdx, Color col) {
    lineColors.put(lineIdx, col);
    painter.invalidateLine(lineIdx);
  }

  /**
   * Clear the background color of a line.
   * 
   * @param lineIdx
   *          0-based line number
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
   * @param lineIdx
   *          0-based line number
   * @return the color or null if no color was set for the specified line
   */
  public Color getLineBgColor(int lineIdx) {
    return lineColors.get(lineIdx);
  }

  /**
   * Convert a character offset to a horizontal pixel position inside the text
   * area. Overridden to take gutter width into account.
   * 
   * @param line
   *          the 0-based line number
   * @param offset
   *          the character offset (0 is the first character on a line)
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
   * @param line
   *          the 0-based line number
   * @param x
   *          the horizontal pixel position
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
          int line = me.getY() / painter.getFontMetrics().getHeight()
              + firstLine;
          if (line >= 0 && line <= getLineCount() - 1) {
            editor.gutterDblClicked(line);
          }
        }
        return;
      }
      
      if (me.getButton() == MouseEvent.BUTTON3) {
        fetchPhrase(me);
      }

      // forward to standard listeners
      for (MouseListener ml : mouseListeners) {
        ml.mousePressed(me);
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

  private CompletionPanel suggestion;

  //JEditTextArea textarea;

  // worthless
  private void addCompletionPopupListner() {
    this.addKeyListener(new KeyListener() {

      @Override
      public void keyTyped(KeyEvent e) {

      }

      @Override
      public void keyReleased(KeyEvent e) {
        if (Character.isLetterOrDigit(e.getKeyChar())
            || e.getKeyChar() == KeyEvent.VK_BACK_SPACE
            || e.getKeyChar() == KeyEvent.VK_DELETE) {
//          SwingUtilities.invokeLater(new Runnable() {
//            @Override
//            public void run() {
//              showSuggestion();
//            }
//
//          });
        } else if (Character.isWhitespace(e.getKeyChar())
            || e.getKeyChar() == KeyEvent.VK_ESCAPE) {
          hideSuggestion();
        }
      }

      @Override
      public void keyPressed(KeyEvent e) {
      }
    });
  }

  public void showSuggestionLater(final DefaultListModel defListModel, final String word) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        showSuggestion(defListModel,word);
      }

    });
  }

  protected void showSuggestion(DefaultListModel defListModel,String subWord) {
    if (defListModel.size() == 0) {
      log("TextArea: No suggestions to show.");
      hideSuggestion();
      return;
    }
    int position = getCaretPosition();
    Point location = new Point();
    try {
      location.x = offsetToX(getCaretLine(), position
          - getLineStartOffset(getCaretLine()));
      location.y = lineToY(getCaretLine())
          + getPainter().getFontMetrics().getHeight();
    } catch (Exception e2) {
      e2.printStackTrace();
      return;
    }

    if (subWord.length() < 2) {
      return;
    }
    if (suggestion == null)
      suggestion = new CompletionPanel(this, position, subWord, defListModel,
                                       location,editor);
    else
      suggestion.updateList(defListModel, subWord, position);
    suggestion.setVisible(true);
//    requestFocusInWindow();
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        requestFocusInWindow();
      }
    });
  }

  private void hideSuggestion() {
    if (suggestion != null) {
      suggestion.hide();
      suggestion = null;
    }
  }

}
