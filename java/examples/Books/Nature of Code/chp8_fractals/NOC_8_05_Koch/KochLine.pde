// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Koch Curve
// A class to describe one line segment in the fractal
// Includes methods to calculate midPVectors along the line according to the Koch algorithm

class KochLine {

  // Two PVectors,
  // a is the "left" PVector and 
  // b is the "right PVector
  PVector a;
  PVector b;

  KochLine(PVector start, PVector end) {
    a = start.get();
    b = end.get();
  }

  void display() {
    stroke(0);
    line(a.x, a.y, b.x, b.y);
  }

  PVector start() {
    return a.get();
  }

  PVector end() {
    return b.get();
  }

  // This is easy, just 1/3 of the way
  PVector kochleft() {
    PVector v = PVector.sub(b, a);
    v.div(3);
    v.add(a);
    return v;
  }    

  // More complicated, have to use a little trig to figure out where this PVector is!
  PVector kochmiddle() {
    PVector v = PVector.sub(b, a);
    v.div(3);
    
    PVector p = a.get();
    p.add(v);
    
    rotate(v,-radians(60));
    p.add(v);
    
    return p;
  }    


  // Easy, just 2/3 of the way
  PVector kochright() {
    PVector v = PVector.sub(a, b);
    v.div(3);
    v.add(b);
    return v;
  }
}

  public void rotate(PVector v, float theta) {
    float xTemp = v.x;
    // Might need to check for rounding errors like with angleBetween function?
    v.x = v.x*cos(theta) - v.y*sin(theta);
    v.y = xTemp*sin(theta) + v.y*cos(theta);
  }


