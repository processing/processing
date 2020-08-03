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

package processing.mode.java.pdex;

import java.awt.EventQueue;
import java.awt.Point;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.SwingWorker;

import processing.app.Messages;
import processing.app.Platform;
import processing.app.syntax.PdeTextArea;
import processing.app.syntax.TextAreaDefaults;
import processing.mode.java.JavaEditor;
import processing.mode.java.JavaInputHandler;
import processing.mode.java.JavaMode;
import processing.mode.java.tweak.ColorControlBox;
import processing.mode.java.tweak.Handle;


/**
 * TextArea implementation for Java Mode. Primary differences from PdeTextArea
 * are completions, suggestions, and tweak handling.
 */
public class JavaTextArea extends PdeTextArea {
  private CompletionPanel suggestion;


  public JavaTextArea(TextAreaDefaults defaults, JavaEditor editor) {
    super(defaults, new JavaInputHandler(editor), editor);

    suggestionGenerator = new CompletionGenerator();

    tweakMode = false;
  }


  public JavaEditor getJavaEditor() {
    return (JavaEditor) editor;
  }


  @Override
  protected JavaTextAreaPainter createPainter(final TextAreaDefaults defaults) {
    return new JavaTextAreaPainter(this, defaults);
  }


  // used by Tweak Mode
  protected JavaTextAreaPainter getJavaPainter() {
    return (JavaTextAreaPainter) painter;
  }


  /**
   * Handles KeyEvents for TextArea (code completion begins from here).
   * TODO Needs explanation of why this implemented with an override
   *      of processKeyEvent() instead of using listeners.
   */
  @Override
  public void processKeyEvent(KeyEvent evt) {
    if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
      if (suggestion != null){
        if (suggestion.isVisible()){
          Messages.log("ESC key");
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
    if (!getJavaEditor().hasJavaTabs()) {
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


  CompletionGenerator suggestionGenerator;

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

    // Adjust line number for tabbed sketches
    //int codeIndex = editor.getSketch().getCodeIndex(getJavaEditor().getCurrentTab());
    int codeIndex = editor.getSketch().getCurrentCodeIndex();
    int lineStartOffset = editor.getTextArea().getLineStartOffset(caretLineIndex);

    getJavaEditor().getPreprocessingService().whenDone(ps -> {
      int lineNumber = ps.tabOffsetToJavaLine(codeIndex, lineStartOffset);

      String phrase = null;
      DefaultListModel<CompletionCandidate> defListModel = null;

      try {
        Messages.log("phrase parse start");
        phrase = parsePhrase(text);
        Messages.log("phrase: " + phrase);
        if (phrase != null) {
          List<CompletionCandidate> candidates;

          candidates = suggestionGenerator.preparePredictions(ps, phrase, lineNumber);

          if (!suggestionRequested) {

    //        // don't show completions when the outline is visible
    //        boolean showSuggestions =
    //          astGenerator.sketchOutline == null || !astGenerator.sketchOutline.isVisible();

    //        if (showSuggestions && phrase != null &&
            if (candidates != null && !candidates.isEmpty()) {
              Collections.sort(candidates);
              defListModel = CompletionGenerator.filterPredictions(candidates);
              Messages.log("Got: " + candidates.size() + " candidates, " + defListModel.size() + " filtered");
            }
          }

        }

        final String finalPhrase = phrase;
        final DefaultListModel<CompletionCandidate> finalDefListModel = defListModel;

        EventQueue.invokeLater(() -> {

          suggestionRunning = false;
          if (suggestionRequested) {
            Messages.log("completion invalidated");
            fetchPhrase();
            return;
          }

          Messages.log("completion finishing");

          if (finalDefListModel != null) {
            showSuggestion(finalDefListModel, finalPhrase);
          } else {
            hideSuggestion();
          }
        });
      } catch (Exception e) {
        Messages.loge("error while preparing suggestions", e);
      }
    });
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


  /**
   * Calculates location of caret and displays the suggestion pop-up.
   */
  protected void showSuggestion(DefaultListModel<CompletionCandidate> listModel, String subWord) {
    // TODO can this be ListModel instead? why is size() in DefaultListModel
    // different from getSize() in ListModel (or are they, really?)
    hideSuggestion();

    if (listModel.size() != 0) {
      int position = getCaretPosition();
      try {
        Point location =
          new Point(offsetToX(getCaretLine(),
                              position - getLineStartOffset(getCaretLine())),
                    lineToY(getCaretLine()) + getPainter().getLineHeight());
        suggestion = new CompletionPanel(this, position, subWord,
                                         listModel, location, getJavaEditor());
        requestFocusInWindow();

      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      Messages.log("TextArea: No suggestions to show.");
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
  protected ComponentListener[] baseCompListeners;
  protected MouseListener[] baseMouseListeners;
  protected MouseMotionListener[] baseMotionListeners;
  protected KeyListener[] baseKeyListeners;
  protected boolean tweakMode;


  /* remove all standard interaction listeners */
  public void tweakRemoveListeners() {
    if (baseCompListeners == null) {
      // First time in tweak mode, grab the default listeners. Moved from the
      // constructor since not all listeners may have been added at that point.
      baseCompListeners = painter.getComponentListeners();
      baseMouseListeners = painter.getMouseListeners();
      baseMotionListeners = painter.getMouseMotionListeners();
      baseKeyListeners = editor.getKeyListeners();
    }
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
      tweakRemoveListeners();
      getJavaPainter().startTweakMode();
      this.editable = false;
      this.caretBlinks = false;
      this.setCaretVisible(false);
      tweakMode = true;
    }
  }


  public void stopTweakMode() {
    // ignore if we are not in interactive mode
    if (tweakMode) {
      tweakRemoveListeners();
      tweakRestoreBaseListeners();
      getJavaPainter().stopTweakMode();
      editable = true;
      caretBlinks = true;
      setCaretVisible(true);
      tweakMode = false;
    }
  }


  private void tweakRestoreBaseListeners() {
    // add the original text-edit listeners
    for (ComponentListener cl : baseCompListeners) {
      painter.addComponentListener(cl);
    }
    for (MouseListener ml : baseMouseListeners) {
      painter.addMouseListener(ml);
    }
    for (MouseMotionListener mml : baseMotionListeners) {
      painter.addMouseMotionListener(mml);
    }
    for (KeyListener kl : baseKeyListeners) {
      editor.addKeyListener(kl);
    }
  }


  public void updateInterface(List<List<Handle>> handles,
                              List<List<ColorControlBox>> colorBoxes) {
    getJavaPainter().updateTweakInterface(handles, colorBoxes);
  }
}
