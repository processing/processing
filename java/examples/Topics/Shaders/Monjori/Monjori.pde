/**
 * Monjori. 
 * 
 * GLSL version of the 1k intro Monjori from the demoscene 
 * (http://www.pouet.net/prod.php?which=52761)
 * Ported from the webGL version available in ShaderToy:
 * http://www.iquilezles.org/apps/shadertoy/
 * (Look for Monjori under the Plane Deformations Presets) 
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
  // This kind of effects are entirely implemented in the
  // fragment shader, they only need a quad covering the  
  // entire view area so every pixel is pushed through the 
  // shader.   
  rect(0, 0, width, height);  
}

