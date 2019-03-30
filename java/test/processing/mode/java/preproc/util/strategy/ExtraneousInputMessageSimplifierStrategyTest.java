package processing.mode.java.preproc.util.strategy;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import processing.mode.java.preproc.util.IssueMessageSimplification;

import java.util.Optional;


public class ExtraneousInputMessageSimplifierStrategyTest {

  private ExtraneousInputMessageSimplifierStrategy strategy;

  @Before
  public void setup() {
    strategy = new ExtraneousInputMessageSimplifierStrategy();
  }

  @Test
  public void testPresent() {
    Optional<IssueMessageSimplification> msg = strategy.simplify("extraneous input 'test' expecting ';'");
    Assert.assertTrue(msg.isPresent());
  }

  @Test
  public void testNotPresent() {
    Optional<IssueMessageSimplification> msg = strategy.simplify("String x = \" \\\" \"");
    Assert.assertTrue(msg.isEmpty());
  }

}