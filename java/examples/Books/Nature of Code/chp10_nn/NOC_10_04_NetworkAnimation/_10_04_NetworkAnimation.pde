// An animated drawing of a Neural Network
// Daniel Shiffman <http://www.shiffman.net>
// Nature of Code

Network network;

void setup() {
  size(800, 200); 
  smooth();
  
  // Create the Network object
  network = new Network(width/2, height/2);

  // Create a bunch of Neurons
  Neuron a = new Neuron(-350, 0);
  Neuron b = new Neuron(-200, 0);
  Neuron c = new Neuron(0, 75);
  Neuron d = new Neuron(0, -75);
  Neuron e = new Neuron(200, 0);
  Neuron f = new Neuron(350, 0);

  // Connect them
  network.connect(a, b,1);
  network.connect(b, c,random(1));
  network.connect(b, d,random(1));
  network.connect(c, e,random(1));
  network.connect(d, e,random(1));
  network.connect(e, f,1);

  // Add them to the Network
  network.addNeuron(a);
  network.addNeuron(b);
  network.addNeuron(c);
  network.addNeuron(d);
  network.addNeuron(e);
  network.addNeuron(f);
}

void draw() {
  background(255);
  // Update and display the Network
  network.update();
  network.display();
  
  // Every 30 frames feed in an input
  if (frameCount % 30 == 0) {
    network.feedforward(random(1));
  }
}

