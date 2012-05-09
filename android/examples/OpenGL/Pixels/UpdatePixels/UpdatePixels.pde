void setup() {
  size(400, 400, P3D);

  background(0);
  loadPixels();  
}

void draw() {  
  for (int i = 0; i < pixels.length; i++) {
    pixels[i] = color(random(255), random(255), random(255));    
  }
  updatePixels();
    
  println(frameRate);
}  
