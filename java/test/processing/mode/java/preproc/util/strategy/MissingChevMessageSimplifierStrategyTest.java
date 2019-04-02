package processing.mode.java.preproc.util.strategy;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import processing.mode.java.preproc.issue.IssueMessageSimplification;
import processing.mode.java.preproc.issue.strategy.MissingChevMessageSimplifierStrategy;

import java.util.Optional;


public class MissingChevMessageSimplifierStrategyTest {

  private processing.mode.java.preproc.issue.strategy.MissingChevMessageSimplifierStrategy strategy;

  @Before
  public void setup() {
    strategy = new MissingChevMessageSimplifierStrategy();
  }

  @Test
  public void testPresent() {
    Optional<IssueMessageSimplification> msg = strategy.simplify("class Test <a extends {");
    Assert.assertTrue(msg.isPresent());
  }

  @Test
  public void testNotPresent() {
    Optional<IssueMessageSimplification> msg = strategy.simplify("class {");
    Assert.assertTrue(msg.isEmpty());
  }

}