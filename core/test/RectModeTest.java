package test;
import processing.core.*;

/**
 *
 * @author  Francis Li
 */
public class RectModeTest extends PMIDlet {
    
    void setup() {
        rectMode(CENTER_DIAMETER); 
        rect(35, 35, 50, 50); 
        rectMode(CORNER); 
        fill(102); 
        rect(35, 35, 50, 50);         
    }
}
