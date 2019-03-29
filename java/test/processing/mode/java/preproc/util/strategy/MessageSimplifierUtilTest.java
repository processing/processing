package processing.mode.java.preproc.util.strategy;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class MessageSimplifierUtilTest {

  @Test
  public void getOffendingAreaMatch() {
    String input = "no viable alternative at input 'ellipse(\n\nellipse();'";
    String output = MessageSimplifierUtil.getOffendingArea(input);
    Assert.assertEquals("ellipse(ellipse();", output);
  }

  @Test
  public void getOffendingAreaNoMatch() {
    String input = "ambig at input 'ellipse(\n\nellipse();'";
    String output = MessageSimplifierUtil.getOffendingArea(input);
    Assert.assertEquals("ambig at input 'ellipse(\n\nellipse();'", output);
  }

  @Test
  public void getCountPresent() {
    String input = "test1,test2\n,test3";
    int count = MessageSimplifierUtil.getCount(input, ",");
    Assert.assertEquals(2, count);
  }

  @Test
  public void getCountNotPresent() {
    String input = "test1 test2 test3";
    int count = MessageSimplifierUtil.getCount(input, ",");
    Assert.assertEquals(0, count);
  }

}