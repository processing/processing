package processing.mode.java.preproc.util.strategy;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;


public class MissingParamTypeMessageSimplifierStrategyTest {

  private MissingParamTypeMessageSimplifierStrategy strategy;

  @Before
  public void setup() {
    strategy = new MissingParamTypeMessageSimplifierStrategy();
  }

  @Test
  public void testPresent() {
    Optional<String> msg = strategy.simplify("void test (int x,\ny) \n{");
    Assert.assertTrue(msg.isPresent());
  }

  @Test
  public void testPresentUnderscore() {
    Optional<String> msg = strategy.simplify("void test (int x,\ny_y) \n{");
    Assert.assertTrue(msg.isPresent());
  }

  @Test
  public void testPresentVarType() {
    Optional<String> msg = strategy.simplify("void test (int x,\nint) \n{");
    Assert.assertTrue(msg.isPresent());
  }

  @Test
  public void testNotPresent() {
    Optional<String> msg = strategy.simplify("int x = y");
    Assert.assertTrue(msg.isEmpty());
  }

}