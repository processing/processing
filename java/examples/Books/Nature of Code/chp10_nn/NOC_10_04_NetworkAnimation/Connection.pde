// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// An animated drawing of a Neural Network

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

