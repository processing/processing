// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// A random walker class!

class Walker {
  PVector location;

  PVector noff;

  Walker() {
    location = new PVector(width/2, height/2);
    noff = new PVector(random(1000),random(1000));
  }

  void display() {
    strokeWeight(2);
    fill(127);
    stroke(0);
    ellipse(location.x, location.y, 48, 48);
  }

  // Randomly move up, down, left, right, or stay in one place
  void walk() {
    
    location.x = map(noise(noff.x),0,1,0,width);
    location.y = map(noise(noff.y),0,1,0,height);
    
    noff.add(0.01,0.01,0);
  }
}

