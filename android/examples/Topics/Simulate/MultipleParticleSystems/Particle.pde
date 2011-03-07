// A simple Particle class

class Particle {
  PVector loc;
  PVector vel;
  PVector acc;
  float r;
  float timer;

  // One constructor
  Particle(PVector a, PVector v, PVector l, float r_) {
    acc = a.get();
    vel = v.get();
    loc = l.get();
    r = r_;
    timer = 100.0;
  }
  
  // Another constructor (the one we are using here)
  Particle(PVector l) {
    acc = new PVector(0,0.05,0);
    vel = new PVector(random(-1,1),random(-2,0),0);
    loc = l.get();
    r = 10.0;
    timer = 100.0;
  }


  void run() {
    update();
    render();
  }

  // Method to update location
  void update() {
    vel.add(acc);
    loc.add(vel);
    timer -= 1.0;
  }

  // Method to display
  void render() {
    ellipseMode(CENTER);
    stroke(255,timer);
    fill(100,timer);
    ellipse(loc.x,loc.y,r,r);
  }
  
  // Is the particle still useful?
  boolean dead() {
    if (timer <= 0.0) {
      return true;
    } else {
      return false;
    }
  }
}
