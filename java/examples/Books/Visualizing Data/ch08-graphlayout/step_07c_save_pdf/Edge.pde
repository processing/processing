// Code from Visualizing Data, First Edition, Copyright 2008 Ben Fry.
// Based on the GraphLayout example by Sun Microsystems.


class Edge {
  Node from;
  Node to;
  float len;
  int count;


  Edge(Node from, Node to) {
    this.from = from;
    this.to = to;
    this.len = 50;
  }
  
  
  void increment() {
    count++;
  }
  
  
  void relax() {
    float vx = to.x - from.x;
    float vy = to.y - from.y;
    float d = mag(vx, vy);
    if (d > 0) {
      float f = (len - d) / (d * 3);
      float dx = f * vx;
      float dy = f * vy;
      to.dx += dx;
      to.dy += dy;
      from.dx -= dx;
      from.dy -= dy;
    }
  }


  void draw() {
    stroke(edgeColor);
    strokeWeight(0.35);
    line(from.x, from.y, to.x, to.y);
  }
}
