package processing.mode.java.preproc.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import processing.mode.java.preproc.issue.IssueLocation;
import processing.mode.java.preproc.issue.IssueLocationFactory;
import processing.mode.java.preproc.issue.IssueMessageSimplification;


public class IssueLocationFactoryTest {

  private String source;
  private IssueLocation issueLocation;

  @Before
  public void setup() {
    source = "//Test\n" +
        "noFill();\n" +
        "/**\n" +
        "**/\n" +
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
  public void getLineWithOffsetApplies() {
    issueLocation = IssueLocationFactory.getLineWithOffset(
        new IssueMessageSimplification("test message", true),
        15,
        0,
        source
    );

    Assert.assertEquals(5, issueLocation.getLine());
    Assert.assertEquals(20, issueLocation.getCharPosition());
  }

  @Test
  public void getLineWithOffsetNotApplies() {
    issueLocation = IssueLocationFactory.getLineWithOffset(
        new IssueMessageSimplification("test message", false),
        15,
        0,
        source
    );

    Assert.assertEquals(15, issueLocation.getLine());
    Assert.assertEquals(0, issueLocation.getCharPosition());
  }

  @Test
  public void getLineWithOffsetEndWhite() {
    issueLocation = IssueLocationFactory.getLineWithOffset(
        new IssueMessageSimplification("test message", true),
        14,
        0,
        "\n\n\n\n\n\n\n\n\n\n\nnoFill()\nellipse(50,50,50,50)\n"
    );

    Assert.assertEquals(13, issueLocation.getLine());
    Assert.assertEquals(20, issueLocation.getCharPosition());
  }

}