package test;
import processing.core.*;

/**
 *
 * @author  Francis Li
 */
public class Vertices extends PMIDlet {

    void setup() {
        // Vertices 
        // by REAS <http://www.groupc.net> 

        size(200, 200); 
        background(0); 

        stroke(102); 
        beginShape(LINE_STRIP); 
        curveVertex(168, 182); 
        curveVertex(168, 182); 
        curveVertex(136,  38); 
        curveVertex(42,  34); 
        curveVertex(64, 200); 
        curveVertex(64, 200); 
        endShape(); 

        stroke(51); 
        beginShape(LINES); 
        vertex(60, 40); 
        vertex(160, 10); 
        vertex(170, 150); 
        vertex(60, 150); 
        endShape(); 

        stroke(126); 
        beginShape(LINE_STRIP); 
        bezierVertex(60, 40); 
        bezierVertex(160, 10); 
        bezierVertex(170, 150); 
        bezierVertex(60, 150); 
        endShape(); 

        stroke(255); 
        beginShape(POINTS); 
        vertex(60, 40); 
        vertex(160, 10); 
        vertex(170, 150); 
        vertex(60, 150); 
        endShape(); 
    }
}
