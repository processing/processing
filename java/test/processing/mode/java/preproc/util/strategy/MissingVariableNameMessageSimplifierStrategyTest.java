package processing.mode.java.preproc.util.strategy;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;


public class MissingVariableNameMessageSimplifierStrategyTest {

  private MissingVariableNameMessageSimplifierStrategy strategy;

  @Before
  public void setup() {
    strategy = new MissingVariableNameMessageSimplifierStrategy();
  }

  @Test
  public void testPresent() {
    Optional<String> msg = strategy.simplify("char = '");
    Assert.assertTrue(msg.isPresent());
  }

  @Test
  public void testNotPresent() {
    Optional<String> msg = strategy.simplify("class test {");
    Assert.assertTrue(msg.isEmpty());
  }

}