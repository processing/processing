package test;
import processing.core.*;

/**
 *
 * @author  Francis Li
 */
public class LineTest2 extends PMIDlet {
    void setup() {
        line(30, 20, 85, 20); 
        stroke(126); 
        line(85, 20, 85, 75); 
        stroke(255); 
        line(85, 75, 30, 75);         
    }
}
