package processing.mode.java.preproc.code;

import processing.mode.java.pdex.TextTransform;

import java.io.PrintWriter;

public class PrintWriterWithEditGen {

  private final PrintWriter writer;
  private final RewriteResultBuilder rewriteResultBuilder;
  private final int insertPoint;
  private final StringBuilder editBuilder;

  public PrintWriterWithEditGen(PrintWriter writer, RewriteResultBuilder newRewriteResultBuilder,
        int newInsertPoint) {

    this.writer = writer;
    rewriteResultBuilder = newRewriteResultBuilder;
    insertPoint = newInsertPoint;
    editBuilder = new StringBuilder();
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

    writer.print(newCode);

    rewriteResultBuilder.addEdit(TextTransform.Edit.insert(insertPoint, newCode));
    rewriteResultBuilder.addOffset(SyntaxUtil.getCount(newCode, "\n"));
  }

}
