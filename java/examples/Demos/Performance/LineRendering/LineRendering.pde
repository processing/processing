
public void setup() {
  size(800, 600, P2D);  
}
  
public void draw() {    
  background(255);
  stroke(0, 10);
  for (int i = 0; i < 50000; i++) {
    float x0 = random(width);
    float y0 = random(height);
    float z0 = random(-100, 100);
    float x1 = random(width);
    float y1 = random(height);
    float z1 = random(-100, 100);    
    
    // purely 2D lines will trigger the GLU 
    // tessellator to add accurate line caps,
    // but performance will be substantially
    // lower.
    line(x0, y0, z0, x1, y1, z1);
  }
  if (frameCount % 10 == 0) println(frameRate);
}
