public void setup() {
  size(400, 400, P3D);
  fill(0);
}
  
public void draw() {    
  background(255);
  for (int i = 0; i < 1000; i++) {
    float x = random(width);
    float y = random(height);
    text("HELLO", x, y);
  }
  if (frameCount % 10 == 0) println(frameRate);
}
