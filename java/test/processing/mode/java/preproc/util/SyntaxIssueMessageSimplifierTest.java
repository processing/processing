package processing.mode.java.preproc.util;

import org.junit.Assert;
import org.junit.Test;


public class SyntaxIssueMessageSimplifierTest {

  @Test
  public void testAssignment() {
    String input = "List<ColoredCircle> =";
    String output = SyntaxIssueMessageSimplifier.get().simplify(input).getMessage();
    Assert.assertTrue(output.contains("assignment"));
  }

  @Test
  public void testBadIdentifier() {
    String input = "List<ColoredCircle> 9";
    String output = SyntaxIssueMessageSimplifier.get().simplify(input).getMessage();
    Assert.assertTrue(output.contains("starting with digit"));
  }

  @Test
  public void testBadParamLead() {
    String input = "x,";
    String output = SyntaxIssueMessageSimplifier.get().simplify(input).getMessage();
    Assert.assertTrue(output.contains("parameter"));
  }

  @Test
  public void testBadParamEnd() {
    String input = "colorGen),";
    String output = SyntaxIssueMessageSimplifier.get().simplify(input).getMessage();
    Assert.assertTrue(output.contains("parameter"));
  }

  @Test
  public void testCaret() {
    String input = "List<ColoredCircle circles";
    String output = SyntaxIssueMessageSimplifier.get().simplify(input).getMessage();
    Assert.assertTrue(output.contains("<"));
  }

  @Test
  public void testMissingIdentifier() {
    String input = "missing Identifier at '{'";
    String output = SyntaxIssueMessageSimplifier.get().simplify(input).getMessage();
    Assert.assertTrue(output.contains("identifier"));
  }

  @Test
  public void simplifyParen() {
    String input = "no viable alternative at input 'ellipse(\n\nellipse();'";
    String output = SyntaxIssueMessageSimplifier.get().simplify(input).getMessage();
    Assert.assertNotNull(output);
  }

  @Test
  public void simplifySemicolon() {
    String input = "no viable alternative at input 'ellipse(\n\nellipse())'";
    String output = SyntaxIssueMessageSimplifier.get().simplify(input).getMessage();
    Assert.assertNotNull(output);
  }

}