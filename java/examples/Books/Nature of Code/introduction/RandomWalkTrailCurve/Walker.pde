// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// A random walker object!

class Walker {
  PVector position;

  ArrayList<PVector> history = new ArrayList<PVector>();

  Walker() {
    position = new PVector(width/2, height/2);
  }

  void render() {
    stroke(0);
    beginShape();
    for (PVector v : history) {
      curveVertex(v.x, v.y);
    }
    endShape();

    noFill();
    stroke(0);
    ellipse(position.x, position.y, 16, 16);
  }

  // Randomly move up, down, left, right, or stay in one place
  void step() {

    position.x += random(-10, 10);
    position.y += random(-10, 10);


    position.x = constrain(position.x, 0, width-1);
    position.y = constrain(position.y, 0, height-1);

    history.add(position.get());
  }
}

