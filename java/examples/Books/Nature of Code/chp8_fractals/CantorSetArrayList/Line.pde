// Cantor Set
// Daniel Shiffman <http://www.shiffman.net>
// Nature of Code, Chapter 8

// Cantor line is a simple horizontal line with a starting point
// and length

class CantorLine {
  float x,y;
  float len;
 
  CantorLine(float x_, float y_, float len_) {
    x = x_;
    y = y_;
    len = len_;
  } 
  
  void display() {
    stroke(0);
    line(x,y,x+len,y); 
  }
  
}
