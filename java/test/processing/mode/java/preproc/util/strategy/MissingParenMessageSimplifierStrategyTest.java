package processing.mode.java.preproc.util.strategy;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;


public class MissingParenMessageSimplifierStrategyTest {

  private MissingParenMessageSimplifierStrategy strategy;

  @Before
  public void setup() {
    strategy = new MissingParenMessageSimplifierStrategy();
  }

  @Test
  public void testPresent() {
    Optional<String> msg = strategy.simplify("int x = ((5 + 4) / 3");
    Assert.assertTrue(msg.isPresent());
  }

  @Test
  public void testNotPresent() {
    Optional<String> msg = strategy.simplify("int x = (y/5)/(\n4)");
    Assert.assertTrue(msg.isEmpty());
  }

}