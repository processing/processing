/**
 * BeginEndContour
 * 
 * How to cut a shape out of another using beginContour() and endContour()
 */
 
PShape s;

void setup() {
  size(640, 360, P2D);
  smooth();
  // Make a shape
  s = createShape();
  s.fill(0);
  s.stroke(255);
  s.strokeWeight(2);
  // Exterior part of shape
  s.beginContour();
  s.vertex(-100,-100);
  s.vertex(100,-100);
  s.vertex(100,100);
  s.vertex(-100,100);
  s.vertex(-100,-100);
  s.endContour();
  
  // Interior part of shape
  s.beginContour();
  s.vertex(-10,-10);
  s.vertex(10,-10);
  s.vertex(10,10);
  s.vertex(-10,10);
  s.vertex(-10,-10);
  s.endContour();
  
  // Finishing off shape
  s.end();
}

void draw() {
  background(52);
  // Display shape
  translate(width/2, height/2);
  // Shapes can be rotated
  s.rotate(0.01);
  shape(s);
}

