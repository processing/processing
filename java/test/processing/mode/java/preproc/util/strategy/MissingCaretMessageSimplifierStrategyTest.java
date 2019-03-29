package processing.mode.java.preproc.util.strategy;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;


public class MissingCaretMessageSimplifierStrategyTest {

  private MissingCaretMessageSimplifierStrategy strategy;

  @Before
  public void setup() {
    strategy = new MissingCaretMessageSimplifierStrategy();
  }

  @Test
  public void testPresent() {
    Optional<String> msg = strategy.simplify("class Test <a extends {");
    Assert.assertTrue(msg.isPresent());
  }

  @Test
  public void testNotPresent() {
    Optional<String> msg = strategy.simplify("class {");
    Assert.assertTrue(msg.isEmpty());
  }

}