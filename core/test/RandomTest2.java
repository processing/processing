package test;
import processing.core.*;

/**
 *
 * @author  Francis Li
 */
public class RandomTest2 extends PMIDlet {

    void setup() {
        for(int i=0; i<100; i++) { 
          int r = random(-50, 50); 
          stroke(abs(r*5)); 
          line(50, i, 50+r, i); 
        } 
    }
}
