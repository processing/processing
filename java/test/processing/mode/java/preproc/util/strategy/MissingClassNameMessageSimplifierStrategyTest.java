package processing.mode.java.preproc.util.strategy;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;


public class MissingClassNameMessageSimplifierStrategyTest {

  private MissingClassNameMessageSimplifierStrategy strategy;

  @Before
  public void setup() {
    strategy = new MissingClassNameMessageSimplifierStrategy();
  }

  @Test
  public void testPresentExtends() {
    Optional<String> msg = strategy.simplify("class extends Base\n{");
    Assert.assertTrue(msg.isPresent());
  }

  @Test
  public void testPresentNoExtends() {
    Optional<String> msg = strategy.simplify("class \n{");
    Assert.assertTrue(msg.isPresent());
  }

  @Test
  public void testNotPresent() {
    Optional<String> msg = strategy.simplify("int x = y");
    Assert.assertTrue(msg.isEmpty());
  }

}