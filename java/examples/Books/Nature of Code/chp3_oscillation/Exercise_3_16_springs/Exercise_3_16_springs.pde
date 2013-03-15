// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Mover object
Bob b1;
Bob b2;
Bob b3;

Spring s1;
Spring s2;
Spring s3;

void setup() {
  size(640, 360);
  // Create objects at starting location
  // Note third argument in Spring constructor is "rest length"
  b1 = new Bob(width/2, 100);
  b2 = new Bob(width/2, 200);
  b3 = new Bob(width/2, 300);

  s1 = new Spring(b1,b2,100);
  s2 = new Spring(b2,b3,100);
  s3 = new Spring(b1,b3,100);
}

void draw() {
  background(255); 

  s1.update();
  s2.update();
  s3.update();
  
  s1.display();
  s2.display();
  s3.display();

  b1.update();
  b1.display();
  b2.update();
  b2.display();
  b3.update();
  b3.display();

  b1.drag(mouseX, mouseY);
}



void mousePressed() {
  b1.clicked(mouseX, mouseY);
}

void mouseReleased() {
  b1.stopDragging();
}

