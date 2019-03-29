package processing.mode.java.preproc.util.strategy;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class SemicolonMessageSimplifierStrategyTest {

  private SemicolonMessageSimplifierStrategy strategy;

  @Before
  public void setup() {
    strategy = new SemicolonMessageSimplifierStrategy();
  }

  @Test
  public void testPresent() {
    Optional<String> msg = strategy.simplify("int x = ((5 + 4) / 3");
    Assert.assertTrue(msg.isPresent());
  }

  @Test
  public void testNotPresent() {
    Optional<String> msg = strategy.simplify("int x = y;");
    Assert.assertTrue(msg.isEmpty());
  }

}