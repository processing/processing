// Issues: flickers and scene doesn't move

PShader edges;
boolean applyFilter = true;

void setup() {
  size(400, 400, P3D);
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
  box(100);
  popMatrix();
    
  if (applyFilter) filter(edges);

  // The sphere doesn't have the edge detection applied 
  // on it because it is drawn after filter() is called.
  rotateY(frameCount * 0.02);
  translate(150, 0);
  sphere(40);
}

void mousePressed() {
  applyFilter = !applyFilter;
}
