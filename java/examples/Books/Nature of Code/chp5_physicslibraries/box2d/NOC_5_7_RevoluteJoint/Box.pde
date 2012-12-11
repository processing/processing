// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// A rectangular box

class Box {

  // We need to keep track of a Body and a width and height
  Body body;
  float w;
  float h;

  // Constructor
  Box(float x, float y, float w_, float h_, boolean lock) {
    w = w_;
    h = h_;
    
    // Define and create the body
    BodyDef bd = new BodyDef();
    bd.position.set(box2d.coordPixelsToWorld(new Vec2(x,y)));
    if (lock) bd.type = BodyType.STATIC;
    else bd.type = BodyType.DYNAMIC;

    body = box2d.createBody(bd);

    // Define the shape -- a  (this is what we use for a rectangle)
    PolygonShape sd = new PolygonShape();
    float box2dW = box2d.scalarPixelsToWorld(w/2);
    float box2dH = box2d.scalarPixelsToWorld(h/2);
    sd.setAsBox(box2dW, box2dH);

    // Define a fixture
    FixtureDef fd = new FixtureDef();
    fd.shape = sd;
    // Parameters that affect physics
    fd.density = 1;
    fd.friction = 0.3;
    fd.restitution = 0.5;

    body.createFixture(fd);

    // Give it some initial random velocity
    body.setLinearVelocity(new Vec2(random(-5,5),random(2,5)));
    body.setAngularVelocity(random(-5,5));
  }

  // This function removes the particle from the box2d world
  void killBody() {
    box2d.destroyBody(body);
  }

  // Drawing the box
  void display() {
    // We look at each body and get its screen position
    Vec2 pos = box2d.getBodyPixelCoord(body);
    // Get its angle of rotation
    float a = body.getAngle();

    rectMode(PConstants.CENTER);
    pushMatrix();
    translate(pos.x,pos.y);
    rotate(-a);
    fill(127);
    stroke(0);
    strokeWeight(2);
    rect(0,0,w,h);
    popMatrix();
  }
}


