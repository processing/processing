// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// A static drawing of a Neural Network

class Neuron {
  
  // Neuron has a location
  PVector location;

  // Neuron has a list of connections
  ArrayList<Connection> connections;
  
  Neuron(float x, float y) {
    location = new PVector(x, y);
    connections = new ArrayList<Connection>();
  }
  
  // Add a Connection
  void addConnection(Connection c) {
    connections.add(c);
  } 

  // Draw Neuron as a circle
  void display() {
    stroke(0);
    strokeWeight(1);
    fill(0);
    ellipse(location.x, location.y, 16, 16);
    
    // Draw all its connections
    for (Connection c : connections) {
      c.display();
    }
  }
}

