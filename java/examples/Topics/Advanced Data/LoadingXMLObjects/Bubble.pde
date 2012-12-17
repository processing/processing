// A Bubble class
class Bubble {
  
  float x,y;
  float diameter;
  color c; 
  
  Bubble(float r,float g, float b, float d) {
    x = width/2;
    y = height/2;
    c = color(r, g, b, 204);
    diameter = d;
  }

  // Display Bubble
  void display() {
    noStroke();
    fill(c);
    ellipse(x, y, diameter, diameter);
  }
  
  // Bubble drifts upwards
  void drift() {
    x += random(-1, 1);
    y += random(-1, 1);
    x = constrain(x, 0, width);
    y = constrain(y, 0, height);
  }
}
