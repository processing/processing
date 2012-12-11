// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// A random walker class!

class Walker {
  PVector location;
  PVector velocity;

  ArrayList<PVector> history;

  PVector noff;


  Walker() {
    location = new PVector(width/2, height/2);
    history = new ArrayList<PVector>();
    noff = new PVector(random(1000), random(1000));
    velocity = new PVector();
  }

  void display() {
    stroke(0);
    fill(175);
    rectMode(CENTER);
    rect(location.x, location.y, 16, 16);

    beginShape();
    stroke(0);
    noFill();
    for (PVector v: history) {
      vertex(v.x, v.y);
    }
    endShape();
  }

  // Randomly move up, down, left, right, or stay in one place
  void walk() {


    velocity.x = map(noise(noff.x), 0, 1, -1, 1);
    velocity.y = map(noise(noff.y), 0, 1, -1, 1);
    velocity.mult(5);

    noff.add(0.01, 0.01, 0);

    location.add(velocity);

    history.add(location.get());
    if (history.size() > 1000) {
      history.remove(0);
    }

    // Stay on the screen
    location.x = constrain(location.x, 0, width-1);
    location.y = constrain(location.y, 0, height-1);
  }
}

