package processing.mode.java.preproc.util.strategy;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import processing.mode.java.preproc.issue.IssueMessageSimplification;
import processing.mode.java.preproc.issue.strategy.MissingCurlyMessageSimplifierStrategy;

import java.util.Optional;


public class MissingCurlyMessageSimplifierStrategyTest {

  private processing.mode.java.preproc.issue.strategy.MissingCurlyMessageSimplifierStrategy strategy;

  @Before
  public void setup() {
    strategy = new MissingCurlyMessageSimplifierStrategy();
  }

  @Test
  public void testPresent() {
    Optional<IssueMessageSimplification> msg = strategy.simplify("class Test {");
    Assert.assertTrue(msg.isPresent());
  }

  @Test
  public void testNotPresent() {
    Optional<IssueMessageSimplification> msg = strategy.simplify("class Test { }");
    Assert.assertTrue(msg.isEmpty());
  }

}