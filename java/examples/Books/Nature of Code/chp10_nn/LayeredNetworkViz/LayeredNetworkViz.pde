
Network network;

void setup() {
  size(640, 360); 
  smooth();
  network = new Network(4,3,1);
}

void draw() {
  background(255);
  network.display();
}

