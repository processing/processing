// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// One array of Shapes
Shape[] shapes = new Shape[30];

void setup() {
  size(200,200);
  for (int i = 0; i < shapes.length; i++ ) {
    int r = int(random(2));
    // Randomly put either circles or squares in our array
    if (r == 0) {
      shapes[i] = new Circle(100,100,10,color(random(255),100));
    } else {
      shapes[i] = new Square(100,100,10);
    }
  }
}

void draw() {
  background(255);
  // Jiggle and display all shapes
  for (int i = 0; i < shapes.length; i++ ) {
    shapes[i].jiggle();
    shapes[i].display();
  }
}
