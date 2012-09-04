// Recursive Tree (w/ ArrayList)
// Daniel Shiffman <http://www.shiffman.net>
// Nature of Code, Chapter 8

// A class for a leaf that gets placed at the end of 
// the last branches

class Leaf {
  PVector loc;

  Leaf(PVector l) {
    loc = l.get();
  }

  void display() {
    noStroke();
    fill(50,100);
    ellipse(loc.x,loc.y,4,4);   
  }
}

