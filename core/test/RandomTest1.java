package test;
import processing.core.*;

/**
 *
 * @author  Francis Li
 */
public class RandomTest1 extends PMIDlet {
    
    void setup() {
        for(int i=0; i<100; i++) { 
          int r = random(50); 
          stroke(r*5); 
          line(50, i, 50+r, i); 
        }
    }
}
