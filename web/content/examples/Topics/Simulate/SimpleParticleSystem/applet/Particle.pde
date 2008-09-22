// A simple Particle class

class Particle {
  Vector3D loc;
  Vector3D vel;
  Vector3D acc;
  float r;
  float timer;
  
  // Another constructor (the one we are using here)
  Particle(Vector3D l) {
    acc = new Vector3D(0,0.05,0);
    vel = new Vector3D(random(-1,1),random(-2,0),0);
    loc = l.copy();
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
    vel.display(loc.x,loc.y,10);
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

