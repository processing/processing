package processing.mode.java.preproc.util.strategy;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class MissingSingleQuoteMessageSimplifierStrategyTest {

  private MissingSingleQuoteMessageSimplifierStrategy strategy;

  @Before
  public void setup() {
    strategy = new MissingSingleQuoteMessageSimplifierStrategy();
  }

  @Test
  public void testPresent() {
    Optional<String> msg = strategy.simplify("char x = '");
    Assert.assertTrue(msg.isPresent());
  }

  @Test
  public void testNotPresent() {
    Optional<String> msg = strategy.simplify("char x = '\\''");
    Assert.assertTrue(msg.isEmpty());
  }

}