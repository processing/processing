// An individual Particle

class Particle {

  // Velocity
  PVector center;
  PVector velocity;
  // Lifespane is tied to alpha
  float lifespan;

  // The particle PShape
  PShape part;
  // The particle size
  float partSize;

  // A single force
  PVector gravity = new PVector(0, 0.1);

  Particle() {    
    partSize = random(10, 60);
    // The particle is a textured quad
    part = createShape();
    part.beginShape(QUAD);
    part.noStroke();
    part.texture(sprite);
    part.normal(0, 0, 1);
    part.vertex(-partSize/2, -partSize/2, 0, 0);
    part.vertex(+partSize/2, -partSize/2, sprite.width, 0);
    part.vertex(+partSize/2, +partSize/2, sprite.width, sprite.height);
    part.vertex(-partSize/2, +partSize/2, 0, sprite.height);
    part.endShape();

    // Initialize center vector
    center = new PVector(); 
    
    // Set the particle starting location
    rebirth(width/2, height/2);
  }

  PShape getShape() {
    return part;
  }

  void rebirth(float x, float y) {
    float a = random(TWO_PI);
    float speed = random(0.5, 4);
    // A velocity with random angle and magnitude
    velocity = PVector.fromAngle(a);
    velocity.mult(speed);
    // Set lifespan
    lifespan = 255;
    // Set location using translate
    part.resetMatrix();
    part.translate(x, y); 
    
    // Update center vector
    center.set(x, y, 0);
  }

  // Is it off the screen, or its lifespan is over?
  boolean isDead() {
    if (center.x > width  || center.x < 0 || 
        center.y > height || center.y < 0 || lifespan < 0) {
      return true;
    } 
    else {
      return false;
    }
  }

  void update() {
    // Decrease life
    lifespan = lifespan - 1;
    // Apply gravity
    velocity.add(gravity);
    part.setTint(color(255, lifespan));
    // Move the particle according to its velocity
    part.translate(velocity.x, velocity.y);
    // and also update the center
    center.add(velocity);
  }
}
