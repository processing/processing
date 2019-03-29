package processing.mode.java.preproc.util.strategy;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;


public class BadIdentifierMessageSimplifierStrategyTest {

  private BadIdentifierMessageSimplifierStrategy strategy;

  @Before
  public void setup() {
    strategy = new BadIdentifierMessageSimplifierStrategy();
  }

  @Test
  public void testPresent() {
    Optional<String> msg = strategy.simplify("test(a,01a");
    Assert.assertTrue(msg.isPresent());
  }

  @Test
  public void testNotPresent() {
    Optional<String> msg = strategy.simplify("class {");
    Assert.assertTrue(msg.isEmpty());
  }

}