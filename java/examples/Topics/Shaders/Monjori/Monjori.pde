/**
 * Monjori
 */
 
 PShader monjori;

void setup() {
  size(640, 360, P2D);
  noStroke();
 
  monjori = loadShader("monjori.glsl");
  monjori.set("resolution", float(width), float(height));
   
}

void draw() {
  monjori.set("time", millis() / 1000.0);
  
  shader(monjori);
  rect(0, 0, width, height);  
}

