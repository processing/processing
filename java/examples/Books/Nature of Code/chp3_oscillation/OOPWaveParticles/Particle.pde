// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

class Particle {
  PVector location;
  
  Particle() {
    location = new PVector(); 
  }
  
  void setLocation(float x, float y) {
    location.x = x;
    location.y = y; 
  }
  
  void display() {
    fill(random(255));
    ellipse(location.x,location.y,16,16); 
  }
  
  
}
