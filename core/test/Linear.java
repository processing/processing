package test;
import processing.core.*;

/**
 *
 * @author  Francis Li
 */
public class Linear extends PMIDlet {
    // Linear 
    // by REAS <http://www.groupc.net> 

    int a = 100; 

    public void setup() 
    { 
      size(200, 200); 
      stroke(255); 
      framerate(30); 
    } 
    
    public void draw() 
    { 
      background(51); 
      a = a - 1; 
      if (a < 0) { 
        a = height; 
      } 
      line(0, a, width, a);  
    } 
}
