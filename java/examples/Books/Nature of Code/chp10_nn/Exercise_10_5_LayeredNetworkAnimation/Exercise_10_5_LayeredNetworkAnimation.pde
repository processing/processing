// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// An animated drawing of a Neural Network

Network network;

void setup() {
  size(640,360); 
  // Create the Network object
  network = new Network(width/2, height/2);

  int layers = 3;
  int inputs = 2;

  Neuron output = new Neuron(250, 0);
  for (int i = 0; i < layers; i++) {
    for (int j = 0; j < inputs; j++) {
      float x = map(i, 0, layers, -250, 300);
      float y = map(j, 0, inputs-1, -75, 75);
      Neuron n = new Neuron(x, y);
      if (i > 0) {
        for (int k = 0; k < inputs; k++) {
          Neuron prev = network.neurons.get(network.neurons.size()-inputs+k-j); 
          network.connect(prev, n, random(1));
        }
      }
      if (i == layers-1) {
        network.connect(n, output, random(1));
      }
      network.addNeuron(n);
    }
  } 
  network.addNeuron(output);
}

void draw() {
  background(255);
  // Update and display the Network
  network.update();
  network.display();

  // Every 30 frames feed in an input
  if (frameCount % 30 == 0) {
    network.feedforward(random(1),random(1));
  }
}

