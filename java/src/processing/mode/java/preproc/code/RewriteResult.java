package processing.mode.java.preproc.code;

import processing.mode.java.pdex.TextTransform;

import java.util.List;


/**
 * Data structure describing the result of preprocessor rewrite.
 */
public class RewriteResult {

  private final int lineOffset;
  private final List<TextTransform.Edit> edits;

  /**
   * Create a new rewrite result structure.
   *
   * @param newLineOffset The number of lines added during rewrite.
   * @param newEdits The edits generated during rewrite.
   */
  public RewriteResult(int newLineOffset, List<TextTransform.Edit> newEdits) {
    lineOffset = newLineOffset;
    edits = newEdits;
  }

  /**
   * Get the number of lines added during rewrite.
   *
   * @return The additional offset to add to the preprocessor line offset.
   */
  public int getLineOffset() {
    return lineOffset;
  }

  /**
   * Get the edits generated during rewrite.
   *
   * @return Edits generated during rewrite.
   */
  public List<TextTransform.Edit> getEdits() {
    return edits;
  }

}
