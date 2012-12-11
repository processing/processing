// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// A static drawing of a Neural Network

class Network {
  
  // The Network has a list of neurons
  ArrayList<Neuron> neurons;
  PVector location;

  Network(float x, float y) {
    location = new PVector(x,y);
    neurons = new ArrayList<Neuron>();
  }
  
  // We can add a Neuron
  void addNeuron(Neuron n) {
    neurons.add(n);
  }
  
  // We can connection two Neurons
  void connect(Neuron a, Neuron b) {
    Connection c = new Connection(a, b, random(1));
    a.addConnection(c);
  } 

  // We can draw the network
  void display() {
    pushMatrix();
    translate(location.x, location.y);
    for (Neuron n : neurons) {
      n.display();
    }
    popMatrix();
  }
}

