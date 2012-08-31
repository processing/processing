/**
 * Blur Filter
 * 
 * Change the default shader to apply a simple, custom blur filter.
 * 
 * Press the mouse to switch between the custom and default shader.
 */

PShader blur;

void setup() {
  size(640, 360, P2D);
  blur = loadShader("blur.glsl"); 
  stroke(255, 0, 0);
}

void draw() {
  filter(blur);  
  rect(mouseX, mouseY, 150, 150); 
  line(mouseX, mouseY, mouseX+150, mouseY+150);  
}



