/**
 * Landscape
 * Simple raymarching shader with camera, originally by Paulo Falcão
 * Ported from the webGL version in GLSL Sandbox:
 * http://glsl.heroku.com/e#3213.0
 *
 */
 
PShader landscape;
PGraphics pg;

void setup() {
  size(640, 360, P2D);
  
  // This effect can be too demanding on older GPUs, 
  // so we render it on a smaller res offscreen surface
  pg = createGraphics(320, 180, P2D);
  pg.noStroke(); 
 
  landscape = loadShader("landscape.glsl");
  landscape.set("resolution", float(width), float(height));
}

void draw() {
  landscape.set("time", millis() / 1000.0);
  landscape.set("mouse", float(mouseX), height - float(mouseY));
  
  // This kind of raymarching effects are entirely implemented in the
  // fragment shader, they only need a quad covering the entire view 
  // area so every pixel is pushed through the shader.  
  pg.beginDraw();
  pg.shader(landscape);  
  pg.rect(0, 0, width, height);
  pg.endDraw();

  // Scaling up offscreen surface to cover entire screen.
  image(pg, 0, 0, width, height);  
}
