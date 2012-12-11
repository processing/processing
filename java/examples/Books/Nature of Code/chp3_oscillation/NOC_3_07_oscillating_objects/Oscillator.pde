// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

class Oscillator {   

  PVector angle;
  PVector velocity;
  PVector amplitude;

  Oscillator() {   
    angle = new PVector();
    velocity = new PVector(random(-0.05, 0.05), random(-0.05, 0.05));
    amplitude = new PVector(random(20,width/2), random(20,height/2));
  }   

  void oscillate() {
    angle.add(velocity);
  }   

  void display() {   

    float x = sin(angle.x)*amplitude.x;
    float y = sin(angle.y)*amplitude.y;

    pushMatrix();
    translate(width/2, height/2);
    stroke(0);
    strokeWeight(2);
    fill(127,127);
    line(0, 0, x, y);  
    ellipse(x, y, 32, 32);  
    popMatrix();
  }
}   


