package processing.app.syntax;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextHitInfo;
import java.awt.font.TextLayout;
import java.awt.im.InputMethodRequests;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;

import javax.swing.text.BadLocationException;

public class InputMethodSupport implements InputMethodRequests,
    InputMethodListener {

  private JEditTextArea textArea;

  private TextLayout composedTextLayout = null;

  private int committed_count = 0;

  private static final int COMPOSING_UNDERBAR_HEIGHT = 5;

  private boolean isComposing;

  public InputMethodSupport(JEditTextArea textArea) {
    this.textArea = textArea;
    textArea.enableInputMethods(true);
    textArea.addInputMethodListener(this);
    isComposing = false;
  }

  public boolean getIsComposing() {
    return isComposing;
  }

  private Point getCaretLocation() {
    Point loc = new Point();
    TextAreaPainter painter = textArea.getPainter();
    FontMetrics fm = painter.getFontMetrics();
    int offsetY = fm.getHeight() - 5;
    int lineIndex = textArea.getCaretLine();
    loc.y = lineIndex * fm.getHeight() + offsetY;
    int offsetX = textArea.getCaretPosition()
        - textArea.getLineStartOffset(lineIndex);
    loc.x = textArea.offsetToX(lineIndex, offsetX);
    return loc;
  }

  public Rectangle getTextLocation(TextHitInfo offset) {
    Point caret = getCaretLocation();
    return getCaretRectangle(caret.x, caret.y);
  }

  private Rectangle getCaretRectangle(int x, int y) {
    TextAreaPainter painter = textArea.getPainter();
    Point origin = painter.getLocationOnScreen();
    int height = painter.getFontMetrics().getHeight();
    return new Rectangle(origin.x + x, origin.y + y, 0, height);
  }

  public TextHitInfo getLocationOffset(int x, int y) {
    return null;
  }

  public int getInsertPositionOffset() {
    if (isComposing) {
      isComposing = false;
    }
    return textArea.getCaretPosition();
  }

  public AttributedCharacterIterator getCommittedText(int beginIndex,
      int endIndex, AttributedCharacterIterator.Attribute[] attributes) {
    return (new AttributedString(textArea.getText(beginIndex, endIndex
        - beginIndex))).getIterator();
  }

  public int getCommittedTextLength() {
    return committed_count;
  }

  public AttributedCharacterIterator cancelLatestCommittedText(
      AttributedCharacterIterator.Attribute[] attributes) {
    return null;
  }

  public AttributedCharacterIterator getSelectedText(
      AttributedCharacterIterator.Attribute[] attributes) {
    return null;
  }

  public void inputMethodTextChanged(InputMethodEvent event) {
    composedTextLayout = null;
    AttributedCharacterIterator text = event.getText();
    committed_count = event.getCommittedCharacterCount();
    if (committed_count == 0) {
      if (text.getEndIndex() == 0) {
        caretPositionChanged(event);
        return;
      }
      if (text.getEndIndex() < text.getBeginIndex()) {
        caretPositionChanged(event);
        return;
      }
      isComposing = true;
      drawComposingText(text, committed_count);
      caretPositionChanged(event);
      return;
    }
    commitText(text, committed_count);
    isComposing = false;
    caretPositionChanged(event);
  }

  private void drawComposingText(AttributedCharacterIterator text, int count) {
    assert ((count == 0 && text.getEndIndex() > 0));
    Point textLocation = getCaretLocation();
    invalidateComposingLine(textArea.getPainter(), textLocation.x,
        textLocation.y);
    composedTextLayout = getTextLayout(text, count);
    composedTextLayout.draw((Graphics2D) (textArea.getPainter().getGraphics()),
        textLocation.x, textLocation.y);
  }

  private void invalidateComposingLine(TextAreaPainter painter, int x, int y) {
    Graphics gfx = painter.getGraphics();
    gfx.setColor(painter.lineHighlightColor);
    gfx.fillRect(x, y
        - (painter.getFontMetrics().getHeight() - COMPOSING_UNDERBAR_HEIGHT), //
        painter.getWidth(), painter.getFontMetrics().getHeight());
  }

  private void commitText(AttributedCharacterIterator text, int count) {
    char c;
    StringBuffer committing = new StringBuffer(count);
    for (c = text.first(); c != AttributedCharacterIterator.DONE && count > 0; c = text
        .next(), --count) {
      committing.append(c);
    }
    int caret = textArea.getCaretPosition();
    String committing_text = committing.toString();
    try {
      textArea.getDocument().insertString(caret, committing_text, null);
    } catch (BadLocationException e) {
      e.printStackTrace();
    }
  }

  private TextLayout getTextLayout(AttributedCharacterIterator text,
      int committed_count) {
    AttributedString composed = new AttributedString(text, committed_count,
        text.getEndIndex());
    Font font = textArea.getPainter().getFont();
    FontRenderContext context = ((Graphics2D) (textArea.getPainter()
        .getGraphics())).getFontRenderContext();
    composed.addAttribute(TextAttribute.FONT, font);
    TextLayout layout = new TextLayout(composed.getIterator(), context);
    return layout;
  }

  public void caretPositionChanged(InputMethodEvent event) {
    event.consume();
  }
}
