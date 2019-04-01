package processing.mode.java.preproc.util.strategy;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import processing.mode.java.preproc.issue.IssueMessageSimplification;
import processing.mode.java.preproc.issue.strategy.MissingMethodNameMessageSimplifierStrategy;

import java.util.Optional;


public class MissingMethodNameMessageSimplifierStrategyTest {

  private processing.mode.java.preproc.issue.strategy.MissingMethodNameMessageSimplifierStrategy strategy;

  @Before
  public void setup() {
    strategy = new MissingMethodNameMessageSimplifierStrategy();
  }

  @Test
  public void testPresent() {
    Optional<IssueMessageSimplification> msg = strategy.simplify("void (int x) \n{");
    Assert.assertTrue(msg.isPresent());
  }

  @Test
  public void testPresentNoSpace() {
    Optional<IssueMessageSimplification> msg = strategy.simplify("test(int x) \n{");
    Assert.assertTrue(msg.isPresent());
  }

  @Test
  public void testPresentUnderscore() {
    Optional<IssueMessageSimplification> msg = strategy.simplify("void (int x_y) \n{");
    Assert.assertTrue(msg.isPresent());
  }

  @Test
  public void testNotPresent() {
    Optional<IssueMessageSimplification> msg = strategy.simplify("int x = y");
    Assert.assertTrue(msg.isEmpty());
  }

}