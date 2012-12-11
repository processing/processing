// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

class Network {
  ArrayList<Neuron> neurons;
  PVector location;
  Network(int layers, int inputs, int outputs) {
    location = new PVector(width/2, height/2);

    neurons = new ArrayList<Neuron>();
    
    Neuron output = new Neuron(250, 0);
    for (int i = 0; i < layers; i++) {
      for (int j = 0; j < inputs; j++) {
        float x = map(i, 0, layers, -200, 200);
        float y = map(j, 0, inputs-1, -100, 100);
        println(j + " " + y);
        Neuron n = new Neuron(x, y);
        
        if (i > 0) {
          for (int k = 0; k < inputs; k++) {
            Neuron prev = neurons.get(neurons.size()-inputs+k-j); 
            prev.connect(n);
          }
        }
        
        if (i == layers-1) {
          n.connect(output);
        }
        neurons.add(n);
      }
    } 
    neurons.add(output);
  }


  void display() {
    pushMatrix();
    translate(location.x, location.y);
    for (Neuron n : neurons) {
      n.display();
    }
    popMatrix();
  }
}

