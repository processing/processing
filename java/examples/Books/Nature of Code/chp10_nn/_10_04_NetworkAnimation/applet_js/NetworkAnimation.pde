// An animated drawing of a Neural Network
// Daniel Shiffman <http://www.shiffman.net>
// Nature of Code

Network network;

void setup() {
  size(590, 360); 
  smooth();
  
  // Create the Network object
  network = new Network(width/2, height/2);

  // Create a bunch of Neurons
  Neuron a = new Neuron(-300, 0);
  Neuron b = new Neuron(-200, 0);
  Neuron c = new Neuron(0, 100);
  Neuron d = new Neuron(0, -100);
  Neuron e = new Neuron(200, 0);
  Neuron f = new Neuron(300, 0);

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

// An animated drawing of a Neural Network
// Daniel Shiffman <http://www.shiffman.net>
// Nature of Code

class Connection {
  // Connection is from Neuron A to B
  Neuron a;
  Neuron b;
  
  // Connection has a weight
  float weight;

  // Variables to track the animation
  boolean sending = false;
  PVector sender;
  
  // Need to store the output for when its time to pass along
  float output = 0;

  Connection(Neuron from, Neuron to, float w) {
    weight = w;
    a = from;
    b = to;
  }
  
  
  // The Connection is active
  void feedforward(float val) {
    output = val*weight;        // Compute output
    sender = a.location.get();  // Start animation at Neuron A
    sending = true;             // Turn on sending
  }
  
  // Update traveling sender
  void update() {
    if (sending) {
      // Use a simple interpolation
      sender.x = lerp(sender.x, b.location.x, 0.1);
      sender.y = lerp(sender.y, b.location.y, 0.1);
      float d = PVector.dist(sender, b.location);
      // If we've reached the end
      if (d < 1) {
        // Pass along the output!
        b.feedforward(output);
        sending = false;
      }
    }
  }
  
  // Draw line and traveling circle
  void display() {
    stroke(0);
    strokeWeight(1+weight*4);
    line(a.location.x, a.location.y, b.location.x, b.location.y);

    if (sending) {
      fill(0);
      strokeWeight(1);
      ellipse(sender.x, sender.y, 16, 16);
    }
  }
}

// An animated drawing of a Neural Network
// Daniel Shiffman <http://www.shiffman.net>
// Nature of Code

class Network {
  
  // The Network has a list of neurons
  ArrayList<Neuron> neurons;
  
  // The Network now keeps a duplicate list of all Connection objects.
  // This makes it easier to draw everything in this class
  ArrayList<Connection> connections;
  PVector location;

  Network(float x, float y) {
    location = new PVector(x, y);
    neurons = new ArrayList<Neuron>();
    connections = new ArrayList<Connection>();
  }

  // We can add a Neuron
  void addNeuron(Neuron n) {
    neurons.add(n);
  }

  // We can connection two Neurons
  void connect(Neuron a, Neuron b, float weight) {
    Connection c = new Connection(a, b, weight);
    a.addConnection(c);
    // Also add the Connection here
    connections.add(c);
  } 
  
  // Sending an input to the first Neuron
  // We should do something better to track multiple inputs
  void feedforward(float input) {
    Neuron start = neurons.get(0);
    start.feedforward(input);
  }
  
  // Update the animation
  void update() {
    for (Connection c : connections) {
      c.update();
    }
  }
  
  // Draw everything
  void display() {
    pushMatrix();
    translate(location.x, location.y);
    for (Neuron n : neurons) {
      n.display();
    }

    for (Connection c : connections) {
      c.display();
    }
    popMatrix();
  }
}

// An animated drawing of a Neural Network
// Daniel Shiffman <http://www.shiffman.net>
// Nature of Code

class Neuron {
  // Neuron has a location
  PVector location;

  // Neuron has a list of connections
  ArrayList<Connection> connections;
  
  // We now track the inputs and sum them
  float sum = 0;
  
  // The Neuron's size can be animated
  float r = 32;
  
  Neuron(float x, float y) {
    location = new PVector(x, y);
    connections = new ArrayList<Connection>();
  }

  // Add a Connection
  void addConnection(Connection c) {
    connections.add(c);
  } 
  
  // Receive an input
  void feedforward(float input) {
    // Accumulate it
    sum += input;
    // Activate it?
    if (sum > 1) {
      fire();
      sum = 0;  // Reset the sum to 0 if it fires
    } 
  }
  
  // The Neuron fires
  void fire() {
    r = 64;   // It suddenly is bigger
    
    // We send the output through all connections
    for (Connection c : connections) {
       c.feedforward(sum);
    } 
  }
  
  // Draw it as a circle
  void display() {
    stroke(0);
    strokeWeight(1);
    // Brightness is mapped to sum
    float b = map(sum,0,1,255,0);
    fill(b);
    ellipse(location.x, location.y, r, r);
    
    // Size shrinks down back to original dimensions
    r = lerp(r,32,0.1);
  }
}


