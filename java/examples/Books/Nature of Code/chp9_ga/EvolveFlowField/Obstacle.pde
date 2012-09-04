// Pathfinding w/ Genetic Algorithms
// Daniel Shiffman <http://www.shiffman.net>

// A class for an obstacle, just a simple rectangle that is drawn
// and can check if a creature touches it

// Also using this class for starting point and target location

import java.awt.Rectangle;

class Obstacle {

  Rectangle r;

  Obstacle(int x, int y, int w, int h) {
    r = new Rectangle(x,y,w,h);
  }

  void display() {
    stroke(0);
    fill(175);
    rectMode(CORNER);
    rect(r.x,r.y,r.width,r.height);
  }

  boolean contains(PVector spot) {
    if (r.contains((int)spot.x,(int)spot.y)) {
      return true;
    } else {
      return false;
    }
  }

}
