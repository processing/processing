package test;
import processing.core.*;

/**
 *
 * @author  Francis Li
 */
public class EllipseModeTest extends PMIDlet {
    void setup() {
        ellipseMode(CENTER_DIAMETER); 
        ellipse(35, 35, 50, 50); 
        ellipseMode(CORNER); 
        fill(102); 
        ellipse(35, 35, 50, 50);         
    }
}
