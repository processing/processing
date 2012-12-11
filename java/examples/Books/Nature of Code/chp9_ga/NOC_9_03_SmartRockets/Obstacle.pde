// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Pathfinding w/ Genetic Algorithms

// A class for an obstacle, just a simple rectangle that is drawn
// and can check if a Rocket touches it

// Also using this class for target location


class Obstacle {

  PVector location;
  float w,h;
  
  Obstacle(float x, float y, float w_, float h_) {
    location = new PVector(x,y);
    w = w_;
    h = h_;
  }

  void display() {
    stroke(0);
    fill(175);
    strokeWeight(2);
    rectMode(CORNER);
    rect(location.x,location.y,w,h);
  }

  boolean contains(PVector spot) {
    if (spot.x > location.x && spot.x < location.x + w && spot.y > location.y && spot.y < location.y + h) {
      return true;
    } else {
      return false;
    }
  }

}
