package processing.mode.java.preproc.util.strategy;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import processing.mode.java.preproc.issue.IssueMessageSimplification;
import processing.mode.java.preproc.issue.strategy.MismatchedInputMessageSimplifierStrategy;

import java.util.Optional;


public class MismatchedInputMessageSimplifierStrategyTest {

  private processing.mode.java.preproc.issue.strategy.MismatchedInputMessageSimplifierStrategy strategy;

  @Before
  public void setup() {
    strategy = new MismatchedInputMessageSimplifierStrategy();
  }

  @Test
  public void testPresent() {
    Optional<IssueMessageSimplification> msg = strategy.simplify("mismatched input 'final' expecting {'instanceof', ';', ',', '.', '>', '<', '==', '<=', '>=', '!=', '&&', '||', '++', '--', '+', '-', '*', '/', '&', '|', '^', '%', '::'}");
    Assert.assertTrue(msg.isPresent());
  }

  @Test
  public void testNotPresent() {
    Optional<IssueMessageSimplification> msg = strategy.simplify("String x = \" \\\" \"");
    Assert.assertTrue(msg.isEmpty());
  }

}