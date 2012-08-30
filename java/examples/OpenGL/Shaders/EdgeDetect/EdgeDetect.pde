// This example shows how to change the default fragment shader used
// in P2D to render textures, by a custom one that applies a simple 
// edge detection filter.
//
// Press the mouse to switch between the custom and the default shader.

PImage img;
PShader edges;  
boolean enabled = true;
    
void setup() {
  size(400, 400, P2D);
  img = loadImage("berlin-1.jpg");      
  edges = loadShader("edges.glsl");
}

void draw() {
  if (enabled) shader(edges);
  image(img, 0, 0, width, height);
}
    
void mousePressed() {
  enabled = !enabled;
  if (!enabled) resetShader();
}