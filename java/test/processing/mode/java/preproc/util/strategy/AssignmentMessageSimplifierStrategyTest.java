package processing.mode.java.preproc.util.strategy;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import processing.mode.java.preproc.util.IssueMessageSimplification;

import java.util.Optional;


public class AssignmentMessageSimplifierStrategyTest {

  private AssignmentMessageSimplifierStrategy strategy;

  @Before
  public void setup() {
    strategy = new AssignmentMessageSimplifierStrategy();
  }

  @Test
  public void testPresent() {
    Optional<IssueMessageSimplification> msg = strategy.simplify("  int x =");
    Assert.assertTrue(msg.isPresent());
  }

  @Test
  public void testPresentDiamond() {
    Optional<IssueMessageSimplification> msg = strategy.simplify("  List<Integer> x =");
    Assert.assertTrue(msg.isPresent());
  }

  @Test
  public void testNotPresent() {
    Optional<IssueMessageSimplification> msg = strategy.simplify("class {");
    Assert.assertTrue(msg.isEmpty());
  }

}