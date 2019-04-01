package processing.mode.java.preproc.code;

import processing.mode.java.pdex.TextTransform;

import java.util.ArrayList;
import java.util.List;

public class RewriteResultBuilder {

  private int lineOffset;
  private List<TextTransform.Edit> edits;

  public RewriteResultBuilder() {
    lineOffset = 0;
    edits = new ArrayList<>();
  }

  public void addOffset(int offset) {
    lineOffset += offset;
  }

  public void addEdit(TextTransform.Edit edit) {
    edits.add(edit);
  }

  public int getLineOffset() {
    return lineOffset;
  }

  public List<TextTransform.Edit> getEdits() {
    return edits;
  }

  public RewriteResult build() {
    return new RewriteResult(lineOffset, edits);
  }
}
