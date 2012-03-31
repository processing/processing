void setup() {
  size(400, 400, P3D);

  background(0);
}

void draw() {
  for (int x = 0; x < width; x++) {
    for (int y = 0; y < height; y++) {    
      int c = color(random(255), random(255), random(255));
      set(x, y, c);    
    }
  }
    
  println(frameRate);
}  
