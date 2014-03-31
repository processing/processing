// GLSL version of Conway's game of life, ported from GLSL sandbox:
// http://glsl.heroku.com/e#207.3
// Exemplifies the use of the ppixels uniform in the shader, that gives
// access to the pixels of the previous frame.
PShader conway;
PGraphics pg;

void setup() {
  size(400, 400, P3D);    
  pg = createGraphics(400, 400, P2D);
  pg.noSmooth();
  conway = loadShader("conway.glsl");
  conway.set("resolution", float(pg.width), float(pg.height));  
}

void draw() {
  conway.set("time", millis()/1000.0);
  float x = map(mouseX, 0, width, 0, 1);
  float y = map(mouseY, 0, height, 1, 0);
  conway.set("mouse", x, y);  
  pg.beginDraw();
  pg.background(0);
  pg.shader(conway);
  pg.rect(0, 0, pg.width, pg.height);
  pg.endDraw();  
  image(pg, 0, 0, width, height);
}
