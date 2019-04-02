package processing.mode.java.preproc.util.strategy;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import processing.mode.java.preproc.issue.IssueMessageSimplification;
import processing.mode.java.preproc.issue.strategy.MissingVariableNameMessageSimplifierStrategy;

import java.util.Optional;


public class MissingVariableNameMessageSimplifierStrategyTest {

  private processing.mode.java.preproc.issue.strategy.MissingVariableNameMessageSimplifierStrategy strategy;

  @Before
  public void setup() {
    strategy = new MissingVariableNameMessageSimplifierStrategy();
  }

  @Test
  public void testPresent() {
    Optional<IssueMessageSimplification> msg = strategy.simplify("char = '");
    Assert.assertTrue(msg.isPresent());
  }

  @Test
  public void testNotPresent() {
    Optional<IssueMessageSimplification> msg = strategy.simplify("class test {");
    Assert.assertTrue(msg.isEmpty());
  }

}