/**
 * Vertices. 
 * 
 * The beginShape() function begins recording vertices 
 * for a shape and endShape() stops recording. 
 * A vertex is a location in space specified by X, Y, 
 * and sometimes Z coordinates. After calling the beginShape() function, 
 * a series of vertex() functions must follow.  
 * To stop drawing the shape, call the endShape() functions.
 */
 
size(640, 360);
background(0);
noFill();

translate(140, 20);
scale(1.5);
strokeWeight(.5);

stroke(102);
beginShape();
curveVertex(168, 182);
curveVertex(168, 182);
curveVertex(136, 38);
curveVertex(42, 34);
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
beginShape();
vertex(60, 40);
bezierVertex(160, 10, 170, 150, 60, 150);
endShape();

stroke(255);
beginShape(POINTS);
vertex(60, 40);
vertex(160, 10);
vertex(170, 150);
vertex(60, 150);
endShape();

