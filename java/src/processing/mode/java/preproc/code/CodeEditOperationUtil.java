package processing.mode.java.preproc.code;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStreamRewriter;
import processing.mode.java.pdex.TextTransform;


/**
 * Utility which generates and performs code edit operations.
 *
 * <p>
 *   Utility which generates and performs code edit operations, performing the edit immediately
 *   within a ANTLR rewriter but also generating a {TextTransform.Edit} for use with the JDT.
 * </p>
 */
public class CodeEditOperationUtil {

  /**
   * Delete a single token.
   *
   * @param start The token to be deleted.
   * @param rewriter The rewriter in which to immediately edit.
   * @return The {TextTransform.Edit} corresponding to this change.
   */
  public static TextTransform.Edit createDelete(Token start, TokenStreamRewriter rewriter) {
    rewriter.delete(start);
    return TextTransform.Edit.delete(start.getStartIndex(), start.getText().length());
  }

  /**
   * Delete tokens between a start end end token inclusive.
   *
   * @param start The token to be deleted.
   * @param stop The final token to be deleted.
   * @param rewriter The rewriter in which to immediately edit.
   * @return The {TextTransform.Edit} corresponding to this change.
   */
  public static TextTransform.Edit createDelete(Token start, Token stop,
        TokenStreamRewriter rewriter) {

    rewriter.delete(start, stop);

    int startIndex = start.getStartIndex();
    int length = stop.getStopIndex() - startIndex + 1;

    return TextTransform.Edit.delete(
        startIndex,
        length
    );
  }

  /**
   * Insert text after a token.
   *
   * @param start The position after which the text should be inserted.
   * @param text The text to insert.
   * @param rewriter The rewriter in which to immediately edit.
   * @return The {TextTransform.Edit} corresponding to this change.
   */
  public static TextTransform.Edit createInsertAfter(int start, String text,
        TokenStreamRewriter rewriter) {

    rewriter.insertAfter(start, text);

    return TextTransform.Edit.insert(
        start + 1,
        text
    );
  }

  /**
   * Insert text after a token.
   *
   * @param start The token after which the text should be inserted.
   * @param text The text to insert.
   * @param rewriter The rewriter in which to immediately edit.
   * @return The {TextTransform.Edit} corresponding to this change.
   */
  public static TextTransform.Edit createInsertAfter(Token start, String text,
        TokenStreamRewriter rewriter) {

    rewriter.insertAfter(start, text);

    return TextTransform.Edit.insert(
        start.getStopIndex() + 1,
        text
    );
  }

  /**
   * Insert text before a token.
   *
   * @param before Token before which the text should be inserted.
   * @param text The text to insert.
   * @param rewriter The rewriter in which to immediately edit.
   * @return The {TextTransform.Edit} corresponding to this change.
   */
  public static TextTransform.Edit createInsertBefore(Token before, String text,
        TokenStreamRewriter rewriter) {

    rewriter.insertBefore(before, text);

    return TextTransform.Edit.insert(
        before.getStartIndex(),
        text
    );
  }

  /**
   * Insert text before a position in code.
   *
   * @param before The location before which to insert the text.
   * @param text The text to insert.
   * @param rewriter The rewriter in which to immediately edit.
   * @return The {TextTransform.Edit} corresponding to this change.
   */
  public static TextTransform.Edit createInsertBefore(int before, String text,
        TokenStreamRewriter rewriter) {

    rewriter.insertBefore(before, text);

    return TextTransform.Edit.insert(
        before,
        text
    );
  }

}
