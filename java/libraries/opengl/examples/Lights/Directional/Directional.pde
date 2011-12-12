/**
 * Directional. 
 * 
 * Move the mouse the change the direction of the light.
 * Directional light comes from one direction and is stronger 
 * when hitting a surface squarely and weaker if it hits at a 
 * a gentle angle. After hitting a surface, a directional lights 
 * scatters in all directions. 
 */

void setup() {
  size(640, 360, P3D);
  noStroke();
  fill(204);
}

void draw() {
  noStroke(); 
  background(0); 
  float dirY = (mouseY / float(height) - 0.5) * 2;
  float dirX = (mouseX / float(width) - 0.5) * 2;
  directionalLight(204, 204, 204, -dirX, -dirY, -1); 
  translate(width/2 - 100, height/2, 0); 
  sphere(80); 
  translate(200, 0, 0); 
  sphere(80); 
}

