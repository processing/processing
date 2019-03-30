package processing.mode.java.preproc.util.strategy;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import processing.mode.java.preproc.util.IssueMessageSimplification;

import java.util.Optional;


public class BadParamMessageSimplifierStrategyTest {

  private BadParamMessageSimplifierStrategy strategy;

  @Before
  public void setup() {
    strategy = new BadParamMessageSimplifierStrategy();
  }

  @Test
  public void testPresent() {
    Optional<IssueMessageSimplification> msg = strategy.simplify("void test (int x,\ny) \n{");
    Assert.assertTrue(msg.isPresent());
  }

  @Test
  public void testPresentUnderscore() {
    Optional<IssueMessageSimplification> msg = strategy.simplify("void test (int x,\ny_y) \n{");
    Assert.assertTrue(msg.isPresent());
  }

  @Test
  public void testPresentVarType() {
    Optional<IssueMessageSimplification> msg = strategy.simplify("void test (int x,\nint) \n{");
    Assert.assertTrue(msg.isPresent());
  }

  @Test
  public void testNotPresent() {
    Optional<IssueMessageSimplification> msg = strategy.simplify("int x = y");
    Assert.assertTrue(msg.isEmpty());
  }

}