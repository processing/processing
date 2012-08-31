
public void setup() {
  size(800, 600, P2D);
  
  noStroke();
  fill(0, 1);  
}
  
public void draw() {    
  background(255);
  for (int i = 0; i < 50000; i++) {
    float x = random(width);
    float y = random(height);
    rect(x, y, 30, 30);
  }
  if (frameCount % 10 == 0) println(frameRate);
}
