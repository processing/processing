/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
Part of the Processing project - http://processing.org
Copyright (c) 2012-15 The Processing Foundation

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

package processing.mode.java.pdex;

import processing.mode.java.JavaInputHandler;
import processing.mode.java.JavaMode;
import processing.mode.java.JavaEditor;
import processing.mode.java.tweak.ColorControlBox;
import processing.mode.java.tweak.Handle;

import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.SwingWorker;

import processing.app.Base;
import processing.app.Editor;
import processing.app.Mode;
import processing.app.syntax.JEditTextArea;
import processing.app.syntax.PdeTextAreaDefaults;
import processing.app.syntax.TextAreaDefaults;


// TODO The way listeners are added/removed here is fragile and
//      likely to cause bugs that are very difficult to find.
//      We shouldn't be re-inventing the wheel with how listeners are handled.
// TODO We're overriding more things in JEditTextArea than we should, which
//      makes it trickier for other Modes (Python, etc) to subclass because
//      they'll need to re-implement what's in here, but first wade through it.
//      To fix, we need to clean this up and put the appropriate cross-Mode
//      changes into JEditTextArea (or a subclass in processing.app)

public class JavaTextArea extends JEditTextArea {
  protected PdeTextAreaDefaults defaults;
  protected JavaEditor editor;

//  static final int LEFT_GUTTER = Editor.LEFT_GUTTER;
//  static final int RIGHT_GUTTER = Editor.RIGHT_GUTTER;
//  static final int GUTTER_MARGIN = 3;

  // cached mouselisteners, these are wrapped by MouseHandler
  protected MouseListener[] mouseListeners;

  // contains line background colors
  protected Map<Integer, Color> lineColors = new HashMap<Integer, Color>();

  // [px] space added to the left and right of gutter chars
  protected int gutterPadding; // = 3;
  protected Color gutterBgColor; // = new Color(252, 252, 252); // gutter background color
  protected Color gutterLineColor; // = new Color(233, 233, 233); // color of vertical separation line

  /// the text marker for highlighting breakpoints in the gutter
  public String breakpointMarker = "<>";
  /// the text marker for highlighting the current line in the gutter
  public String currentLineMarker = "->";

  /// maps line index to gutter text
  protected Map<Integer, String> gutterText = new HashMap<Integer, String>();

  /// maps line index to gutter text color
  protected Map<Integer, Color> gutterTextColors = new HashMap<Integer, Color>();

//  protected ErrorCheckerService errorCheckerService;
  private CompletionPanel suggestion;


  protected JavaTextAreaPainter getCustomPainter() {
    return (JavaTextAreaPainter) painter;
  }


  public JavaTextArea(TextAreaDefaults defaults, JavaEditor editor) {
    super(defaults, new JavaInputHandler(editor));
    this.editor = editor;

    // removed all this since we have the createPainter() method and we
    // won't have to remove/re-add the custom painter object [fry 150122]
    // although there's also something bad happening here, that we're
    // re-forwarding all those events to all the other listeners?
    // that's making a hacky mess, plus the tweak code is also doing
    // something similar? [fry 150512]

//    // replace the painter:
//    // first save listeners, these are package-private in JEditTextArea, so not accessible
//    ComponentListener[] componentListeners = painter.getComponentListeners();
    mouseListeners = painter.getMouseListeners();
//    MouseMotionListener[] mouseMotionListeners = painter.getMouseMotionListeners();
//
//    remove(painter);
//    // set new painter
//    customPainter = new TextAreaPainter(this, defaults);
//    painter = customPainter;
//
//    // set listeners
//    for (ComponentListener cl : componentListeners) {
//      painter.addComponentListener(cl);
//    }
//
//    for (MouseMotionListener mml : mouseMotionListeners) {
//      painter.addMouseMotionListener(mml);
//    }

    // use a custom mouse handler instead of directly using mouseListeners
    MouseHandler mouseHandler = new MouseHandler();
    painter.addMouseListener(mouseHandler);
    painter.addMouseMotionListener(mouseHandler);
    //addCompletionPopupListner();
    add(CENTER, painter);

    // load settings from theme.txt
    Mode mode = editor.getMode();
    gutterBgColor = mode.getColor("gutter.bgcolor");  //, gutterBgColor);
    gutterLineColor = mode.getColor("gutter.linecolor"); //, gutterLineColor);
    gutterPadding = mode.getInteger("gutter.padding");
    breakpointMarker = mode.getString("breakpoint.marker");  //, breakpointMarker);
    currentLineMarker = mode.getString("currentline.marker"); //, currentLineMarker);

    // TweakMode code
    prevCompListeners = painter.getComponentListeners();
    prevMouseListeners = painter.getMouseListeners();
    prevMMotionListeners = painter.getMouseMotionListeners();
    prevKeyListeners = editor.getKeyListeners();

    interactiveMode = false;
    addPrevListeners();
  }


  protected JavaTextAreaPainter createPainter(final TextAreaDefaults defaults) {
    return new JavaTextAreaPainter(this, defaults);
  }


  /**
   * Sets ErrorCheckerService and loads theme for TextArea(XQMode)
   *
   * @param ecs
   * @param mode
   */
  public void setMode(JavaMode mode) {
//    errorCheckerService = ecs;
    getCustomPainter().setMode(mode);
  }


  /**
   * Handles KeyEvents for TextArea (code completion begins from here).
   */
  public void processKeyEvent(KeyEvent evt) {
    if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
      if (suggestion != null){
        if (suggestion.isVisible()){
          Base.log("esc key");
          hideSuggestion();
          evt.consume();
          return;
        }
      }

    } else if (evt.getKeyCode() == KeyEvent.VK_ENTER &&
               evt.getID() == KeyEvent.KEY_PRESSED) {
      if (suggestion != null) {
        if (suggestion.isVisible()) {
          if (suggestion.insertSelection(CompletionPanel.KEYBOARD_COMPLETION)) {
            //hideSuggestion(); // Kill it!
            evt.consume();
            // Still try to show suggestions after inserting if it's
            // the case of overloaded methods. See #2755
            if(suggestion.isVisible())
              prepareSuggestions(evt);
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
        Base.log("BK Key");
        break;
      case KeyEvent.VK_SPACE:
        if (suggestion != null) {
          if (suggestion.isVisible()) {
            Base.log("Space bar, hide completion list");
            suggestion.setInvisible();
          }
        }
        break;
      }
    }
    super.processKeyEvent(evt);

    // code completion disabled if Java tabs present
    if (!editor.hasJavaTabs()) {
      if (evt.getID() == KeyEvent.KEY_TYPED) {
        processCompletionKeys(evt);

      } else if (Base.isMacOS() && evt.getID() == KeyEvent.KEY_RELEASED) {
        processControlSpace(evt);
      }
    }
  }


  // Special case for OS X, where Ctrl-Space is not detected as KEY_TYPED
  // https://github.com/processing/processing/issues/2699
  private void processControlSpace(final KeyEvent event) {
    if (event.getKeyCode() == KeyEvent.VK_SPACE && event.isControlDown()) {
      SwingWorker<Object, Object> worker = new SwingWorker<Object, Object>() {
        protected Object doInBackground() throws Exception {
          // Provide completions only if it's enabled
          if (JavaMode.codeCompletionsEnabled) {
            Base.log("[KeyEvent]" + KeyEvent.getKeyText(event.getKeyCode()) + "  |Prediction started");
            Base.log("Typing: " + fetchPhrase(event));
          }
          return null;
        }
      };
      worker.execute();
    }
  }


  private void processCompletionKeys(final KeyEvent event) {
    char keyChar = event.getKeyChar();
    if (keyChar == KeyEvent.VK_ENTER ||
        keyChar == KeyEvent.VK_ESCAPE ||
        keyChar == KeyEvent.VK_TAB ||
        keyChar == KeyEvent.CHAR_UNDEFINED) {
      return;

    } else if (keyChar == ')') {
      hideSuggestion(); // See #2741
      return;
    }

    if (keyChar == '.') {
      if (JavaMode.codeCompletionsEnabled) {
        Base.log("[KeyEvent]" + KeyEvent.getKeyText(event.getKeyCode()) + "  |Prediction started");
        Base.log("Typing: " + fetchPhrase(event));
      }
    } else if (keyChar == ' ') { // Trigger on Ctrl-Space
      if (!Base.isMacOS() && JavaMode.codeCompletionsEnabled &&
          (event.isControlDown() || event.isMetaDown())) {
        SwingWorker<Object, Object> worker = new SwingWorker<Object, Object>() {
          protected Object doInBackground() throws Exception {
            // Provide completions only if it's enabled
            if (JavaMode.codeCompletionsEnabled) {
              getDocument().remove(getCaretPosition() - 1, 1); // Remove the typed space
              Base.log("[KeyEvent]" + event.getKeyChar() + "  |Prediction started");
              Base.log("Typing: " + fetchPhrase(event));
            }
            return null;
          }
        };
        worker.execute();
      } else {
        hideSuggestion(); // hide on spacebar
      }
    } else {
      if (JavaMode.codeCompletionsEnabled) {
        prepareSuggestions(event);
      }
    }
  }


  /** Kickstart auto-complete suggestions */
  private void prepareSuggestions(final KeyEvent evt){
    SwingWorker<Object, Object> worker = new SwingWorker<Object, Object>() {
      protected Object doInBackground() throws Exception {
        // Provide completions only if it's enabled
        if (JavaMode.codeCompletionsEnabled &&
            (JavaMode.ccTriggerEnabled || suggestion.isVisible())) {
          Base.log("[KeyEvent]" + evt.getKeyChar() + "  |Prediction started");
          Base.log("Typing: " + fetchPhrase(evt));
        }
        return null;
      }
    };
    worker.execute();
  }


  /**
   * Retrieves the word on which the mouse pointer is present
   * @param evt - the MouseEvent which triggered this method
   */
  private String fetchPhrase(MouseEvent evt) {
    Base.log("--handle Mouse Right Click--");
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
      Base.log("x=" + x);
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
            xLS--;
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
      if (Character.isDigit(word.charAt(0))) {
        return null;
      }
      Base.log("Mouse click, word: " + word.trim());
      editor.getErrorChecker().getASTGenerator().setLastClickedWord(line, word, xLS);
      return word.trim();
    }
  }


  /**
   * Retrieves the current word typed just before the caret.
   * Then triggers code completion for that word.
   * @param evt - the KeyEvent which triggered this method
   */
  public String fetchPhrase(KeyEvent evt) {
    int off = getCaretPosition();
    Base.log("off " + off);
    if (off < 0)
      return null;
    int line = getCaretLine();
    if (line < 0)
      return null;
    String s = getLineText(line);
    Base.log("  line " + line);

    //log2(s + " len " + s.length());

    int x = getCaretPosition() - getLineStartOffset(line) - 1, x1 = x - 1;
    if(x >= s.length() || x < 0) {
      //log("X is " + x + ". Returning null");
      hideSuggestion();
      return null; //TODO: Does this check cause problems? Verify.
    }

    Base.log("  x char: " + s.charAt(x));

    if (!(Character.isLetterOrDigit(s.charAt(x)) || s.charAt(x) == '_'
        || s.charAt(x) == '(' || s.charAt(x) == '.')) {
      //log("Char before caret isn't a letter/digit/_(. so no predictions");
      hideSuggestion();
      return null;
    } else if (x > 0 && (s.charAt(x - 1) == ' ' || s.charAt(x - 1) == '(')
        && Character.isDigit(s.charAt(x))) {
      //log("Char before caret isn't a letter, but ' ' or '(', so no predictions");
      hideSuggestion(); // See #2755, Option 2 comment
      return null;
    } else if (x == 0){
      //log("X is zero");
      hideSuggestion();
      return null;
    }

    //int xLS = off - getLineStartNonWhiteSpaceOffset(line);

    String word = (x < s.length() ? s.charAt(x) : "") + "";
    if (s.trim().length() == 1) {
//      word = ""
//          + (keyChar == KeyEvent.CHAR_UNDEFINED ? s.charAt(x - 1) : keyChar);
      //word = (x < s.length()?s.charAt(x):"") + "";
      word = word.trim();
      if (word.endsWith("."))
        word = word.substring(0, word.length() - 1);

      editor.getErrorChecker().getASTGenerator().preparePredictions(word, line + editor.getErrorChecker().mainClassOffset,0);
      return word;
    }

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

      if (i > 200) {
        // time out!
        break;
      }
    }

    if (Character.isDigit(word.charAt(0)))
      return null;
    word = word.trim();
    //    if (word.endsWith("."))
    //      word = word.substring(0, word.length() - 1);
    int lineStartNonWSOffset = 0;
    if (word.length() >= JavaMode.codeCompletionTriggerLength) {
      editor.getErrorChecker().getASTGenerator()
          .preparePredictions(word, line + editor.getErrorChecker().mainClassOffset,
                              lineStartNonWSOffset);
    }
    return word;

  }


//  /**
//   * Retrieve the total width of the gutter area.
//   * @return gutter width in pixels
//   */
//  protected int getGutterWidth() {
//    if (!editor.isDebugToolbarEnabled()) {
//      return 0;
//    }
//
//    FontMetrics fm = painter.getFontMetrics();
//    int textWidth = Math.max(fm.stringWidth(breakpointMarker),
//                             fm.stringWidth(currentLineMarker));
//    return textWidth + 2 * gutterPadding;
//  }
//
//
//  /**
//   * Retrieve the width of margins applied to the left and right of the gutter
//   * text.
//   *
//   * @return margins in pixels
//   */
//  protected int getGutterMargins() {
//    if (!editor.isDebugToolbarEnabled()) {
//      return 0;
//    }
//    return gutterPadding;
//  }


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
    return super._offsetToX(line, offset) + Editor.LEFT_GUTTER;
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
    return super.xToOffset(line, x - Editor.LEFT_GUTTER);
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
//      // check if this happened in the gutter area
//      if (me.getX() < Editor.LEFT_GUTTER) {
//        if (me.getButton() == MouseEvent.BUTTON1) { // && me.getClickCount() == 2) {
//          //int line = me.getY() / painter.getFontMetrics().getHeight() + firstLine;
//          int offset = xyToOffset(me.getX(), me.getY());
//          if (offset >= 0) {
//            int lineIndex = getLineOfOffset(offset);
//            editor.toggleBreakpoint(lineIndex);
//          }
////          if (line >= 0 && line < getLineCount()) {
////            //editor.gutterDblClicked(line);
////            editor.toggleBreakpoint(line);
////          }
//        }
//        return;
//      }

      if (me.getButton() == MouseEvent.BUTTON3) {
        if (!editor.hasJavaTabs()) { // tooltips, etc disabled for java tabs
          fetchPhrase(me);
        }
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
      if (me.getX() < Editor.LEFT_GUTTER) {
        if (lastX >= Editor.LEFT_GUTTER) {
          painter.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
      } else {
        if (lastX < Editor.LEFT_GUTTER) {
          painter.setCursor(new Cursor(Cursor.TEXT_CURSOR));
        }
      }
      lastX = me.getX();
    }
  }


  // appears unused, removed when looking to change completion trigger [fry 140801]
  /*
  public void showSuggestionLater(final DefaultListModel defListModel, final String word) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        showSuggestion(defListModel,word);
      }

    });
  }
  */


  /**
   * Calculates location of caret and displays the suggestion popup at the location.
   *
   * @param listModel
   * @param subWord
   */
  protected void showSuggestion(DefaultListModel<CompletionCandidate> listModel, String subWord) {
    hideSuggestion();

    if (listModel.size() == 0) {
      Base.log("TextArea: No suggestions to show.");

    } else {
      int position = getCaretPosition();
      Point location = new Point();
      try {
        location.x = offsetToX(getCaretLine(), position
                               - getLineStartOffset(getCaretLine()));
        location.y = lineToY(getCaretLine())
            + getPainter().getFontMetrics().getHeight() + getPainter().getFontMetrics().getDescent();
        //log("TA position: " + location);
      } catch (Exception e2) {
        e2.printStackTrace();
        return;
      }

      if (subWord.length() < 2) {
        return;
      }
      suggestion = new CompletionPanel(this, position, subWord,
                                       listModel, location, editor);
      requestFocusInWindow();
    }
  }


  /** Hides suggestion popup */
  public void hideSuggestion() {
    if (suggestion != null) {
      suggestion.setInvisible();
      //log("Suggestion hidden.");
      suggestion = null;
    }
  }


  // TweakMode code

  // save input listeners to stop/start text edit
  ComponentListener[] prevCompListeners;
  MouseListener[] prevMouseListeners;
  MouseMotionListener[] prevMMotionListeners;
  KeyListener[] prevKeyListeners;
  boolean interactiveMode;

  /* remove all standard interaction listeners */
  public void removeAllListeners() {
    ComponentListener[] componentListeners = painter.getComponentListeners();
    MouseListener[] mouseListeners = painter.getMouseListeners();
    MouseMotionListener[] mouseMotionListeners = painter.getMouseMotionListeners();
    KeyListener[] keyListeners = editor.getKeyListeners();

    for (ComponentListener cl : componentListeners) {
      painter.removeComponentListener(cl);
    }
    for (MouseListener ml : mouseListeners) {
      painter.removeMouseListener(ml);
    }
    for (MouseMotionListener mml : mouseMotionListeners) {
      painter.removeMouseMotionListener(mml);
    }
    for (KeyListener kl : keyListeners) {
      editor.removeKeyListener(kl);
    }
  }


  public void startInteractiveMode() {
    // ignore if we are already in interactiveMode
    if (interactiveMode) return;

    removeAllListeners();

    // add our private interaction listeners
    getCustomPainter().startInterativeMode();
    this.editable = false;
    this.caretBlinks = false;
    this.setCaretVisible(false);
    interactiveMode = true;
  }


  public void stopInteractiveMode() {
    // ignore if we are not in interactive mode
    if (!interactiveMode) return;

    removeAllListeners();
    addPrevListeners();

    getCustomPainter().stopInteractiveMode();
    this.editable = true;
    this.caretBlinks = true;
    this.setCaretVisible(true);
    interactiveMode = false;
  }


  public int getHorizontalScroll() {
    return horizontal.getValue();
  }


  private void addPrevListeners() {
    // add the original text-edit listeners
    for (ComponentListener cl : prevCompListeners) {
      painter.addComponentListener(cl);
    }
    for (MouseListener ml : prevMouseListeners) {
      painter.addMouseListener(ml);
    }
    for (MouseMotionListener mml : prevMMotionListeners) {
      painter.addMouseMotionListener(mml);
    }
    for (KeyListener kl : prevKeyListeners) {
      editor.addKeyListener(kl);
    }
  }


  public void updateInterface(List<List<Handle>> handles,
                              List<List<ColorControlBox>> colorBoxes) {
    getCustomPainter().updateInterface(handles, colorBoxes);
  }
}
