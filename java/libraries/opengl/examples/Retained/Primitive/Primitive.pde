PShape3D circle;
  
public void setup() {
  size(400, 400, P3D);
  frameRate(90);
 
  fill(255, 0, 0);
  stroke(0, 0, 255);  
  strokeWeight(3);
  circle = (PShape3D) createShape(ELLIPSE, 0, 0, 100, 100);  
} 

public void draw () {	  
  background(0);
    
  if (5000 < millis()) {
    circle.fill(map(mouseX, 0, width, 255, 0), 0, 0);
    circle.stroke(0, 0, map(mouseY, 0, height, 255, 0));
  }
  
  
  translate(mouseX, mouseY);
  shape(circle);  
}
