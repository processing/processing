PShape system;
  
void setup() {
  size(500, 500, P3D);
    
  system = createShape(PShape.GROUP);
    
  for (int i = 0; i < 10; i++) {
    PShape pt = createShape(POINT);
    pt.strokeWeight(20);
    pt.stroke(random(255), random(255), random(255));
    pt.vertex(random(width), random(height));
    system.addShape(pt);
    pt.end();
  }
} 

public void draw () {
  background(255);

  shape(system);
    
  for (int i = 0; i < system.getChildCount(); i++) {
    PShape pt = system.getChild(i);
     pt.translate(random(-5, 5), random(-5, 5), random(-5, 5));
  }
}
