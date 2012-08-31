/**
 * Edge Detection
 * 
 * Change the default shader to apply a simple, custom edge detection filter.
 * 
 * Press the mouse to switch between the custom and default shader.
 */

PShader edges;  
PImage img;
boolean enabled = true;
    
void setup() {
  size(640, 360, P2D);
  img = loadImage("leaves.jpg");      
  edges = loadShader("edges.glsl");
}

void draw() {
  if (enabled == true) {
    shader(edges);
  }
  image(img, 0, 0);
}
    
void mousePressed() {
  enabled = !enabled;
  if (!enabled == true) {
    resetShader();
  }
}
