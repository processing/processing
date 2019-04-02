package processing.mode.java.pdex.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class TabLineFactoryTest {

  private List<Integer> starts;

  @Before
  public void setUp() {
    starts = new ArrayList<>();
    starts.add(0);
    starts.add(5);
    starts.add(10);
  }

  @Test
  public void getTabStart() {
    Assert.assertEquals(0, TabLineFactory.getTab(starts, 0).getTab());
  }

  @Test
  public void getTabMiddleFrontEdge() {
    Assert.assertEquals(1, TabLineFactory.getTab(starts, 5).getTab());
  }

  @Test
  public void getTabMiddle() {
    Assert.assertEquals(1, TabLineFactory.getTab(starts, 7).getTab());
  }

  @Test
  public void getTabMiddleLocalLine() {
    Assert.assertEquals(2, TabLineFactory.getTab(starts, 7).getLineInTab());
  }

  @Test
  public void getTabMiddleBackEdge() {
    Assert.assertEquals(2, TabLineFactory.getTab(starts, 10).getTab());
  }

  @Test
  public void getTabEnd() {
    Assert.assertEquals(2, TabLineFactory.getTab(starts, 15).getTab());
  }

}