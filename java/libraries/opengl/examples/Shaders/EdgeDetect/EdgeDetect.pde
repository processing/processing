// This example shows how to change the default fragment shader used
// in P3D to render textures, by a custom one that applies a simple 
// edge detection filter.
//
// Press any key to switch between the custom and the default shader.

PImage img;
PShader shader;
PGraphicsOpenGL pg;  
boolean customShader;
  
void setup() {
  size(400, 400, P3D);
  img = loadImage("berlin-1.jpg");
    
  pg = (PGraphicsOpenGL)g;
  shader = pg.loadShader("edges.glsl", POLY_SHADER_TEX);
  pg.setShader(shader, POLY_SHADER_TEX);
  customShader = true;
}

public void draw() {
  image(img, 0, 0, width, height);
}
  
public void keyPressed() {
  if (customShader) {
    pg.defaultShader(POLY_SHADER_TEX);
    customShader = false;
  } else {
    pg.setShader(shader, POLY_SHADER_TEX);
    customShader = true;
  }
}
