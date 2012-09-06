/**
 * Nebula. 
 * 
 * From CoffeeBreakStudios.com (CBS)
 * Ported from the webGL version in GLSL Sandbox:
 * http://glsl.heroku.com/e#3265.2
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
  // This kind of raymarching effects are entirely implemented in the
  // fragment shader, they only need a quad covering the entire view 
  // area so every pixel is pushed through the shader. 
  rect(0, 0, width, height);
}

