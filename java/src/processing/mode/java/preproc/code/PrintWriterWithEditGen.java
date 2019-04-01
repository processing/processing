package processing.mode.java.preproc.code;

import org.antlr.v4.runtime.TokenStreamRewriter;
import processing.mode.java.pdex.TextTransform;

import java.io.PrintWriter;

public class PrintWriterWithEditGen {

  private final TokenStreamRewriter writer;
  private final RewriteResultBuilder rewriteResultBuilder;
  private final int insertPoint;
  private final StringBuilder editBuilder;
  private final boolean before;

  public PrintWriterWithEditGen(TokenStreamRewriter writer,
        RewriteResultBuilder newRewriteResultBuilder, int newInsertPoint, boolean newBefore) {

    this.writer = writer;
    rewriteResultBuilder = newRewriteResultBuilder;
    insertPoint = newInsertPoint;
    editBuilder = new StringBuilder();
    before = newBefore;
  }

  public void addEmptyLine() {
    addCode("\n");
  }

  public void addCodeLine(String newCode) {
    addCode(newCode + "\n");
  }

  public void addCode(String newCode) {
    editBuilder.append(newCode);
  }

  public void finish() {
    String newCode = editBuilder.toString();

    if (before) {
      rewriteResultBuilder.addEdit(CodeEditOperationUtil.createInsertBefore(
          insertPoint,
          newCode,
          writer
      ));
    } else {
      rewriteResultBuilder.addEdit(CodeEditOperationUtil.createInsertAfter(
          insertPoint,
          newCode,
          writer
      ));
    }

    rewriteResultBuilder.addOffset(SyntaxUtil.getCount(newCode, "\n"));
  }

}
