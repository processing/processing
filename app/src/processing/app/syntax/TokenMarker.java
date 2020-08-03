/*
 * TokenMarker.java - Generic token marker
 * Copyright (C) 1998, 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

package processing.app.syntax;

import javax.swing.text.Segment;

/**
 * A token marker that splits lines of text into tokens. Each token carries
 * a length field and an indentification tag that can be mapped to a color
 * for painting that token.<p>
 * <p>
 * For performance reasons, the linked list of tokens is reused after each
 * line is tokenized. Therefore, the return value of <code>markTokens</code>
 * should only be used for immediate painting. Notably, it cannot be
 * cached.
 *
 * @author Slava Pestov
 */
public abstract class TokenMarker {

  public interface TokenListener {
    void addToken(int length, byte id);
  }

  // Only needed during markTokensImpl() call so addToken() can be forwarded
  private TokenListener tokenListener = null;

  public final void setTokenListener(TokenListener listener) {
    this.tokenListener = listener;
  }

  public final TokenMarkerState createStateInstance() {
    return new TokenMarkerState(this);
  }

  /**
   * Creates a new <code>TokenMarker</code>. This DOES NOT create
   * a lineInfo array; an initial call to <code>insertLines()</code>
   * does that.
   */
  protected TokenMarker() { }

  abstract public void addColoring(String keyword, String coloring);

  /**
   * An abstract method that splits a line up into tokens. It
   * should parse the line, and call <code>addToken()</code> to
   * add syntax tokens to the token list. Then, it should return
   * the initial token type for the next line.<p>
   * <p>
   * For example if the current line contains the start of a
   * multiline comment that doesn't end on that line, this method
   * should return the comment token type so that it continues on
   * the next line.
   *
   * @param token     The initial token type for this line
   * @param line      The line to be tokenized
   * @param lineIndex The index of the line in the document,
   *                  starting at 0
   * @return The initial token type for the next line
   */
  protected abstract byte markTokensImpl(byte token, Segment line,
                                         int lineIndex);

  protected final void addToken(int length, byte id) {
    if (tokenListener != null) {
      tokenListener.addToken(length, id);
    }
  }

  /**
   * Returns if the token marker supports tokens that span multiple
   * lines. If this is true, the object using this token marker is
   * required to pass all lines in the document to the
   * <code>markTokens()</code> method (in turn).<p>
   * <p>
   * The default implementation returns true; it should be overridden
   * to return false on simpler token markers for increased speed.
   */
  public boolean supportsMultilineTokens() {
    return true;
  }
}
