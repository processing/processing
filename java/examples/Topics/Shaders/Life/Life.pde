PShader life;
void setup() {
  size(640, 480, P2D);    
  life = loadShader("Life.glsl");
  noStroke();
}

void draw() {
  life.set("time", millis() / 1000.0f);
  life.set("mouse", (float)mouseX, height - (float)mouseY);
  shader(life);  
  rect(0, 0, width, height);
}