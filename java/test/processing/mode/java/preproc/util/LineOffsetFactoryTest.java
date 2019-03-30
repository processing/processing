package processing.mode.java.preproc.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class LineOffsetFactoryTest {

  private String source;
  private LineOffset lineOffset;

  @Before
  public void setup() {
    source = "//Test\n" +
        "noFill();\n" +
        "ellipse(50,50,50,50)\n" +
        "\n" +
        "/**\n" +
        "Test\n" +
        "* Test\n" +
        "** Test\n" +
        "*/\n" +
        "\n" +
        "// Testing\n" +
        "\n";
  }

  @Test
  public void getInitialized() {
    Assert.assertNotNull(LineOffsetFactory.get());
  }

  /*@Test
  public void getLineWithOffsetApplies() {
    lineOffset = LineOffsetFactory.get().getLineWithOffset(
        new IssueMessageSimplification("test message", true),
        13,
        0,
        source
    );

    Assert.assertEquals(3, lineOffset.getLine());
    Assert.assertEquals(20, lineOffset.getCharPosition());
  }

  @Test
  public void getLineWithOffsetNotApplies() {
    lineOffset = LineOffsetFactory.get().getLineWithOffset(
        new IssueMessageSimplification("test message", false),
        13,
        0,
        source
    );

    Assert.assertEquals(13, lineOffset.getLine());
    Assert.assertEquals(0, lineOffset.getCharPosition());
  }

  @Test
  public void getLineWithOffsetNoMatch() {
    lineOffset = LineOffsetFactory.get().getLineWithOffset(
        new IssueMessageSimplification("test message", true),
        13,
        0,
        "\n\n\n\n\n\n\n\n\n\n\nnoFill()\nellipse(50,50,50,50)"
    );

    Assert.assertEquals(12, lineOffset.getLine());
    Assert.assertEquals(8, lineOffset.getCharPosition());
  }*/

}