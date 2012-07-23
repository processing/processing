PShader edges;

void setup() {
  size(400, 400, P2D);
  edges = loadShader(PShader.TEXTURED, "edges.glsl"); 
}

void draw() {
  rect(mouseX, mouseY, 50, 50);    
  filter(edges);    
}
