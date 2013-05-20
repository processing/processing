// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// A static drawing of a Neural Network

Network network;

void setup() {
  size(640, 360); 
  // Create the Network object
  network = new Network(width/2,height/2);
  
  // Create a bunch of Neurons
  Neuron a = new Neuron(-200,0);
  Neuron b = new Neuron(0,75);
  Neuron c = new Neuron(0,-75);
  Neuron d = new Neuron(200,0);
  
  // Connect them
  network.connect(a,b);
  network.connect(a,c);
  network.connect(b,d);
  network.connect(c,d);
  
  // Add them to the Network
  network.addNeuron(a);
  network.addNeuron(b);
  network.addNeuron(c);
  network.addNeuron(d);
}

void draw() {
  background(255);
  // Draw the Network
  network.display();
  noLoop();
}

