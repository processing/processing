package processing.mode.java.preproc.code;

import processing.mode.java.pdex.TextTransform;

import java.util.List;

public class RewriteResult {

  private final int lineOffset;
  private final List<TextTransform.Edit> edits;

  public RewriteResult(int newLineOffset, List<TextTransform.Edit> newEdits) {
    lineOffset = newLineOffset;
    edits = newEdits;
  }

  public int getLineOffset() {
    return lineOffset;
  }

  public List<TextTransform.Edit> getEdits() {
    return edits;
  }

}
