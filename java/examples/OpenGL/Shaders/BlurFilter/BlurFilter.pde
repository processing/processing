PShader blur;

void setup() {
  size(400, 400, P2D);
  blur = loadShader("blur.glsl"); 
}

void draw() {
  rect(mouseX, mouseY, 50, 50);    
  filter(blur);    
}



