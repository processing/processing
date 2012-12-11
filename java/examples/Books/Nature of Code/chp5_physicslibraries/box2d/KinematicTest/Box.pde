// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// A rectangular box

class Box {

  // We need to keep track of a Body and a width and height
  Body body;
  float w;
  float h;
  
  boolean dragged = false;

  // Constructor
  Box(float x_, float y_) {
    float x = x_;
    float y = y_;
    w = 24;
    h = 24;
    // Add the box to the box2d world
    makeBody(new Vec2(x,y),w,h);
    body.setUserData(this);
  }

  // This function removes the particle from the box2d world
  void killBody() {
    box2d.destroyBody(body);
  }

  boolean contains(float x, float y) {
    Vec2 worldPoint = box2d.coordPixelsToWorld(x, y);
    Fixture f = body.getFixtureList();
    boolean inside = f.testPoint(worldPoint);
    return inside;
  }
  
  void setAngularVelocity(float a) {
    body.setAngularVelocity(a); 
  }
  void setVelocity(Vec2 v) {
     body.setLinearVelocity(v);
  }
  
  void setLocation(float x, float y) {
    Vec2 pos = body.getWorldCenter();
    Vec2 target = box2d.coordPixelsToWorld(x,y);
    Vec2 diff = new Vec2(target.x-pos.x,target.y-pos.y);
    diff.mulLocal(50);
    setVelocity(diff);
    setAngularVelocity(0);
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
    rotate(a);
    fill(175);
    stroke(0);
    rect(0,0,w,h);
    popMatrix();
  }


  // This function adds the rectangle to the box2d world
  void makeBody(Vec2 center, float w_, float h_) {
    // Define and create the body
    BodyDef bd = new BodyDef();
    bd.type = BodyType.KINEMATIC;
    bd.position.set(box2d.coordPixelsToWorld(center));
    bd.fixedRotation = true;
    body = box2d.createBody(bd);

    // Define a polygon (this is what we use for a rectangle)
    PolygonShape ps = new PolygonShape();
    float box2dW = box2d.scalarPixelsToWorld(w_/2);
    float box2dH = box2d.scalarPixelsToWorld(h_/2);
    ps.setAsBox(box2dW, box2dH);

    // Define a fixture
    FixtureDef fd = new FixtureDef();
    fd.shape = ps;
    // Parameters that affect physics
    fd.density = 1;
    fd.friction = 0.3;
    fd.restitution = 0.5;

    body.createFixture(fd);
  }

}



