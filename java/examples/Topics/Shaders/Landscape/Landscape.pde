PShader landscape;

void setup() {
  size(320, 240, P2D);
 
  landscape = loadShader("landscape.glsl");
  landscape.set("resolution", float(width), float(height));
  noStroke(); 
}

void draw() {
  landscape.set("time", millis() / 1000.0);
  landscape.set("mouse", float(mouseX), height - float(mouseY));
  shader(landscape);
  rect(0, 0, width, height);  
}
