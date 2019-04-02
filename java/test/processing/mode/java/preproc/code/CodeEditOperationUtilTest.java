package processing.mode.java.preproc.code;

import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import processing.mode.java.pdex.TextTransform;

import static org.junit.Assert.*;


public class CodeEditOperationUtilTest {

  private TokenStreamRewriter tokenStreamRewriter;
  private Token sampleStart;
  private Token sampleEnd;

  @Before
  public void setUp() {
    tokenStreamRewriter = Mockito.mock(TokenStreamRewriter.class);

    sampleStart = Mockito.mock(Token.class);
    Mockito.when(sampleStart.getStartIndex()).thenReturn(5);
    Mockito.when(sampleStart.getText()).thenReturn("test");

    sampleEnd = Mockito.mock(Token.class);
    Mockito.when(sampleEnd.getStartIndex()).thenReturn(10);
    Mockito.when(sampleEnd.getText()).thenReturn("testing");
  }

  @Test
  public void createDeleteSingle() {
    TextTransform.Edit edit = CodeEditOperationUtil.createDelete(sampleStart, tokenStreamRewriter);
    Assert.assertNotNull(edit);
    Mockito.verify(tokenStreamRewriter).delete(sampleStart);
  }

  @Test
  public void createDeleteRange() {
    TextTransform.Edit edit = CodeEditOperationUtil.createDelete(
        sampleStart,
        sampleEnd,
        tokenStreamRewriter
    );

    Assert.assertNotNull(edit);
    Mockito.verify(tokenStreamRewriter).delete(sampleStart, sampleEnd);
  }

  @Test
  public void createInsertAfterLocation() {
    TextTransform.Edit edit = CodeEditOperationUtil.createInsertAfter(
        5,
        "text",
        tokenStreamRewriter
    );

    Assert.assertNotNull(edit);
    Mockito.verify(tokenStreamRewriter).insertAfter(5, "text");
  }

  @Test
  public void createInsertAfterToken() {
    TextTransform.Edit edit = CodeEditOperationUtil.createInsertAfter(
        sampleStart,
        "text",
        tokenStreamRewriter
    );

    Assert.assertNotNull(edit);
    Mockito.verify(tokenStreamRewriter).insertAfter(sampleStart, "text");
  }

  @Test
  public void createInsertBeforeToken() {
    TextTransform.Edit edit = CodeEditOperationUtil.createInsertBefore(
        sampleStart,
        "text",
        tokenStreamRewriter
    );

    Assert.assertNotNull(edit);
    Mockito.verify(tokenStreamRewriter).insertBefore(sampleStart, "text");
  }

  @Test
  public void createInsertBeforeLocation() {
    TextTransform.Edit edit = CodeEditOperationUtil.createInsertBefore(
        5,
        "text",
        tokenStreamRewriter
    );

    Assert.assertNotNull(edit);
    Mockito.verify(tokenStreamRewriter).insertBefore(5, "text");
  }

}