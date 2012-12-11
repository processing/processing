// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// A random walker class!

class Walker {
  PVector location;

  ArrayList<PVector> history;


  Walker() {
    location = new PVector(width/2, height/2);
    history = new ArrayList<PVector>();
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
    PVector vel = new PVector(random(-2, 2), random(-2, 2));
    location.add(vel);

    // Stay on the screen
    location.x = constrain(location.x, 0, width-1);
    location.y = constrain(location.y, 0, height-1);


    history.add(location.get());
    if (history.size() > 1000) {
      history.remove(0);
    }
  }
}

