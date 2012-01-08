PShape circle;
boolean filled = true;
boolean stroked = true;  
  
public void setup() {
  size(400, 400, P3D);
  frameRate(90);
 
  fill(255, 0, 0);
  stroke(0, 0, 255);  
  strokeWeight(3);
  circle = createShape(ELLIPSE, 0, 0, 100, 100);  
} 

public void draw () {	  
  background(0);
    
  if (5000 < millis()) {
    if (filled) circle.fill(map(mouseX, 0, width, 255, 0), 0, 0);
    if (stroked) circle.stroke(0, 0, map(mouseY, 0, height, 255, 0));
  }
  
  translate(mouseX, mouseY);
  shape(circle);  
}

void keyPressed() {
  if (key == 's') {
    if (stroked) {
      circle.noStroke();
      stroked = false;
    } else {
      stroked = true;    
    }  
  } else if (key == 'f') {
   if (filled) {
      circle.noFill();
      filled = false;
    } else {
      filled = true;    
    }    
  }
}
