package processing.mode.java.preproc.util.strategy;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;


public class MissingDoubleQuoteMessageSimplifierStrategyTest {

  private MissingDoubleQuoteMessageSimplifierStrategy strategy;

  @Before
  public void setup() {
    strategy = new MissingDoubleQuoteMessageSimplifierStrategy();
  }

  @Test
  public void testPresent() {
    Optional<String> msg = strategy.simplify("String x = \" \" \"");
    Assert.assertTrue(msg.isPresent());
  }

  @Test
  public void testNotPresent() {
    Optional<String> msg = strategy.simplify("String x = \" \\\" \"");
    Assert.assertTrue(msg.isEmpty());
  }

}