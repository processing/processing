package processing.mode.java.preproc.util;

import org.junit.Assert;
import org.junit.Test;


public class SyntaxIssueMessageSimplifierTest {

  @Test
  public void simplifyParen() {
    String input = "no viable alternative at input 'ellipse(\n\nellipse();'";
    String output = SyntaxIssueMessageSimplifier.get().simplify(input);
    Assert.assertNotNull(output);
  }

  @Test
  public void simplifySemicolon() {
    String input = "no viable alternative at input 'ellipse(\n\nellipse())'";
    String output = SyntaxIssueMessageSimplifier.get().simplify(input);
    Assert.assertNotNull(output);
  }

}