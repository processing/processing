// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Path Following

class Path {

  // A Path is line between two points (PVector objects)
  PVector start;
  PVector end;
  // A path has a radius, i.e how far is it ok for the boid to wander off
  float radius;

  Path() {
    // Arbitrary radius of 20
    radius = 20;
    start = new PVector(0,height/3);
    end = new PVector(width,2*height/3);
  }

  // Draw the path
  void display() {

    strokeWeight(radius*2);
    stroke(0,100);
    line(start.x,start.y,end.x,end.y);

    strokeWeight(1);
    stroke(0);
    line(start.x,start.y,end.x,end.y);
  }
}











