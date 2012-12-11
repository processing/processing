// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

class Connection {
  float weight;
  Neuron a;
  Neuron b;

  Connection(Neuron from, Neuron to,float w) {
    weight = w;
    a = from;
    b = to;
  }

  void display() {
    stroke(0);
    strokeWeight(weight*4);
    line(a.location.x, a.location.y, b.location.x, b.location.y);
  }
}



