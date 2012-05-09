void setup() {
  size(255, 255, P3D);
  
//  for (int x = 0; x < width; x++) {
//    for (int y = 0; y < height; y++) {    
//      int c = color(x, y, 0);
//      set(x, y, c);    
//    }
//  }  

  loadPixels();
  for (int i = 0; i < pixels.length; i++) {
    int x = i % width;
    int y = i / height;
    int c = color(x, y, 0);
    set(x, y, c);    
  }
  updatePixels();
}

void draw() {
  for (int x = 0; x < width; x++) {
    for (int y = 0; y < height; y++) {    
      int c = get(x, y);
      set(x, y, c);
    }
  }
    
  println(frameRate);
}  
