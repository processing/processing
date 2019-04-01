package processing.mode.java.preproc.util.strategy;

import org.junit.Assert;
import org.junit.Test;
import processing.mode.java.preproc.issue.strategy.MessageSimplifierUtil;

public class MessageSimplifierUtilTest {

  @Test
  public void getOffendingAreaMatch() {
    String input = "no viable alternative at input 'ellipse(\n\nellipse();'";
    String output = processing.mode.java.preproc.issue.strategy.MessageSimplifierUtil.getOffendingArea(input);
    Assert.assertEquals("ellipse(ellipse();", output);
  }

  @Test
  public void getOffendingAreaNoMatch() {
    String input = "ambig at input 'ellipse(\n\nellipse();'";
    String output = MessageSimplifierUtil.getOffendingArea(input);
    Assert.assertEquals("ambig at input 'ellipse(\n\nellipse();'", output);
  }

}