// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

void setup() {
  size(200,200);
  PImage img = loadImage("texture.png");
  background(0);
  image(img,0,0,width,height);
  save("blob.tif");
  
  background(0);
  fill(255);
  noStroke();
  ellipse(100,100,width,height);
  save("circle.tif");
}

void draw() {
  
  
}


