package processing.mode.java.preproc.util.strategy;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;


public class MissingMethodNameMessageSimplifierStrategyTest {

  private MissingMethodNameMessageSimplifierStrategy strategy;

  @Before
  public void setup() {
    strategy = new MissingMethodNameMessageSimplifierStrategy();
  }

  @Test
  public void testPresent() {
    Optional<String> msg = strategy.simplify("void (int x) \n{");
    Assert.assertTrue(msg.isPresent());
  }

  @Test
  public void testPresentUnderscore() {
    Optional<String> msg = strategy.simplify("void (int x_y) \n{");
    Assert.assertTrue(msg.isPresent());
  }

  @Test
  public void testNotPresent() {
    Optional<String> msg = strategy.simplify("int x = y");
    Assert.assertTrue(msg.isEmpty());
  }

}