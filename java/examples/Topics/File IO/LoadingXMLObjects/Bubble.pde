// Learning Processing
// Daniel Shiffman
// http://www.learningprocessing.com

// Example 18-9: Using Processing's XML library

// A Bubble class
class Bubble {
  
  float x,y;
  float diameter;
color c;  
  Bubble(float r,float g, float b, float d) {
    x = width/2;
    y = height/2;
    c = color(r,g,b,150);
    diameter = d;
  }

  // Display Bubble
  void display() {
    stroke(0);
    fill(c);
    ellipse(x,y,diameter,diameter);
  }
  
  // Bubble drifts upwards
  void drift() {
    x += random(-1,1);
    y += random(-1,1);
    x = constrain(x,0,width);
    y = constrain(y,0,height);
  }
}
