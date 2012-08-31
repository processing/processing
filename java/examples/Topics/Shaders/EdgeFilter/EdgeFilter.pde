/**
 * Edge Filter
 * 
 * Apply a custom shader to the filter() function to affect the geometry drawn to the screen.
 * 
 * Press the mouse to turn the filter on and off.
 */
 
 PShader edges;
boolean applyFilter = true;

void setup() {
  size(640, 360, P3D);
  edges = loadShader("edges.glsl");
  noStroke(); 
}

void draw() {
  background(0);
  lights();
  
  translate(width/2, height/2);  
  pushMatrix();
  rotateX(frameCount * 0.01);  
  rotateY(frameCount * 0.01);
  box(120);
  popMatrix();
    
  if (applyFilter == true) {
    filter(edges);
  }
  
  // The sphere doesn't have the edge detection applied 
  // on it because it is drawn after filter() is called.
  rotateY(frameCount * 0.02);
  translate(150, 0);
  sphere(40);
}

void mousePressed() {
  applyFilter = !applyFilter;
}
