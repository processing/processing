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
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.swing.DefaultListModel;
import javax.swing.SwingWorker;

import processing.app.Messages;
import processing.app.Mode;
import processing.app.Platform;
import processing.app.syntax.JEditTextArea;
import processing.app.syntax.TextAreaDefaults;
import processing.app.ui.Editor;


// TODO The way listeners are added/removed here is fragile and
//      likely to cause bugs that are very difficult to find.
//      We shouldn't be re-inventing the wheel with how listeners are handled.
// TODO We're overriding more things in JEditTextArea than we should, which
//      makes it trickier for other Modes (Python, etc) to subclass because
//      they'll need to re-implement what's in here, but first wade through it.
//      To fix, we need to clean this up and put the appropriate cross-Mode
//      changes into JEditTextArea (or a subclass in processing.app)

public class JavaTextArea extends JEditTextArea {
  protected final JavaEditor editor;

  protected Image gutterGradient;

  /// the text marker for highlighting breakpoints in the gutter
  static public final String BREAK_MARKER = "<>";
  /// the text marker for highlighting the current line in the gutter
  static public final String STEP_MARKER = "->";

  /// maps line index to gutter text
  protected final Map<Integer, String> gutterText = new HashMap<>();

  private CompletionPanel suggestion;


  public JavaTextArea(TextAreaDefaults defaults, JavaEditor editor) {
    super(defaults, new JavaInputHandler(editor));
    this.editor = editor;

    // handle right click on a word
    painter.addMouseListener(rightClickMouseAdapter);

    // change cursor to pointer in the gutter area
    painter.addMouseMotionListener(gutterCursorMouseAdapter);

    //addCompletionPopupListner();
    add(CENTER, painter);

    // load settings from theme.txt
    Mode mode = editor.getMode();
    gutterGradient = mode.makeGradient("editor", Editor.LEFT_GUTTER, 500);

    // TweakMode code
    prevCompListeners = painter.getComponentListeners();
    prevMouseListeners = painter.getMouseListeners();
    prevMMotionListeners = painter.getMouseMotionListeners();
    prevKeyListeners = editor.getKeyListeners();

    tweakMode = false;
  }


  @Override
  protected JavaTextAreaPainter createPainter(final TextAreaDefaults defaults) {
    return new JavaTextAreaPainter(this, defaults);
  }


  protected JavaTextAreaPainter getCustomPainter() {
    return (JavaTextAreaPainter) painter;
  }


  public void setMode(JavaMode mode) {
    getCustomPainter().setMode(mode);
  }


  /**
   * Handles KeyEvents for TextArea (code completion begins from here).
   */
  @Override
  public void processKeyEvent(KeyEvent evt) {
    if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
      if (suggestion != null){
        if (suggestion.isVisible()){
          Messages.log("esc key");
          hideSuggestion();
          evt.consume();
          return;
        }
      }

    } else if (evt.getKeyCode() == KeyEvent.VK_ENTER &&
               evt.getID() == KeyEvent.KEY_PRESSED) {
      if (suggestion != null &&
          suggestion.isVisible() &&
          suggestion.insertSelection(CompletionPanel.KEYBOARD_COMPLETION)) {
        evt.consume();
        // Still try to show suggestions after inserting if it's
        // the case of overloaded methods. See #2755
        if (suggestion.isVisible()) {
          prepareSuggestions(evt);
        }
        return;
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
        Messages.log("BK Key");
        break;
      case KeyEvent.VK_SPACE:
        if (suggestion != null) {
          if (suggestion.isVisible()) {
            Messages.log("Space bar, hide completion list");
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
      } else if (!Platform.isMacOS() && evt.getID() == KeyEvent.KEY_RELEASED) {
        processCompletionKeys(evt);
      } else if (Platform.isMacOS() && evt.getID() == KeyEvent.KEY_RELEASED) {
        processControlSpace(evt);
      }
    }
  }


  // Special case for OS X, where Ctrl-Space is not detected as KEY_TYPED
  // https://github.com/processing/processing/issues/2699
  private void processControlSpace(final KeyEvent event) {
    if (event.getKeyCode() == KeyEvent.VK_SPACE && event.isControlDown()) {
      // Provide completions only if it's enabled
      if (JavaMode.codeCompletionsEnabled) {
        Messages.log("[KeyEvent]" + KeyEvent.getKeyText(event.getKeyCode()) + "  |Prediction started");
        fetchPhrase();
      }
    }
  }


  private void processCompletionKeys(final KeyEvent event) {
    char keyChar = event.getKeyChar();
    int keyCode = event.getKeyCode();
    if (keyChar == KeyEvent.VK_ENTER ||
        keyChar == KeyEvent.VK_ESCAPE ||
        keyChar == KeyEvent.VK_TAB ||
        (event.getID() == KeyEvent.KEY_RELEASED &&
            keyCode != KeyEvent.VK_LEFT && keyCode != KeyEvent.VK_RIGHT)) {
      // ignore
    } else if (keyChar == ')') {
      // https://github.com/processing/processing/issues/2741
      hideSuggestion();

    } else if (keyChar == '.') {
      if (JavaMode.codeCompletionsEnabled) {
        Messages.log("[KeyEvent]" + KeyEvent.getKeyText(event.getKeyCode()) + "  |Prediction started");
        fetchPhrase();
      }
    } else if (keyChar == ' ') { // Trigger on Ctrl-Space
      if (!Platform.isMacOS() && JavaMode.codeCompletionsEnabled &&
          (event.isControlDown() || event.isMetaDown())) {
        // Provide completions only if it's enabled
        if (JavaMode.codeCompletionsEnabled) {
          // Removed for https://github.com/processing/processing/issues/3847
          //try {
          //  getDocument().remove(getCaretPosition() - 1, 1); // Remove the typed space
          Messages.log("[KeyEvent]" + event.getKeyChar() + "  |Prediction started");
          fetchPhrase();
          //} catch (BadLocationException e) {
          //  e.printStackTrace();
          //}
        }
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
  private void prepareSuggestions(final KeyEvent evt) {
    // Provide completions only if it's enabled
    if (JavaMode.codeCompletionsEnabled &&
        (JavaMode.ccTriggerEnabled ||
        (suggestion != null && suggestion.isVisible()))) {
      Messages.log("[KeyEvent]" + evt.getKeyChar() + "  |Prediction started");
      fetchPhrase();
    }
  }


  /**
   * Retrieves the word on which the mouse pointer is present
   * @param evt - the MouseEvent which triggered this method
   */
  private String fetchPhrase(MouseEvent evt) {
    Messages.log("--handle Mouse Right Click--");
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
      Messages.log("x=" + x);
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
      if (Character.isDigit(word.charAt(0))) {
        return null;
      }
      Messages.log("Mouse click, word: " + word.trim());
      ASTGenerator astGenerator = editor.getErrorChecker().getASTGenerator();

      int tabIndex = editor.getSketch().getCurrentCodeIndex();

      synchronized (astGenerator) {
        astGenerator.setLastClickedWord(tabIndex, off, word);
      }
      return word.trim();
    }
  }


  SwingWorker<Void, Void> suggestionWorker = null;

  volatile boolean suggestionRunning = false;
  volatile boolean suggestionRequested = false;

  /**
   * Retrieves the current word typed just before the caret.
   * Then triggers code completion for that word.
   * @param evt - the KeyEvent which triggered this method
   */
  protected void fetchPhrase() {

    if (suggestionRunning) {
      suggestionRequested = true;
      return;
    }

    suggestionRunning = true;
    suggestionRequested = false;

    final String text;
    final int caretLineIndex;
    final int caretLinePosition;
    {
      // Get caret position
      int caretPosition = getCaretPosition();
      if (caretPosition < 0) {
        suggestionRunning = false;
        return;
      }

      // Get line index
      caretLineIndex = getCaretLine();
      if (caretLineIndex < 0) {
        suggestionRunning = false;
        return;
      }

      // Get text of the line
      String lineText = getLineText(caretLineIndex);
      if (lineText == null) {
        suggestionRunning = false;
        return;
      }

      // Get caret position on the line
      caretLinePosition = getCaretPosition() - getLineStartOffset(caretLineIndex);
      if (caretLinePosition <= 0) {
        suggestionRunning = false;
        return;
      }

      // Get part of the line to the left of the caret
      if (caretLinePosition > lineText.length()) {
        suggestionRunning = false;
        return;
      }
      text = lineText.substring(0, caretLinePosition);
    }

    suggestionWorker = new SwingWorker<Void, Void>() {

      String phrase = null;
      DefaultListModel<CompletionCandidate> defListModel = null;

      @Override
      protected Void doInBackground() throws Exception {
        Messages.log("phrase parse start");
        phrase = parsePhrase(text);
        Messages.log("phrase: " + phrase);
        if (phrase == null) return null;

        List<CompletionCandidate> candidates;

        ASTGenerator astGenerator = editor.getErrorChecker().getASTGenerator();
        synchronized (astGenerator) {
          int lineOffset = caretLineIndex;

          candidates = astGenerator.preparePredictions(phrase, lineOffset);
        }

        if (suggestionRequested) return null;

//        // don't show completions when the outline is visible
//        boolean showSuggestions =
//          astGenerator.sketchOutline == null || !astGenerator.sketchOutline.isVisible();

//        if (showSuggestions && phrase != null &&
        if (phrase != null && candidates != null && !candidates.isEmpty()) {
          Collections.sort(candidates);
          defListModel = ASTGenerator.filterPredictions(candidates);
          Messages.log("Got: " + candidates.size() + " candidates, " + defListModel.size() + " filtered");
        }
        return null;
      }

      @Override
      protected void done() {

        try {
          get();
        } catch (ExecutionException e) {
          Messages.loge("error while preparing suggestions", e.getCause());
        } catch (InterruptedException e) {
          // don't care
        }

        suggestionRunning = false;
        if (suggestionRequested) {
          Messages.log("completion invalidated");
          hideSuggestion();
          fetchPhrase();
          return;
        }

        Messages.log("completion finishing");

        if (defListModel != null) {
          showSuggestion(defListModel, phrase);
        } else {
          hideSuggestion();
        }
      }
    };

    suggestionWorker.execute();
  }

  protected static String parsePhrase(final String lineText) {

    boolean overloading = false;

    { // Check if we can provide suggestions for this phrase ending
      String trimmedLineText = lineText.trim();
      if (trimmedLineText.length() == 0) return null;

      char lastChar = trimmedLineText.charAt(trimmedLineText.length() - 1);
      if (lastChar == '.') {
        trimmedLineText = trimmedLineText.substring(0, trimmedLineText.length() - 1).trim();
        if (trimmedLineText.length() == 0) return null;
        lastChar = trimmedLineText.charAt(trimmedLineText.length() - 1);
        switch (lastChar) {
          case ')':
          case ']':
          case '"':
            break; // We can suggest for these
          default:
            if (!Character.isJavaIdentifierPart(lastChar)) {
              return null; // Not something we can suggest
            }
            break;
        }
      } else if (lastChar == '(') {
        overloading = true; // We can suggest overloaded methods
      } else if (!Character.isJavaIdentifierPart(lastChar)) {
        return null; // Not something we can suggest
      }
    }

    final int currentCharIndex = lineText.length() - 1;

    { // Check if the caret is in the comment
      int commentStart = lineText.indexOf("//", 0);
      if (commentStart >= 0 && currentCharIndex > commentStart) {
        return null;
      }
    }

    // Index the line
    BitSet isInLiteral = new BitSet(lineText.length());
    BitSet isInBrackets = new BitSet(lineText.length());

    { // Mark parts in literals
      boolean inString = false;
      boolean inChar = false;
      boolean inEscaped = false;

      for (int i = 0; i < lineText.length(); i++) {
        if (!inEscaped) {
          switch (lineText.charAt(i)) {
            case '\"':
              if (!inChar) inString = !inString;
              break;
            case '\'':
              if (!inString) inChar = !inChar;
              break;
            case '\\':
              if (inString || inChar) {
                inEscaped = true;
              }
              break;
          }
        } else {
          inEscaped = false;
        }
        isInLiteral.set(i, inString || inChar);
      }
    }

    if (isInLiteral.get(currentCharIndex)) return null;

    { // Mark parts in top level brackets
      int depth = overloading ? 1 : 0;
      int bracketStart = overloading ? lineText.length() : 0;
      int squareDepth = 0;
      int squareBracketStart = 0;

      bracketLoop: for (int i = lineText.length() - 1; i >= 0; i--) {
        if (!isInLiteral.get(i)) {
          switch (lineText.charAt(i)) {
            case ')':
              if (depth == 0) bracketStart = i;
              depth++;
              break;
            case '(':
              depth--;
              if (depth == 0) {
                isInBrackets.set(i, bracketStart);
              } else if (depth < 0) {
                break bracketLoop;
              }
              break;
            case ']':
              if (squareDepth == 0) squareBracketStart = i;
              squareDepth++;
              break;
            case '[':
              squareDepth--;
              if (squareDepth == 0) {
                isInBrackets.set(i, squareBracketStart);
              } else if (squareDepth < 0) {
                break bracketLoop;
              }
              break;
          }
        }
      }

      if (depth > 0) isInBrackets.set(0, bracketStart);
      if (squareDepth > 0) isInBrackets.set(0, squareBracketStart);
    }

    // Walk the line from the end while it makes sense
    int position = currentCharIndex;
    parseLoop: while (position >= 0) {
      int currChar = lineText.charAt(position);
      switch (currChar) {
        case '.': // Grab it
          position--;
          break;
        case '[':
          break parseLoop; // End of scope
        case ']': // Grab the whole region in square brackets
          position = isInBrackets.previousClearBit(position-1);
          break;
        case '(':
          if (isInBrackets.get(position)) {
            position--; // This checks for first bracket while overloading
            break;
          }
          break parseLoop; // End of scope
        case ')': // Grab the whole region in brackets
          position = isInBrackets.previousClearBit(position-1);
          break;
        case '"': // Grab the whole literal and quit
          position = isInLiteral.previousClearBit(position - 1);
          break parseLoop;
        default:
          if (Character.isJavaIdentifierPart(currChar)) {
            position--; // Grab the identifier
          } else if (Character.isWhitespace(currChar)) {
            position--; // Grab whitespace too
          } else {
            break parseLoop; // Got a char ending the phrase
          }
          break;
      }
    }

    position++;

    // Extract phrase
    String phrase = lineText.substring(position, lineText.length()).trim();
    Messages.log(phrase);

    if (phrase.length() == 0 || Character.isDigit(phrase.charAt(0))) {
      return null; // Can't suggest for numbers or empty phrases
    }

    return phrase;
  }


  public Image getGutterGradient() {
    return gutterGradient;
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
   * Fetches word under the cursor on right click
   */
  protected final MouseAdapter rightClickMouseAdapter = new MouseAdapter() {
    @Override
    public void mousePressed(MouseEvent me) {
      if (me.getButton() == MouseEvent.BUTTON3 &&
          !editor.hasJavaTabs()) { // tooltips, etc disabled for java tabs
        fetchPhrase(me);
      }
    }
  };


  /**
   * Sets default cursor (instead of text cursor) in the gutter area.
   */
  protected final MouseMotionAdapter gutterCursorMouseAdapter = new MouseMotionAdapter() {
    private int lastX; // previous horizontal positon of the mouse cursor

    @Override
    public void mouseMoved(MouseEvent me) {
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
  };


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
    //new Exception(System.currentTimeMillis() + "").printStackTrace(System.out);
    hideSuggestion();

    if (listModel.size() == 0) {
      Messages.log("TextArea: No suggestions to show.");

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
      suggestion = null; // TODO: check if we dispose the window properly
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  // TWEAK MODE


  // save input listeners to stop/start text edit
  protected final ComponentListener[] prevCompListeners;
  protected final MouseListener[] prevMouseListeners;
  protected final MouseMotionListener[] prevMMotionListeners;
  protected final KeyListener[] prevKeyListeners;
  protected boolean tweakMode;


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


  public void startTweakMode() {
    // ignore if we are already in interactiveMode
    if (!tweakMode) {
      removeAllListeners();
      getCustomPainter().startTweakMode();
      this.editable = false;
      this.caretBlinks = false;
      this.setCaretVisible(false);
      tweakMode = true;
    }
  }


  public void stopTweakMode() {
    // ignore if we are not in interactive mode
    if (tweakMode) {
      removeAllListeners();
      addPrevListeners();
      getCustomPainter().stopTweakMode();
      editable = true;
      caretBlinks = true;
      setCaretVisible(true);
      tweakMode = false;
    }
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
