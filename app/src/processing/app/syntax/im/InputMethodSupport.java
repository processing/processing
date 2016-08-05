package processing.app.syntax.im;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextHitInfo;
import java.awt.font.TextLayout;
import java.awt.im.InputMethodRequests;
import java.text.AttributedCharacterIterator;
import java.text.AttributedCharacterIterator.Attribute;

import java.text.AttributedString;

import processing.app.Base;
import processing.app.Messages;
import processing.app.Preferences;
import processing.app.syntax.InputHandler;
import processing.app.syntax.JEditTextArea;
import processing.app.syntax.TextAreaPainter;


/**
 * On-the-spot style input support for CJK (Chinese, Japanese, Korean).
 *
 * @see <a href="https://processing.org/bugs/bugzilla/854.html">Bugzilla 854: implement input method support for Japanese (and other languages)</a>
 * @see <a href="https://processing.org/bugs/bugzilla/1531.html">Bugzilla 1531: Can't input full-width space when Japanese IME is on.</a>
 * @see <a href="http://docs.oracle.com/javase/8/docs/technotes/guides/imf/index.html">Java Input Method Framework (IMF) Technology</a>
 * @see <a href="http://docs.oracle.com/javase/tutorial/2d/text/index.html">The Java Tutorials</a>
 *
 * @author Takashi Maekawa (takachin@generative.info)
 * @author Satoshi Okita
 */
public class InputMethodSupport implements InputMethodRequests, InputMethodListener {

  /*
  public interface Callback {
    public void onCommitted(char c);
  }

  private Callback callback;
  */

  static private final Attribute[] CUSTOM_IM_ATTRIBUTES = {
    TextAttribute.INPUT_METHOD_HIGHLIGHT,
  };

  private JEditTextArea textArea;
  private InputHandler inputHandler;

  private int committedCount = 0;
  private AttributedString composedTextString;

  public InputMethodSupport(JEditTextArea textArea, InputHandler inputHandler) {
    this.textArea = textArea;
    this.inputHandler = inputHandler;

    textArea.enableInputMethods(true);
    textArea.addInputMethodListener(this);
  }


  /*
  public void setCallback(Callback callback) {
    this.callback = callback;
  }
  */


  /////////////////////////////////////////////////////////////////////////////

  // InputMethodRequest

  /////////////////////////////////////////////////////////////////////////////

  @Override
  public Rectangle getTextLocation(TextHitInfo offset) {
    if (Base.DEBUG) {
      Messages.log("#Called getTextLocation:" + offset);
    }
    int line = textArea.getCaretLine();
    int offsetX = textArea.getCaretPosition() - textArea.getLineStartOffset(line);
    // '+1' mean textArea.lineToY(line) + textArea.getPainter().getFontMetrics().getHeight().
    // TextLayout#draw method need at least one height of font.
    Rectangle rectangle = new Rectangle(textArea.offsetToX(line, offsetX), textArea.lineToY(line + 1), 0, 0);

    Point location = textArea.getPainter().getLocationOnScreen();
    rectangle.translate(location.x, location.y);

    return rectangle;
  }


  @Override
  public TextHitInfo getLocationOffset(int x, int y) {
    return null;
  }


  @Override
  public int getInsertPositionOffset() {
    return -textArea.getCaretPosition();
  }


  @Override
  public AttributedCharacterIterator getCommittedText(int beginIndex,
      int endIndex, AttributedCharacterIterator.Attribute[] attributes) {
    int length = endIndex - beginIndex;
    String textAreaString = textArea.getText(beginIndex, length);
    return new AttributedString(textAreaString).getIterator();
  }


  @Override
  public int getCommittedTextLength() {
    return committedCount;
  }


  @Override
  public AttributedCharacterIterator cancelLatestCommittedText(
      AttributedCharacterIterator.Attribute[] attributes) {
    return null;
  }


  @Override
  public AttributedCharacterIterator getSelectedText(
      AttributedCharacterIterator.Attribute[] attributes) {
    return null;
  }


  /////////////////////////////////////////////////////////////////////////////

  // InputMethodListener

  /////////////////////////////////////////////////////////////////////////////

  /**
   * Handles events from InputMethod.
   *
   * @param event event from Input Method.
   */
  @Override
  public void inputMethodTextChanged(InputMethodEvent event) {
    if (Base.DEBUG) {
      StringBuilder sb = new StringBuilder();
      sb.append("#Called inputMethodTextChanged");
      sb.append("\t ID: " + event.getID());
      sb.append("\t timestamp: " + new java.util.Date(event.getWhen()));
      sb.append("\t parmString: " + event.paramString());
      Messages.log(sb.toString());
    }

    AttributedCharacterIterator text = event.getText(); // text = composedText + commitedText
    committedCount = event.getCommittedCharacterCount();

    // The caret for Input Method. If you type a character by a input method,
    // original caret position will be incorrect. JEditTextArea is not
    // implemented using AttributedString and TextLayout.
    textArea.setCaretVisible(false);

    // Japanese         : if the enter key pressed, event.getText is null.
    // Japanese         : if first space key pressed, event.getText is null.
    // Chinese (pinin)  : if a space key pressed, event.getText is null.
    // Taiwan (bopomofo): ?
    // Korean           : ?

    // Korean Input Method
    if (text != null && text.getEndIndex() - (text.getBeginIndex() + committedCount) <= 0) {
      textArea.setCaretVisible(true);
    }
    // Japanese Input Method
    if (text == null) {
      textArea.setCaretVisible(true);
    }

    if (text != null) {
      if (committedCount > 0) {
        char[] insertion = new char[committedCount];
        char c = text.first();
        for (int i = 0; i < committedCount; i++) {
          insertion[i] = c;
          c = text.next();
        }
        // Insert this as a compound edit
        textArea.setSelectedText(new String(insertion), true);
        inputHandler.handleInputMethodCommit();
      }

      CompositionTextPainter compositionPainter = textArea.getPainter().getCompositionTextpainter();
      if (Base.DEBUG) {
        Messages.log(" textArea.getCaretPosition() + committed_count: " + (textArea.getCaretPosition() + committedCount));
      }
      compositionPainter.setComposedTextLayout(getTextLayout(text, committedCount), textArea.getCaretPosition() + committedCount);
      compositionPainter.setCaret(event.getCaret());

    } else {  // otherwise hide the input method
      CompositionTextPainter compositionPainter = textArea.getPainter().getCompositionTextpainter();
      compositionPainter.setComposedTextLayout(null, 0);
      compositionPainter.setCaret(null);
    }
    event.consume();
    textArea.repaint();
  }


  private TextLayout getTextLayout(AttributedCharacterIterator text, int committedCount) {
    boolean antialias = Preferences.getBoolean("editor.smooth");
    TextAreaPainter painter = textArea.getPainter();

    // create attributed string with font info.
    //if (text.getEndIndex() - (text.getBeginIndex() + committedCharacterCount) > 0) {
    if (text.getEndIndex() - (text.getBeginIndex() + committedCount) > 0) {
      composedTextString = new AttributedString(text, committedCount, text.getEndIndex(), CUSTOM_IM_ATTRIBUTES);
      Font font = painter.getFontMetrics().getFont();
      composedTextString.addAttribute(TextAttribute.FONT, font);
      composedTextString.addAttribute(TextAttribute.BACKGROUND, Color.WHITE);
    } else {
      composedTextString = new AttributedString("");
      return null;
    }

    // set hint of antialiasing to render target.
    Graphics2D g2d = (Graphics2D)painter.getGraphics();
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        antialias ?
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON :
                        RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
    FontRenderContext frc = g2d.getFontRenderContext();
    if (Base.DEBUG) {
      Messages.log("debug: FontRenderContext is Antialiased = " + frc.getAntiAliasingHint());
    }

    return new TextLayout(composedTextString.getIterator(), frc);
  }


  @Override
  public void caretPositionChanged(InputMethodEvent event) {
    event.consume();
  }


  /*
  private void insertCharacter(char c) {
    if (Base.DEBUG) {
      Messages.log("debug: insertCharacter(char c) textArea.getCaretPosition()=" + textArea.getCaretPosition());
    }
    try {
      textArea.getDocument().insertString(textArea.getCaretPosition(), Character.toString(c), null);
      if (Base.DEBUG) {
        Messages.log("debug: \t after:insertCharacter(char c) textArea.getCaretPosition()=" + textArea.getCaretPosition());
      }
    } catch (BadLocationException e) {
      e.printStackTrace();
    }
  }
  */
}
