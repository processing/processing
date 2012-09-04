/**
 * Nebula
 */
 
PShader nebula;

void setup() {
  size(640, 360, P2D);
  noStroke();

  nebula = loadShader("nebula.glsl");
  nebula.set("resolution", float(width), float(height));
}

void draw() {
  nebula.set("time", millis() / 500.0);  
  shader(nebula); 
  fill(0);
  rect(0, 0, width, height);
}

