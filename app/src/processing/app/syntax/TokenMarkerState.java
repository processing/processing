package processing.app.syntax;

import javax.swing.text.Segment;

/**
 * This class serves only as a workaround to preserve API and should be removed
 * in the next major version. Base TokenMarker which serves as superclass for
 * token markes for various modes is stateful, but single instance was shared
 * between all tabs and Editors. This caused inherent bugs by leaking state
 * between contexts.
 *
 * TokenMarker subclasses now serve two purposes: they keep keyword list and
 * they override markTokensImpl to provide the marking logic.
 *
 * Since each tab and Editor should have it's own token marker state, I extracted
 * most of the fields and associated metods working with them from TokenMarker
 * into this class, and allowed TokenMarker to create instances of this class
 * when requested.
 *
 * The way marking logic is handled is unfortunate, since markTokensImpl is
 * expected to call addToken() of TokenMarker superclass instead of – for
 * example - returning a List of tokens. I worked around this by plugging in
 * TokenMarkerState instance as listener before markTokensImpl is called.
 * This behavior is safe since TokenMarker is always operated from Event
 * Dispatch Thread and no multithreading is involved.
 *
 * This allows having only single instance of TokenMarker in a way it was
 * intended before while keeping state separate for each tab.
 *
 * In the next major version TokenMarker shound be redesigned with following
 * requirements in mind:
 * - Single instance of keyword list and other common data, initialized by Mode
 * - Each tab should have its own instance of TokenMarker containing its state
 * - Support multiple flavors for different doc types
 * - Other modes should provide logic in a way which is compatible with
 *     multiple states (pure function? Function object?).
 *     Currently state, logic and keywords list are tied together into one
 *     TokenMarker instance, which leads to need for this workaround.
 */
public class TokenMarkerState {

  protected TokenMarker marker;

  protected TokenMarkerState(TokenMarker marker) {
    this.marker = marker;
  }

  /**
   * The first token in the list. This should be used as the return
   * value from <code>markTokens()</code>.
   */
  protected Token firstToken;

  /**
   * The last token in the list. New tokens are added here.
   * This should be set to null before a new line is to be tokenized.
   */
  protected Token lastToken;

  /**
   * An array for storing information about lines. It is enlarged and
   * shrunk automatically by the <code>insertLines()</code> and
   * <code>deleteLines()</code> methods.
   */
  protected byte[] lineInfo;

  /**
   * The number of lines in the model being tokenized. This can be
   * less than the length of the <code>lineInfo</code> array.
   */
  protected int length;

  /**
   * The last tokenized line.
   */
  protected int lastLine = -1;

  /**
   * True if the next line should be painted.
   */
  protected boolean nextLineRequested;

  /**
   * A wrapper for the lower-level <code>markTokensImpl</code> method
   * that is called to split a line up into tokens.
   *
   * @param line      The line
   * @param lineIndex The line number
   */
  public Token markTokens(Segment line, int lineIndex) {
    if (lineIndex >= length) {
      throw new IllegalArgumentException("Tokenizing invalid line: "
                                             + lineIndex);
    }

    marker.setTokenListener(this::addToken);

    lastToken = null;

    byte prev = (lineIndex == 0) ? Token.NULL : lineInfo[lineIndex - 1];

    byte oldToken = lineInfo[lineIndex];
    byte token = marker.markTokensImpl(prev, line, lineIndex);

    marker.setTokenListener(null);

    lineInfo[lineIndex] = token;

    /*
     * This is a foul hack. It stops nextLineRequested
     * from being cleared if the same line is marked twice.
     *
     * Why is this necessary? It's all JEditTextArea's fault.
     * When something is inserted into the text, firing a
     * document event, the insertUpdate() method shifts the
     * caret (if necessary) by the amount inserted.
     *
     * All caret movement is handled by the select() method,
     * which eventually pipes the new position to scrollTo()
     * and calls repaint().
     *
     * Note that at this point in time, the new line hasn't
     * yet been painted; the caret is moved first.
     *
     * scrollTo() calls offsetToX(), which tokenizes the line
     * unless it is being called on the last line painted
     * (in which case it uses the text area's painter cached
     * token list). What scrollTo() does next is irrelevant.
     *
     * After scrollTo() has done it's job, repaint() is
     * called, and eventually we end up in paintLine(), whose
     * job is to paint the changed line. It, too, calls
     * markTokens().
     *
     * The problem was that if the line started a multiline
     * token, the first markTokens() (done in offsetToX())
     * would set nextLineRequested (because the line end
     * token had changed) but the second would clear it
     * (because the line was the same that time) and therefore
     * paintLine() would never know that it needed to repaint
     * subsequent lines.
     *
     * This bug took me ages to track down, that's why I wrote
     * all the relevant info down so that others wouldn't
     * duplicate it.
     */
    if (!(lastLine == lineIndex && nextLineRequested)) {
      nextLineRequested = (oldToken != token);
    }

    lastLine = lineIndex;

    addToken(0, Token.END);

    return firstToken;
  }

  /**
   * Informs the token marker that lines have been inserted into
   * the document. This inserts a gap in the <code>lineInfo</code>
   * array.
   *
   * @param index The first line number
   * @param lines The number of lines
   */
  public void insertLines(int index, int lines) {
    if (lines <= 0)
      return;
    length += lines;
    ensureCapacity(length);
    int len = index + lines;
    System.arraycopy(lineInfo, index, lineInfo, len,
                     lineInfo.length - len);

    for (int i = index + lines - 1; i >= index; i--) {
      lineInfo[i] = Token.NULL;
    }
  }

  /**
   * Informs the token marker that line have been deleted from
   * the document. This removes the lines in question from the
   * <code>lineInfo</code> array.
   *
   * @param index The first line number
   * @param lines The number of lines
   */
  public void deleteLines(int index, int lines) {
    if (lines <= 0)
      return;
    int len = index + lines;
    length -= lines;
    System.arraycopy(lineInfo, len, lineInfo,
                     index, lineInfo.length - len);
  }

  /**
   * Returns the number of lines in this token marker.
   */
  public int getLineCount() {
    return length;
  }

  /**
   * Returns true if the next line should be repainted. This
   * will return true after a line has been tokenized that starts
   * a multiline token that continues onto the next line.
   */
  public boolean isNextLineRequested() {
    return nextLineRequested;
  }

  /**
   * Ensures that the <code>lineInfo</code> array can contain the
   * specified index. This enlarges it if necessary. No action is
   * taken if the array is large enough already.<p>
   * <p>
   * It should be unnecessary to call this under normal
   * circumstances; <code>insertLine()</code> should take care of
   * enlarging the line info array automatically.
   *
   * @param index The array index
   */
  protected void ensureCapacity(int index) {
    if (lineInfo == null) {
      lineInfo = new byte[index + 1];
    } else if (lineInfo.length <= index) {
      byte[] lineInfoN = new byte[(index + 1) * 2];
      System.arraycopy(lineInfo, 0, lineInfoN, 0,
                       lineInfo.length);
      lineInfo = lineInfoN;
    }
  }

  /**
   * Adds a token to the token list.
   *
   * @param length The length of the token
   * @param id     The id of the token
   */
  protected void addToken(int length, byte id) {
    if (id >= Token.INTERNAL_FIRST && id <= Token.INTERNAL_LAST) {
      throw new InternalError("Invalid id: " + id);
    }

    if (length == 0 && id != Token.END) {
      return;
    }

    if (firstToken == null) {
      firstToken = new Token(length, id);
      lastToken = firstToken;
    } else if (lastToken == null) {
      lastToken = firstToken;
      firstToken.length = length;
      firstToken.id = id;
    } else if (lastToken.next == null) {
      lastToken.next = new Token(length, id);
      lastToken = lastToken.next;
    } else {
      lastToken = lastToken.next;
      lastToken.length = length;
      lastToken.id = id;
    }
  }
}
