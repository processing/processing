// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// A circular particle

class Particle {

  // We need to keep track of a Body and a radius
  Body body;
  float r;

  Particle(float x, float y, float r_, boolean fixed) {
    r = r_;
    
    // Define a body
    BodyDef bd = new BodyDef();
    if (fixed) bd.type = BodyType.STATIC;
    else bd.type = BodyType.DYNAMIC;

    // Set its position
    bd.position = box2d.coordPixelsToWorld(x,y);
    body = box2d.world.createBody(bd);

    // Make the body's shape a circle
    // Make the body's shape a circle
    CircleShape cs = new CircleShape();
    cs.m_radius = box2d.scalarPixelsToWorld(r);
    
    FixtureDef fd = new FixtureDef();
    fd.shape = cs;
    // Parameters that affect physics
    fd.density = 1;
    fd.friction = 0.3;
    fd.restitution = 0.5;
    
    body.createFixture(fd);
  }

  // This function removes the particle from the box2d world
  void killBody() {
    box2d.destroyBody(body);
  }
  
  // Is the particle ready for deletion?
  boolean done() {
    // Let's find the screen position of the particle
    Vec2 pos = box2d.getBodyPixelCoord(body);
    // Is it off the bottom of the screen?
    if (pos.y > height+r*2) {
      killBody();
      return true;
    }
    return false;
  }

  // 
  void display() {
    // We look at each body and get its screen position
    Vec2 pos = box2d.getBodyPixelCoord(body);
    // Get its angle of rotation
    float a = body.getAngle();
    pushMatrix();
    translate(pos.x,pos.y);
    rotate(a);
    stroke(0);
    fill(127);
    strokeWeight(2);
    ellipse(0,0,r*2,r*2);
    // Let's add a line so we can see the rotation
    line(0,0,r,0);
    popMatrix();
  }


}


