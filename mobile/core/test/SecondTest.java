package test;
import processing.core.*;

/**
 *
 * @author  Francis Li
 */
public class SecondTest extends PMIDlet {
    
    public void draw() { 
      background(204); 
      int s = second();  // Values from 0 - 59 
      int m = minute();  // Values from 0 - 59 
      int h = hour();    // Values from 0 - 23 
      line(s, 0, s, 33); 
      line(m, 33, m, 66); 
      line(h, 66, h, 100); 
    } 
}
