/**
 * Create Graphics. 
 * 
 * The createGraphics() function creates an object from the PGraphics class 
 * (PGraphics is the main graphics and rendering context for Processing). 
 * The beginDraw() method is necessary to prepare for drawing and endDraw() is
 * necessary to finish. Use this class if you need to draw into an off-screen 
 * graphics buffer or to maintain two contexts with different properties.
 */

PGraphics pg;

void setup() {
  size(200, 200);
  pg = createGraphics(80, 80, P2D);
}

void draw() {
  fill(0, 12);
  rect(0, 0, width, height);
  fill(255);
  noStroke();
  ellipse(mouseX, mouseY, 60, 60);
  
  pg.beginDraw();
  pg.background(102);
  pg.noFill();
  pg.stroke(255);
  pg.ellipse(mouseX-60, mouseY-60, 60, 60);
  pg.endDraw();
  
  image(pg, 60, 60); 
}
