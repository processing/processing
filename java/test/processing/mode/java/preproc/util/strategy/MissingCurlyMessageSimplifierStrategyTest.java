package processing.mode.java.preproc.util.strategy;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;


public class MissingCurlyMessageSimplifierStrategyTest {

  private MissingCurlyMessageSimplifierStrategy strategy;

  @Before
  public void setup() {
    strategy = new MissingCurlyMessageSimplifierStrategy();
  }

  @Test
  public void testPresent() {
    Optional<String> msg = strategy.simplify("class Test {");
    Assert.assertTrue(msg.isPresent());
  }

  @Test
  public void testNotPresent() {
    Optional<String> msg = strategy.simplify("class Test { }");
    Assert.assertTrue(msg.isEmpty());
  }

}