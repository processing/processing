package test;
import processing.core.*;

/**
 *
 * @author  Francis Li
 */
public class BezierTest1 extends PMIDlet {
    
    void setup() {
        stroke(255, 102, 0); 
        line(85, 20, 10, 10); 
        line(90, 90, 15, 80); 
        stroke(0, 0, 0); 
        bezier(85, 20, 10, 10, 90, 90, 15, 80); 
    }
}
