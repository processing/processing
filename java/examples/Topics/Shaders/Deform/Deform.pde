/**
 * Deform
 */
 
PImage tex;
PShader deform;

void setup() {
  size(640, 360, P2D);
  
  tex = loadImage("tex1.jpg");
 
  deform = loadShader("deform.glsl");
  deform.set("resolution", float(width), float(height));
}

void draw() {
  deform.set("time", millis() / 1000.0);
  deform.set("mouse", float(mouseX), float(mouseY));
  shader(deform);
  image(tex, 0, 0, width, height);
}
