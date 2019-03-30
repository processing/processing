package processing.mode.java.preproc.util.strategy;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import processing.mode.java.preproc.util.IssueMessageSimplification;

import java.util.Optional;

public class NonTermMessageSimplifierStrategyTest {

  private NonTermMessageSimplifierStrategy strategy;

  @Before
  public void setup() {
    strategy = new NonTermMessageSimplifierStrategy();
  }

  @Test
  public void testPresent() {
    Optional<IssueMessageSimplification> msg = strategy.simplify("int x = ((5 + 4) / 3");
    Assert.assertTrue(msg.isPresent());
  }

  @Test
  public void testNotPresent() {
    Optional<IssueMessageSimplification> msg = strategy.simplify("int x = y;");
    Assert.assertTrue(msg.isEmpty());
  }

}