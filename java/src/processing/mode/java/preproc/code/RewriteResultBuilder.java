package processing.mode.java.preproc.code;

import processing.mode.java.pdex.TextTransform;

import java.util.ArrayList;
import java.util.List;


/**
 * Builder to help create a {RewriteResult}.
 */
public class RewriteResultBuilder {

  private int lineOffset;
  private List<TextTransform.Edit> edits;

  /**
   * Create a new rewrite result builder.
   */
  public RewriteResultBuilder() {
    lineOffset = 0;
    edits = new ArrayList<>();
  }

  /**
   * Indicate that lines were added to the sketch.
   *
   * @param offset By how much to change the current offset.
   */
  public void addOffset(int offset) {
    lineOffset += offset;
  }

  /**
   * Record an edit made during rewrite.
   *
   * @param edit The edit made.
   */
  public void addEdit(TextTransform.Edit edit) {
    edits.add(edit);
  }

  /**
   * Get the number of lines written.
   *
   * @return The offset to add to current preprocessor offset.
   */
  public int getLineOffset() {
    return lineOffset;
  }

  /**
   * Get the edits generated during rewrite.
   *
   * @return The edits generated during rewrite.
   */
  public List<TextTransform.Edit> getEdits() {
    return edits;
  }

  /**
   * Build a new rewrite result.
   *
   * @return Immutable rewrite result.
   */
  public RewriteResult build() {
    return new RewriteResult(lineOffset, edits);
  }
}
