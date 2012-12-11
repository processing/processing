// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Simple Recursion

void setup() {
  size(800, 200);  
}

void draw() {
  background(255);
  drawLines(100,100,700,100);
  noLoop();
}

void drawLines(float x1, float y1, float x2, float y2) {
  
  line(x1,y1,x2,y2);
  
  float dx = x2-x1;
  float dy = y2-y1;
  
  //println(dx + " " + dy);
  
  if (dx == 0 && dy > 4) {
    //println(dy);
    drawLines(x1-dy/3,y1,x1+dy/3,y1);
    drawLines(x1-dy/3,y2,x1+dy/3,y2);
  } else if (dy == 0 && dx > 4) {
    //println(dx);
    drawLines(x1,y1-dx/3,x1,y1+dx/3);
    drawLines(x2,y1-dx/3,x2,y1+dx/3);
  }
}

