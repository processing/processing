package processing.mode.java.preproc.util.strategy;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import processing.mode.java.preproc.issue.IssueMessageSimplification;
import processing.mode.java.preproc.issue.strategy.BadIdentifierMessageSimplifierStrategy;

import java.util.Optional;


public class BadIdentifierMessageSimplifierStrategyTest {

  private processing.mode.java.preproc.issue.strategy.BadIdentifierMessageSimplifierStrategy strategy;

  @Before
  public void setup() {
    strategy = new BadIdentifierMessageSimplifierStrategy();
  }

  @Test
  public void testPresent() {
    Optional<IssueMessageSimplification> msg = strategy.simplify("test(a,01a");
    Assert.assertTrue(msg.isPresent());
  }

  @Test
  public void testNotPresent() {
    Optional<IssueMessageSimplification> msg = strategy.simplify("class {");
    Assert.assertTrue(msg.isEmpty());
  }

}