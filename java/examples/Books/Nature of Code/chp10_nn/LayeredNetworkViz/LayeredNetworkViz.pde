// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

Network network;

void setup() {
  size(640, 360); 
  network = new Network(4,3,1);
}

void draw() {
  background(255);
  network.display();
}

