// An object that wraps the PShape

class Wiggler {
  
  // The PShape to be "wiggled"
  PShape s;
  // Its location
  float x, y;
  
  // For 2D Perlin noise
  float yoff = 0;
  
  // We are using an ArrayList to keep a duplicate copy
  // of vertices original locations.
  ArrayList<PVector> original;

  Wiggler() {
    x = width/2;
    y = height/2; 

    // The "original" locations of the vertices make up a circle
    original = new ArrayList<PVector>();
    for (float a = 0; a < TWO_PI; a+=0.2) {
      PVector v = PVector.fromAngle(a);
      v.mult(100);
      original.add(v);
    }
    
    // Now make the PShape with those vertices
    s = createShape();
    s.beginShape();
    s.fill(127);
    s.stroke(0);
    s.strokeWeight(2);
    for (PVector v : original) {
      s.vertex(v.x, v.y);
    }
    s.endShape(CLOSE);
  }

  void wiggle() {
    float xoff = 0;
    // Apply an offset to each vertex
    for (int i = 0; i < s.getVertexCount(); i++) {
      // Calculate a new vertex location based on noise around "original" location
      PVector pos = original.get(i);
      float a = TWO_PI*noise(xoff,yoff);
      PVector r = PVector.fromAngle(a);
      r.mult(4);
      r.add(pos);
      // Set the location of each vertex to the new one
      s.setVertex(i, r.x, r.y);
      // increment perlin noise x value
      xoff+= 0.5;
    }
    // Increment perlin noise y value
    yoff += 0.02;
  }

  void display() {
    pushMatrix();
    translate(x, y);
    shape(s);
    popMatrix();
  }
}

