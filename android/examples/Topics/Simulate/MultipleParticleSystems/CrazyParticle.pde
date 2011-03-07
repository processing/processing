// A subclass of Particle

class CrazyParticle extends Particle {

  // Just adding one new variable to a CrazyParticle
  // It inherits all other fields from "Particle", and we don't have to retype them!
  float theta;

  // The CrazyParticle constructor can call the parent class (super class) constructor
  CrazyParticle(PVector l) {
    // "super" means do everything from the constructor in Particle
    super(l);
    // One more line of code to deal with the new variable, theta
    theta = 0.0;

  }

  // Notice we don't have the method run() here; it is inherited from Particle

  // This update() method overrides the parent class update() method
  void update() {
    super.update();
    // Increment rotation based on horizontal velocity
    float theta_vel = (vel.x * vel.mag()) / 10.0f;
    theta += theta_vel;
  }

  // Override timer
  void timer() {
    timer -= 0.5;
  }
  
  // Method to display
  void render() {
    // Render the ellipse just like in a regular particle
    super.render();

    // Then add a rotating line
    pushMatrix();
    translate(loc.x,loc.y);
    rotate(theta);
    stroke(255,timer);
    line(0,0,25,0);
    popMatrix();
  }
}

