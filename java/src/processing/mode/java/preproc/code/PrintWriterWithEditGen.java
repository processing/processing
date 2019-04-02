package processing.mode.java.preproc.code;

import org.antlr.v4.runtime.TokenStreamRewriter;


/**
 * Decorator around a {TokenStreamRewriter}.
 *
 * <p>
 *   Decorator around a {TokenStreamRewriter} which converts input commands into something that the
 *   rewriter can understand but also generates edits saved to an input RewriteResultBuilder.
 *   Requires a call to finish() after completion of preprocessing.
 * </p>
 */
public class PrintWriterWithEditGen {

  private final TokenStreamRewriter writer;
  private final RewriteResultBuilder rewriteResultBuilder;
  private final int insertPoint;
  private final StringBuilder editBuilder;
  private final boolean before;

  /**
   * Create a new edit generator decorator.
   *
   * @param writer The writer to which edits should be immediately made.
   * @param newRewriteResultBuilder The builder to which edits should be saved.
   * @param newInsertPoint The point at which new values should be inserted.
   * @param newBefore If true, the values will be inserted before the given insert point. If false,
   *    will, insert after the insertion point.
   */
  public PrintWriterWithEditGen(TokenStreamRewriter writer,
        RewriteResultBuilder newRewriteResultBuilder, int newInsertPoint, boolean newBefore) {

    this.writer = writer;
    rewriteResultBuilder = newRewriteResultBuilder;
    insertPoint = newInsertPoint;
    editBuilder = new StringBuilder();
    before = newBefore;
  }

  /**
   * Add an empty line into the code.
   */
  public void addEmptyLine() {
    addCode("\n");
  }

  /**
   * Add code with a newline automatically appended.
   *
   * @param newCode The code to add.
   */
  public void addCodeLine(String newCode) {
    addCode(newCode + "\n");
  }

  /**
   * Add code without a new line.
   *
   * @param newCode The code to add.
   */
  public void addCode(String newCode) {
    editBuilder.append(newCode);
  }

  /**
   * Finalize edits made through this decorator.
   */
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
