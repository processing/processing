// This example shows how to change the default fragment shader used
// in P2D to render textures, by a custom one that applies a simple 
// edge detection filter.
//
// Press the mouse to switch between the custom and the default shader.

PImage img;
PShader shader;
PGraphicsOpenGL pg;  
boolean customShader;
  
void setup() {
  size(400, 400, P2D);
  img = loadImage("berlin-1.jpg");
    
  pg = (PGraphicsOpenGL)g;
  shader = pg.loadShader("edges.glsl", PShader.TEXTURE_SHADER);
  pg.setShader(shader, PShader.TEXTURE_SHADER);
  customShader = true;
}

public void draw() {
  image(img, 0, 0, width, height);
}
  
public void mousePressed() {
  if (customShader) {
    pg.defaultShader(PShader.TEXTURE_SHADER);
    customShader = false;
  } else {
    pg.setShader(shader, PShader.TEXTURE_SHADER);
    customShader = true;
  }
}
